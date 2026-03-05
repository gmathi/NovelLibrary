package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for AdvancedSettingsScreen.
 * 
 * Tests:
 * - Verify technical settings are grouped into sections
 * - Test cache clear functionality
 * - Verify debug toggles (developer mode, debug logging)
 * - Test network settings (Cloudflare bypass, JavaScript)
 * - Test data management (reset settings)
 */
class AdvancedSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): AdvancedSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return AdvancedSettingsViewModel(repository)
    }

    @Test
    fun advancedSettingsScreen_displaysFourSections() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Verify all 4 sections are displayed
        composeTestRule.onNodeWithText("Network").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cache").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()
    }

    @Test
    fun advancedSettingsScreen_displaysNetworkSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Cloudflare Bypass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disable JavaScript").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network Timeout").assertIsDisplayed()
    }

    @Test
    fun advancedSettingsScreen_cloudflareBypass_isClickable() {
        // Given
        val viewModel = createViewModel()
        var cloudflareBypassCalled = false

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onCloudflareBypass = { cloudflareBypassCalled = true }
            )
        }

        // Click Cloudflare Bypass
        composeTestRule.onNodeWithText("Cloudflare Bypass").performClick()

        // Then
        assert(cloudflareBypassCalled) { "Cloudflare bypass callback should be called" }
    }

    @Test
    fun advancedSettingsScreen_disableJavaScriptSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
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
    fun advancedSettingsScreen_displaysCacheSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Clear Cache").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cache Management").assertIsDisplayed()
    }

    @Test
    fun advancedSettingsScreen_clearCache_triggersCallback() {
        // Given
        val viewModel = createViewModel()
        var clearCacheCalled = false

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onClearCache = { clearCacheCalled = true }
            )
        }

        // Click Clear Cache
        composeTestRule.onNodeWithText("Clear Cache").performClick()

        // Then
        assert(clearCacheCalled) { "Clear cache callback should be called" }
    }

    @Test
    fun advancedSettingsScreen_displaysDebugSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Developer Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug Logging").assertIsDisplayed()
    }

    @Test
    fun advancedSettingsScreen_developerModeSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
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
    fun advancedSettingsScreen_debugLoggingSwitch_togglesState() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Get initial state
        val initialState = runBlocking { viewModel.isDeveloper.first() }

        // Toggle switch (debug logging uses same state as developer mode for now)
        composeTestRule.onNodeWithText("Debug Logging").performClick()
        composeTestRule.waitForIdle()

        // Then
        val updatedState = runBlocking { viewModel.isDeveloper.first() }
        assert(updatedState != initialState) { "Debug logging should toggle" }
    }

    @Test
    fun advancedSettingsScreen_displaysDataSettings() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Data Migration Tools").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset Settings").assertIsDisplayed()
    }

    @Test
    fun advancedSettingsScreen_resetSettings_triggersCallback() {
        // Given
        val viewModel = createViewModel()
        var resetSettingsCalled = false

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {},
                onResetSettings = { resetSettingsCalled = true }
            )
        }

        // Click Reset Settings
        composeTestRule.onNodeWithText("Reset Settings").performClick()

        // Then
        assert(resetSettingsCalled) { "Reset settings callback should be called" }
    }

    @Test
    fun advancedSettingsScreen_verifyTechnicalSettingsGrouped() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {}
            )
        }

        // Then - Verify technical settings are properly grouped
        // Network section should contain Cloudflare and JavaScript
        composeTestRule.onNode(
            hasText("Cloudflare Bypass") and hasAnyAncestor(hasText("Network"))
        ).assertExists()
        
        composeTestRule.onNode(
            hasText("Disable JavaScript") and hasAnyAncestor(hasText("Network"))
        ).assertExists()

        // Cache section should contain cache-related settings
        composeTestRule.onNode(
            hasText("Clear Cache") and hasAnyAncestor(hasText("Cache"))
        ).assertExists()

        // Debug section should contain developer options
        composeTestRule.onNode(
            hasText("Developer Mode") and hasAnyAncestor(hasText("Debug"))
        ).assertExists()

        // Data section should contain data management
        composeTestRule.onNode(
            hasText("Reset Settings") and hasAnyAncestor(hasText("Data"))
        ).assertExists()
    }

    @Test
    fun advancedSettingsScreen_navigatesBack() {
        // Given
        val viewModel = createViewModel()
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }
}
