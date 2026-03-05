package io.github.gmathi.novellibrary.settings.api

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.ui.navigation.SettingsNavGraph
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel

/**
 * Standalone Activity for settings that can be launched from the app module.
 * 
 * This Activity provides a complete settings experience using Compose Navigation.
 * It's useful for apps that haven't fully migrated to Compose Navigation yet.
 * 
 * Usage from app module:
 * ```
 * val intent = Intent(context, SettingsActivity::class.java)
 * context.startActivity(intent)
 * ```
 * 
 * Or use the SettingsNavigator helper:
 * ```
 * SettingsNavigator.openSettings(context)
 * ```
 * 
 * For apps using Compose Navigation, use SettingsNavigator.addSettingsGraph() instead.
 */
class SettingsActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create settings repository
        val settingsRepository = SettingsRepositoryDataStore(applicationContext)
        
        // Create ViewModels
        val mainSettingsViewModel = MainSettingsViewModel(settingsRepository)
        val readerSettingsViewModel = ReaderSettingsViewModel(settingsRepository)
        val generalSettingsViewModel = GeneralSettingsViewModel(settingsRepository)
        val backupSettingsViewModel = BackupSettingsViewModel(settingsRepository)
        val syncSettingsViewModel = SyncSettingsViewModel(settingsRepository)
        val advancedSettingsViewModel = AdvancedSettingsViewModel(settingsRepository)
        
        setContent {
            MaterialTheme {
                SettingsActivityContent(
                    mainSettingsViewModel = mainSettingsViewModel,
                    readerSettingsViewModel = readerSettingsViewModel,
                    generalSettingsViewModel = generalSettingsViewModel,
                    backupSettingsViewModel = backupSettingsViewModel,
                    syncSettingsViewModel = syncSettingsViewModel,
                    advancedSettingsViewModel = advancedSettingsViewModel,
                    appVersionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown",
                    appVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode,
                    onNavigateBack = { finish() },
                    onNavigateToContributors = {
                        // Navigate to existing ContributionsActivity
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.ContributionsActivity")))
                    },
                    onNavigateToCopyright = {
                        // Navigate to existing CopyrightActivity
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.CopyrightActivity")))
                    },
                    onNavigateToLicenses = {
                        // Navigate to existing LibrariesUsedActivity
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.LibrariesUsedActivity")))
                    },
                    onOpenPrivacyPolicy = {
                        // Open privacy policy in browser
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/gmathi/NovelLibrary/blob/master/Privacy-Policy"))
                        startActivity(intent)
                    },
                    onOpenTermsOfService = {
                        // Open terms of service (same as privacy policy for now)
                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/gmathi/NovelLibrary/blob/master/Privacy-Policy"))
                        startActivity(intent)
                    },
                    onCheckForUpdates = {
                        // TODO: Implement update checking - this requires app-specific logic
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsActivityContent(
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
    onCheckForUpdates: () -> Unit
) {
    val navController = rememberNavController()
    
    SettingsNavGraph(
        mainSettingsViewModel = mainSettingsViewModel,
        readerSettingsViewModel = readerSettingsViewModel,
        generalSettingsViewModel = generalSettingsViewModel,
        backupSettingsViewModel = backupSettingsViewModel,
        syncSettingsViewModel = syncSettingsViewModel,
        advancedSettingsViewModel = advancedSettingsViewModel,
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        navController = navController,
        onNavigateBack = onNavigateBack,
        onNavigateToContributors = onNavigateToContributors,
        onNavigateToCopyright = onNavigateToCopyright,
        onNavigateToLicenses = onNavigateToLicenses,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenTermsOfService = onOpenTermsOfService,
        onCheckForUpdates = onCheckForUpdates
    )
}
