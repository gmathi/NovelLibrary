package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for GeneralSettingsScreen.
 * 
 * Tests:
 * - Verify inline language selection (reduces navigation depth)
 * - Test notification switches
 * - Verify theme selection
 * - Test other general settings switches
 */
class GeneralSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): GeneralSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return GeneralSettingsViewModel(repository)
    }

    @Test
    fun generalSettingsScreen_displaysFourSections() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Verify all 4 sections are displayed
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other Settings").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_displaysDarkThemeSwitch() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Dark Theme").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_darkThemeSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.isDarkTheme.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Dark Theme").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.isDarkTheme.first() }
        assert(updatedState != initialState) { "Dark theme should toggle" }
    }

    @Test
    fun generalSettingsScreen_displaysInlineLanguageSelection() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Language dropdown should be inline (not a separate screen)
        composeTestRule.onNodeWithText("App Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_languageDropdown_isClickable() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Language setting should be clickable
        composeTestRule.onNode(
            hasText("App Language") and hasClickAction()
        ).assertExists()
    }

    @Test
    fun generalSettingsScreen_displaysNotificationSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show Chapters Left Badge").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_enableNotificationsSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.enableNotifications.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Enable Notifications").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.enableNotifications.first() }
        assert(updatedState != initialState) { "Enable notifications should toggle" }
    }

    @Test
    fun generalSettingsScreen_showChaptersLeftBadgeSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.showChaptersLeftBadge.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Show Chapters Left Badge").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.showChaptersLeftBadge.first() }
        assert(updatedState != initialState) { "Show chapters left badge should toggle" }
    }

    @Test
    fun generalSettingsScreen_displaysOtherSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Disable JavaScript").assertIsDisplayed()
        composeTestRule.onNodeWithText("Load Library on Startup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developer Mode").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_disableJavaScriptSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.javascriptDisabled.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Disable JavaScript").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.javascriptDisabled.first() }
        assert(updatedState != initialState) { "JavaScript disabled should toggle" }
    }

    @Test
    fun generalSettingsScreen_loadLibraryScreenSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.loadLibraryScreen.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Load Library on Startup").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.loadLibraryScreen.first() }
        assert(updatedState != initialState) { "Load library screen should toggle" }
    }

    @Test
    fun generalSettingsScreen_developerModeSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.isDeveloper.first() }

        // Toggle switch
        composeTestRule.onNodeWithText("Developer Mode").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.isDeveloper.first() }
        assert(updatedState != initialState) { "Developer mode should toggle" }
    }

    @Test
    fun generalSettingsScreen_verifyThemeDescriptionUpdates() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Set dark theme to false
        viewModel.setDarkTheme(false)
        composeTestRule.waitForIdle()

        // Then - Should show light theme description
        composeTestRule.onNodeWithText("Using light theme").assertIsDisplayed()

        // Set dark theme to true
        viewModel.setDarkTheme(true)
        composeTestRule.waitForIdle()

        // Then - Should show dark theme description
        composeTestRule.onNodeWithText("Using dark theme").assertIsDisplayed()
    }

    @Test
    fun generalSettingsScreen_navigatesBack() {
        // Given
        val viewModel = createViewModel()
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            GeneralSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }
}
