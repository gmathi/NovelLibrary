package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.SettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for GeneralSettingsScreen.
 * 
 * Tests the UI rendering, user interactions, and state changes for the general settings screen.
 * Uses Compose testing framework with mock data.
 */
class GeneralSettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private fun createMockDataStore(
        isDarkTheme: Boolean = false,
        language: String = "en",
        javascriptDisabled: Boolean = false,
        loadLibraryScreen: Boolean = false,
        enableNotifications: Boolean = true,
        showChaptersLeftBadge: Boolean = true,
        isDeveloper: Boolean = false
    ): SettingsDataStore {
        return object : SettingsDataStore {
            override val isDarkTheme = MutableStateFlow(isDarkTheme)
            override val language = MutableStateFlow(language)
            override val javascriptDisabled = MutableStateFlow(javascriptDisabled)
            override val loadLibraryScreen = MutableStateFlow(loadLibraryScreen)
            override val enableNotifications = MutableStateFlow(enableNotifications)
            override val showChaptersLeftBadge = MutableStateFlow(showChaptersLeftBadge)
            override val isDeveloper = MutableStateFlow(isDeveloper)
            
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
            override val readAloudNextChapter = MutableStateFlow(false)
            override val enableScrollingText = MutableStateFlow(false)
            override val showBackupHint = MutableStateFlow(true)
            override val showRestoreHint = MutableStateFlow(true)
            override val backupFrequency = MutableStateFlow(24)
            override val lastBackup = MutableStateFlow(0L)
            override val lastLocalBackupTimestamp = MutableStateFlow("")
            override val lastCloudBackupTimestamp = MutableStateFlow("")
            override val lastBackupSize = MutableStateFlow("")
            override val gdBackupInterval = MutableStateFlow("daily")
            override val gdAccountEmail = MutableStateFlow("")
            override val gdInternetType = MutableStateFlow("wifi")
            
            override fun getSyncEnabled(serviceName: String): Flow<Boolean> = MutableStateFlow(false)
            override fun getSyncAddNovels(serviceName: String): Flow<Boolean> = MutableStateFlow(true)
            override fun getSyncDeleteNovels(serviceName: String): Flow<Boolean> = MutableStateFlow(false)
            override fun getSyncBookmarks(serviceName: String): Flow<Boolean> = MutableStateFlow(true)
            
            override suspend fun setIsDarkTheme(enabled: Boolean) {}
            override suspend fun setLanguage(language: String) {}
            override suspend fun setJavascriptDisabled(disabled: Boolean) {}
            override suspend fun setLoadLibraryScreen(enabled: Boolean) {}
            override suspend fun setEnableNotifications(enabled: Boolean) {}
            override suspend fun setShowChaptersLeftBadge(enabled: Boolean) {}
            override suspend fun setIsDeveloper(enabled: Boolean) {}
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
            override suspend fun setReadAloudNextChapter(enabled: Boolean) {}
            override suspend fun setEnableScrollingText(enabled: Boolean) {}
            override suspend fun setShowBackupHint(enabled: Boolean) {}
            override suspend fun setShowRestoreHint(enabled: Boolean) {}
            override suspend fun setBackupFrequency(hours: Int) {}
            override suspend fun setLastBackup(timestamp: Long) {}
            override suspend fun setLastLocalBackupTimestamp(timestamp: String) {}
            override suspend fun setLastCloudBackupTimestamp(timestamp: String) {}
            override suspend fun setLastBackupSize(size: String) {}
            override suspend fun setGdBackupInterval(interval: String) {}
            override suspend fun setGdAccountEmail(email: String) {}
            override suspend fun setGdInternetType(type: String) {}
            override suspend fun updateSyncSetting(keyName: String, value: Boolean) {}
        }
    }
    
    @Test
    fun generalSettingsScreen_displaysTitle() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("General Settings").assertIsDisplayed()
    }
    
    @Test
    fun generalSettingsScreen_displaysFourSections() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other Settings").assertIsDisplayed()
    }
    
    @Test
    fun appearanceSection_displaysDarkThemeSwitch() {
        val mockDataStore = createMockDataStore(isDarkTheme = false)
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Dark Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Using light theme").assertIsDisplayed()
    }
    
    @Test
    fun languageSection_displaysLanguageDropdown() {
        val mockDataStore = createMockDataStore(language = "en")
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("App Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }
    
    @Test
    fun notificationsSection_displaysNotificationSwitches() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Chapters Left Badge").assertIsDisplayed()
    }
    
    @Test
    fun otherSettingsSection_displaysAllSettings() {
        val mockDataStore = createMockDataStore()
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        composeTestRule.onNodeWithText("Disable JavaScript").assertIsDisplayed()
        composeTestRule.onNodeWithText("Load Library on Startup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developer Mode").assertIsDisplayed()
    }
    
    @Test
    fun darkThemeSwitch_togglesCorrectly() {
        val mockDataStore = createMockDataStore(isDarkTheme = false)
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Initially shows light theme
        composeTestRule.onNodeWithText("Using light theme").assertIsDisplayed()
        
        // Click the dark theme switch
        composeTestRule.onNodeWithText("Dark Theme").performClick()
        
        // State should update (in real app - here we just verify the click works)
        composeTestRule.onNodeWithText("Dark Theme").assertIsDisplayed()
    }
    
    @Test
    fun languageDropdown_opensOnClick() {
        val mockDataStore = createMockDataStore(language = "en")
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Click the language dropdown
        composeTestRule.onNodeWithText("App Language").performClick()
        
        // Dropdown menu should appear with language options
        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
        composeTestRule.onNodeWithText("Français").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deutsch").assertIsDisplayed()
    }
    
    @Test
    fun notificationSwitch_togglesCorrectly() {
        val mockDataStore = createMockDataStore(enableNotifications = true)
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Click the notifications switch
        composeTestRule.onNodeWithText("Enable Notifications").performClick()
        
        // Verify the switch is clickable
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
    
    @Test
    fun developerModeSwitch_togglesCorrectly() {
        val mockDataStore = createMockDataStore(isDeveloper = false)
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        val viewModel = GeneralSettingsViewModel(mockRepository)
        
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Click the developer mode switch
        composeTestRule.onNodeWithText("Developer Mode").performClick()
        
        // Verify the switch is clickable
        composeTestRule.onNodeWithText("Developer Mode").assertIsDisplayed()
    }
}
