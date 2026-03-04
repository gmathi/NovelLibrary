package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for AdvancedSettingsScreen.
 * 
 * Tests rendering, user interactions, and state changes for the advanced settings screen.
 * Verifies all four sections (Network, Cache, Debug, Data) are displayed correctly.
 */
class AdvancedSettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var fakeDataStore: FakeSettingsDataStore
    private lateinit var repository: SettingsRepositoryDataStore
    private lateinit var viewModel: AdvancedSettingsViewModel
    
    private var navigateBackCalled = false
    private var clearCacheCalled = false
    private var resetSettingsCalled = false
    private var cloudflareBypassCalled = false
    
    @Before
    fun setup() {
        fakeDataStore = FakeSettingsDataStore()
        repository = SettingsRepositoryDataStore(fakeDataStore)
        viewModel = AdvancedSettingsViewModel(repository)
        
        navigateBackCalled = false
        clearCacheCalled = false
        resetSettingsCalled = false
        cloudflareBypassCalled = false
    }
    
    //region Screen Rendering Tests
    
    @Test
    fun advancedSettingsScreen_displaysTitle() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Advanced Settings").assertIsDisplayed()
    }
    
    @Test
    fun advancedSettingsScreen_displaysAllSections() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then - Verify all four sections are displayed
        composeTestRule.onNodeWithText("Network").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cache").assertIsDisplayed()
        composeTestRule.onNodeWithText("Debug").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()
    }
    
    //endregion
    
    //region Network Section Tests
    
    @Test
    fun networkSection_displaysCloudflareBypass() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Cloudflare Bypass").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure Cloudflare bypass for protected sources")
            .assertIsDisplayed()
    }
    
    @Test
    fun networkSection_displaysJavascriptDisabled() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Disable JavaScript").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disable JavaScript in web views (may break some sources)")
            .assertIsDisplayed()
    }
    
    @Test
    fun networkSection_displaysNetworkTimeout() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Network Timeout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure network request timeout settings")
            .assertIsDisplayed()
    }
    
    @Test
    fun networkSection_cloudflareBypassClick_triggersCallback() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true },
                onCloudflareBypass = { cloudflareBypassCalled = true }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Cloudflare Bypass").performClick()
        
        // Then
        assert(cloudflareBypassCalled)
    }
    
    @Test
    fun networkSection_javascriptDisabledToggle_updatesState() = runTest {
        // Given
        fakeDataStore.javascriptDisabled.value = false
        
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then - Initially off
        composeTestRule.onNodeWithText("Disable JavaScript")
            .assertIsDisplayed()
        
        // When - Toggle on
        composeTestRule.onNodeWithText("Disable JavaScript").performClick()
        
        // Then - State should update
        composeTestRule.waitForIdle()
        assert(fakeDataStore.javascriptDisabled.value)
    }
    
    //endregion
    
    //region Cache Section Tests
    
    @Test
    fun cacheSection_displaysClearCache() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Clear Cache").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear all cached data to free up storage")
            .assertIsDisplayed()
    }
    
    @Test
    fun cacheSection_displaysCacheManagement() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Cache Management").assertIsDisplayed()
        composeTestRule.onNodeWithText("View and manage cached content")
            .assertIsDisplayed()
    }
    
    @Test
    fun cacheSection_clearCacheClick_triggersCallback() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true },
                onClearCache = { clearCacheCalled = true }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Clear Cache").performClick()
        
        // Then
        assert(clearCacheCalled)
    }
    
    //endregion
    
    //region Debug Section Tests
    
    @Test
    fun debugSection_displaysDeveloperMode() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Developer Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable advanced developer options and debugging")
            .assertIsDisplayed()
    }
    
    @Test
    fun debugSection_displaysDebugLogging() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Debug Logging").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enable detailed logging for troubleshooting")
            .assertIsDisplayed()
    }
    
    @Test
    fun debugSection_developerModeToggle_updatesState() = runTest {
        // Given
        fakeDataStore.isDeveloper.value = false
        
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then - Initially off
        composeTestRule.onNodeWithText("Developer Mode")
            .assertIsDisplayed()
        
        // When - Toggle on
        composeTestRule.onNodeWithText("Developer Mode").performClick()
        
        // Then - State should update
        composeTestRule.waitForIdle()
        assert(fakeDataStore.isDeveloper.value)
    }
    
    //endregion
    
    //region Data Section Tests
    
    @Test
    fun dataSection_displaysDataMigrationTools() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Data Migration Tools").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import or export app data")
            .assertIsDisplayed()
    }
    
    @Test
    fun dataSection_displaysResetSettings() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Reset Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset all settings to default values")
            .assertIsDisplayed()
    }
    
    @Test
    fun dataSection_resetSettingsClick_triggersCallback() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true },
                onResetSettings = { resetSettingsCalled = true }
            )
        }
        
        // When
        composeTestRule.onNodeWithText("Reset Settings").performClick()
        
        // Then
        assert(resetSettingsCalled)
    }
    
    //endregion
    
    //region Navigation Tests
    
    @Test
    fun backButton_triggersNavigateBack() {
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // When - Click back button (top bar navigation icon)
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Then
        assert(navigateBackCalled)
    }
    
    //endregion
    
    //region State Persistence Tests
    
    @Test
    fun advancedSettingsScreen_preservesStateAcrossRecomposition() = runTest {
        // Given
        fakeDataStore.javascriptDisabled.value = true
        fakeDataStore.isDeveloper.value = true
        
        // When
        composeTestRule.setContent {
            AdvancedSettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigateBackCalled = true }
            )
        }
        
        // Then - Verify state is preserved
        composeTestRule.waitForIdle()
        assert(fakeDataStore.javascriptDisabled.value)
        assert(fakeDataStore.isDeveloper.value)
    }
    
    //endregion
}
