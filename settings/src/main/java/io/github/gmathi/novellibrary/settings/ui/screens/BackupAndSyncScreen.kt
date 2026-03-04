package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.ui.components.*
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel

/**
 * Backup and Sync Settings Screen with Tabbed Interface
 * 
 * Consolidates backup and sync settings from multiple old activities:
 * - BackupSettingsActivity (local backup)
 * - GoogleBackupActivity (Google Drive backup)
 * - SyncSettingsActivity (sync configuration)
 * - SyncLoginActivity (sync authentication - now a modal dialog)
 * - SyncSettingsSelectionActivity (sync selection)
 * 
 * Organized into 2 tabs:
 * 1. Backup - Local backup and Google Drive backup settings
 * 2. Sync - Sync service configuration and settings selection
 * 
 * @param backupViewModel ViewModel managing backup settings state
 * @param syncViewModel ViewModel managing sync settings state
 * @param onNavigateBack Callback to navigate back to main settings
 * @param onCreateBackup Callback to trigger backup creation
 * @param onRestoreBackup Callback to trigger backup restoration
 * @param onConfigureGoogleDrive Callback to configure Google Drive
 * @param onSyncLogin Callback to show sync login dialog
 * @param modifier Modifier for the screen
 */
@Composable
fun BackupAndSyncScreen(
    backupViewModel: BackupSettingsViewModel,
    syncViewModel: SyncSettingsViewModel,
    onNavigateBack: () -> Unit,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    onConfigureGoogleDrive: () -> Unit = {},
    onSyncLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Tab state
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    
    // Collect backup state
    val lastLocalBackupTimestamp by backupViewModel.lastLocalBackupTimestamp.collectAsState()
    val lastCloudBackupTimestamp by backupViewModel.lastCloudBackupTimestamp.collectAsState()
    val lastBackupSize by backupViewModel.lastBackupSize.collectAsState()
    val gdBackupInterval by backupViewModel.gdBackupInterval.collectAsState()
    val gdAccountEmail by backupViewModel.gdAccountEmail.collectAsState()
    val gdInternetType by backupViewModel.gdInternetType.collectAsState()
    
    // Collect sync state (using "default" as service name)
    val syncEnabled by syncViewModel.getSyncEnabled("default").collectAsState()
    val syncAddNovels by syncViewModel.getSyncAddNovels("default").collectAsState()
    val syncDeleteNovels by syncViewModel.getSyncDeleteNovels("default").collectAsState()
    val syncBookmarks by syncViewModel.getSyncBookmarks("default").collectAsState()
    
    SettingsScreen(
        title = "Backup & Sync",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Backup") },
                icon = { Icon(Icons.Default.Backup, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Sync") },
                icon = { Icon(Icons.Default.Sync, contentDescription = null) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tab Content
        when (selectedTabIndex) {
            0 -> BackupTabContent(
                lastLocalBackupTimestamp = lastLocalBackupTimestamp,
                lastCloudBackupTimestamp = lastCloudBackupTimestamp,
                lastBackupSize = lastBackupSize,
                gdBackupInterval = gdBackupInterval,
                onGdBackupIntervalChange = backupViewModel::setGdBackupInterval,
                gdAccountEmail = gdAccountEmail,
                gdInternetType = gdInternetType,
                onGdInternetTypeChange = backupViewModel::setGdInternetType,
                onCreateBackup = onCreateBackup,
                onRestoreBackup = onRestoreBackup,
                onConfigureGoogleDrive = onConfigureGoogleDrive
            )
            1 -> SyncTabContent(
                syncEnabled = syncEnabled,
                onSyncEnabledChange = { syncViewModel.setSyncEnabled("default", it) },
                syncAddNovels = syncAddNovels,
                onSyncAddNovelsChange = { syncViewModel.setSyncAddNovels("default", it) },
                syncDeleteNovels = syncDeleteNovels,
                onSyncDeleteNovelsChange = { syncViewModel.setSyncDeleteNovels("default", it) },
                syncBookmarks = syncBookmarks,
                onSyncBookmarksChange = { syncViewModel.setSyncBookmarks("default", it) },
                onSyncLogin = onSyncLogin
            )
        }
    }
}

/**
 * Content for the Backup tab.
 * 
 * Contains local backup and Google Drive backup sections.
 */
@Composable
private fun ColumnScope.BackupTabContent(
    lastLocalBackupTimestamp: String,
    lastCloudBackupTimestamp: String,
    lastBackupSize: String,
    gdBackupInterval: String,
    onGdBackupIntervalChange: (String) -> Unit,
    gdAccountEmail: String,
    gdInternetType: String,
    onGdInternetTypeChange: (String) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onConfigureGoogleDrive: () -> Unit
) {
    // Section 1: Local Backup
    SettingsSection(title = "Local Backup") {
        SettingsItem(
            title = "Create Backup",
            description = "Save your library and settings to device storage",
            icon = Icons.Default.Save,
            onClick = onCreateBackup
        )
        
        SettingsItem(
            title = "Restore Backup",
            description = "Restore from a previous backup file",
            icon = Icons.Default.Restore,
            onClick = onRestoreBackup
        )
        
        if (lastLocalBackupTimestamp.isNotEmpty()) {
            SettingsItem(
                title = "Last Backup",
                description = lastLocalBackupTimestamp + if (lastBackupSize.isNotEmpty()) " • $lastBackupSize" else "",
                icon = Icons.Default.Schedule,
                onClick = null
            )
        }
    }
    
    // Section 2: Google Drive Backup
    SettingsSection(title = "Google Drive Backup") {
        if (gdAccountEmail.isNotEmpty()) {
            SettingsItem(
                title = "Account",
                description = gdAccountEmail,
                icon = Icons.Default.AccountCircle,
                onClick = onConfigureGoogleDrive
            )
        } else {
            SettingsItem(
                title = "Configure Google Drive",
                description = "Set up automatic cloud backup",
                icon = Icons.Default.Cloud,
                onClick = onConfigureGoogleDrive
            )
        }
        
        SettingsDropdown(
            title = "Backup Interval",
            description = "How often to backup to Google Drive",
            icon = Icons.Default.Schedule,
            selectedValue = gdBackupInterval.replaceFirstChar { it.uppercase() },
            options = listOf("Daily", "Weekly", "Monthly", "Manual"),
            onOptionSelected = { onGdBackupIntervalChange(it.lowercase()) }
        )
        
        SettingsDropdown(
            title = "Network Type",
            description = "When to perform automatic backups",
            icon = Icons.Default.Wifi,
            selectedValue = when (gdInternetType) {
                "wifi" -> "Wi-Fi Only"
                "any" -> "Any Connection"
                else -> "Wi-Fi Only"
            },
            options = listOf("Wi-Fi Only", "Any Connection"),
            onOptionSelected = { 
                onGdInternetTypeChange(
                    when (it) {
                        "Wi-Fi Only" -> "wifi"
                        "Any Connection" -> "any"
                        else -> "wifi"
                    }
                )
            }
        )
        
        if (lastCloudBackupTimestamp.isNotEmpty()) {
            SettingsItem(
                title = "Last Cloud Backup",
                description = lastCloudBackupTimestamp,
                icon = Icons.Default.CloudDone,
                onClick = null
            )
        }
    }
}

/**
 * Content for the Sync tab.
 * 
 * Contains sync service configuration and settings selection.
 */
@Composable
private fun ColumnScope.SyncTabContent(
    syncEnabled: Boolean,
    onSyncEnabledChange: (Boolean) -> Unit,
    syncAddNovels: Boolean,
    onSyncAddNovelsChange: (Boolean) -> Unit,
    syncDeleteNovels: Boolean,
    onSyncDeleteNovelsChange: (Boolean) -> Unit,
    syncBookmarks: Boolean,
    onSyncBookmarksChange: (Boolean) -> Unit,
    onSyncLogin: () -> Unit
) {
    // Section 1: Sync Configuration
    SettingsSection(title = "Sync Configuration") {
        SettingsSwitch(
            title = "Enable Sync",
            description = "Synchronize your library across devices",
            icon = Icons.Default.Sync,
            checked = syncEnabled,
            onCheckedChange = onSyncEnabledChange
        )
        
        SettingsItem(
            title = if (syncEnabled) "Manage Account" else "Login to Sync",
            description = if (syncEnabled) "View or change sync account" else "Sign in to enable synchronization",
            icon = if (syncEnabled) Icons.Default.AccountCircle else Icons.Default.Login,
            onClick = onSyncLogin,
            enabled = true
        )
    }
    
    // Section 2: Sync Settings Selection (only shown when sync is enabled)
    if (syncEnabled) {
        SettingsSection(title = "What to Sync") {
            SettingsSwitch(
                title = "Sync Added Novels",
                description = "Synchronize novels added to your library",
                icon = Icons.Default.LibraryAdd,
                checked = syncAddNovels,
                onCheckedChange = onSyncAddNovelsChange
            )
            
            SettingsSwitch(
                title = "Sync Deleted Novels",
                description = "Synchronize novels removed from your library",
                icon = Icons.Default.Delete,
                checked = syncDeleteNovels,
                onCheckedChange = onSyncDeleteNovelsChange
            )
            
            SettingsSwitch(
                title = "Sync Bookmarks",
                description = "Synchronize reading progress and bookmarks",
                icon = Icons.Default.Bookmark,
                checked = syncBookmarks,
                onCheckedChange = onSyncBookmarksChange
            )
        }
    }
}


// ============================================================================
// Preview Functions
// ============================================================================

@Preview(
    name = "Backup & Sync - Full Screen Light",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewBackupAndSyncScreenFullLight() {
    MaterialTheme {
        BackupAndSyncScreen(
            backupViewModel = createPreviewBackupViewModel(),
            syncViewModel = createPreviewSyncViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Backup & Sync - Full Screen Dark",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewBackupAndSyncScreenFullDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            BackupAndSyncScreen(
                backupViewModel = createPreviewBackupViewModel(),
                syncViewModel = createPreviewSyncViewModel(),
                onNavigateBack = {}
            )
        }
    }
}

@Preview(name = "Backup and Sync Screen - Backup Tab", showBackground = true)
@Composable
private fun PreviewBackupAndSyncScreenBackupTab() {
    MaterialTheme {
        BackupAndSyncScreen(
            backupViewModel = createPreviewBackupViewModel(),
            syncViewModel = createPreviewSyncViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Backup and Sync Screen - Sync Tab", showBackground = true)
@Composable
private fun PreviewBackupAndSyncScreenSyncTab() {
    MaterialTheme {
        BackupAndSyncScreen(
            backupViewModel = createPreviewBackupViewModel(),
            syncViewModel = createPreviewSyncViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Backup and Sync Dark", showBackground = true)
@Composable
private fun PreviewBackupAndSyncScreenDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            BackupAndSyncScreen(
                backupViewModel = createPreviewBackupViewModel(),
                syncViewModel = createPreviewSyncViewModel(),
                onNavigateBack = {}
            )
        }
    }
}

/**
 * Creates a preview BackupSettingsViewModel with mock data for Compose previews.
 */
private fun createPreviewBackupViewModel(): BackupSettingsViewModel {
    val fakeDataStore = FakeSettingsDataStore()
    val repository = SettingsRepositoryDataStore(fakeDataStore)
    return BackupSettingsViewModel(repository)
}

/**
 * Creates a preview SyncSettingsViewModel with mock data for Compose previews.
 */
private fun createPreviewSyncViewModel(): SyncSettingsViewModel {
    val fakeDataStore = FakeSettingsDataStore()
    val repository = SettingsRepositoryDataStore(fakeDataStore)
    return SyncSettingsViewModel(repository)
}
