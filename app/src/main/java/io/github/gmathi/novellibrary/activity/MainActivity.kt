package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.ActivityMainBinding
import io.github.gmathi.novellibrary.model.UiState
import io.github.gmathi.novellibrary.util.navigation.BackStackManager
import io.github.gmathi.novellibrary.util.navigation.DeepLinkHandler
import io.github.gmathi.novellibrary.util.navigation.NavigationManager
import io.github.gmathi.novellibrary.viewmodel.MainViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // ViewModel injection using architecture guide pattern
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var navigationManager: NavigationManager

    @Inject
    lateinit var deepLinkHandler: DeepLinkHandler

    @Inject
    lateinit var backStackManager: BackStackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupDrawerNavigation()
        observeViewModel()
        handleDeepLinks()
        setupBackPressHandling()
    }

    /**
     * Observe ViewModel state changes following architecture guide patterns
     */
    private fun observeViewModel() {
        mainViewModel.uiState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    // Handle loading state if needed
                }
                is UiState.Success -> {
                    // Handle successful state with main UI state data
                    handleMainUiState(state.data)
                }
                is UiState.Error -> {
                    // Handle error state
                    handleError(state.message)
                }
            }
        }

        mainViewModel.isDrawerOpen.observe(this) { isOpen ->
            // Sync drawer state with ViewModel
            if (isOpen != binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                if (isOpen) {
                    binding.drawerLayout.openDrawer(GravityCompat.START)
                } else {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        }

        mainViewModel.currentDestination.observe(this) { destinationId ->
            // Handle destination changes if needed
        }
    }

    /**
     * Handle main UI state changes
     */
    private fun handleMainUiState(uiState: io.github.gmathi.novellibrary.viewmodel.MainUiState) {
        // Apply theme changes, language changes, etc.
        // This can be expanded based on requirements
    }

    /**
     * Handle error states
     */
    private fun handleError(message: String) {
        // Show error message to user
        // This can be implemented with Snackbar or Toast
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Define top-level destinations for the drawer
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.libraryFragment,
                R.id.searchFragment,
                R.id.extensionsFragment,
                R.id.downloadsFragment,
                R.id.mainSettingsFragment
            ),
            binding.drawerLayout
        )

        // Setup toolbar with navigation
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setup destination change listener for back stack management
        navController.addOnDestinationChangedListener { _, destination, _ ->
            backStackManager.onDestinationChanged(destination)
            mainViewModel.setCurrentDestination(destination.id)
        }
    }

    private fun setupDrawerNavigation() {
        binding.navigationView.setupWithNavController(navController)
        binding.navigationView.setNavigationItemSelectedListener(this)
    }

    private fun handleDeepLinks() {
        intent?.let { intent ->
            deepLinkHandler.handleDeepLink(intent)?.let { deepLinkRequest ->
                navController.navigate(deepLinkRequest)
            }
        }
    }

    private fun setupBackPressHandling() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        mainViewModel.setDrawerOpen(false)
                    }
                    backStackManager.canHandleBack(navController) -> {
                        if (!backStackManager.handleBackNavigation(navController)) {
                            // If back stack manager can't handle it, finish the activity
                            finish()
                        }
                    }
                    else -> {
                        // If we can't handle back navigation, finish the activity
                        finish()
                    }
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val handled = when (item.itemId) {
            R.id.nav_library -> {
                navigationManager.navigateToLibrary(navController)
                mainViewModel.setCurrentDestination(R.id.libraryFragment)
                true
            }
            R.id.nav_search -> {
                navigationManager.navigateToSearch(navController)
                mainViewModel.setCurrentDestination(R.id.searchFragment)
                true
            }
            R.id.nav_extensions -> {
                navigationManager.navigateToExtensions(navController)
                mainViewModel.setCurrentDestination(R.id.extensionsFragment)
                true
            }
            R.id.nav_downloads -> {
                navigationManager.navigateToDownloads(navController)
                mainViewModel.setCurrentDestination(R.id.downloadsFragment)
                true
            }
            R.id.nav_settings -> {
                navigationManager.navigateToSettings(navController)
                mainViewModel.setCurrentDestination(R.id.mainSettingsFragment)
                true
            }
            else -> false
        }

        if (handled) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            mainViewModel.setDrawerOpen(false)
        }

        return handled
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deepLinkHandler.handleDeepLink(intent)?.let { deepLinkRequest ->
            navController.navigate(deepLinkRequest)
        }
    }
}