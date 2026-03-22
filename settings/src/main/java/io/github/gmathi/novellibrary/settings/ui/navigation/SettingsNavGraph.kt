package io.github.gmathi.novellibrary.settings.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.gmathi.novellibrary.settings.ui.screens.AboutScreen
import io.github.gmathi.novellibrary.settings.ui.screens.AdvancedSettingsScreen
import io.github.gmathi.novellibrary.settings.ui.screens.BackupAndSyncScreen
import io.github.gmathi.novellibrary.settings.ui.screens.GeneralSettingsScreen
import io.github.gmathi.novellibrary.settings.ui.screens.MainSettingsScreen
import io.github.gmathi.novellibrary.settings.ui.screens.ReaderSettingsScreen
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel

/**
 * Navigation routes for settings screens.
 * 
 * Defines all navigation destinations within the settings module.
 * Uses sealed class pattern for type-safe navigation.
 * 
 * These routes can be used for deep linking to specific settings screens:
 * - Main: Entry point showing all settings categories
 * - Reader: Reading experience customization
 * - BackupSync: Data backup and synchronization
 * - General: App-wide preferences
 * - Advanced: Technical and power-user settings
 * - About: App information and credits
 */
sealed class SettingsRoute(val route: String) {
    data object Main : SettingsRoute("settings_main")
    data object Reader : SettingsRoute("settings_reader")
    data object BackupSync : SettingsRoute("settings_backup_sync")
    data object General : SettingsRoute("settings_general")
    data object Advanced : SettingsRoute("settings_advanced")
    data object About : SettingsRoute("settings_about")
    
    companion object {
        /**
         * Returns all available settings routes.
         * Useful for validation or iteration.
         */
        fun getAllRoutes(): List<String> = listOf(
            Main.route,
            Reader.route,
            BackupSync.route,
            General.route,
            Advanced.route,
            About.route
        )
    }
}

/**
 * Settings navigation graph.
 * 
 * Defines the navigation structure for all settings screens using Compose Navigation.
 * The main settings screen serves as the entry point, with navigation to each category.
 * 
 * Navigation flow:
 * - Main Settings (entry point)
 *   ├── Reader Settings
 *   ├── Backup & Sync Settings
 *   ├── General Settings
 *   ├── Advanced Settings
 *   └── About
 * 
 * @param mainSettingsViewModel ViewModel for the main settings screen
 * @param readerSettingsViewModel ViewModel for the reader settings screen
 * @param generalSettingsViewModel ViewModel for the general settings screen
 * @param backupSettingsViewModel ViewModel for the backup settings screen
 * @param syncSettingsViewModel ViewModel for the sync settings screen
 * @param advancedSettingsViewModel ViewModel for the advanced settings screen
 * @param appVersionName The app version name for the about screen
 * @param appVersionCode The app version code for the about screen
 * @param navController Navigation controller for managing navigation
 * @param onNavigateBack Callback to exit settings and return to app
 * @param onNavigateToContributors Callback to navigate to contributors screen
 * @param onNavigateToCopyright Callback to navigate to copyright screen
 * @param onNavigateToLicenses Callback to navigate to open source licenses screen
 * @param onOpenPrivacyPolicy Callback to open privacy policy
 * @param onOpenTermsOfService Callback to open terms of service
 * @param onCheckForUpdates Callback to check for app updates
 * @param onCreateBackup Callback to trigger local backup creation (launches file picker)
 * @param onRestoreBackup Callback to trigger local backup restoration (launches file picker)
 * @param onConfigureGoogleDrive Callback to configure Google Drive backup (deprecated, kept for compat)
 * @param onGoogleSignIn Callback to initiate Google Sign-In flow
 * @param onGoogleSignOut Callback to sign out of Google
 * @param onGoogleDriveBackup Callback to trigger Google Drive backup with selected options
 * @param onGoogleDriveRestore Callback to trigger Google Drive restore with selected options
 * @param onRefreshBackupInfo Callback to refresh Google Drive backup info
 * @param modifier Modifier for the navigation host
 */
@Composable
fun SettingsNavGraph(
    mainSettingsViewModel: MainSettingsViewModel,
    readerSettingsViewModel: ReaderSettingsViewModel,
    generalSettingsViewModel: GeneralSettingsViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,
    syncSettingsViewModel: SyncSettingsViewModel,
    advancedSettingsViewModel: AdvancedSettingsViewModel,
    appVersionName: String,
    appVersionCode: Int,
    navController: NavHostController = rememberNavController(),
    onNavigateBack: () -> Unit,
    onNavigateToContributors: () -> Unit,
    onNavigateToCopyright: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    onConfigureGoogleDrive: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onGoogleDriveBackup: (simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean) -> Unit = { _, _, _, _ -> },
    onGoogleDriveRestore: (simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean) -> Unit = { _, _, _, _ -> },
    onRefreshBackupInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val enterAnim = slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
    val exitAnim = slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(150))
    val popEnterAnim = slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))
    val popExitAnim = slideOutHorizontally(tween(300)) { it } + fadeOut(tween(150))

    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route,
        modifier = modifier,
        enterTransition = { enterAnim },
        exitTransition = { exitAnim },
        popEnterTransition = { popEnterAnim },
        popExitTransition = { popExitAnim }
    ) {
        // Main Settings Screen
        composable(SettingsRoute.Main.route) {
            MainSettingsScreen(
                viewModel = mainSettingsViewModel,
                onNavigateToReader = {
                    navController.navigate(SettingsRoute.Reader.route)
                },
                onNavigateToBackupSync = {
                    navController.navigate(SettingsRoute.BackupSync.route)
                },
                onNavigateToGeneral = {
                    navController.navigate(SettingsRoute.General.route)
                },
                onNavigateToAdvanced = {
                    navController.navigate(SettingsRoute.Advanced.route)
                },
                onNavigateToAbout = {
                    navController.navigate(SettingsRoute.About.route)
                },
                onNavigateBack = onNavigateBack
            )
        }
        
        // Reader Settings Screen
        composable(SettingsRoute.Reader.route) {
            ReaderSettingsScreen(
                viewModel = readerSettingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Backup & Sync Settings Screen
        composable(SettingsRoute.BackupSync.route) {
            BackupAndSyncScreen(
                backupViewModel = backupSettingsViewModel,
                syncViewModel = syncSettingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
                onConfigureGoogleDrive = onConfigureGoogleDrive,
                onGoogleSignIn = onGoogleSignIn,
                onGoogleSignOut = onGoogleSignOut,
                onGoogleDriveBackup = onGoogleDriveBackup,
                onGoogleDriveRestore = onGoogleDriveRestore,
                onRefreshBackupInfo = onRefreshBackupInfo,
                onSyncLogin = {
                    // TODO: Show sync login dialog
                }
            )
        }
        
        // General Settings Screen
        composable(SettingsRoute.General.route) {
            GeneralSettingsScreen(
                viewModel = generalSettingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Advanced Settings Screen
        composable(SettingsRoute.Advanced.route) {
            AdvancedSettingsScreen(
                viewModel = advancedSettingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onClearCache = {
                    // TODO: Implement cache clearing logic
                },
                onResetSettings = {
                    // TODO: Implement settings reset logic
                },
                onCloudflareBypass = {
                    // TODO: Implement Cloudflare bypass navigation
                }
            )
        }
        
        // About Screen (placeholder - to be implemented in task 6.6)
        composable(SettingsRoute.About.route) {
            AboutScreen(
                appVersionName = appVersionName,
                appVersionCode = appVersionCode,
                onNavigateToContributors = onNavigateToContributors,
                onNavigateToCopyright = onNavigateToCopyright,
                onNavigateToLicenses = onNavigateToLicenses,
                onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                onOpenTermsOfService = onOpenTermsOfService,
                onCheckForUpdates = onCheckForUpdates,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
