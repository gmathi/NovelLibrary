package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme

/**
 * Boolean settings item with a switch control.
 * 
 * Built on top of SettingsItem, this component provides a consistent way to display
 * boolean settings with a Material 3 switch control.
 * 
 * @param title The main title text for the setting
 * @param checked The current state of the switch
 * @param onCheckedChange Callback invoked when the switch state changes
 * @param modifier Modifier for the item container
 * @param description Optional description text shown below the title
 * @param icon Optional leading icon
 * @param enabled Whether the switch is enabled and can be toggled
 */
@Composable
fun SettingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    SettingsItem(
        title = title,
        description = description,
        icon = icon,
        enabled = enabled,
        modifier = modifier,
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by item click
                enabled = enabled
            )
        }
    )
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Switch Checked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchChecked() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Enable Notifications",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "Switch Unchecked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchUnchecked() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Enable Notifications",
            checked = false,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "With Description Checked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchWithDescription() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Auto Sync",
            description = "Automatically sync your reading progress",
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "With Icon Checked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchWithIcon() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Push Notifications",
            description = "Receive notifications for new chapters",
            icon = Icons.Default.Notifications,
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "Disabled Checked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchDisabledChecked() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Premium Feature",
            description = "Available in premium version",
            icon = Icons.Default.Sync,
            checked = true,
            enabled = false,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "Disabled Unchecked", showBackground = true)
@Composable
private fun PreviewSettingsSwitchDisabledUnchecked() {
    NovelLibraryBaseTheme {
        SettingsSwitch(
            title = "Premium Feature",
            description = "Available in premium version",
            icon = Icons.Default.Sync,
            checked = false,
            enabled = false,
            onCheckedChange = {}
        )
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsSwitchDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsSwitch(
            title = "Volume Key Navigation",
            description = "Use volume keys to navigate pages",
            icon = Icons.Default.VolumeUp,
            checked = true,
            onCheckedChange = {}
        )
    }
}
