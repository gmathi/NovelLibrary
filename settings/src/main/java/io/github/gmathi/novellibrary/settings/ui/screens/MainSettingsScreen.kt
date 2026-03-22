package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import android.widget.Toast
import io.github.gmathi.novellibrary.settings.ui.components.SettingsItem
import io.github.gmathi.novellibrary.settings.ui.components.SettingsScreen
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Main settings screen displaying 5 primary settings categories.
 *
 * This screen serves as the entry point to all settings functionality,
 * organized into logical categories for improved UX:
 *
 * 1. Reader - Customize reading experience
 * 2. Backup & Sync - Protect your data
 * 3. General - App preferences
 * 4. Advanced - Technical settings
 * 5. About - App info & credits
 *
 * Each category uses a descriptive icon and subtitle to help users
 * quickly find the settings they need.
 *
 * @param viewModel The ViewModel managing main settings state
 * @param onNavigateToReader Callback to navigate to reader settings
 * @param onNavigateToBackupSync Callback to navigate to backup & sync settings
 * @param onNavigateToGeneral Callback to navigate to general settings
 * @param onNavigateToAdvanced Callback to navigate to advanced settings
 * @param onNavigateToAbout Callback to navigate to about screen
 * @param onNavigateBack Callback to navigate back from settings
 * @param modifier Modifier for the screen
 */
@Composable
fun MainSettingsScreen(
    viewModel: MainSettingsViewModel,
    onNavigateToReader: () -> Unit,
    onNavigateToBackupSync: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    val context = LocalContext.current
    
    // Observe toast messages and display them
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    MainSettingsScreenContent(
        isDarkTheme = isDarkTheme,
        isDeveloper = isDeveloper,
        onNavigateToReader = onNavigateToReader,
        onNavigateToBackupSync = onNavigateToBackupSync,
        onNavigateToGeneral = onNavigateToGeneral,
        onNavigateToAdvanced = onNavigateToAdvanced,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateBack = onNavigateBack,
        onToggleDeveloper = { viewModel.setDeveloper() },
        modifier = modifier
    )
}

/**
 * Stateless content composable for the main settings screen.
 *
 * Separated from MainSettingsScreen to enable easier testing and previews.
 *
 * @param isDarkTheme Whether dark theme is enabled
 * @param isDeveloper Whether developer mode is enabled
 * @param onNavigateToReader Callback to navigate to reader settings
 * @param onNavigateToBackupSync Callback to navigate to backup & sync settings
 * @param onNavigateToGeneral Callback to navigate to general settings
 * @param onNavigateToAdvanced Callback to navigate to advanced settings
 * @param onNavigateToAbout Callback to navigate to about screen
 * @param onNavigateBack Callback to navigate back from settings
 * @param modifier Modifier for the screen
 */
@Composable
fun MainSettingsScreenContent(
    isDarkTheme: Boolean,
    isDeveloper: Boolean,
    onNavigateToReader: () -> Unit,
    onNavigateToBackupSync: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
    onToggleDeveloper: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        title = "Settings",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {

        // Category 1: Reader Settings
        SettingsItem(
            title = "Reader",
            description = "Customize reading experience",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            onClick = onNavigateToReader
        )
        HorizontalDivider()

        // Category 2: Backup & Sync Settings
        SettingsItem(
            title = "Backup & Sync",
            description = "Protect your data",
            icon = Icons.Default.CloudUpload,
            onClick = onNavigateToBackupSync
        )
        HorizontalDivider()

        // Category 3: General Settings
        SettingsItem(
            title = "General",
            description = "App preferences",
            icon = Icons.Default.Settings,
            onClick = onNavigateToGeneral
        )
        HorizontalDivider()

        // Category 4: Advanced Settings
        SettingsItem(
            title = "Advanced",
            description = "Technical settings",
            icon = Icons.Default.Build,
            onClick = onNavigateToAdvanced
        )
        HorizontalDivider()

        // Category 5: About
        SettingsItem(
            title = "About",
            description = "App info & credits",
            icon = Icons.Default.Info,
            onClick = onNavigateToAbout
        )
        HorizontalDivider()

        if (!isDeveloper) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onToggleDeveloper() }
            )
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Main Settings - Light Theme", showBackground = true)
@Composable
private fun PreviewMainSettingsScreenLight() {
    NovelLibraryBaseTheme {
        MainSettingsScreenContent(
            isDarkTheme = false,
            isDeveloper = false,
            onNavigateToReader = {},
            onNavigateToBackupSync = {},
            onNavigateToGeneral = {},
            onNavigateToAdvanced = {},
            onNavigateToAbout = {},
            onNavigateBack = {},
            onToggleDeveloper = {}
        )
    }
}

@Preview(name = "Main Settings - Dark Theme", showBackground = true)
@Composable
private fun PreviewMainSettingsScreenDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        MainSettingsScreenContent(
            isDarkTheme = true,
            isDeveloper = false,
            onNavigateToReader = {},
            onNavigateToBackupSync = {},
            onNavigateToGeneral = {},
            onNavigateToAdvanced = {},
            onNavigateToAbout = {},
            onNavigateBack = {},
            onToggleDeveloper = {}
        )
    }
}

@Preview(name = "Main Settings - Developer Mode", showBackground = true)
@Composable
private fun PreviewMainSettingsScreenDeveloper() {
    NovelLibraryBaseTheme {
        MainSettingsScreenContent(
            isDarkTheme = false,
            isDeveloper = true,
            onNavigateToReader = {},
            onNavigateToBackupSync = {},
            onNavigateToGeneral = {},
            onNavigateToAdvanced = {},
            onNavigateToAbout = {},
            onNavigateBack = {},
            onToggleDeveloper = {}
        )
    }
}
