package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.gmathi.novellibrary.settings.ui.components.*
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel

/**
 * General Settings Screen
 * 
 * Consolidates general app settings from multiple old activities:
 * - GeneralSettingsActivity (app theme, notifications, general preferences)
 * - LanguageActivity (language selection - now inline dropdown)
 * - MentionSettingsActivity (mention notifications)
 * 
 * Organized into 4 sections:
 * 1. Appearance - app theme (light/dark/system)
 * 2. Language - language selection (inline dropdown, reduces navigation depth)
 * 3. Notifications - notification preferences, mention notifications
 * 4. Other Settings - JavaScript, library screen, developer mode, badges
 * 
 * @param viewModel ViewModel managing general settings state
 * @param onNavigateBack Callback to navigate back to main settings
 * @param modifier Modifier for the screen
 */
@Composable
fun GeneralSettingsScreen(
    viewModel: GeneralSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val language by viewModel.language.collectAsState()
    val javascriptDisabled by viewModel.javascriptDisabled.collectAsState()
    val loadLibraryScreen by viewModel.loadLibraryScreen.collectAsState()
    val enableNotifications by viewModel.enableNotifications.collectAsState()
    val showChaptersLeftBadge by viewModel.showChaptersLeftBadge.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    
    SettingsScreen(
        title = "General Settings",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        GeneralSettingsContent(
            isDarkTheme = isDarkTheme,
            onDarkThemeChange = viewModel::setDarkTheme,
            language = language,
            onLanguageChange = viewModel::setLanguage,
            javascriptDisabled = javascriptDisabled,
            onJavascriptDisabledChange = viewModel::setJavascriptDisabled,
            loadLibraryScreen = loadLibraryScreen,
            onLoadLibraryScreenChange = viewModel::setLoadLibraryScreen,
            enableNotifications = enableNotifications,
            onEnableNotificationsChange = viewModel::setEnableNotifications,
            showChaptersLeftBadge = showChaptersLeftBadge,
            onShowChaptersLeftBadgeChange = viewModel::setShowChaptersLeftBadge,
            isDeveloper = isDeveloper,
            onIsDeveloperChange = viewModel::setIsDeveloper
        )
    }
}

/**
 * Content for General Settings Screen.
 * 
 * Separated from the screen composable for easier testing and preview.
 * Contains all four sections with their respective settings.
 */
@Composable
private fun ColumnScope.GeneralSettingsContent(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    javascriptDisabled: Boolean,
    onJavascriptDisabledChange: (Boolean) -> Unit,
    loadLibraryScreen: Boolean,
    onLoadLibraryScreenChange: (Boolean) -> Unit,
    enableNotifications: Boolean,
    onEnableNotificationsChange: (Boolean) -> Unit,
    showChaptersLeftBadge: Boolean,
    onShowChaptersLeftBadgeChange: (Boolean) -> Unit,
    isDeveloper: Boolean,
    onIsDeveloperChange: (Boolean) -> Unit
) {
    // Section 1: Appearance
    SettingsSection(title = "Appearance") {
        SettingsSwitch(
            title = "Dark Theme",
            description = if (isDarkTheme) "Using dark theme" else "Using light theme",
            icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
            checked = isDarkTheme,
            onCheckedChange = onDarkThemeChange
        )
    }
    
    // Section 2: Language
    SettingsSection(title = "Language") {
        val languageOptions = listOf(
            "en" to "English",
            "es" to "Español",
            "fr" to "Français",
            "de" to "Deutsch",
            "it" to "Italiano",
            "pt" to "Português",
            "ru" to "Русский",
            "ja" to "日本語",
            "ko" to "한국어",
            "zh" to "中文"
        )
        
        SettingsDropdown(
            title = "App Language",
            description = getLanguageDisplayName(language),
            icon = Icons.Default.Language,
            selectedValue = languageOptions.find { it.first == language } ?: languageOptions.first(),
            options = languageOptions,
            onOptionSelected = { onLanguageChange(it.first) },
            optionLabel = { it.second }
        )
    }
    
    // Section 3: Notifications
    SettingsSection(title = "Notifications") {
        SettingsSwitch(
            title = "Enable Notifications",
            description = "Receive notifications for new chapters and updates",
            icon = Icons.Default.Notifications,
            checked = enableNotifications,
            onCheckedChange = onEnableNotificationsChange
        )
        
        SettingsSwitch(
            title = "Show Chapters Left Badge",
            description = "Display badge showing unread chapters count",
            icon = Icons.Default.Badge,
            checked = showChaptersLeftBadge,
            onCheckedChange = onShowChaptersLeftBadgeChange
        )
    }
    
    // Section 4: Other Settings
    SettingsSection(title = "Other Settings") {
        SettingsSwitch(
            title = "Disable JavaScript",
            description = "Disable JavaScript in web views (may break some sources)",
            icon = Icons.Default.Code,
            checked = javascriptDisabled,
            onCheckedChange = onJavascriptDisabledChange
        )
        
        SettingsSwitch(
            title = "Load Library on Startup",
            description = "Open library screen when app starts",
            icon = Icons.Default.LibraryBooks,
            checked = loadLibraryScreen,
            onCheckedChange = onLoadLibraryScreenChange
        )
        
        SettingsSwitch(
            title = "Developer Mode",
            description = "Enable advanced developer options and debugging",
            icon = Icons.Default.DeveloperMode,
            checked = isDeveloper,
            onCheckedChange = onIsDeveloperChange
        )
    }
}

/**
 * Get display name for language code.
 * 
 * @param languageCode ISO 639-1 language code
 * @return Human-readable language name
 */
private fun getLanguageDisplayName(languageCode: String): String {
    return when (languageCode) {
        "en" -> "English"
        "es" -> "Español"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "it" -> "Italiano"
        "pt" -> "Português"
        "ru" -> "Русский"
        "ja" -> "日本語"
        "ko" -> "한국어"
        "zh" -> "中文"
        else -> "English"
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(
    name = "General Settings - Full Screen Light",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewGeneralSettingsScreenFullLight() {
    NovelLibraryBaseTheme {
        SettingsScreen(
            title = "General Settings",
            onNavigateBack = {}
        ) {
            GeneralSettingsContent(
                isDarkTheme = false,
                onDarkThemeChange = {},
                language = "en",
                onLanguageChange = {},
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                loadLibraryScreen = false,
                onLoadLibraryScreenChange = {},
                enableNotifications = true,
                onEnableNotificationsChange = {},
                showChaptersLeftBadge = true,
                onShowChaptersLeftBadgeChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {}
            )
        }
    }
}

@Preview(
    name = "General Settings - Full Screen Dark",
    showBackground = true,
    heightDp = 1500
)
@Composable
private fun PreviewGeneralSettingsScreenFullDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsScreen(
            title = "General Settings",
            onNavigateBack = {}
        ) {
            GeneralSettingsContent(
                isDarkTheme = true,
                onDarkThemeChange = {},
                language = "ja",
                onLanguageChange = {},
                javascriptDisabled = true,
                onJavascriptDisabledChange = {},
                loadLibraryScreen = true,
                onLoadLibraryScreenChange = {},
                enableNotifications = false,
                onEnableNotificationsChange = {},
                showChaptersLeftBadge = false,
                onShowChaptersLeftBadgeChange = {},
                isDeveloper = true,
                onIsDeveloperChange = {}
            )
        }
    }
}

@Preview(name = "General Settings Content", showBackground = true)
@Composable
private fun PreviewGeneralSettingsContent() {
    NovelLibraryBaseTheme {
        SettingsScreen(
            title = "General Settings",
            onNavigateBack = {}
        ) {
            GeneralSettingsContent(
                isDarkTheme = false,
                onDarkThemeChange = {},
                language = "en",
                onLanguageChange = {},
                javascriptDisabled = false,
                onJavascriptDisabledChange = {},
                loadLibraryScreen = false,
                onLoadLibraryScreenChange = {},
                enableNotifications = true,
                onEnableNotificationsChange = {},
                showChaptersLeftBadge = true,
                onShowChaptersLeftBadgeChange = {},
                isDeveloper = false,
                onIsDeveloperChange = {}
            )
        }
    }
}

@Preview(name = "General Settings Dark", showBackground = true)
@Composable
private fun PreviewGeneralSettingsContentDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsScreen(
            title = "General Settings",
            onNavigateBack = {}
        ) {
            GeneralSettingsContent(
                isDarkTheme = true,
                onDarkThemeChange = {},
                language = "ja",
                onLanguageChange = {},
                javascriptDisabled = true,
                onJavascriptDisabledChange = {},
                loadLibraryScreen = true,
                onLoadLibraryScreenChange = {},
                enableNotifications = false,
                onEnableNotificationsChange = {},
                showChaptersLeftBadge = false,
                onShowChaptersLeftBadgeChange = {},
                isDeveloper = true,
                onIsDeveloperChange = {}
            )
        }
    }
}
