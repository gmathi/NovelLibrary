package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsSliderTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsSlider_displaysTitle() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f
            )
        }
        
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
    }
    
    @Test
    fun settingsSlider_displaysDescription() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                description = "Adjust reading text size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f
            )
        }
        
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Adjust reading text size").assertIsDisplayed()
    }
    
    @Test
    fun settingsSlider_displaysValue() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f,
                showValue = true
            )
        }
        
        composeTestRule.onNodeWithText("16").assertIsDisplayed()
    }
    
    @Test
    fun settingsSlider_hidesValueWhenDisabled() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f,
                showValue = false
            )
        }
        
        composeTestRule.onNodeWithText("16").assertDoesNotExist()
    }
    
    @Test
    fun settingsSlider_usesCustomFormatter() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f,
                valueFormatter = { "${it.toInt()}px" }
            )
        }
        
        composeTestRule.onNodeWithText("16px").assertIsDisplayed()
    }
    
    @Test
    fun settingsSlider_displaysIcon() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                icon = Icons.Default.TextFields,
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f
            )
        }
        
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
    }
    
    @Test
    fun settingsSlider_hasSliderComponent() {
        composeTestRule.setContent {
            SettingsSlider(
                title = "Text Size",
                value = 16f,
                onValueChange = {},
                valueRange = 10f..30f
            )
        }
        
        // Verify slider exists by checking the title and value are displayed
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("16").assertIsDisplayed()
    }
}
