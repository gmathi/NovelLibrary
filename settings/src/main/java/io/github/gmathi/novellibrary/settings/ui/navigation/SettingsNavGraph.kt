package io.github.gmathi.novellibrary.settings.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
 */
sealed class SettingsRoute(val route: String) {
    data object Main : SettingsRoute("settings_main")
    data object Reader : SettingsRoute("settings_reader")
    data object BackupSync : SettingsRoute("settings_backup_sync")
    data object General : SettingsRoute("settings_general")
    data object Advanced : SettingsRoute("settings_advanced")
    data object About : SettingsRoute("settings_about")
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
 * @param navController Navigation controller for managing navigation
 * @param onNavigateBack Callback to exit settings and return to app
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
    navController: NavHostController = rememberNavController(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route,
        modifier = modifier
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
                onCreateBackup = {
                    // TODO: Implement backup creation logic
                },
                onRestoreBackup = {
                    // TODO: Implement backup restoration logic
                },
                onConfigureGoogleDrive = {
                    // TODO: Implement Google Drive configuration
                },
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
            // TODO: Implement AboutScreen in task 6.6
            PlaceholderSettingsScreen(
                title = "About",
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Placeholder settings screen for routes not yet implemented.
 * 
 * Displays a simple screen with the title and back navigation.
 * Will be replaced with actual screen implementations in subsequent tasks.
 * 
 * @param title The screen title
 * @param onNavigateBack Callback to navigate back
 */
@Composable
private fun PlaceholderSettingsScreen(
    title: String,
    onNavigateBack: () -> Unit
) {
    io.github.gmathi.novellibrary.settings.ui.components.SettingsScreen(
        title = title,
        onNavigateBack = onNavigateBack
    ) {
        io.github.gmathi.novellibrary.settings.ui.components.SettingsItem(
            title = "Coming Soon",
            description = "This screen will be implemented in a future task",
            onClick = {}
        )
    }
}
