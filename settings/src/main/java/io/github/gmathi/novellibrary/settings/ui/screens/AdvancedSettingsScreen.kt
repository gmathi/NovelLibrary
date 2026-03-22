package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.settings.ui.components.*
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import kotlin.math.roundToInt

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
    val networkTimeoutSeconds by viewModel.networkTimeoutSeconds.collectAsState()
    
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
            networkTimeoutSeconds = networkTimeoutSeconds,
            onNetworkTimeoutChange = viewModel::setNetworkTimeoutSeconds,
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
    networkTimeoutSeconds: Int,
    onNetworkTimeoutChange: (Int) -> Unit,
    onClearCache: () -> Unit,
    onResetSettings: () -> Unit,
    onCloudflareBypass: () -> Unit
) {
    // Dialog states
    var showNetworkTimeoutDialog by remember { mutableStateOf(false) }
    var showCacheManagementDialog by remember { mutableStateOf(false) }
    var showDataMigrationDialog by remember { mutableStateOf(false) }
    
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
            description = "Current: ${networkTimeoutSeconds}s",
            icon = Icons.Default.Timer,
            onClick = { showNetworkTimeoutDialog = true }
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
            onClick = { showCacheManagementDialog = true }
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
            onClick = { showDataMigrationDialog = true }
        )
        
        SettingsItem(
            title = "Reset Settings",
            description = "Reset all settings to default values",
            icon = Icons.Default.RestartAlt,
            onClick = onResetSettings
        )
    }
    
    // Network Timeout Dialog
    if (showNetworkTimeoutDialog) {
        NetworkTimeoutDialog(
            currentTimeout = networkTimeoutSeconds,
            onDismiss = { showNetworkTimeoutDialog = false },
            onConfirm = { newTimeout ->
                onNetworkTimeoutChange(newTimeout)
                showNetworkTimeoutDialog = false
            }
        )
    }
    
    // Cache Management Dialog
    if (showCacheManagementDialog) {
        AlertDialog(
            onDismissRequest = { showCacheManagementDialog = false },
            title = { Text("Cache Management") },
            text = {
                Column {
                    Text(
                        text = "Cached content includes downloaded chapters, images, and web data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Use \"Clear Cache\" to remove all cached data and free up storage space.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCacheManagementDialog = false
                    onClearCache()
                }) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheManagementDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Data Migration Dialog
    if (showDataMigrationDialog) {
        AlertDialog(
            onDismissRequest = { showDataMigrationDialog = false },
            title = { Text("Data Migration") },
            text = {
                Text(
                    text = "Use Backup & Sync settings to import or export your library data, reading progress, and preferences.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showDataMigrationDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Dialog for configuring network timeout with a slider.
 */
@Composable
private fun NetworkTimeoutDialog(
    currentTimeout: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentTimeout.toFloat()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Network Timeout") },
        text = {
            Column {
                Text(
                    text = "Set the timeout for network requests (${sliderValue.roundToInt()} seconds)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..120f,
                    steps = 10
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.roundToInt()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
    NovelLibraryBaseTheme {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {},
                networkTimeoutSeconds = 30,
                onNetworkTimeoutChange = {},
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
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = true,
                onJavascriptDisabledChange = {},
                isDeveloper = true,
                onIsDeveloperChange = {},
                networkTimeoutSeconds = 60,
                onNetworkTimeoutChange = {},
                onClearCache = {},
                onResetSettings = {},
                onCloudflareBypass = {}
            )
        }
    }
}

@Preview(name = "Advanced Settings Content", showBackground = true)
@Composable
private fun PreviewAdvancedSettingsContent() {
    NovelLibraryBaseTheme {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {},
                networkTimeoutSeconds = 30,
                onNetworkTimeoutChange = {},
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
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsScreen(
            title = "Advanced Settings",
            onNavigateBack = {}
        ) {
            AdvancedSettingsContent(
                javascriptDisabled = true,
                onJavascriptDisabledChange = {},
                isDeveloper = true,
                onIsDeveloperChange = {},
                networkTimeoutSeconds = 60,
                onNetworkTimeoutChange = {},
                onClearCache = {},
                onResetSettings = {},
                onCloudflareBypass = {}
            )
        }
    }
}
