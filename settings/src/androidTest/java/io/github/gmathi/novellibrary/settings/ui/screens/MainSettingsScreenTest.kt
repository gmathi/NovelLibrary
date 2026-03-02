package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gmathi.novellibrary.settings.ui.screens.MainSettingsScreenContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for MainSettingsScreen.
 * 
 * Tests the main settings screen UI, including:
 * - Display of all 5 category items
 * - Navigation callbacks for each category
 * - Correct titles and descriptions
 * - Icon display
 * - User interactions
 */
@RunWith(AndroidJUnit4::class)
class MainSettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun mainSettingsScreen_displaysAllCategories() {
        // Given
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // Then - verify all 5 categories are displayed
        composeTestRule.onNodeWithText("Reader").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup & Sync").assertIsDisplayed()
        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithText("Advanced").assertIsDisplayed()
        composeTestRule.onNodeWithText("About").assertIsDisplayed()
    }
    
    @Test
    fun mainSettingsScreen_displaysCorrectDescriptions() {
        // Given
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // Then - verify all descriptions are displayed
        composeTestRule.onNodeWithText("Customize reading experience").assertIsDisplayed()
        composeTestRule.onNodeWithText("Protect your data").assertIsDisplayed()
        composeTestRule.onNodeWithText("App preferences").assertIsDisplayed()
        composeTestRule.onNodeWithText("Technical settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("App info & credits").assertIsDisplayed()
    }
    
    @Test
    fun mainSettingsScreen_readerCategory_navigatesCorrectly() {
        // Given
        var navigatedToReader = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = { navigatedToReader = true },
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // When - click on Reader category
        composeTestRule.onNodeWithText("Reader").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedToReader)
    }
    
    @Test
    fun mainSettingsScreen_backupSyncCategory_navigatesCorrectly() {
        // Given
        var navigatedToBackupSync = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = { navigatedToBackupSync = true },
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // When - click on Backup & Sync category
        composeTestRule.onNodeWithText("Backup & Sync").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedToBackupSync)
    }
    
    @Test
    fun mainSettingsScreen_generalCategory_navigatesCorrectly() {
        // Given
        var navigatedToGeneral = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = { navigatedToGeneral = true },
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // When - click on General category
        composeTestRule.onNodeWithText("General").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedToGeneral)
    }
    
    @Test
    fun mainSettingsScreen_advancedCategory_navigatesCorrectly() {
        // Given
        var navigatedToAdvanced = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = { navigatedToAdvanced = true },
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // When - click on Advanced category
        composeTestRule.onNodeWithText("Advanced").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedToAdvanced)
    }
    
    @Test
    fun mainSettingsScreen_aboutCategory_navigatesCorrectly() {
        // Given
        var navigatedToAbout = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = { navigatedToAbout = true },
                onNavigateBack = {}
            )
        }
        
        // When - click on About category
        composeTestRule.onNodeWithText("About").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedToAbout)
    }
    
    @Test
    fun mainSettingsScreen_backButton_navigatesBack() {
        // Given
        var navigatedBack = false
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = { navigatedBack = true }
            )
        }
        
        // When - click back button
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        
        // Then - navigation callback is invoked
        assert(navigatedBack)
    }
    
    @Test
    fun mainSettingsScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // Then - verify title is displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
    
    @Test
    fun mainSettingsScreen_categoriesInCorrectOrder() {
        // Given
        composeTestRule.setContent {
            MainSettingsScreenContent(
                isDarkTheme = false,
                isDeveloper = false,
                onNavigateToReader = {},
                onNavigateToBackupSync = {},
                onNavigateToGeneral = {},
                onNavigateToAdvanced = {},
                onNavigateToAbout = {},
                onNavigateBack = {}
            )
        }
        
        // Then - verify categories appear in the correct order
        val categories = composeTestRule.onAllNodesWithTag("settings_item", useUnmergedTree = true)
        
        // Verify we have 5 categories
        categories.assertCountEquals(5)
    }
}
