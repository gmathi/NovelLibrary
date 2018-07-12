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
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.fragment.DownloadFragment
import io.github.gmathi.novellibrary.fragment.LibraryPagerFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.CloudFlare
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import kotlinx.android.synthetic.main.app_bar_nav_drawer.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import io.fabric.sdk.android.Fabric




class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    private var cloudFlareLoadingDialog: MaterialDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer)
        navigationView.setNavigationItemSelectedListener(this)

        //Initialize custom logging
        Fabric.with(this, Crashlytics())

        try {
            currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search
        } catch (e: Exception) {
            Crashlytics.logException(e)
            MaterialDialog.Builder(this@NavDrawerActivity)
                    .content("Error initiating the app. The developer has been notified about this!")
                    .positiveText("Quit")
                    .cancelable(false)
                    .onPositive { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                    .show()
        }

        if (intent.hasExtra("currentNavId"))
            currentNavId = intent.getIntExtra("currentNavId", currentNavId)

        if (savedInstanceState != null && savedInstanceState.containsKey("currentNavId")) {
            currentNavId = savedInstanceState.getInt("currentNavId")
        }

        snackBar = Snackbar.make(navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)

        if (dataCenter.enableCloudFlare && Utils.isConnectedToNetwork(this)) {
            checkForCloudFlare()
        } else {
            checkIntentForNotificationData()
            loadFragment(currentNavId)
        }

        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
            MaterialDialog.Builder(this)
                    .title("📢 What's New!")
                    .content("\uD83D\uDEE0 Fixed chapters not showing up from WLN & other sources!\n" +
//                            "\uD83D\uDEE0 Fixed Novel Notifications showing 0 Chapters\n" +
//                            //"✨ Improved performance/decrease load time on the chapters screen\n" +
//                            "\uD83D\uDEE0 Improved performance/decrease load time on the chapters screen\n" +
//                            "⚠️ Downloads will be fixed in next update (as always I guess :( )\n" +
                            "\uD83D\uDEE0️ Bug Fixes for reported & unreported crashes!")
                    .positiveText("Ok")
                    .onPositive { dialog, _ -> dialog.dismiss() }
                    .show()
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }

    }

    private fun checkForCloudFlare() {

        cloudFlareLoadingDialog = Utils.dialogBuilder(this@NavDrawerActivity, content = getString(R.string.cloud_flare_bypass_description), isProgress = true).cancelable(false).build()

        val listener = object : CloudFlare.Companion.Listener {
            override fun onSuccess() {
                Handler(Looper.getMainLooper()).post {
                    Crashlytics.log(getString(R.string.cloud_flare_bypass_success))
                    loadFragment(currentNavId)
                    checkIntentForNotificationData()
                    cloudFlareLoadingDialog?.dismiss()
                }
            }

            override fun onFailure() {
                Handler(Looper.getMainLooper()).post {
                    cloudFlareLoadingDialog?.hide()
                    MaterialDialog.Builder(this@NavDrawerActivity)
                            .content(getString(R.string.cloud_flare_bypass_success))
                            .positiveText(getString(R.string.try_again))
                            .onPositive { dialog, _ ->
                                dialog.dismiss()
                                checkForCloudFlare()
                            }
                            .show()
                }
            }
        }

        cloudFlareLoadingDialog?.show()

        async {
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
                replaceFragment(LibraryPagerFragment(), LibraryPagerFragment::class.toString())
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
        when {
            resultCode == Constants.OPEN_DOWNLOADS_RES_CODE -> loadFragment(R.id.nav_downloads)
            requestCode == Constants.IWV_ACT_REQ_CODE -> checkIntentForNotificationData()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkIntentForNotificationData() {
        if (intent.extras != null && intent.extras.containsKey("novel")) {
            val novel = intent.extras.getSerializable("novel") as? Novel
            novel?.let {
                intent.extras.remove("novel")
                startChaptersActivity(novel)
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
