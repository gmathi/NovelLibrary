package io.github.gmathi.novellibrary.activity

import CloudFlareByPasser
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.crashlytics.android.Crashlytics
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import io.fabric.sdk.android.Fabric
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.fragment.LibraryPagerFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import kotlinx.android.synthetic.main.app_bar_nav_drawer.*
import org.cryse.widget.persistentsearch.PersistentSearchView


class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    private var cloudFlareLoadingDialog: MaterialDialog? = null
    private var mAuth: FirebaseAuth? = null

    private val fragments: SparseArray<Fragment> = SparseArray(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer)
        mAuth = FirebaseAuth.getInstance()
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

        if (Utils.isConnectedToNetwork(this)) {
            checkForCloudFlare(savedInstanceState)
        } else {
            checkIntentForNotificationData()
            loadFragment(currentNavId, savedInstanceState)
            showWhatsNewDialog()
        }

        if (intent.hasExtra("showDownloads")) {
            intent.removeExtra("showDownloads")
            startNovelDownloadsActivity()
        }

//        drawerLayout.viewTreeObserver.addOnGlobalLayoutListener (object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                drawerLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                authSignInButton.setOnClickListener {
//                    val providers = arrayListOf(
//                            AuthUI.IdpConfig.EmailBuilder().build(),
//                            AuthUI.IdpConfig.PhoneBuilder().build(),
//                            AuthUI.IdpConfig.GoogleBuilder().build())
//
//                    // Create and launch sign-in intent
//                    startActivityForResult(
//                            AuthUI.getInstance()
//                                    .createSignInIntentBuilder()
//                                    .setAvailableProviders(providers)
//                                    .build(),
//                            Constants.OPEN_FIREBASE_AUTH_UI)
//                }
//            }
//        })

    }

    private fun showWhatsNewDialog() {
        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
            MaterialDialog.Builder(this)
                    .title("\uD83C\uDF89 Merry Christmas!!")
                    .content(//"** Fixed Cloud Flare for 6.0.1**\n\n" +
                            //"✨ Make life easier fixes\n" +
//                                    "✨ Updated search results to load. (NU Updated their website)\n" +
//                                    "\uD83D\uDEE0 Bug Fixes for crashes in Downloads\n" +
//                                    "\uD83D\uDEE0 Bug Fixes for Recommendations not showing\n" +
                                    "⚠️ Fix to show sources for the novel chapters.\n" +
//                                    "✨ Added Hidden Buttons to unlock some hidden functionality!" +
//                            "\uD83D\uDEE️ Bug Fixes for reported & unreported crashes!" +
                                    "")
                    .positiveText("Ok")
                    .onPositive { dialog, _ -> dialog.dismiss() }
                    .show()
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }
    }

    private fun checkForCloudFlare(savedInstanceState: Bundle? = null) {

        cloudFlareLoadingDialog = Utils
                .dialogBuilder(this@NavDrawerActivity, content = getString(R.string.cloud_flare_bypass_description), isProgress = true)
                .cancelable(false)
                .negativeText("Skip")
                .onNegative { _, _ ->
                    Crashlytics.log(getString(R.string.cloud_flare_bypass_failure_title))
                    loadFragment(currentNavId, savedInstanceState)
                    showWhatsNewDialog()
                    checkIntentForNotificationData()
                }
                .build()

        cloudFlareLoadingDialog?.show()

        CloudFlareByPasser.check(this, "novelupdates.com") { state ->

            val isActivityRunning = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) !isDestroyed else !isFinishing
            if (isActivityRunning) {
                if (state == CloudFlareByPasser.State.CREATED || state == CloudFlareByPasser.State.UNNEEDED) {
                    if (cloudFlareLoadingDialog?.isShowing == true) {
                        Crashlytics.log(getString(R.string.cloud_flare_bypass_success))
                        loadFragment(currentNavId, savedInstanceState)
                        showWhatsNewDialog()
                        checkIntentForNotificationData()
                        cloudFlareLoadingDialog?.dismiss()
                    }
                }
            }
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

    private fun loadFragment(id: Int, savedInstanceState: Bundle? = null) {
        currentNavId = id
        when (id) {
            R.id.nav_library -> {
                val fragment =
                        when {
                            fragments[id] != null -> fragments[id]
                            savedInstanceState == null -> LibraryPagerFragment()
                            else -> restoreFragment(id, savedInstanceState) ?: LibraryPagerFragment()
                        }
                replaceFragment(fragment, LibraryPagerFragment::class.toString(), id)
            }
            R.id.nav_search -> {
                val fragment =
                        when {
                            fragments[id] != null -> fragments[id]
                            savedInstanceState == null -> SearchFragment()
                            else -> restoreFragment(id, savedInstanceState) ?: SearchFragment()
                        }
                replaceFragment(fragment, SearchFragment::class.toString(), id)
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

    private fun replaceFragment(fragment: Fragment, tag: String, id: Int) {
        fragments.put(id, fragment)
        supportFragmentManager.beginTransaction()
                .replace(R.id.navFragmentContainer, fragment, tag)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(tag)
                .commitAllowingStateLoss()
    }

    private fun restoreFragment(id: Int, savedInstanceState: Bundle): Fragment? {
        val fragment =  supportFragmentManager.getFragment(savedInstanceState, id.toString())
        if (fragment != null)
            fragments.put(id, fragment)
        return fragment
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
            val novel = intent.extras!!.getParcelable<Novel>("novel")
            novel?.let {
                intent.extras!!.remove("novel")
                startChaptersActivity(novel)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentNavId", currentNavId)
        val fragment = fragments[currentNavId]
        if (fragment != null)
            supportFragmentManager.putFragment(outState, currentNavId.toString(), fragment)
    }

    override fun onDestroy() {
        async.cancelAll()
        super.onDestroy()
    }

}
