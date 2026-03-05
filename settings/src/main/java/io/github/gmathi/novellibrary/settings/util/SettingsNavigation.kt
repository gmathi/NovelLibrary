package io.github.gmathi.novellibrary.settings.util

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Common navigation helpers for settings screens.
 * 
 * Provides reusable navigation patterns to ensure consistent navigation
 * behavior across all settings screens.
 */
object SettingsNavigation {
    
    /**
     * Navigates to a destination with standard animation and back stack handling.
     * 
     * @param navController Navigation controller
     * @param route Destination route
     * @param singleTop If true, avoids multiple copies of the same destination
     */
    fun navigateTo(
        navController: NavController,
        route: String,
        singleTop: Boolean = true
    ) {
        navController.navigate(route) {
            launchSingleTop = singleTop
        }
    }
    
    /**
     * Navigates back to the previous screen.
     * 
     * @param navController Navigation controller
     * @return True if navigation was successful, false if already at root
     */
    fun navigateBack(navController: NavController): Boolean {
        return navController.popBackStack()
    }
    
    /**
     * Navigates back to a specific destination in the back stack.
     * 
     * @param navController Navigation controller
     * @param route Destination route to navigate back to
     * @param inclusive If true, also pops the destination route
     * @return True if navigation was successful
     */
    fun navigateBackTo(
        navController: NavController,
        route: String,
        inclusive: Boolean = false
    ): Boolean {
        return navController.popBackStack(route, inclusive)
    }
    
    /**
     * Navigates to a destination and clears the back stack.
     * 
     * Useful for navigating to a "home" screen where back navigation
     * should exit the settings entirely.
     * 
     * @param navController Navigation controller
     * @param route Destination route
     */
    fun navigateAndClearBackStack(
        navController: NavController,
        route: String
    ) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
    
    /**
     * Navigates to a destination, replacing the current screen.
     * 
     * The current screen is removed from the back stack, so pressing back
     * will skip it.
     * 
     * @param navController Navigation controller
     * @param route Destination route
     */
    fun navigateAndReplace(
        navController: NavController,
        route: String
    ) {
        navController.navigate(route) {
            popUpTo(navController.currentDestination?.id ?: return@navigate) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
    
    /**
     * Checks if the navigation controller can navigate back.
     * 
     * @param navController Navigation controller
     * @return True if there are destinations in the back stack
     */
    fun canNavigateBack(navController: NavController): Boolean {
        return navController.previousBackStackEntry != null
    }
    
    /**
     * Gets the current route from the navigation controller.
     * 
     * @param navController Navigation controller
     * @return Current route or null if not available
     */
    fun getCurrentRoute(navController: NavController): String? {
        return navController.currentBackStackEntry?.destination?.route
    }
    
    /**
     * Standard navigation options builder for settings screens.
     * 
     * Provides consistent navigation behavior:
     * - Single top: Avoids duplicate destinations
     * - Restore state: Preserves screen state when navigating back
     * - Save state: Saves screen state when navigating away
     * 
     * @return NavOptionsBuilder configured with standard options
     */
    fun standardNavOptions(): NavOptionsBuilder.() -> Unit = {
        launchSingleTop = true
        restoreState = true
    }
    
    /**
     * Navigation options for replacing the current screen.
     * 
     * @param currentDestinationId ID of the current destination to pop
     * @return NavOptionsBuilder configured for replacement
     */
    fun replaceNavOptions(currentDestinationId: Int): NavOptionsBuilder.() -> Unit = {
        popUpTo(currentDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
