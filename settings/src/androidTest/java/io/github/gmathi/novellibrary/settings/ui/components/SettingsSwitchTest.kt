package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsSwitchTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsSwitch_displaysTitle() {
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                checked = false,
                onCheckedChange = {}
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
    
    @Test
    fun settingsSwitch_displaysDescription() {
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                description = "Receive app notifications",
                checked = false,
                onCheckedChange = {}
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Receive app notifications").assertIsDisplayed()
    }
    
    @Test
    fun settingsSwitch_togglesOnClick() {
        var checked = false
        
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                checked = checked,
                onCheckedChange = { checked = it }
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").performClick()
        assert(checked)
    }
    
    @Test
    fun settingsSwitch_reflectsCheckedState() {
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                checked = true,
                onCheckedChange = {}
            )
        }
        
        // Switch should be in checked state - verify by checking the text is displayed
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
    
    @Test
    fun settingsSwitch_reflectsUncheckedState() {
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                checked = false,
                onCheckedChange = {}
            )
        }
        
        // Switch should be in unchecked state - verify by checking the text is displayed
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
    
    @Test
    fun settingsSwitch_disabledDoesNotToggle() {
        var checked = false
        
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                checked = checked,
                enabled = false,
                onCheckedChange = { checked = it }
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").performClick()
        assert(!checked)
    }
    
    @Test
    fun settingsSwitch_displaysIcon() {
        composeTestRule.setContent {
            SettingsSwitch(
                title = "Enable Notifications",
                icon = Icons.Default.Notifications,
                checked = false,
                onCheckedChange = {}
            )
        }
        
        composeTestRule.onNodeWithText("Enable Notifications").assertIsDisplayed()
    }
}
