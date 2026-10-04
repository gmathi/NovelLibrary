package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * About & Help — the renamed "Mentions" screen (plan §7 decision 4), also
 * absorbing the top-level app-info actions (Check for Updates, Donate, About,
 * Report a Bug) and the Auto App Update toggle, so the top level holds only
 * real setting categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutHelpSettingsScreen(
    autoAppUpdate: Boolean,
    onAutoAppUpdateChange: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onReportBug: () -> Unit,
    onDonate: () -> Unit,
    onAboutUs: () -> Unit,
    onLanguagesSupported: () -> Unit,
    onLibrariesUsed: () -> Unit,
    onContributions: () -> Unit,
    onCopyrightNotice: () -> Unit,
    onWhatsChanged: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About & Help") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { SettingsSectionHeader(title = "Updates") }
            item {
                ActionSettingRow(
                    title = "Check For Update",
                    subtitle = "Look for a newer app version now",
                    onClick = onCheckForUpdates
                )
            }
            item {
                SwitchSettingRow(
                    title = "Auto App Update",
                    subtitle = "Automatically check for app updates",
                    checked = autoAppUpdate,
                    onCheckedChange = onAutoAppUpdateChange
                )
            }
            item { SettingsSectionHeader(title = "What's new") }
            item {
                ActionSettingRow(
                    title = "What's Changed",
                    onClick = onWhatsChanged
                )
            }

            item { SettingsSectionHeader(title = "Help") }
            item {
                ActionSettingRow(
                    title = "Report a Bug",
                    subtitle = "Copy debug info and open the support channel",
                    onClick = onReportBug
                )
            }

            item { SettingsSectionHeader(title = "About") }
            item {
                ActionSettingRow(
                    title = "About Us",
                    onClick = onAboutUs
                )
            }
            item {
                ActionSettingRow(
                    title = "Donate Developer",
                    onClick = onDonate
                )
            }
            item {
                ActionSettingRow(
                    title = "Languages Supported",
                    onClick = onLanguagesSupported
                )
            }
            item {
                ActionSettingRow(
                    title = "Libraries Used",
                    onClick = onLibrariesUsed
                )
            }
            item {
                ActionSettingRow(
                    title = "Contributions",
                    onClick = onContributions
                )
            }
            item {
                ActionSettingRow(
                    title = "Copyright Notice",
                    onClick = onCopyrightNotice
                )
            }
        }
    }
}
