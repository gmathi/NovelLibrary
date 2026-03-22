package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme

/**
 * Standard settings list item with title, description, icon, and trailing content.
 * 
 * This is the base component for all settings items, providing a consistent layout
 * and appearance across all settings screens.
 * 
 * @param title The main title text for the setting
 * @param modifier Modifier for the item container
 * @param description Optional description text shown below the title
 * @param icon Optional leading icon
 * @param enabled Whether the item is enabled and clickable
 * @param onClick Optional click handler
 * @param trailingContent Optional composable content shown at the end (e.g., switch, value)
 */
@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(24.dp),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            // Title and description
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                
                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            
            // Trailing content
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Basic Item", showBackground = true)
@Composable
private fun PreviewSettingsItemBasic() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "General Settings",
            onClick = {}
        )
    }
}

@Preview(name = "With Description", showBackground = true)
@Composable
private fun PreviewSettingsItemWithDescription() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "Theme",
            description = "Choose your preferred app theme",
            onClick = {}
        )
    }
}

@Preview(name = "With Icon", showBackground = true)
@Composable
private fun PreviewSettingsItemWithIcon() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "Notifications",
            description = "Manage notification preferences",
            icon = Icons.Default.Notifications,
            onClick = {}
        )
    }
}

@Preview(name = "With Trailing Text", showBackground = true)
@Composable
private fun PreviewSettingsItemWithTrailing() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "Language",
            description = "Select app language",
            icon = Icons.Default.Settings,
            onClick = {},
            trailingContent = {
                Text(
                    text = "English",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Preview(name = "Disabled State", showBackground = true)
@Composable
private fun PreviewSettingsItemDisabled() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "Premium Feature",
            description = "Available in premium version",
            icon = Icons.Default.Palette,
            enabled = false,
            onClick = {}
        )
    }
}

@Preview(name = "Long Text", showBackground = true)
@Composable
private fun PreviewSettingsItemLongText() {
    NovelLibraryBaseTheme {
        SettingsItem(
            title = "Synchronization Settings",
            description = "Configure automatic synchronization of your reading progress, bookmarks, and preferences across all your devices",
            icon = Icons.Default.Settings,
            onClick = {},
            trailingContent = {
                Text(
                    text = "Enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsItemDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsItem(
            title = "Reader Theme",
            description = "Customize reading experience",
            icon = Icons.Default.Palette,
            onClick = {}
        )
    }
}
