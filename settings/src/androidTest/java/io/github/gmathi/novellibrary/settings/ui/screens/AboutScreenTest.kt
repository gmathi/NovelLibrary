package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for AboutScreen.
 * 
 * Tests:
 * - Verify version info is displayed
 * - Test navigation to sub-screens (Contributors, Copyright, Licenses)
 * - Verify all about sections are present
 * - Test check for updates functionality
 */
class AboutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutScreen_displaysVersionInfo() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then - Version info should be displayed
        composeTestRule.onNodeWithText("Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.0.0 (100)").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysAppInfoSection() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("App Information").assertIsDisplayed()
        composeTestRule.onNodeWithText("Version").assertIsDisplayed()
        composeTestRule.onNodeWithText("Check for Updates").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysCreditsSection() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Credits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contributors").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Source Licenses").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_displaysLegalSection() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Legal").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copyright").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
    }

    @Test
    fun aboutScreen_checkForUpdates_triggersCallback() {
        // Given
        var checkForUpdatesCalled = false

        // When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = { checkForUpdatesCalled = true }
            )
        }

        // Click Check for Updates
        composeTestRule.onNodeWithText("Check for Updates").performClick()

        // Then
        assert(checkForUpdatesCalled) { "Check for updates callback should be called" }
    }

    @Test
    fun aboutScreen_navigatesToContributors() {
        // Given
        var navigatedToContributors = false

        // When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = { navigatedToContributors = true },
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Click Contributors
        composeTestRule.onNodeWithText("Contributors").performClick()

        // Then
        assert(navigatedToContributors) { "Should navigate to Contributors screen" }
    }

    @Test
    fun aboutScreen_navigatesToCopyright() {
        // Given
        var navigatedToCopyright = false

        // When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = { navigatedToCopyright = true },
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Click Copyright
        composeTestRule.onNodeWithText("Copyright").performClick()

        // Then
        assert(navigatedToCopyright) { "Should navigate to Copyright screen" }
    }

    @Test
    fun aboutScreen_navigatesToLicenses() {
        // Given
        var navigatedToLicenses = false

        // When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = { navigatedToLicenses = true },
                onCheckForUpdates = {}
            )
        }

        // Click Open Source Licenses
        composeTestRule.onNodeWithText("Open Source Licenses").performClick()

        // Then
        assert(navigatedToLicenses) { "Should navigate to Licenses screen" }
    }

    @Test
    fun aboutScreen_privacyPolicy_isClickable() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then - Privacy Policy should be clickable
        composeTestRule.onNode(
            hasText("Privacy Policy") and hasClickAction()
        ).assertExists()
    }

    @Test
    fun aboutScreen_termsOfService_isClickable() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then - Terms of Service should be clickable
        composeTestRule.onNode(
            hasText("Terms of Service") and hasClickAction()
        ).assertExists()
    }

    @Test
    fun aboutScreen_verifyAllNavigationItemsPresent() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then - Verify all navigation items are present
        val navigationItems = listOf(
            "Check for Updates",
            "Contributors",
            "Copyright",
            "Open Source Licenses",
            "Privacy Policy",
            "Terms of Service"
        )

        navigationItems.forEach { item ->
            composeTestRule.onNodeWithText(item).assertIsDisplayed()
        }
    }

    @Test
    fun aboutScreen_navigatesBack() {
        // Given
        var navigatedBack = false

        // When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "1.0.0",
                buildNumber = "100",
                onNavigateBack = { navigatedBack = true },
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        assert(navigatedBack) { "Should navigate back" }
    }

    @Test
    fun aboutScreen_displaysCorrectVersionFormat() {
        // Given & When
        composeTestRule.setContent {
            AboutScreen(
                appVersion = "2.5.3",
                buildNumber = "250",
                onNavigateBack = {},
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onCheckForUpdates = {}
            )
        }

        // Then - Version should be in format "version (build)"
        composeTestRule.onNodeWithText("2.5.3 (250)").assertIsDisplayed()
    }
}
