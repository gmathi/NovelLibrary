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
 * Appearance & Theme — pulls the pure look-and-feel toggles out of the legacy
 * General screen plus the reader color palette, and keeps a small "General"
 * header for the two global-app toggles (per plan §7 decision 1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    nightMode: Boolean,
    scrollingText: Boolean,
    showChaptersLeftBadge: Boolean,
    loadLibraryScreen: Boolean,
    enableNotifications: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    onScrollingTextChange: (Boolean) -> Unit,
    onShowChaptersLeftBadgeChange: (Boolean) -> Unit,
    onLoadLibraryScreenChange: (Boolean) -> Unit,
    onEnableNotificationsChange: (Boolean) -> Unit,
    onOpenReaderColors: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance & Theme") },
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
            item { SettingsSectionHeader(title = "Theme") }
            item {
                SwitchSettingRow(
                    title = "Night Mode",
                    subtitle = "Use the dark theme",
                    checked = nightMode,
                    onCheckedChange = onNightModeChange
                )
            }
            item {
                ChevronSettingRow(
                    title = "Reader Mode Colors",
                    subtitle = "Day and night background / text colors",
                    onClick = onOpenReaderColors
                )
            }

            item { SettingsSectionHeader(title = "Display") }
            item {
                SwitchSettingRow(
                    title = "Scrolling Text",
                    subtitle = "Marquee long titles instead of truncating",
                    checked = scrollingText,
                    onCheckedChange = onScrollingTextChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Show remaining chapters in badge",
                    checked = showChaptersLeftBadge,
                    onCheckedChange = onShowChaptersLeftBadgeChange
                )
            }

            item { SettingsSectionHeader(title = "General") }
            item {
                SwitchSettingRow(
                    title = "Make \"Library\" main screen",
                    subtitle = "Open the Library screen on app start",
                    checked = loadLibraryScreen,
                    onCheckedChange = onLoadLibraryScreenChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Enable Notifications",
                    subtitle = "Background chapter-update notifications",
                    checked = enableNotifications,
                    onCheckedChange = onEnableNotificationsChange
                )
            }
        }
    }
}
