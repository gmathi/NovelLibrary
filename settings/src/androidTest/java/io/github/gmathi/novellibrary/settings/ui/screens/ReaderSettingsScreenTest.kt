package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.SettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for ReaderSettingsScreen.
 * 
 * Tests the reader settings screen UI including:
 * - Section rendering (Text & Display, Theme, Scroll Behavior, Auto-Scroll)
 * - Settings controls (sliders, switches, dropdowns)
 * - State updates and user interactions
 * - Conditional rendering (volume scroll distance, auto-scroll settings)
 */
class ReaderSettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private fun createMockViewModel(
        textSize: Int = 16,
        enableVolumeScroll: Boolean = false,
        volumeScrollLength: Int = 100,
        enableAutoScroll: Boolean = false,
        autoScrollLength: Int = 100,
        autoScrollInterval: Int = 1000,
        limitImageWidth: Boolean = true,
        keepTextColor: Boolean = false,
        alternativeTextColors: Boolean = false,
        readerMode: Boolean = true,
        japSwipe: Boolean = false,
        showReaderScroll: Boolean = true,
        keepScreenOn: Boolean = false,
        enableImmersiveMode: Boolean = false
    ): ReaderSettingsViewModel {
        val mockDataStore = object : SettingsDataStore {
            override val textSize = MutableStateFlow(textSize)
            override val fontPath = MutableStateFlow("")
            override val limitImageWidth = MutableStateFlow(limitImageWidth)
            override val dayModeBackgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
            override val nightModeBackgroundColor = MutableStateFlow(0xFF000000.toInt())
            override val dayModeTextColor = MutableStateFlow(0xFF000000.toInt())
            override val nightModeTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
            override val keepTextColor = MutableStateFlow(keepTextColor)
            override val alternativeTextColors = MutableStateFlow(alternativeTextColors)
            override val readerMode = MutableStateFlow(readerMode)
            override val japSwipe = MutableStateFlow(japSwipe)
            override val showReaderScroll = MutableStateFlow(showReaderScroll)
            override val enableVolumeScroll = MutableStateFlow(enableVolumeScroll)
            override val volumeScrollLength = MutableStateFlow(volumeScrollLength)
            override val keepScreenOn = MutableStateFlow(keepScreenOn)
            override val enableImmersiveMode = MutableStateFlow(enableImmersiveMode)
            override val showNavbarAtChapterEnd = MutableStateFlow(true)
            override val enableAutoScroll = MutableStateFlow(enableAutoScroll)
            override val autoScrollLength = MutableStateFlow(autoScrollLength)
            override val autoScrollInterval = MutableStateFlow(autoScrollInterval)
            override val showChapterComments = MutableStateFlow(true)
            override val enableClusterPages = MutableStateFlow(false)
            override val enableDirectionalLinks = MutableStateFlow(true)
            override val isReaderModeButtonVisible = MutableStateFlow(true)
            
            // Other required properties
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
            
            override suspend fun setTextSize(size: Int) { this.textSize.value = size }
            override suspend fun setFontPath(path: String) {}
            override suspend fun setLimitImageWidth(enabled: Boolean) { this.limitImageWidth.value = enabled }
            override suspend fun setDayModeBackgroundColor(color: Int) {}
            override suspend fun setNightModeBackgroundColor(color: Int) {}
            override suspend fun setDayModeTextColor(color: Int) {}
            override suspend fun setNightModeTextColor(color: Int) {}
            override suspend fun setKeepTextColor(enabled: Boolean) { this.keepTextColor.value = enabled }
            override suspend fun setAlternativeTextColors(enabled: Boolean) { this.alternativeTextColors.value = enabled }
            override suspend fun setReaderMode(enabled: Boolean) { this.readerMode.value = enabled }
            override suspend fun setJapSwipe(enabled: Boolean) { this.japSwipe.value = enabled }
            override suspend fun setShowReaderScroll(enabled: Boolean) { this.showReaderScroll.value = enabled }
            override suspend fun setEnableVolumeScroll(enabled: Boolean) { this.enableVolumeScroll.value = enabled }
            override suspend fun setVolumeScrollLength(length: Int) { this.volumeScrollLength.value = length }
            override suspend fun setKeepScreenOn(enabled: Boolean) { this.keepScreenOn.value = enabled }
            override suspend fun setEnableImmersiveMode(enabled: Boolean) { this.enableImmersiveMode.value = enabled }
            override suspend fun setShowNavbarAtChapterEnd(enabled: Boolean) {}
            override suspend fun setEnableAutoScroll(enabled: Boolean) { this.enableAutoScroll.value = enabled }
            override suspend fun setAutoScrollLength(length: Int) { this.autoScrollLength.value = length }
            override suspend fun setAutoScrollInterval(interval: Int) { this.autoScrollInterval.value = interval }
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
        
        val mockRepository = SettingsRepositoryDataStore(mockDataStore)
        return ReaderSettingsViewModel(mockRepository)
    }
    
    @Test
    fun readerSettingsScreen_displaysAllSections() {
        // Given
        val viewModel = createMockViewModel()
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then - verify all 4 sections are displayed
        composeTestRule.onNodeWithText("Text & Display").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scroll Behavior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto-Scroll").assertIsDisplayed()
    }
    
    @Test
    fun textAndDisplaySection_displaysAllSettings() {
        // Given
        val viewModel = createMockViewModel(textSize = 18)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
        composeTestRule.onNodeWithText("Limit Image Width").assertIsDisplayed()
    }
    
    @Test
    fun themeSection_displaysAllSettings() {
        // Given
        val viewModel = createMockViewModel()
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Day Mode Background").assertIsDisplayed()
        composeTestRule.onNodeWithText("Night Mode Background").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Text Color").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alternative Text Colors").assertIsDisplayed()
    }
    
    @Test
    fun scrollBehaviorSection_displaysAllSettings() {
        // Given
        val viewModel = createMockViewModel()
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Reader Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Japanese Swipe Direction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Scroll Indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume Key Navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Screen On").assertIsDisplayed()
        composeTestRule.onNodeWithText("Immersive Mode").assertIsDisplayed()
    }
    
    @Test
    fun volumeScrollDistance_hiddenWhenVolumeScrollDisabled() {
        // Given
        val viewModel = createMockViewModel(enableVolumeScroll = false)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Volume Scroll Distance").assertDoesNotExist()
    }
    
    @Test
    fun volumeScrollDistance_shownWhenVolumeScrollEnabled() {
        // Given
        val viewModel = createMockViewModel(enableVolumeScroll = true)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Volume Scroll Distance").assertIsDisplayed()
    }
    
    @Test
    fun autoScrollSettings_hiddenWhenAutoScrollDisabled() {
        // Given
        val viewModel = createMockViewModel(enableAutoScroll = false)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Scroll Distance").assertDoesNotExist()
        composeTestRule.onNodeWithText("Scroll Interval").assertDoesNotExist()
    }
    
    @Test
    fun autoScrollSettings_shownWhenAutoScrollEnabled() {
        // Given
        val viewModel = createMockViewModel(enableAutoScroll = true)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Scroll Distance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scroll Interval").assertIsDisplayed()
    }
    
    @Test
    fun switchSettings_canBeToggled() {
        // Given
        val viewModel = createMockViewModel(limitImageWidth = false)
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then - find and click the switch
        composeTestRule.onNodeWithText("Limit Image Width").performClick()
        
        // Note: In a real test, we would verify the state changed in the ViewModel
        // For now, we just verify the interaction is possible
    }
    
    @Test
    fun backButton_triggersNavigateBack() {
        // Given
        val viewModel = createMockViewModel()
        var navigateBackCalled = false
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then - click back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Verify callback was invoked
        assert(navigateBackCalled)
    }
    
    @Test
    fun screenTitle_isDisplayed() {
        // Given
        val viewModel = createMockViewModel()
        
        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Reader Settings").assertIsDisplayed()
    }
}
