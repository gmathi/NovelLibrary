package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
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
import io.github.gmathi.novellibrary.NovelLibraryApplication
import io.github.gmathi.novellibrary.databinding.ActivityNavDrawerBinding
import io.github.gmathi.novellibrary.fragment.LibraryPagerFragment
import io.github.gmathi.novellibrary.compose.search.SearchFragmentCompose
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.changelog.WhatsChanged
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startExtensionsPagerActivity
import io.github.gmathi.novellibrary.util.system.startNovelDownloadsActivity
import io.github.gmathi.novellibrary.util.system.startRecentNovelsPagerActivity
import io.github.gmathi.novellibrary.util.system.startSettingsActivity
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.util.system.showAlertDialog
import io.github.gmathi.novellibrary.network.AppUpdateChecker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.random.Random


class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var snackBar: Snackbar? = null
    private var currentNavId: Int = R.id.nav_search

    /** The top-level destination shown at launch. Back from here confirms exit. */
    private var startNavId: Int = R.id.nav_search

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

        // Remember the launch destination so back navigation knows when we've
        // returned to the "home" screen and should confirm exit instead.
        startNavId = if (savedInstanceState != null && savedInstanceState.containsKey("startNavId")) {
            savedInstanceState.getInt("startNavId")
        } else {
            currentNavId
        }

        snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)

        checkIntentForNotificationData()
        // Only load the initial fragment on a fresh start. On recreation the
        // FragmentManager restores the existing fragment, so re-adding it here
        // would stack a duplicate and could leave the container empty on back.
        if (savedInstanceState == null) {
            loadFragment(currentNavId)
        }
        showWhatsNewDialog()
        checkForAppUpdate()
        maybePromptDatabaseRecovery()

        if (intent.hasExtra("showDownloads")) {
            intent.removeExtra("showDownloads")
            startNovelDownloadsActivity()
        }

        newIconsImageView = binding.navigationView.getHeaderView(0).findViewWithTag<ImageView>("icon")
        newIconsImageView.setOnClickListener { setNewImageInNavigationHeaderView() }

        onBackPress()
    }

    private fun showWhatsNewDialog() {
        if (dataCenter.appVersionCode < BuildConfig.VERSION_CODE) {
            MaterialDialog(this).show {
                title(text = "\uD83C\uDF89 What's New ${BuildConfig.VERSION_NAME}!")
                message(text = WhatsChanged.LATEST)
                positiveButton(text = "Ok")
            }
            dataCenter.appVersionCode = BuildConfig.VERSION_CODE
        }
    }

    /**
     * If startup cleanup flagged the database as corrupt, offer the user an explicit choice to
     * reset it. Recovery wipes the entire local library, so it is never done automatically.
     */
    private fun maybePromptDatabaseRecovery() {
        if (!dataCenter.databaseCorruptionDetected) return

        MaterialDialog(this).show {
            cancelable(false)
            title(text = "Database problem detected")
            message(
                text = "Your library database appears to be corrupted. You can reset it to fix " +
                    "the issue, but this will permanently delete all novels and reading data " +
                    "stored on this device. Restore from a backup afterwards if you have one.\n\n" +
                    "Reset the database now?"
            )
            positiveButton(text = "Reset database") {
                val recovered = NovelLibraryApplication.recoverFromCorruptDatabase(applicationContext)
                dataCenter.databaseCorruptionDetected = false
                if (recovered) {
                    toast("Database reset. Restarting...")
                    restartApp()
                } else {
                    showAlertDialog(
                        title = "Reset failed",
                        message = "The database could not be reset. Please try clearing the app's " +
                            "data from system settings."
                    )
                }
            }
            negativeButton(text = "Not now") {
                // Leave the flag set so the user is prompted again next launch.
                it.dismiss()
            }
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
        Runtime.getRuntime().exit(0)
    }

    private fun checkForAppUpdate() {
        lifecycleScope.launch {
            AppUpdateChecker(applicationContext).checkAndPromptUpdate()
        }
    }

    private fun onBackPress() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. An open drawer always closes first.
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    return
                }

                // NOTE: The Compose search screen owns its own back handling for
                // editing/searching sub-states via a BackHandler that is only
                // enabled in those states. When it's at its base browse state the
                // BackHandler is disabled and back falls through to here.

                // 2. Don't navigate away mid-sync.
                (supportFragmentManager.findFragmentByTag(LibraryPagerFragment::class.toString()) as? LibraryPagerFragment)?.let {
                    if (it.getLibraryFragment()?.isSyncing() == true) {
                        return
                    }
                }

                // 3. If we're on a non-home top-level destination, go back to home.
                if (currentNavId != startNavId) {
                    loadFragment(startNavId)
                    return
                }

                // 4. We're on the home fragment - confirm exit with a snackbar,
                //    finishing only if it's already showing.
                if (snackBar != null && snackBar!!.isShown) {
                    finish()
                } else {
                    if (snackBar == null)
                        snackBar = Snackbar.make(binding.appBarNavDrawer.navFragmentContainer, getString(R.string.app_exit), Snackbar.LENGTH_SHORT)
                    snackBar?.show()
                }
            }
        })
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
        when (id) {
            R.id.nav_library -> {
                currentNavId = id
                replaceFragment(LibraryPagerFragment(), LibraryPagerFragment::class.toString())
            }

            R.id.nav_search -> {
                currentNavId = id
                replaceFragment(SearchFragmentCompose(), SearchFragmentCompose::class.toString())
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
        // Top-level drawer destinations are swapped without a back stack. Back
        // navigation is handled centrally in onBackPress(): non-home destinations
        // return to the home destination, and home confirms exit. This avoids the
        // empty-container black screen caused by popping the start destination.
        supportFragmentManager.commit {
            replace(binding.appBarNavDrawer.navFragmentContainer.id, fragment, tag)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
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
        outState.putInt("startNavId", startNavId)
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
