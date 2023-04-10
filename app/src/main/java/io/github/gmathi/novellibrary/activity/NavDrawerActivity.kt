package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.ActivityNavDrawerBinding
import io.github.gmathi.novellibrary.databinding.NavHeaderNavDrawerBinding
import io.github.gmathi.novellibrary.fragment.LibraryPagerFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.WhatsChanged
import io.github.gmathi.novellibrary.util.system.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import java.util.*
import kotlin.random.Random


class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    private var mAuth: FirebaseAuth? = null

    lateinit var binding: ActivityNavDrawerBinding
    lateinit var newIconsImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNavDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        binding.navigationView.setNavigationItemSelectedListener(this)

        //Initialize custom logging
        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("currentNavId"))
            currentNavId = intent.getIntExtra("currentNavId", currentNavId)

        if (savedInstanceState != null && savedInstanceState.containsKey("currentNavId")) {
            currentNavId = savedInstanceState.getInt("currentNavId")
        }

        snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)

        checkIntentForNotificationData()
        loadFragment(currentNavId)
        showWhatsNewDialog()
        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("showDownloads")) {
            intent.removeExtra("showDownloads")
            startNovelDownloadsActivity()
        }

        newIconsImageView = binding.navigationView.getHeaderView(0).findViewWithTag<ImageView>("icon")
        newIconsImageView.setOnClickListener { setNewImageInNavigationHeaderView() }
    }

    private fun showWhatsNewDialog() {
        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
            MaterialDialog(this).show {
                title(text = "\uD83C\uDF89 What's New ${BuildConfig.VERSION_NAME}!")
                message(
                    text = WhatsChanged.VERSION_23
                )
                positiveButton(text = "Ok")
            }
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val existingSearchFrag = supportFragmentManager.findFragmentByTag(SearchFragment::class.toString())
            if (existingSearchFrag != null) {
                val searchView = existingSearchFrag.view?.findViewById<PersistentSearchView>(R.id.searchView)
                if (searchView != null && (searchView.isEditing || searchView.isSearching)) {
                    (existingSearchFrag as SearchFragment).closeSearch()
                    return
                }
            }

            (supportFragmentManager.findFragmentByTag(LibraryPagerFragment::class.toString()) as? LibraryPagerFragment)?.let {
                if (it.getLibraryFragment()?.isSyncing() == true) {
                    return
                }
            }

            if (snackBar != null && snackBar!!.isShown)
                finish()
            else {
                if (snackBar == null)
                    snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)
                snackBar?.show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
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
            R.id.nav_recent_novels -> {
                startRecentNovelsPagerActivity()
            }
            R.id.nav_extensions -> {
                startExtensionsPagerActivity()
            }

            R.id.nav_discord_link -> {
                openInBrowser("https://discord.gg/cPMxEVn")
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.commit(true) {
            replace(binding.appBarNavDrawer.navFragmentContainer.id, fragment, tag)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            addToBackStack(tag)
        }
    }

    fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu_white_vector)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            requestCode == Constants.OPEN_FIREBASE_AUTH_UI -> {
                val response = IdpResponse.fromResultIntent(data)
                if (resultCode == Activity.RESULT_OK) {
                    // Successfully signed in
                    val user = FirebaseAuth.getInstance().currentUser
                    Logs.error("NAV USER", user?.displayName)
                    // ...
                } else {
                    // Sign in failed. If response is null the user canceled the
                    // sign-in flow using the back button. Otherwise check
                    // response.getError().getErrorCode() and handle the error.
                    // ...
                }
            }
            resultCode == Constants.OPEN_DOWNLOADS_RES_CODE -> loadFragment(R.id.nav_downloads)
            requestCode == Constants.IWV_ACT_REQ_CODE -> checkIntentForNotificationData()
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkIntentForNotificationData() {
        if (intent.extras != null && intent.extras!!.containsKey("novel")) {
            val novel = intent.extras!!.getSerializable("novel") as? Novel
            novel?.let {
                intent.extras!!.remove("novel")
                startChaptersActivity(novel)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentNavId", currentNavId)
    }

    override fun onResume() {
        setNewImageInNavigationHeaderView()
        super.onResume()
    }

    private fun setNewImageInNavigationHeaderView() {
        val randomNumber = Random(Date().time).nextInt(12) + 1 //since we have only 12 images to rotate from.
        val uri = Uri.parse("file:///android_asset/album_arts/$randomNumber.png")
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(newIconsImageView)
    }


}
