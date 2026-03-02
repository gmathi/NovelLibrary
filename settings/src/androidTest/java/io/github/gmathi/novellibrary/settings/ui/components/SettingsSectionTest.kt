package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsSectionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsSection_displaysSectionTitle() {
        composeTestRule.setContent {
            SettingsSection(title = "Display Settings") {
                SettingsItem(title = "Theme")
                SettingsItem(title = "Text Size")
            }
        }
        
        composeTestRule.onNodeWithText("Display Settings").assertIsDisplayed()
    }
    
    @Test
    fun settingsSection_displaysContent() {
        composeTestRule.setContent {
            SettingsSection(title = "Display Settings") {
                SettingsItem(title = "Theme")
                SettingsItem(title = "Text Size")
            }
        }
        
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
    }
    
    @Test
    fun settingsSection_groupsMultipleItems() {
        composeTestRule.setContent {
            SettingsSection(title = "Reader Settings") {
                SettingsSwitch(
                    title = "Volume Key Navigation",
                    checked = false,
                    onCheckedChange = {}
                )
                SettingsSlider(
                    title = "Text Size",
                    value = 16f,
                    onValueChange = {},
                    valueRange = 10f..30f
                )
                SettingsDropdown(
                    title = "Font",
                    selectedValue = "System Default",
                    options = listOf("System Default", "Serif", "Sans Serif"),
                    onOptionSelected = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Reader Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume Key Navigation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Font").assertIsDisplayed()
    }
    
    @Test
    fun settingsSection_multipleSectionsDisplayed() {
        composeTestRule.setContent {
            SettingsSection(title = "Section 1") {
                SettingsItem(title = "Item 1")
            }
            SettingsSection(title = "Section 2") {
                SettingsItem(title = "Item 2")
            }
        }
        
        composeTestRule.onNodeWithText("Section 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Section 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Item 2").assertIsDisplayed()
    }
}
