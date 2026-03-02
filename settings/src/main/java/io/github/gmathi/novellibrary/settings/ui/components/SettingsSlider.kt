package io.github.gmathi.novellibrary.settings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Numeric range settings item with a slider control.
 * 
 * Provides a slider for selecting numeric values within a range, with optional
 * value labels and step increments.
 * 
 * @param title The main title text for the setting
 * @param value The current value of the slider
 * @param onValueChange Callback invoked when the slider value changes
 * @param valueRange The range of values the slider can represent
 * @param modifier Modifier for the item container
 * @param description Optional description text shown below the title
 * @param icon Optional leading icon
 * @param enabled Whether the slider is enabled and can be adjusted
 * @param steps Number of discrete steps between min and max (0 for continuous)
 * @param showValue Whether to display the current value as a label
 * @param valueFormatter Optional function to format the value label (default shows integer)
 */
@Composable
fun SettingsSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    steps: Int = 0,
    showValue: Boolean = true,
    valueFormatter: (Float) -> String = { it.roundToInt().toString() }
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header row with icon, title, and value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Leading icon
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 16.dp),
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
                
                // Title
                Column(
                    modifier = Modifier.weight(1f)
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
                
                // Value label
                if (showValue) {
                    Text(
                        text = valueFormatter(value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            
            // Slider
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================================================
// Preview Functions
// ============================================================================

@Preview(name = "Basic Slider", showBackground = true)
@Composable
private fun PreviewSettingsSliderBasic() {
    MaterialTheme {
        SettingsSlider(
            title = "Text Size",
            value = 16f,
            onValueChange = {},
            valueRange = 12f..24f
        )
    }
}

@Preview(name = "With Description", showBackground = true)
@Composable
private fun PreviewSettingsSliderWithDescription() {
    MaterialTheme {
        SettingsSlider(
            title = "Text Size",
            description = "Adjust the reading text size",
            value = 18f,
            onValueChange = {},
            valueRange = 12f..24f
        )
    }
}

@Preview(name = "With Icon", showBackground = true)
@Composable
private fun PreviewSettingsSliderWithIcon() {
    MaterialTheme {
        SettingsSlider(
            title = "Text Size",
            description = "Adjust the reading text size",
            icon = Icons.Default.FormatSize,
            value = 16f,
            onValueChange = {},
            valueRange = 12f..24f
        )
    }
}

@Preview(name = "With Steps", showBackground = true)
@Composable
private fun PreviewSettingsSliderWithSteps() {
    MaterialTheme {
        SettingsSlider(
            title = "Scroll Speed",
            description = "Control auto-scroll speed",
            icon = Icons.Default.Speed,
            value = 5f,
            onValueChange = {},
            valueRange = 1f..10f,
            steps = 8
        )
    }
}

@Preview(name = "Custom Formatter", showBackground = true)
@Composable
private fun PreviewSettingsSliderCustomFormatter() {
    MaterialTheme {
        SettingsSlider(
            title = "Volume",
            description = "Adjust TTS volume level",
            icon = Icons.Default.VolumeUp,
            value = 75f,
            onValueChange = {},
            valueRange = 0f..100f,
            valueFormatter = { "${it.roundToInt()}%" }
        )
    }
}

@Preview(name = "Without Value Label", showBackground = true)
@Composable
private fun PreviewSettingsSliderNoValue() {
    MaterialTheme {
        SettingsSlider(
            title = "Line Spacing",
            description = "Adjust space between lines",
            value = 1.5f,
            onValueChange = {},
            valueRange = 1.0f..2.0f,
            showValue = false
        )
    }
}

@Preview(name = "Disabled State", showBackground = true)
@Composable
private fun PreviewSettingsSliderDisabled() {
    MaterialTheme {
        SettingsSlider(
            title = "Premium Setting",
            description = "Available in premium version",
            icon = Icons.Default.FormatSize,
            value = 16f,
            onValueChange = {},
            valueRange = 12f..24f,
            enabled = false
        )
    }
}

@Preview(name = "Dark Theme", showBackground = true)
@Composable
private fun PreviewSettingsSliderDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            SettingsSlider(
                title = "Text Size",
                description = "Adjust the reading text size",
                icon = Icons.Default.FormatSize,
                value = 18f,
                onValueChange = {},
                valueRange = 12f..24f,
                steps = 11
            )
        }
    }
}
