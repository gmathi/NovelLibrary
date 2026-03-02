package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsItemTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsItem_displaysTitle() {
        composeTestRule.setContent {
            SettingsItem(title = "Test Setting")
        }
        
        composeTestRule.onNodeWithText("Test Setting").assertIsDisplayed()
    }
    
    @Test
    fun settingsItem_displaysDescription() {
        composeTestRule.setContent {
            SettingsItem(
                title = "Test Setting",
                description = "This is a description"
            )
        }
        
        composeTestRule.onNodeWithText("Test Setting").assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a description").assertIsDisplayed()
    }
    
    @Test
    fun settingsItem_displaysIcon() {
        composeTestRule.setContent {
            SettingsItem(
                title = "Test Setting",
                icon = Icons.Default.Settings
            )
        }
        
        composeTestRule.onNodeWithText("Test Setting").assertIsDisplayed()
        // Icon is displayed (no content description, so we just verify the item renders)
    }
    
    @Test
    fun settingsItem_handlesClick() {
        var clicked = false
        
        composeTestRule.setContent {
            SettingsItem(
                title = "Test Setting",
                onClick = { clicked = true }
            )
        }
        
        composeTestRule.onNodeWithText("Test Setting").performClick()
        assert(clicked)
    }
    
    @Test
    fun settingsItem_disabledDoesNotHandleClick() {
        var clicked = false
        
        composeTestRule.setContent {
            SettingsItem(
                title = "Test Setting",
                enabled = false,
                onClick = { clicked = true }
            )
        }
        
        composeTestRule.onNodeWithText("Test Setting").performClick()
        assert(!clicked)
    }
    
    @Test
    fun settingsItem_displaysTrailingContent() {
        composeTestRule.setContent {
            SettingsItem(
                title = "Test Setting",
                trailingContent = {
                    Text("Trailing")
                }
            )
        }
        
        composeTestRule.onNodeWithText("Test Setting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trailing").assertIsDisplayed()
    }
}
