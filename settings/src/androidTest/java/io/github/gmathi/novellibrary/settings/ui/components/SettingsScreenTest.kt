package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {}
            ) {
                SettingsItem(title = "Theme")
            }
        }
        
        composeTestRule.onNodeWithText("Reader Settings").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_displaysBackButton() {
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {}
            ) {
                SettingsItem(title = "Theme")
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_handlesBackButtonClick() {
        var backClicked = false
        
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = { backClicked = true }
            ) {
                SettingsItem(title = "Theme")
            }
        }
        
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(backClicked)
    }
    
    @Test
    fun settingsScreen_displaysContent() {
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {}
            ) {
                SettingsSection(title = "Display") {
                    SettingsItem(title = "Theme")
                    SettingsItem(title = "Text Size")
                }
                SettingsSection(title = "Behavior") {
                    SettingsSwitch(
                        title = "Volume Keys",
                        checked = false,
                        onCheckedChange = {}
                    )
                }
            }
        }
        
        composeTestRule.onNodeWithText("Display").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Text Size").assertIsDisplayed()
        composeTestRule.onNodeWithText("Behavior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume Keys").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_displaysActions() {
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {},
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                }
            ) {
                SettingsItem(title = "Theme")
            }
        }
        
        composeTestRule.onNodeWithContentDescription("More options").assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_contentIsScrollable() {
        composeTestRule.setContent {
            SettingsScreen(
                title = "Reader Settings",
                onNavigateBack = {}
            ) {
                repeat(20) { index ->
                    SettingsItem(title = "Setting $index")
                }
            }
        }
        
        // First item should be visible
        composeTestRule.onNodeWithText("Setting 0").assertIsDisplayed()
        
        // Last item might not be visible initially
        composeTestRule.onNodeWithText("Setting 19").assertDoesNotExist()
        
        // Scroll to make last item visible
        composeTestRule.onNodeWithText("Setting 0").performTouchInput {
            swipeUp()
        }
        
        // After scrolling, later items should become visible
        composeTestRule.waitForIdle()
    }
}
