package io.github.gmathi.novellibrary.util.navigation

import androidx.navigation.NavController
import androidx.navigation.NavDestination
import dagger.hilt.android.scopes.ActivityScoped
import io.github.gmathi.novellibrary.R
import javax.inject.Inject

/**
 * BackStackManager handles complex back stack management scenarios for the single activity architecture.
 * 
 * This utility provides:
 * - Smart back navigation handling
 * - Back stack state tracking
 * - Custom back stack manipulation
 * - Deep link back stack restoration
 */
@ActivityScoped
class BackStackManager @Inject constructor() {

    private var backStackHistory = mutableListOf<Int>()
    private var isTrackingEnabled = true

    /**
     * Track navigation destination changes
     */
    fun onDestinationChanged(destination: NavDestination) {
        if (isTrackingEnabled) {
            destination.id.let { destinationId ->
                // Remove if already exists to avoid duplicates
                backStackHistory.remove(destinationId)
                // Add to end of history
                backStackHistory.add(destinationId)
                
                // Limit history size to prevent memory issues
                if (backStackHistory.size > MAX_HISTORY_SIZE) {
                    backStackHistory.removeAt(0)
                }
            }
        }
    }

    /**
     * Handle back navigation with custom logic
     */
    fun handleBackNavigation(navController: NavController): Boolean {
        return when {
            // If we're at a top-level destination, don't handle back
            isTopLevelDestination(navController.getCurrentDestinationId()) -> false
            
            // Try standard back navigation first
            navController.popBackStackSafely() -> true
            
            // If standard back navigation fails, try custom logic
            else -> handleCustomBackNavigation(navController)
        }
    }

    /**
     * Handle custom back navigation scenarios
     */
    private fun handleCustomBackNavigation(navController: NavController): Boolean {
        val currentDestination = navController.getCurrentDestinationId()
        
        return when (currentDestination) {
            R.id.readerFragment -> {
                // From reader, go back to chapters or novel details
                navigateBackFromReader(navController)
            }
            
            R.id.chaptersFragment -> {
                // From chapters, go back to novel details
                navigateBackFromChapters(navController)
            }
            
            R.id.novelDetailsFragment -> {
                // From novel details, go back to library or search
                navigateBackFromNovelDetails(navController)
            }
            
            else -> {
                // For other destinations, try to go to library as fallback
                navigateToLibraryAsFallback(navController)
            }
        }
    }

    /**
     * Navigate back from reader fragment
     */
    private fun navigateBackFromReader(navController: NavController): Boolean {
        return when {
            // Check if chapters is in back stack
            backStackHistory.contains(R.id.chaptersFragment) -> {
                navController.popBackStackSafely(R.id.chaptersFragment, false)
            }
            
            // Check if novel details is in back stack
            backStackHistory.contains(R.id.novelDetailsFragment) -> {
                navController.popBackStackSafely(R.id.novelDetailsFragment, false)
            }
            
            // Fallback to library
            else -> {
                navController.navigateToTopLevel(R.id.libraryFragment)
                true
            }
        }
    }

    /**
     * Navigate back from chapters fragment
     */
    private fun navigateBackFromChapters(navController: NavController): Boolean {
        return when {
            // Check if novel details is in back stack
            backStackHistory.contains(R.id.novelDetailsFragment) -> {
                navController.popBackStackSafely(R.id.novelDetailsFragment, false)
            }
            
            // Fallback to library
            else -> {
                navController.navigateToTopLevel(R.id.libraryFragment)
                true
            }
        }
    }

    /**
     * Navigate back from novel details fragment
     */
    private fun navigateBackFromNovelDetails(navController: NavController): Boolean {
        return when {
            // Check if library is in back stack
            backStackHistory.contains(R.id.libraryFragment) -> {
                navController.popBackStackSafely(R.id.libraryFragment, false)
            }
            
            // Check if search is in back stack
            backStackHistory.contains(R.id.searchFragment) -> {
                navController.popBackStackSafely(R.id.searchFragment, false)
            }
            
            // Fallback to library
            else -> {
                navController.navigateToTopLevel(R.id.libraryFragment)
                true
            }
        }
    }

    /**
     * Navigate to library as fallback
     */
    private fun navigateToLibraryAsFallback(navController: NavController): Boolean {
        navController.navigateToTopLevel(R.id.libraryFragment)
        return true
    }

    /**
     * Check if destination is a top-level destination
     */
    private fun isTopLevelDestination(destinationId: Int?): Boolean {
        return destinationId in TOP_LEVEL_DESTINATIONS
    }

    /**
     * Clear back stack history
     */
    fun clearHistory() {
        backStackHistory.clear()
    }

    /**
     * Get current back stack history
     */
    fun getHistory(): List<Int> {
        return backStackHistory.toList()
    }

    /**
     * Enable or disable back stack tracking
     */
    fun setTrackingEnabled(enabled: Boolean) {
        isTrackingEnabled = enabled
    }

    /**
     * Restore back stack from deep link
     */
    fun restoreBackStackFromDeepLink(navController: NavController, targetDestination: Int, novelId: Long? = null) {
        clearHistory()
        
        when (targetDestination) {
            R.id.novelDetailsFragment -> {
                // Add library to back stack for novel details
                backStackHistory.add(R.id.libraryFragment)
                backStackHistory.add(R.id.novelDetailsFragment)
            }
            
            R.id.chaptersFragment -> {
                // Add library -> novel details -> chapters to back stack
                backStackHistory.add(R.id.libraryFragment)
                novelId?.let { backStackHistory.add(R.id.novelDetailsFragment) }
                backStackHistory.add(R.id.chaptersFragment)
            }
            
            R.id.readerFragment -> {
                // Add full navigation path to back stack
                backStackHistory.add(R.id.libraryFragment)
                novelId?.let { 
                    backStackHistory.add(R.id.novelDetailsFragment)
                    backStackHistory.add(R.id.chaptersFragment)
                }
                backStackHistory.add(R.id.readerFragment)
            }
        }
    }

    /**
     * Check if we can handle back navigation
     */
    fun canHandleBack(navController: NavController): Boolean {
        val currentDestination = navController.getCurrentDestinationId()
        return !isTopLevelDestination(currentDestination) || backStackHistory.size > 1
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 20
        
        private val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.libraryFragment,
            R.id.searchFragment,
            R.id.extensionsFragment,
            R.id.downloadsFragment,
            R.id.mainSettingsFragment
        )
    }
}