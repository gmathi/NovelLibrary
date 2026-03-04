package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.gmathi.novellibrary.settings.ui.components.*
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel

/**
 * Advanced Settings Screen
 * 
 * Consolidates technical/power-user settings from multiple old activities:
 * - CloudFlareBypassActivity (Cloudflare bypass configuration)
 * - Other advanced settings scattered across the app
 * 
 * Organized into 4 sections:
 * 1. Network - Cloudflare bypass, network timeout, JavaScript settings
 * 2. Cache - cache management, clear cache
 * 3. Debug - debug logging, developer options
 * 4. Data - data migration tools, reset settings
 * 
 * @param viewModel ViewModel managing advanced settings state
 * @param onNavigateBack Callback to navigate back to main settings
 * @param onClearCache Callback to clear app cache
 * @param onResetSettings Callback to reset all settings to defaults
 * @param onCloudflareBypass Callback to open Cloudflare bypass configuration
 * @param modifier Modifier for the screen
 */
@Composable
fun AdvancedSettingsScreen(
    viewModel: AdvancedSettingsViewModel,
    onNavigateBack: () -> Unit,
    onClearCache: () -> Unit = {},
    onResetSettings: () -> Unit = {},
    onCloudflareBypass: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val javascriptDisabled by viewModel.javascriptDisabled.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    
    SettingsScreen(
        title = "Advanced Settings",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        AdvancedSettingsContent(
            javascriptDisabled = javascriptDisabled,
            onJavascriptDisabledChange = viewModel::setJavascriptDisabled,
            isDeveloper = isDeveloper,
            onIsDeveloperChange = viewModel::setIsDeveloper,
            onClearCache = onClearCache,
            onResetSettings = onResetSettings,
            onCloudflareBypass = onCloudflareBypass
        )
    }
}

/**
 * Content for Advanced Settings Screen.
 * 
 * Separated from the screen composable for easier testing and preview.
 * Contains all four sections with their respective settings.
 */
@Composable
private fun ColumnScope.AdvancedSettingsContent(
    javascriptDisabled: Boolean,
    onJavascriptDisabledChange: (Boolean) -> Unit,
    isDeveloper: Boolean,
    onIsDeveloperChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onResetSettings: () -> Unit,
    onCloudflareBypass: () -> Unit
) {
    // Section 1: Network
    SettingsSection(title = "Network") {
        SettingsItem(
            title = "Cloudflare Bypass",
            description = "Configure Cloudflare bypass for protected sources",
            icon = Icons.Default.Cloud,
            onClick = onCloudflareBypass
        )
        
        SettingsSwitch(
            title = "Disable JavaScript",
            description = "Disable JavaScript in web views (may break some sources)",
            icon = Icons.Default.Code,
            checked = javascriptDisabled,
            onCheckedChange = onJavascriptDisabledChange
        )
        
        SettingsItem(
            title = "Network Timeout",
            description = "Configure network request timeout settings",
            icon = Icons.Default.Timer,
            onClick = { /* TODO: Implement network timeout configuration */ }
        )
    }
    
    // Section 2: Cache
    SettingsSection(title = "Cache") {
        SettingsItem(
            title = "Clear Cache",
            description = "Clear all cached data to free up storage",
            icon = Icons.Default.DeleteSweep,
            onClick = onClearCache
        )
        
        SettingsItem(
            title = "Cache Management",
            description = "View and manage cached content",
            icon = Icons.Default.Storage,
            onClick = { /* TODO: Implement cache management */ }
        )
    }
    
    // Section 3: Debug
    SettingsSection(title = "Debug") {
        SettingsSwitch(
            title = "Developer Mode",
            description = "Enable advanced developer options and debugging",
            icon = Icons.Default.DeveloperMode,
            checked = isDeveloper,
            onCheckedChange = onIsDeveloperChange
        )
        
        SettingsSwitch(
            title = "Debug Logging",
            description = "Enable detailed logging for troubleshooting",
            icon = Icons.Default.BugReport,
            checked = isDeveloper, // Reuse developer mode for now
            onCheckedChange = onIsDeveloperChange
        )
    }
    
    // Section 4: Data
    SettingsSection(title = "Data") {
        SettingsItem(
            title = "Data Migration Tools",
            description = "Import or export app data",
            icon = Icons.Default.ImportExport,
            onClick = { /* TODO: Implement data migration */ }
        )
        
        SettingsItem(
            title = "Reset Settings",
            description = "Reset all settings to default values",
            icon = Icons.Default.RestartAlt,
            onClick = onResetSettings
        )
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(
    name = "Advanced Settings - Full Screen Light",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewAdvancedSettingsScreenFullLight() {
    MaterialTheme {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {},
                onClearCache = {},
                onResetSettings = {},
                onCloudflareBypass = {}
            )
        }
    }
}

@Preview(
    name = "Advanced Settings - Full Screen Dark",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewAdvancedSettingsScreenFullDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SettingsScreen(
                title = "Advanced Settings",
                onNavigateBack = {}
            ) {
                AdvancedSettingsContent(
                    javascriptDisabled = true,
                    onJavascriptDisabledChange = {},
                    isDeveloper = true,
                    onIsDeveloperChange = {},
                    onClearCache = {},
                    onResetSettings = {},
                    onCloudflareBypass = {}
                )
            }
        }
    }
}

@Preview(name = "Advanced Settings Content", showBackground = true)
@Composable
private fun PreviewAdvancedSettingsContent() {
    MaterialTheme {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {},
                onClearCache = {},
                onResetSettings = {},
                onCloudflareBypass = {}
            )
        }
    }
}

@Preview(name = "Advanced Settings Dark", showBackground = true)
@Composable
private fun PreviewAdvancedSettingsContentDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SettingsScreen(
                title = "Advanced Settings",
                onNavigateBack = {}
            ) {
                AdvancedSettingsContent(
                    javascriptDisabled = true,
                    onJavascriptDisabledChange = {},
                    isDeveloper = true,
                    onIsDeveloperChange = {},
                    onClearCache = {},
                    onResetSettings = {},
                    onCloudflareBypass = {}
                )
            }
        }
    }
}
