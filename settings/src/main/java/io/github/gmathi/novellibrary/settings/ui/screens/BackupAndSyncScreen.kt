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
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme

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
 * @param onConfigureGoogleDrive Callback to configure Google Drive (deprecated, kept for compat)
 * @param onGoogleSignIn Callback to initiate Google Sign-In flow
 * @param onGoogleSignOut Callback to sign out of Google
 * @param onGoogleDriveBackup Callback to trigger Google Drive backup with selected options (simpleText, database, preferences, files)
 * @param onGoogleDriveRestore Callback to trigger Google Drive restore with selected options
 * @param onRefreshBackupInfo Callback to refresh Google Drive backup info, returns info string via the provided lambda
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
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onGoogleDriveBackup: (simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean) -> Unit = { _, _, _, _ -> },
    onGoogleDriveRestore: (simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean) -> Unit = { _, _, _, _ -> },
    onRefreshBackupInfo: () -> Unit = {},
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
                onGoogleSignIn = onGoogleSignIn,
                onGoogleSignOut = onGoogleSignOut,
                onGoogleDriveBackup = onGoogleDriveBackup,
                onGoogleDriveRestore = onGoogleDriveRestore,
                onRefreshBackupInfo = onRefreshBackupInfo
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
 * Google Drive operations are handled inline via dialogs instead of launching a separate Activity.
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
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    onGoogleDriveBackup: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onGoogleDriveRestore: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onRefreshBackupInfo: () -> Unit
) {
    // Dialog states
    var showBackupOptionsDialog by remember { mutableStateOf(false) }
    var showRestoreOptionsDialog by remember { mutableStateOf(false) }
    var showSignOutConfirmDialog by remember { mutableStateOf(false) }

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
    val isSignedIn = gdAccountEmail.isNotEmpty() && gdAccountEmail != "-"
    
    SettingsSection(title = "Google Drive Backup") {
        // Account row
        if (isSignedIn) {
            SettingsItem(
                title = "Account",
                description = gdAccountEmail,
                icon = Icons.Default.AccountCircle,
                onClick = null
            )
        } else {
            SettingsItem(
                title = "Sign in to Google",
                description = "Connect your Google account for cloud backup",
                icon = Icons.Default.Cloud,
                onClick = onGoogleSignIn
            )
        }
        
        // Backup action
        if (isSignedIn) {
            SettingsItem(
                title = "Backup to Google Drive",
                description = "Upload your library backup to the cloud",
                icon = Icons.Default.CloudUpload,
                onClick = { showBackupOptionsDialog = true }
            )
        }
        
        // Restore action
        if (isSignedIn) {
            SettingsItem(
                title = "Restore from Google Drive",
                description = "Download and restore your cloud backup",
                icon = Icons.Default.CloudDownload,
                onClick = { showRestoreOptionsDialog = true }
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
        
        // Backup info
        if (isSignedIn) {
            SettingsItem(
                title = "Backup Info",
                description = if (lastCloudBackupTimestamp.isNotEmpty()) lastCloudBackupTimestamp else "Tap to refresh",
                icon = Icons.Default.Info,
                onClick = onRefreshBackupInfo
            )
        }
        
        // Sign out
        if (isSignedIn) {
            SettingsItem(
                title = "Sign Out",
                description = "Disconnect your Google account",
                icon = Icons.Default.Logout,
                onClick = { showSignOutConfirmDialog = true }
            )
        }
    }
    
    // Backup options dialog
    if (showBackupOptionsDialog) {
        BackupRestoreOptionsDialog(
            title = "Backup to Google Drive",
            confirmLabel = "Backup",
            onDismiss = { showBackupOptionsDialog = false },
            onConfirm = { simpleText, database, preferences, files ->
                showBackupOptionsDialog = false
                onGoogleDriveBackup(simpleText, database, preferences, files)
            }
        )
    }
    
    // Restore options dialog
    if (showRestoreOptionsDialog) {
        BackupRestoreOptionsDialog(
            title = "Restore from Google Drive",
            confirmLabel = "Restore",
            warningMessage = "This will overwrite your current data with the backup. Are you sure?",
            onDismiss = { showRestoreOptionsDialog = false },
            onConfirm = { simpleText, database, preferences, files ->
                showRestoreOptionsDialog = false
                onGoogleDriveRestore(simpleText, database, preferences, files)
            }
        )
    }
    
    // Sign out confirmation dialog
    if (showSignOutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirmDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to disconnect your Google account? Automatic backups will stop.") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirmDialog = false
                    onGoogleSignOut()
                }) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Dialog for selecting backup/restore options (simple text, database, preferences, files).
 */
@Composable
private fun BackupRestoreOptionsDialog(
    title: String,
    confirmLabel: String,
    warningMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean) -> Unit
) {
    var simpleText by remember { mutableStateOf(true) }
    var database by remember { mutableStateOf(true) }
    var preferences by remember { mutableStateOf(true) }
    var files by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (warningMessage != null) {
                    Text(
                        text = warningMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = simpleText, onCheckedChange = { simpleText = it })
                    Text("Simple Text", modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = database, onCheckedChange = { database = it })
                    Text("Database", modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = preferences, onCheckedChange = { preferences = it })
                    Text("Preferences", modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = files, onCheckedChange = { files = it })
                    Text("Files", modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(simpleText, database, preferences, files) },
                enabled = simpleText || database || preferences || files
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    NovelLibraryBaseTheme {
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
    NovelLibraryBaseTheme(darkTheme = true) {
        BackupAndSyncScreen(
            backupViewModel = createPreviewBackupViewModel(),
            syncViewModel = createPreviewSyncViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Backup and Sync Screen - Backup Tab", showBackground = true)
@Composable
private fun PreviewBackupAndSyncScreenBackupTab() {
    NovelLibraryBaseTheme {
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
    NovelLibraryBaseTheme {
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
    NovelLibraryBaseTheme(darkTheme = true) {
        BackupAndSyncScreen(
            backupViewModel = createPreviewBackupViewModel(),
            syncViewModel = createPreviewSyncViewModel(),
            onNavigateBack = {}
        )
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
