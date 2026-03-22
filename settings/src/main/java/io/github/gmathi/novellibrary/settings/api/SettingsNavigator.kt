package io.github.gmathi.novellibrary.settings.api

import android.content.Context
import android.content.Intent
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.gmathi.novellibrary.settings.ui.navigation.SettingsNavGraph
import io.github.gmathi.novellibrary.settings.ui.navigation.SettingsRoute
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel

/**
 * Public API for navigating to settings screens.
 * This is the primary interface between the app module and settings module.
 * 
 * Provides two navigation approaches:
 * 1. Compose Navigation - For modern Compose-based apps (recommended)
 * 2. Activity-based Navigation - For legacy Activity-based apps (deprecated)
 */
object SettingsNavigator {
    
    // ========================================================================
    // Compose Navigation API (Recommended)
    // ========================================================================
    
    /**
     * Route constant for settings navigation graph.
     * Use this when adding settings to your app's navigation graph.
     */
    const val SETTINGS_ROUTE = "settings"
    
    /**
     * Opens the settings Activity.
     * 
     * This is a convenience method for apps that haven't fully migrated to Compose Navigation.
     * It launches a standalone Activity that contains the full settings navigation graph.
     * 
     * For apps using Compose Navigation, use addSettingsGraph() instead.
     * 
     * @param context The context to use for launching the Activity
     */
    fun openSettings(context: Context) {
        val intent = Intent(context, SettingsActivity::class.java)
        context.startActivity(intent)
    }
    
    /**
     * Adds the settings navigation graph to the app's navigation graph.
     * 
     * This is the recommended way to integrate settings into a Compose-based app.
     * Call this from your app's NavHost to add all settings screens.
     * 
     * Example usage:
     * ```
     * NavHost(navController = navController, startDestination = "home") {
     *     composable("home") { HomeScreen() }
     *     
     *     // Add settings navigation
     *     SettingsNavigator.addSettingsGraph(
     *         navGraphBuilder = this,
     *         mainSettingsViewModel = mainSettingsViewModel,
     *         readerSettingsViewModel = readerSettingsViewModel,
     *         generalSettingsViewModel = generalSettingsViewModel,
     *         backupSettingsViewModel = backupSettingsViewModel,
     *         syncSettingsViewModel = syncSettingsViewModel,
     *         advancedSettingsViewModel = advancedSettingsViewModel,
     *         appVersionName = BuildConfig.VERSION_NAME,
     *         appVersionCode = BuildConfig.VERSION_CODE,
     *         onNavigateBack = { navController.popBackStack() },
     *         onNavigateToContributors = { /* navigate to contributors */ },
     *         onNavigateToCopyright = { /* navigate to copyright */ },
     *         onNavigateToLicenses = { /* navigate to licenses */ },
     *         onOpenPrivacyPolicy = { /* open privacy policy */ },
     *         onOpenTermsOfService = { /* open terms */ },
     *         onCheckForUpdates = { /* check updates */ }
     *     )
     * }
     * 
     * // Navigate to settings
     * navController.navigate(SettingsNavigator.SETTINGS_ROUTE)
     * ```
     * 
     * @param navGraphBuilder The NavGraphBuilder to add settings routes to
     * @param mainSettingsViewModel ViewModel for the main settings screen
     * @param readerSettingsViewModel ViewModel for the reader settings screen
     * @param generalSettingsViewModel ViewModel for the general settings screen
     * @param backupSettingsViewModel ViewModel for the backup settings screen
     * @param syncSettingsViewModel ViewModel for the sync settings screen
     * @param advancedSettingsViewModel ViewModel for the advanced settings screen
     * @param appVersionName The app version name for the about screen
     * @param appVersionCode The app version code for the about screen
     * @param onNavigateBack Callback to exit settings and return to app
     * @param onNavigateToContributors Callback to navigate to contributors screen
     * @param onNavigateToCopyright Callback to navigate to copyright screen
     * @param onNavigateToLicenses Callback to navigate to open source licenses screen
     * @param onOpenPrivacyPolicy Callback to open privacy policy
     * @param onOpenTermsOfService Callback to open terms of service
     * @param onCheckForUpdates Callback to check for app updates
     */
    fun addSettingsGraph(
        navGraphBuilder: NavGraphBuilder,
        mainSettingsViewModel: MainSettingsViewModel,
        readerSettingsViewModel: ReaderSettingsViewModel,
        generalSettingsViewModel: GeneralSettingsViewModel,
        backupSettingsViewModel: BackupSettingsViewModel,
        syncSettingsViewModel: SyncSettingsViewModel,
        advancedSettingsViewModel: AdvancedSettingsViewModel,
        appVersionName: String,
        appVersionCode: Int,
        onNavigateBack: () -> Unit,
        onNavigateToContributors: () -> Unit,
        onNavigateToCopyright: () -> Unit,
        onNavigateToLicenses: () -> Unit,
        onOpenPrivacyPolicy: () -> Unit,
        onOpenTermsOfService: () -> Unit,
        onCheckForUpdates: () -> Unit,
        onCreateBackup: () -> Unit = {},
        onRestoreBackup: () -> Unit = {},
        onConfigureGoogleDrive: () -> Unit = {}
    ) {
        navGraphBuilder.composable(SETTINGS_ROUTE) {
            SettingsNavGraph(
                mainSettingsViewModel = mainSettingsViewModel,
                readerSettingsViewModel = readerSettingsViewModel,
                generalSettingsViewModel = generalSettingsViewModel,
                backupSettingsViewModel = backupSettingsViewModel,
                syncSettingsViewModel = syncSettingsViewModel,
                advancedSettingsViewModel = advancedSettingsViewModel,
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                onNavigateBack = onNavigateBack,
                onNavigateToContributors = onNavigateToContributors,
                onNavigateToCopyright = onNavigateToCopyright,
                onNavigateToLicenses = onNavigateToLicenses,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onOpenTermsOfService = onOpenTermsOfService,
                onCheckForUpdates = onCheckForUpdates,
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
                onConfigureGoogleDrive = onConfigureGoogleDrive
            )
        }
    }
    
    /**
     * Navigates to the settings screen using Compose Navigation.
     * 
     * Call this from your app to open the settings screen.
     * 
     * @param navController The NavController to use for navigation
     */
    fun navigateToSettings(navController: NavController) {
        navController.navigate(SETTINGS_ROUTE)
    }
    
    /**
     * Navigates to a specific settings category using Compose Navigation.
     * 
     * This allows deep linking directly to a specific settings category.
     * 
     * @param navController The NavController to use for navigation
     * @param route The settings route to navigate to (use SettingsRoute constants)
     */
    fun navigateToSettingsRoute(navController: NavController, route: String) {
        navController.navigate(route)
    }
    
    // ========================================================================
    // Activity-based Navigation API (Deprecated - for backward compatibility)
    // ========================================================================
    
    private const val SETTINGS_PACKAGE = "io.github.gmathi.novellibrary.settings.activity"
    private const val READER_PACKAGE = "$SETTINGS_PACKAGE.reader"
    
    /**
     * Opens the main settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation with addSettingsGraph() instead.
     * This method is provided for backward compatibility with Activity-based apps.
     */
    @Deprecated(
        message = "Use Compose Navigation with addSettingsGraph() instead",
        replaceWith = ReplaceWith("navigateToSettings(navController)")
    )
    fun openMainSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.MainSettingsActivity")
    }
    
    /**
     * Opens the general settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openGeneralSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.GeneralSettingsActivity")
    }
    
    /**
     * Opens the backup settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openBackupSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.BackupSettingsActivity")
    }
    
    /**
     * Opens the sync settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openSyncSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncSettingsActivity")
    }
    
    /**
     * Opens the sync login screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openSyncLogin(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncLoginActivity")
    }
    
    /**
     * Opens the sync settings selection screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openSyncSettingsSelection(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.SyncSettingsSelectionActivity")
    }
    
    /**
     * Opens the Google backup screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openGoogleBackup(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.GoogleBackupActivity")
    }
    
    /**
     * Opens the TTS (Text-to-Speech) settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openTTSSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.TTSSettingsActivity")
    }
    
    /**
     * Opens the language selection screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openLanguage(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.LanguageActivity")
    }
    
    /**
     * Opens the CloudFlare bypass settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openCloudFlareBypass(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.CloudFlareBypassActivity")
    }
    
    /**
     * Opens the mention settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openMentionSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.MentionSettingsActivity")
    }
    
    /**
     * Opens the contributions screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openContributions(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.ContributionsActivity")
    }
    
    /**
     * Opens the copyright information screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openCopyright(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.CopyrightActivity")
    }
    
    /**
     * Opens the libraries used screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openLibrariesUsed(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.LibrariesUsedActivity")
    }
    
    /**
     * Opens the reader settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openReaderSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ReaderSettingsActivity")
    }
    
    /**
     * Opens the reader background settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openReaderBackgroundSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ReaderBackgroundSettingsActivity")
    }
    
    /**
     * Opens the scroll behaviour settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openScrollBehaviourSettings(context: Context) {
        launchActivity(context, "$READER_PACKAGE.ScrollBehaviourSettingsActivity")
    }
    
    /**
     * Opens the base settings screen using Activity-based navigation.
     * 
     * @deprecated Use Compose Navigation instead.
     */
    @Deprecated("Use Compose Navigation instead")
    fun openBaseSettings(context: Context) {
        launchActivity(context, "$SETTINGS_PACKAGE.BaseSettingsActivity")
    }
    
    /**
     * Helper method to launch an activity using reflection.
     * This avoids compile-time dependencies on activity classes.
     */
    private fun launchActivity(context: Context, className: String) {
        try {
            val activityClass = Class.forName(className)
            val intent = Intent(context, activityClass)
            context.startActivity(intent)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Settings activity not found: $className", e)
        }
    }
}
