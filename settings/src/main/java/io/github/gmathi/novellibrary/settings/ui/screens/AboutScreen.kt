package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.settings.ui.components.SettingsItem
import io.github.gmathi.novellibrary.settings.ui.components.SettingsScreen
import io.github.gmathi.novellibrary.settings.ui.components.SettingsSection

/**
 * About screen displaying app information, credits, and legal information.
 *
 * This screen consolidates three old activities:
 * - ContributionsActivity (contributors list)
 * - CopyrightActivity (copyright information)
 * - LibrariesUsedActivity (open source licenses)
 *
 * The screen provides:
 * - App version and build information
 * - Navigation to Contributors screen
 * - Navigation to Copyright screen
 * - Navigation to Open Source Licenses screen
 * - Privacy policy and terms of service links
 * - Check for updates functionality
 *
 * This is a static informational screen with no state management needed.
 *
 * @param appVersionName The app version name (e.g., "1.0.0")
 * @param appVersionCode The app version code (e.g., 120)
 * @param onNavigateToContributors Callback to navigate to contributors screen
 * @param onNavigateToCopyright Callback to navigate to copyright screen
 * @param onNavigateToLicenses Callback to navigate to open source licenses screen
 * @param onOpenPrivacyPolicy Callback to open privacy policy
 * @param onOpenTermsOfService Callback to open terms of service
 * @param onCheckForUpdates Callback to check for app updates
 * @param onNavigateBack Callback to navigate back from about screen
 * @param modifier Modifier for the screen
 */
@Composable
fun AboutScreen(
    appVersionName: String,
    appVersionCode: Int,
    onNavigateToContributors: () -> Unit,
    onNavigateToCopyright: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    AboutScreenContent(
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        onNavigateToContributors = onNavigateToContributors,
        onNavigateToCopyright = onNavigateToCopyright,
        onNavigateToLicenses = onNavigateToLicenses,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenTermsOfService = onOpenTermsOfService,
        onCheckForUpdates = onCheckForUpdates,
        onNavigateBack = onNavigateBack,
        modifier = modifier
    )
}

/**
 * Stateless content composable for the about screen.
 *
 * Separated from AboutScreen to enable easier testing and previews.
 *
 * @param appVersionName The app version name
 * @param appVersionCode The app version code
 * @param onNavigateToContributors Callback to navigate to contributors screen
 * @param onNavigateToCopyright Callback to navigate to copyright screen
 * @param onNavigateToLicenses Callback to navigate to open source licenses screen
 * @param onOpenPrivacyPolicy Callback to open privacy policy
 * @param onOpenTermsOfService Callback to open terms of service
 * @param onCheckForUpdates Callback to check for app updates
 * @param onNavigateBack Callback to navigate back
 * @param modifier Modifier for the screen
 */
@Composable
fun AboutScreenContent(
    appVersionName: String,
    appVersionCode: Int,
    onNavigateToContributors: () -> Unit,
    onNavigateToCopyright: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        title = "About",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        // App Version Section
        SettingsSection(title = "App Information") {
            // App Icon and Version Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon Placeholder
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "App Icon",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // App Name
                Text(
                    text = "Novel Library",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version Information
                Text(
                    text = "Version $appVersionName ($appVersionCode)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider()

            // Check for Updates
            SettingsItem(
                title = "Check for Updates",
                description = "Check if a new version is available",
                icon = Icons.Default.Update,
                onClick = onCheckForUpdates
            )
        }

        // Credits Section
        SettingsSection(title = "Credits") {
            // Contributors
            SettingsItem(
                title = "Contributors",
                description = "People who made this app possible",
                icon = Icons.Default.People,
                onClick = onNavigateToContributors
            )


            // Open Source Licenses
            SettingsItem(
                title = "Open Source Licenses",
                description = "Third-party libraries used in this app",
                icon = Icons.Default.Code,
                onClick = onNavigateToLicenses
            )
        }

        // Legal Section
        SettingsSection(title = "Legal") {
            // Copyright
            SettingsItem(
                title = "Copyright",
                description = "Copyright and licensing information",
                icon = Icons.Default.Copyright,
                onClick = onNavigateToCopyright
            )


            // Privacy Policy
            SettingsItem(
                title = "Privacy Policy",
                description = "How we handle your data",
                icon = Icons.Default.PrivacyTip,
                onClick = onOpenPrivacyPolicy
            )


            // Terms of Service
            SettingsItem(
                title = "Terms of Service",
                description = "Terms and conditions of use",
                icon = Icons.Default.Description,
                onClick = onOpenTermsOfService
            )
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(
    name = "About Screen - Light Theme",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewAboutScreenLight() {
    MaterialTheme {
        AboutScreenContent(
            appVersionName = "1.0.0",
            appVersionCode = 120,
            onNavigateToContributors = {},
            onNavigateToCopyright = {},
            onNavigateToLicenses = {},
            onOpenPrivacyPolicy = {},
            onOpenTermsOfService = {},
            onCheckForUpdates = {},
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "About Screen - Dark Theme",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewAboutScreenDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            AboutScreenContent(
                appVersionName = "1.0.0",
                appVersionCode = 120,
                onNavigateToContributors = {},
                onNavigateToCopyright = {},
                onNavigateToLicenses = {},
                onOpenPrivacyPolicy = {},
                onOpenTermsOfService = {},
                onCheckForUpdates = {},
                onNavigateBack = {}
            )
        }
    }
}
