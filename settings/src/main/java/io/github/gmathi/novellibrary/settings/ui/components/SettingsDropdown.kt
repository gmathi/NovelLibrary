package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme

/**
 * Selection settings item with a dropdown menu.
 * 
 * Provides a dropdown menu for selecting from multiple options. The selected
 * value is displayed in the trailing content area.
 * 
 * @param title The main title text for the setting
 * @param selectedValue The currently selected value
 * @param options List of available options to choose from
 * @param onOptionSelected Callback invoked when an option is selected
 * @param modifier Modifier for the item container
 * @param description Optional description text shown below the title
 * @param icon Optional leading icon
 * @param enabled Whether the dropdown is enabled and can be opened
 * @param optionLabel Function to convert an option to its display label
 */
@Composable
fun <T> SettingsDropdown(
    title: String,
    selectedValue: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() }
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        SettingsItem(
            title = title,
            description = description,
            icon = icon,
            enabled = enabled,
            onClick = if (enabled) {
                { expanded = true }
            } else {
                null
            },
            trailingContent = {
                Row {
                    Text(
                        text = optionLabel(selectedValue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        )
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(option),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selectedValue) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Basic Dropdown", showBackground = true)
@Composable
private fun PreviewSettingsDropdownBasic() {
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "Theme",
            selectedValue = "Light",
            options = listOf("Light", "Dark", "System"),
            onOptionSelected = {}
        )
    }
}

@Preview(name = "With Description", showBackground = true)
@Composable
private fun PreviewSettingsDropdownWithDescription() {
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "Language",
            description = "Select your preferred language",
            selectedValue = "English",
            options = listOf("English", "Spanish", "French", "German", "Japanese"),
            onOptionSelected = {}
        )
    }
}

@Preview(name = "With Icon", showBackground = true)
@Composable
private fun PreviewSettingsDropdownWithIcon() {
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "App Language",
            description = "Choose your preferred language",
            icon = Icons.Default.Language,
            selectedValue = "English",
            options = listOf("English", "Spanish", "French", "German"),
            onOptionSelected = {}
        )
    }
}

@Preview(name = "Reader Theme", showBackground = true)
@Composable
private fun PreviewSettingsDropdownReaderTheme() {
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "Reader Theme",
            description = "Customize your reading experience",
            icon = Icons.Default.Palette,
            selectedValue = "Sepia",
            options = listOf("Light", "Dark", "Sepia", "Black"),
            onOptionSelected = {}
        )
    }
}

@Preview(name = "Custom Type", showBackground = true)
@Composable
private fun PreviewSettingsDropdownCustomType() {
    data class FontOption(val name: String, val size: Int)
    
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "Font",
            description = "Select reading font",
            selectedValue = FontOption("Roboto", 16),
            options = listOf(
                FontOption("Roboto", 16),
                FontOption("Open Sans", 16),
                FontOption("Lora", 16)
            ),
            onOptionSelected = {},
            optionLabel = { it.name }
        )
    }
}

@Preview(name = "Disabled State", showBackground = true)
@Composable
private fun PreviewSettingsDropdownDisabled() {
    NovelLibraryBaseTheme {
        SettingsDropdown(
            title = "Premium Theme",
            description = "Available in premium version",
            icon = Icons.Default.Palette,
            selectedValue = "Default",
            options = listOf("Default", "Premium 1", "Premium 2"),
            enabled = false,
            onOptionSelected = {}
        )
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsDropdownDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        SettingsDropdown(
            title = "Language",
            description = "Select your preferred language",
            icon = Icons.Default.Language,
            selectedValue = "English",
            options = listOf("English", "Spanish", "French", "German"),
            onOptionSelected = {}
        )
    }
}
