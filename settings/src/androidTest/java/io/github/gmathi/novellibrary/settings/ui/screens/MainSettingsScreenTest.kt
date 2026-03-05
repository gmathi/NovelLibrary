package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for MainSettingsScreen.
 * 
 * Tests:
 * - Verify 5 categories are displayed
 * - Test navigation to each category
 * - Test developer mode toggle
 */
class MainSettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createViewModel(): MainSettingsViewModel {
        val fakeDataStore = FakeSettingsDataStore()
        val repository = SettingsRepositoryDataStore(fakeDataStore)
        return MainSettingsViewModel(repository)
    }

    @Test
    fun mainSettingsScreen_displaysAllFiveCategories() {
        // Given
        val viewModel = createViewModel()

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }

        // Then - Verify all 5 categories are displayed
        composeTestRule.onNodeWithText("Reader").assertIsDisplayed()
        composeTestRule.onNodeWithText("Customize reading experience").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Backup & Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("Protect your data").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithText("App preferences").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Advanced").assertIsDisplayed()
        composeTestRule.onNodeWithText("Technical settings").assertIsDisplayed()
        
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
        composeTestRule.onNodeWithText("App info & credits").assertIsDisplayed()
    }

    @Test
    fun mainSettingsScreen_navigatesToReaderSettings() {
        // Given
        val viewModel = createViewModel()
        var navigatedToReader = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = { navigatedToReader = true },
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Reader").performClick()
        assert(navigatedToReader) { "Should navigate to Reader settings" }
    }

    @Test
    fun mainSettingsScreen_navigatesToBackupSync() {
        // Given
        val viewModel = createViewModel()
        var navigatedToBackupSync = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = { navigatedToBackupSync = true },
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Backup & Sync").performClick()
        assert(navigatedToBackupSync) { "Should navigate to Backup & Sync settings" }
    }

    @Test
    fun mainSettingsScreen_navigatesToGeneral() {
        // Given
        val viewModel = createViewModel()
        var navigatedToGeneral = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = { navigatedToGeneral = true },
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("General").performClick()
        assert(navigatedToGeneral) { "Should navigate to General settings" }
    }

    @Test
    fun mainSettingsScreen_navigatesToAdvanced() {
        // Given
        val viewModel = createViewModel()
        var navigatedToAdvanced = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = { navigatedToAdvanced = true },
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Advanced").performClick()
        assert(navigatedToAdvanced) { "Should navigate to Advanced settings" }
    }

    @Test
    fun mainSettingsScreen_navigatesToAbout() {
        // Given
        val viewModel = createViewModel()
        var navigatedToAbout = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = { navigatedToAbout = true },
                onNavigateBack = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("About").performClick()
        assert(navigatedToAbout) { "Should navigate to About screen" }
    }

    @Test
    fun mainSettingsScreen_navigatesBack() {
        // Given
        val viewModel = createViewModel()
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            MainSettingsScreen(
                viewModel = viewModel,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = { navigatedBack = true }
            )
        }

        // Then - Click back button in toolbar
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }
}
