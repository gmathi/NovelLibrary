package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsDropdownTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsDropdown_displaysTitle() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
    }
    
    @Test
    fun settingsDropdown_displaysSelectedValue() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
    }
    
    @Test
    fun settingsDropdown_displaysDescription() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                description = "Choose app language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose app language").assertIsDisplayed()
    }
    
    @Test
    fun settingsDropdown_opensMenuOnClick() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        // Click to open dropdown
        composeTestRule.onNodeWithText("Language").performClick()
        
        // Verify all options are displayed
        composeTestRule.onNodeWithText("Spanish").assertIsDisplayed()
        composeTestRule.onNodeWithText("French").assertIsDisplayed()
    }
    
    @Test
    fun settingsDropdown_selectsOption() {
        var selectedValue = "English"
        
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = selectedValue,
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = { selectedValue = it }
            )
        }
        
        // Open dropdown
        composeTestRule.onNodeWithText("Language").performClick()
        
        // Select Spanish
        composeTestRule.onAllNodesWithText("Spanish")[1].performClick()
        
        assert(selectedValue == "Spanish")
    }
    
    @Test
    fun settingsDropdown_closesMenuAfterSelection() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        // Open dropdown
        composeTestRule.onNodeWithText("Language").performClick()
        
        // Select an option
        composeTestRule.onAllNodesWithText("Spanish")[1].performClick()
        
        // Menu should be closed - only one "Spanish" node should exist (the selected value display)
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Spanish").assertCountEquals(1)
    }
    
    @Test
    fun settingsDropdown_displaysIcon() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                icon = Icons.Default.Language,
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                onOptionSelected = {}
            )
        }
        
        composeTestRule.onNodeWithText("Language").assertIsDisplayed()
    }
    
    @Test
    fun settingsDropdown_disabledDoesNotOpenMenu() {
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = "English",
                options = listOf("English", "Spanish", "French"),
                enabled = false,
                onOptionSelected = {}
            )
        }
        
        // Try to click
        composeTestRule.onNodeWithText("Language").performClick()
        
        // Menu should not open - Spanish should not be visible
        composeTestRule.onAllNodesWithText("Spanish").assertCountEquals(0)
    }
    
    @Test
    fun settingsDropdown_usesCustomOptionLabel() {
        data class Language(val code: String, val name: String)
        
        composeTestRule.setContent {
            SettingsDropdown(
                title = "Language",
                selectedValue = Language("en", "English"),
                options = listOf(
                    Language("en", "English"),
                    Language("es", "Spanish"),
                    Language("fr", "French")
                ),
                onOptionSelected = {},
                optionLabel = { it.name }
            )
        }
        
        composeTestRule.onNodeWithText("English").assertIsDisplayed()
        
        // Open dropdown
        composeTestRule.onNodeWithText("Language").performClick()
        
        composeTestRule.onNodeWithText("Spanish").assertIsDisplayed()
        composeTestRule.onNodeWithText("French").assertIsDisplayed()
    }
}
