package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.SettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for BackupAndSyncScreen.
 * 
 * Tests the tabbed interface, backup settings, sync settings, and user interactions.
 */
class BackupAndSyncScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private fun createMockDataStore(): SettingsDataStore {
        return object : SettingsDataStore {
            override val showBackupHint = MutableStateFlow(true)
            override val showRestoreHint = MutableStateFlow(true)
            override val backupFrequency = MutableStateFlow(24)
            override val lastBackup = MutableStateFlow(System.currentTimeMillis())
            override val lastLocalBackupTimestamp = MutableStateFlow("Today at 10:30 AM")
            override val lastCloudBackupTimestamp = MutableStateFlow("Yesterday at 8:00 PM")
            override val lastBackupSize = MutableStateFlow("2.5 MB")
            override val gdBackupInterval = MutableStateFlow("daily")
            override val gdAccountEmail = MutableStateFlow("user@example.com")
            override val gdInternetType = MutableStateFlow("wifi")
            
            // Implement other required properties with default values
            override val textSize = MutableStateFlow(16)
            override val fontPath = MutableStateFlow("")
            override val limitImageWidth = MutableStateFlow(true)
            override val dayModeBackgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
            override val nightModeBackgroundColor = MutableStateFlow(0xFF000000.toInt())
            override val dayModeTextColor = MutableStateFlow(0xFF000000.toInt())
            override val nightModeTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
            override val keepTextColor = MutableStateFlow(false)
            override val alternativeTextColors = MutableStateFlow(false)
            override val readerMode = MutableStateFlow(true)
            override val japSwipe = MutableStateFlow(false)
            override val showReaderScroll = MutableStateFlow(true)
            override val enableVolumeScroll = MutableStateFlow(true)
            override val volumeScrollLength = MutableStateFlow(100)
            override val keepScreenOn = MutableStateFlow(false)
            override val enableImmersiveMode = MutableStateFlow(false)
            override val showNavbarAtChapterEnd = MutableStateFlow(true)
            override val enableAutoScroll = MutableStateFlow(false)
            override val autoScrollLength = MutableStateFlow(100)
            override val autoScrollInterval = MutableStateFlow(1000)
            override val showChapterComments = MutableStateFlow(true)
            override val enableClusterPages = MutableStateFlow(false)
            override val enableDirectionalLinks = MutableStateFlow(true)
            override val isReaderModeButtonVisible = MutableStateFlow(true)
            override val appLanguage = MutableStateFlow("en")
            override val enableNotifications = MutableStateFlow(true)
            override val enableMentionNotifications = MutableStateFlow(true)
            override val enableBackup = MutableStateFlow(false)
            override val backupLocation = MutableStateFlow("")
            override val autoBackupEnabled = MutableStateFlow(false)
            override val enableSync = MutableStateFlow(false)
            override val syncAccount = MutableStateFlow("")
            override val lastSyncTime = MutableStateFlow(0L)
            override val ttsEnabled = MutableStateFlow(false)
            override val ttsVoice = MutableStateFlow("")
            override val ttsSpeed = MutableStateFlow(1.0f)
            override val cloudflareBypassEnabled = MutableStateFlow(false)
            override val debugLoggingEnabled = MutableStateFlow(false)
            
            override suspend fun setShowBackupHint(show: Boolean) {}
            override suspend fun setShowRestoreHint(show: Boolean) {}
            override suspend fun setBackupFrequency(hours: Int) {}
            override suspend fun setLastBackup(timestamp: Long) {}
            override suspend fun setLastLocalBackupTimestamp(timestamp: String) {}
            override suspend fun setLastCloudBackupTimestamp(timestamp: String) {}
            override suspend fun setLastBackupSize(size: String) {}
            override suspend fun setGdBackupInterval(interval: String) {}
            override suspend fun setGdAccountEmail(email: String) {}
            override suspend fun setGdInternetType(type: String) {}
            override suspend fun setTextSize(size: Int) {}
            override suspend fun setFontPath(path: String) {}
            override suspend fun setLimitImageWidth(enabled: Boolean) {}
            override suspend fun setDayModeBackgroundColor(color: Int) {}
            override suspend fun setNightModeBackgroundColor(color: Int) {}
            override suspend fun setDayModeTextColor(color: Int) {}
            override suspend fun setNightModeTextColor(color: Int) {}
            override suspend fun setKeepTextColor(enabled: Boolean) {}
            override suspend fun setAlternativeTextColors(enabled: Boolean) {}
            override suspend fun setReaderMode(enabled: Boolean) {}
            override suspend fun setJapSwipe(enabled: Boolean) {}
            override suspend fun setShowReaderScroll(enabled: Boolean) {}
            override suspend fun setEnableVolumeScroll(enabled: Boolean) {}
            override suspend fun setVolumeScrollLength(length: Int) {}
            override suspend fun setKeepScreenOn(enabled: Boolean) {}
            override suspend fun setEnableImmersiveMode(enabled: Boolean) {}
            override suspend fun setShowNavbarAtChapterEnd(enabled: Boolean) {}
            override suspend fun setEnableAutoScroll(enabled: Boolean) {}
            override suspend fun setAutoScrollLength(length: Int) {}
            override suspend fun setAutoScrollInterval(interval: Int) {}
            override suspend fun setShowChapterComments(enabled: Boolean) {}
            override suspend fun setEnableClusterPages(enabled: Boolean) {}
            override suspend fun setEnableDirectionalLinks(enabled: Boolean) {}
            override suspend fun setIsReaderModeButtonVisible(enabled: Boolean) {}
            override suspend fun setAppLanguage(language: String) {}
            override suspend fun setEnableNotifications(enabled: Boolean) {}
            override suspend fun setEnableMentionNotifications(enabled: Boolean) {}
            override suspend fun setEnableBackup(enabled: Boolean) {}
            override suspend fun setBackupLocation(location: String) {}
            override suspend fun setAutoBackupEnabled(enabled: Boolean) {}
            override suspend fun setEnableSync(enabled: Boolean) {}
            override suspend fun setSyncAccount(account: String) {}
            override suspend fun setLastSyncTime(time: Long) {}
            override suspend fun setTtsEnabled(enabled: Boolean) {}
            override suspend fun setTtsVoice(voice: String) {}
            override suspend fun setTtsSpeed(speed: Float) {}
            override suspend fun setCloudflareBypassEnabled(enabled: Boolean) {}
            override suspend fun setDebugLoggingEnabled(enabled: Boolean) {}
        }
    }
    
    @Test
    fun backupAndSyncScreen_displaysTitle() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Backup & Sync").assertExists()
    }
    
    @Test
    fun backupAndSyncScreen_displaysTwoTabs() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Backup").assertExists()
        composeTestRule.onNodeWithText("Sync").assertExists()
    }
    
    @Test
    fun backupTab_displaysLocalBackupSection() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        // Backup tab should be selected by default
        composeTestRule.onNodeWithText("Local Backup").assertExists()
        composeTestRule.onNodeWithText("Create Backup").assertExists()
        composeTestRule.onNodeWithText("Restore Backup").assertExists()
    }
    
    @Test
    fun backupTab_displaysGoogleDriveSection() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Google Drive Backup").assertExists()
        composeTestRule.onNodeWithText("Account").assertExists()
        composeTestRule.onNodeWithText("user@example.com").assertExists()
        composeTestRule.onNodeWithText("Backup Interval").assertExists()
        composeTestRule.onNodeWithText("Network Type").assertExists()
    }
    
    @Test
    fun backupTab_displaysLastBackupTimestamp() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Last Backup").assertExists()
        composeTestRule.onNodeWithText("Today at 10:30 AM • 2.5 MB").assertExists()
        composeTestRule.onNodeWithText("Last Cloud Backup").assertExists()
        composeTestRule.onNodeWithText("Yesterday at 8:00 PM").assertExists()
    }
    
    @Test
    fun syncTab_displaysSyncConfiguration() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        // Switch to Sync tab
        composeTestRule.onNodeWithText("Sync").performClick()
        
        composeTestRule.onNodeWithText("Sync Configuration").assertExists()
        composeTestRule.onNodeWithText("Enable Sync").assertExists()
        composeTestRule.onNodeWithText("Login to Sync").assertExists()
    }
    
    @Test
    fun syncTab_showsSyncSettingsWhenEnabled() {
        val mockDataStore = createMockDataStore()
        mockDataStore.enableSync.value = true
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        // Switch to Sync tab
        composeTestRule.onNodeWithText("Sync").performClick()
        
        // When sync is enabled, should show "What to Sync" section
        composeTestRule.onNodeWithText("What to Sync").assertExists()
        composeTestRule.onNodeWithText("Sync Added Novels").assertExists()
        composeTestRule.onNodeWithText("Sync Deleted Novels").assertExists()
        composeTestRule.onNodeWithText("Sync Bookmarks").assertExists()
    }
    
    @Test
    fun syncTab_hidesSyncSettingsWhenDisabled() {
        val mockDataStore = createMockDataStore()
        mockDataStore.enableSync.value = false
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        // Switch to Sync tab
        composeTestRule.onNodeWithText("Sync").performClick()
        
        // When sync is disabled, should NOT show "What to Sync" section
        composeTestRule.onNodeWithText("What to Sync").assertDoesNotExist()
        composeTestRule.onNodeWithText("Sync Added Novels").assertDoesNotExist()
    }
    
    @Test
    fun tabNavigation_switchesBetweenTabs() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {}
            )
        }
        
        // Initially on Backup tab
        composeTestRule.onNodeWithText("Local Backup").assertExists()
        
        // Switch to Sync tab
        composeTestRule.onNodeWithText("Sync").performClick()
        composeTestRule.onNodeWithText("Sync Configuration").assertExists()
        composeTestRule.onNodeWithText("Local Backup").assertDoesNotExist()
        
        // Switch back to Backup tab
        composeTestRule.onNodeWithText("Backup").performClick()
        composeTestRule.onNodeWithText("Local Backup").assertExists()
        composeTestRule.onNodeWithText("Sync Configuration").assertDoesNotExist()
    }
    
    @Test
    fun backupTab_createBackupButtonClickable() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        var createBackupClicked = false
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onCreateBackup = { createBackupClicked = true }
            )
        }
        
        composeTestRule.onNodeWithText("Create Backup").performClick()
        assert(createBackupClicked)
    }
    
    @Test
    fun backupTab_restoreBackupButtonClickable() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        var restoreBackupClicked = false
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onRestoreBackup = { restoreBackupClicked = true }
            )
        }
        
        composeTestRule.onNodeWithText("Restore Backup").performClick()
        assert(restoreBackupClicked)
    }
    
    @Test
    fun syncTab_loginButtonClickable() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val backupViewModel = BackupSettingsViewModel(mockRepository)
        val syncViewModel = SyncSettingsViewModel(mockRepository)
        var loginClicked = false
        
        composeTestRule.setContent {
            BackupAndSyncScreen(
                backupViewModel = backupViewModel,
                syncViewModel = syncViewModel,
                onNavigateBack = {},
                onSyncLogin = { loginClicked = true }
            )
        }
        
        // Switch to Sync tab
        composeTestRule.onNodeWithText("Sync").performClick()
        
        composeTestRule.onNodeWithText("Login to Sync").performClick()
        assert(loginClicked)
    }
}
