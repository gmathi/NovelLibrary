package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Root of the restructured settings tree, Compose-only. This is a NEW parallel
 * surface — the existing [io.github.gmathi.novellibrary.activity.settings.MainSettingsActivity]
 * and all legacy settings activities are left untouched and keep working.
 *
 * Entries map to the owner-approved plan (docs/plans/settings-restructuring-plan.md §7):
 * Appearance & Theme · Reader · Text-to-Speech · Network & Privacy · Storage & Backup · Sync · About & Help.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRootSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenReader: () -> Unit,
    onOpenTextToSpeech: () -> Unit,
    onOpenNetworkPrivacy: () -> Unit,
    onOpenStorageBackup: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenAboutHelp: () -> Unit,
    onSwitchToClassic: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            item {
                ChevronSettingRow(
                    title = "Appearance & Theme",
                    subtitle = "Night mode, scrolling text, badges, reader colors",
                    onClick = onOpenAppearance
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "Reader",
                    subtitle = "Reader mode, content, text and behavior",
                    onClick = onOpenReader
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "Text-to-Speech",
                    subtitle = "Classic TTS and AI TTS engines",
                    onClick = onOpenTextToSpeech
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "Network & Privacy",
                    subtitle = "DNS over HTTPS, forced WebView, Cloudflare cookies",
                    onClick = onOpenNetworkPrivacy
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "Storage & Backup",
                    subtitle = "Download location, backup and restore",
                    onClick = onOpenStorageBackup
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "Sync",
                    subtitle = "NovelUpdates sync",
                    onClick = onOpenSync
                )
            }
            item { HorizontalDivider() }
            item {
                ChevronSettingRow(
                    title = "About & Help",
                    subtitle = "Updates, credits, report a bug, donate",
                    onClick = onOpenAboutHelp
                )
            }
            item { HorizontalDivider() }
            item {
                ActionSettingRow(
                    title = "Switch to classic settings",
                    subtitle = "Use the old settings screens instead",
                    onClick = onSwitchToClassic
                )
            }
        }
    }
}
