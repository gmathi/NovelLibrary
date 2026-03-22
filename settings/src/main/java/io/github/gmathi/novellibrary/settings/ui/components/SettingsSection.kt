package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme

/**
 * Groups related settings with a section header.
 * 
 * Provides visual separation and organization for groups of related settings items.
 * The section header uses Material 3 typography and spacing guidelines.
 * 
 * @param title The section header title
 * @param modifier Modifier for the section container
 * @param content The settings items to display in this section
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section header
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Section content
        content()
        
        // Divider after section
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Basic Section", showBackground = true)
@Composable
private fun PreviewSettingsSectionBasic() {
    NovelLibraryBaseTheme {
        SettingsSection(title = "Display") {
            SettingsItem(
                title = "Theme",
                description = "Choose app theme",
                onClick = {}
            )
            SettingsItem(
                title = "Text Size",
                description = "Adjust reading text size",
                onClick = {}
            )
        }
    }
}

@Preview(name = "With Switches", showBackground = true)
@Composable
private fun PreviewSettingsSectionWithSwitches() {
    NovelLibraryBaseTheme {
        SettingsSection(title = "Notifications") {
            SettingsSwitch(
                title = "Push Notifications",
                description = "Receive notifications for new chapters",
                icon = Icons.Default.Notifications,
                checked = true,
                onCheckedChange = {}
            )
            SettingsSwitch(
                title = "Email Notifications",
                description = "Receive email updates",
                icon = Icons.Default.Notifications,
                checked = false,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(name = "Mixed Content", showBackground = true)
@Composable
private fun PreviewSettingsSectionMixed() {
    NovelLibraryBaseTheme {
        SettingsSection(title = "Reader Settings") {
            SettingsDropdown(
                title = "Theme",
                description = "Choose reader theme",
                icon = Icons.Default.Palette,
                selectedValue = "Sepia",
                options = listOf("Light", "Dark", "Sepia"),
                onOptionSelected = {}
            )
            SettingsSlider(
                title = "Text Size",
                description = "Adjust reading text size",
                value = 16f,
                onValueChange = {},
                valueRange = 12f..24f
            )
            SettingsSwitch(
                title = "Keep Screen On",
                description = "Prevent screen from sleeping while reading",
                checked = true,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(name = "Multiple Sections", showBackground = true)
@Composable
private fun PreviewMultipleSections() {
    NovelLibraryBaseTheme {
        Column {
            SettingsSection(title = "Display") {
                SettingsItem(
                    title = "Theme",
                    description = "Choose app theme",
                    onClick = {}
                )
            }
            SettingsSection(title = "Notifications") {
                SettingsSwitch(
                    title = "Enable Notifications",
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsSectionDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsSection(title = "Reader Settings") {
            SettingsItem(
                title = "Theme",
                description = "Choose reader theme",
                icon = Icons.Default.Palette,
                onClick = {}
            )
            SettingsItem(
                title = "Text Size",
                description = "Adjust reading text size",
                onClick = {}
            )
        }
    }
}
