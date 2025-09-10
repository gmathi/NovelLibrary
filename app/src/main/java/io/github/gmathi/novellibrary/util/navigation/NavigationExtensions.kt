package io.github.gmathi.novellibrary.util.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import io.github.gmathi.novellibrary.R

/**
 * Navigation extension functions for common navigation patterns.
 * These extensions provide convenient methods for navigation throughout the app.
 */

/**
 * Safe navigation that handles navigation exceptions
 */
fun NavController.navigateSafely(directions: NavDirections) {
    try {
        navigate(directions)
    } catch (e: Exception) {
        // Log the exception and handle gracefully
        android.util.Log.e("NavigationExtensions", "Navigation failed", e)
    }
}

/**
 * Safe navigation with custom nav options
 */
fun NavController.navigateSafely(directions: NavDirections, navOptions: NavOptions) {
    try {
        navigate(directions, navOptions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Navigation failed", e)
    }
}

/**
 * Safe navigation by destination ID
 */
fun NavController.navigateSafely(destinationId: Int) {
    try {
        navigate(destinationId)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Navigation failed", e)
    }
}

/**
 * Extension function for Fragment to navigate safely
 */
fun Fragment.navigateSafely(directions: NavDirections) {
    try {
        findNavController().navigate(directions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Fragment navigation failed", e)
    }
}

/**
 * Extension function for Fragment to navigate safely with options
 */
fun Fragment.navigateSafely(directions: NavDirections, navOptions: NavOptions) {
    try {
        findNavController().navigate(directions, navOptions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Fragment navigation failed", e)
    }
}

/**
 * Navigate with slide animations
 */
fun NavController.navigateWithSlideAnimation(directions: NavDirections) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setExitAnim(R.anim.slide_out_left)
        .setPopEnterAnim(R.anim.slide_in_left)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()
    
    navigateSafely(directions, navOptions)
}

/**
 * Navigate with fade animations
 */
fun NavController.navigateWithFadeAnimation(directions: NavDirections) {
    val navOptions = NavOptions.Builder()
        .setEnterAnim(android.R.anim.fade_in)
        .setExitAnim(android.R.anim.fade_out)
        .setPopEnterAnim(android.R.anim.fade_in)
        .setPopExitAnim(android.R.anim.fade_out)
        .build()
    
    navigateSafely(directions, navOptions)
}

/**
 * Navigate and clear back stack to destination
 */
fun NavController.navigateAndClearBackStack(directions: NavDirections, destinationId: Int) {
    val navOptions = NavOptions.Builder()
        .setPopUpTo(destinationId, true)
        .setLaunchSingleTop(true)
        .build()
    
    navigateSafely(directions, navOptions)
}

/**
 * Navigate to top level destination (for drawer navigation)
 */
fun NavController.navigateToTopLevel(destinationId: Int) {
    val navOptions = NavOptions.Builder()
        .setPopUpTo(graph.startDestinationId, false)
        .setLaunchSingleTop(true)
        .setRestoreState(true)
        .build()
    
    navigate(destinationId, null, navOptions)
}

/**
 * Pop back stack safely
 */
fun NavController.popBackStackSafely(): Boolean {
    return try {
        popBackStack()
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Pop back stack failed", e)
        false
    }
}

/**
 * Pop back stack to destination safely
 */
fun NavController.popBackStackSafely(destinationId: Int, inclusive: Boolean): Boolean {
    return try {
        popBackStack(destinationId, inclusive)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Pop back stack to destination failed", e)
        false
    }
}

/**
 * Check if we can navigate up
 */
fun NavController.canNavigateUp(): Boolean {
    return try {
        previousBackStackEntry != null
    } catch (e: Exception) {
        false
    }
}

/**
 * Get current destination ID safely
 */
fun NavController.getCurrentDestinationId(): Int? {
    return try {
        currentDestination?.id
    } catch (e: Exception) {
        null
    }
}

/**
 * Check if current destination matches the given ID
 */
fun NavController.isCurrentDestination(destinationId: Int): Boolean {
    return getCurrentDestinationId() == destinationId
}

/**
 * Navigate to novel details with animation
 */
fun Fragment.navigateToNovelDetails(novelId: Long) {
    try {
        val navController = findNavController()
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        navController.navigate(R.id.novelDetailsFragment, bundle, navOptions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Navigate to novel details failed", e)
    }
}

/**
 * Navigate to reader with animation
 */
fun Fragment.navigateToReader(novelId: Long, chapterId: Long) {
    try {
        val navController = findNavController()
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
            putLong("chapterId", chapterId)
        }
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        navController.navigate(R.id.readerFragment, bundle, navOptions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Navigate to reader failed", e)
    }
}

/**
 * Navigate to chapters with animation
 */
fun Fragment.navigateToChapters(novelId: Long) {
    try {
        val navController = findNavController()
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        val navOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right)
            .setExitAnim(R.anim.slide_out_left)
            .setPopEnterAnim(R.anim.slide_in_left)
            .setPopExitAnim(R.anim.slide_out_right)
            .build()
        navController.navigate(R.id.chaptersFragment, bundle, navOptions)
    } catch (e: Exception) {
        android.util.Log.e("NavigationExtensions", "Navigate to chapters failed", e)
    }
}