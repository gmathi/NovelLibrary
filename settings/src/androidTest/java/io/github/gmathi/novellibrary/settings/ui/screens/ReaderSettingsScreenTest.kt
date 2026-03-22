package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for ReaderSettingsScreen.
 * 
 * Tests:
 * - Verify 4 sections are displayed (Text & Display, Theme, Scroll Behavior, Auto-Scroll)
 * - Test sliders (text size, volume scroll distance, auto-scroll settings)
 * - Test switches (limit image width, keep text color, volume key navigation, etc.)
 * - Test dropdowns (font selection)
 * - Verify state updates
 */
class ReaderSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): ReaderSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return ReaderSettingsViewModel(repository)
    }

    @Test
    fun readerSettingsScreen_displaysFourSections() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Verify all 4 sections are displayed
        composeTestRule.onNodeWithText("Text & Display").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scroll Behavior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto-Scroll").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_displaysTextSizeSlider() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNode(hasText("Text Size") and hasClickAction()).assertExists()
    }

    @Test
    fun readerSettingsScreen_textSizeSlider_updatesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Change text size
        val initialTextSize = runBlocking { viewModel.textSize.first() }
        viewModel.setTextSize(24)
        composeTestRule.waitForIdle()

        // Then
        val updatedTextSize = runBlocking { viewModel.textSize.first() }
        assert(updatedTextSize == 24) { "Text size should be updated to 24" }
        assert(updatedTextSize != initialTextSize) { "Text size should have changed" }
    }

    @Test
    fun readerSettingsScreen_displaysFontDropdown() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
        composeTestRule.onNodeWithText("System Default").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_displaysLimitImageWidthSwitch() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Limit Image Width").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prevent images from exceeding screen width").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_limitImageWidthSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.limitImageWidth.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Limit Image Width").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.limitImageWidth.first() }
        assert(updatedState != initialState) { "Limit image width should toggle" }
    }

    @Test
    fun readerSettingsScreen_displaysThemeSettings() {
        // Given
        val viewModel = createViewModel()

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
    fun readerSettingsScreen_keepTextColorSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.keepTextColor.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Keep Text Color").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.keepTextColor.first() }
        assert(updatedState != initialState) { "Keep text color should toggle" }
    }

    @Test
    fun readerSettingsScreen_displaysScrollBehaviorSettings() {
        // Given
        val viewModel = createViewModel()

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
    fun readerSettingsScreen_volumeKeyNavigationSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.enableVolumeScroll.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Volume Key Navigation").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.enableVolumeScroll.first() }
        assert(updatedState != initialState) { "Volume key navigation should toggle" }
    }

    @Test
    fun readerSettingsScreen_volumeScrollDistance_showsWhenEnabled() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Enable volume scroll
        viewModel.setEnableVolumeScroll(true)
        composeTestRule.waitForIdle()

        // Then - Volume scroll distance should be visible
        composeTestRule.onNodeWithText("Volume Scroll Distance").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_displaysAutoScrollSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Enable Auto-Scroll").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_autoScrollSettings_showWhenEnabled() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Enable auto-scroll
        viewModel.setEnableAutoScroll(true)
        composeTestRule.waitForIdle()

        // Then - Auto-scroll settings should be visible
        composeTestRule.onNodeWithText("Scroll Distance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scroll Interval").assertIsDisplayed()
    }

    @Test
    fun readerSettingsScreen_navigatesBack() {
        // Given
        val viewModel = createViewModel()
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            ReaderSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }
}
