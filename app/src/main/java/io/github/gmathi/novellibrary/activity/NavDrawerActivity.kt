package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.view.GravityCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.widget.Toast
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.updateNewChapterCount
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.DownloadFragment
import io.github.gmathi.novellibrary.fragment.LibraryFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.CloudFlare
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import kotlinx.android.synthetic.main.app_bar_nav_drawer.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer)
        navigationView.setNavigationItemSelectedListener(this)

        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("currentNavId"))
            currentNavId = intent.getIntExtra("currentNavId", currentNavId)

        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt("currentNavId")
        }

        if (intent.extras != null) {
            if (intent.extras.containsKey("novelsChapMap")) {
                @Suppress("UNCHECKED_CAST")
                val novelsMap = intent.extras.getSerializable("novelsChapMap") as? HashMap<Novel, Int>
                novelsMap?.keys?.forEach {
                    dbHelper.updateNewChapterCount(it.id, novelsMap[it]!!.toLong())
                }
            }
        }

        snackBar = Snackbar.make(navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)


        if (Utils.checkNetwork(this))
            checkForCloudFlare()
        else
            loadFragment(currentNavId)
        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
//            @Suppress("DEPRECATION")
//            MaterialDialog.Builder(this)
//                .title("📢 Announcement!")
//                .content(Html.fromHtml("<b><u>New Features:</u></b> " +
//                    "<br/><b>Experimental: Cluster Pages</b> - All links in one page. Can be toggled from Reader Settings. Don't use it, if you are not sure." +
//                    "<br/><b>Fixed (I think so...): Google Cloud Backup/Restore</b> - Backup and Restore from google cloud " +
//                    "<br/>For the glory of the app!! Ahooo Ahooo!!! 🎊🎉"))
//                .positiveText("Yay")
//                .onPositive { dialog, _ -> dialog.dismiss() }
//                .show()
//            try {
            //dataCenter.resetVerifiedHosts()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }
//

    }

    private fun checkForCloudFlare() {
        async {

            val loadingDialog = Utils.dialogBuilder(this@NavDrawerActivity, content = "Please wait while cloud flare cookies is bypassed", isProgress = true).cancelable(false).build()

            val listener = object : CloudFlare.Companion.Listener {
                override fun onSuccess() {
                    loadFragment(currentNavId)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@NavDrawerActivity, "Cloud Flare Bypassed", Toast.LENGTH_SHORT).show()
                        loadingDialog.dismiss()
                    }
                }

                override fun onFailure() {
                    Handler(Looper.getMainLooper()).post {
                        loadingDialog.hide()
                        MaterialDialog.Builder(this@NavDrawerActivity)
                            .content("Cloud Flare ByPass Failed")
                            .positiveText("Try Again")
                            .onPositive { dialog, _ -> dialog.dismiss(); checkForCloudFlare() }
                            .show()
                    }
                }
            }

            loadingDialog.show()
            await { CloudFlare(this@NavDrawerActivity, listener).check() }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val existingSearchFrag = supportFragmentManager.findFragmentByTag(SearchFragment::class.toString())
            if (existingSearchFrag != null) {
                val searchView = existingSearchFrag.view?.findViewById<PersistentSearchView>(R.id.searchView)
                if (searchView != null && (searchView.isEditing || searchView.isSearching)) {
                    (existingSearchFrag as SearchFragment).closeSearch()
                    return
                }
            }
            val existingDownloadFrag = supportFragmentManager.findFragmentByTag(DownloadFragment::class.toString())
            if (existingDownloadFrag != null) {
                loadFragment(R.id.nav_library)
                return
            }

            if (snackBar != null && snackBar!!.isShown)
                finish()
            else {
                if (snackBar == null) snackBar = Snackbar.make(navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)
                snackBar?.show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun loadFragment(id: Int) {
        currentNavId = id
        when (id) {
            R.id.nav_library -> {
                replaceFragment(LibraryFragment(), LibraryFragment::class.toString())
            }
            R.id.nav_search -> {
                replaceFragment(SearchFragment(), SearchFragment::class.toString())
            }
            R.id.nav_downloads -> {
                startNovelDownloadsActivity()
                //replaceFragment(DownloadFragment(), DownloadFragment::class.toString())
            }
            R.id.nav_settings -> {
                startSettingsActivity()
            }
            R.id.nav_recently_viewed -> {
                startRecentlyViewedNovelsActivity()
            }
            R.id.nav_recently_updated -> {
                startRecentlyUpdatedNovelsActivity()
            }
            R.id.nav_discord_link -> {
                openInBrowser("https://discord.gg/g2cQswh")
            }
            R.id.nav_refresh_cloud_flare_cookies -> {
                //startInitialWebViewActivity()
                if (Utils.checkNetwork(this))
                    checkForCloudFlare()
                else
                    Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navFragmentContainer, fragment, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_vector)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.OPEN_DOWNLOADS_RES_CODE) {
            loadFragment(R.id.nav_downloads)
        }
        if (requestCode == Constants.IWV_ACT_REQ_CODE) {
            if (intent.extras != null && intent.extras.containsKey("novel")) {
                val novel = intent.extras.getSerializable("novel") as? Novel
                novel?.let {
                    intent.extras.remove("novel")
                    startChaptersActivity(novel)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentNavId", currentNavId)
    }

    override fun onDestroy() {
        async.cancelAll()
        super.onDestroy()
    }


}
