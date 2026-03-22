package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.settings.data.datastore.FakeSettingsDataStore
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.ui.components.*
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel

/**
 * Reader Settings Screen
 * 
 * Consolidates reader-related settings from multiple old activities:
 * - ReaderSettingsActivity (text size, font, theme)
 * - ReaderBackgroundSettingsActivity (background colors)
 * - ScrollBehaviourSettingsActivity (scroll behavior, volume keys)
 * - TTSSettingsActivity (text-to-speech settings)
 * 
 * Organized into 4 sections:
 * 1. Text & Display - text size, font, line spacing
 * 2. Theme - light/dark/sepia selection, custom background
 * 3. Scroll Behavior - scroll speed, volume key navigation, tap to scroll
 * 4. Text-to-Speech - TTS enable, voice configuration
 * 
 * @param viewModel ViewModel managing reader settings state
 * @param onNavigateBack Callback to navigate back to main settings
 * @param modifier Modifier for the screen
 */
@Composable
fun ReaderSettingsScreen(
    viewModel: ReaderSettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Collect state from ViewModel
    val textSize by viewModel.textSize.collectAsState()
    val fontPath by viewModel.fontPath.collectAsState()
    val limitImageWidth by viewModel.limitImageWidth.collectAsState()
    
    val dayModeBackgroundColor by viewModel.dayModeBackgroundColor.collectAsState()
    val nightModeBackgroundColor by viewModel.nightModeBackgroundColor.collectAsState()
    val keepTextColor by viewModel.keepTextColor.collectAsState()
    val alternativeTextColors by viewModel.alternativeTextColors.collectAsState()
    
    val readerMode by viewModel.readerMode.collectAsState()
    val japSwipe by viewModel.japSwipe.collectAsState()
    val showReaderScroll by viewModel.showReaderScroll.collectAsState()
    val enableVolumeScroll by viewModel.enableVolumeScroll.collectAsState()
    val volumeScrollLength by viewModel.volumeScrollLength.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val enableImmersiveMode by viewModel.enableImmersiveMode.collectAsState()
    
    val enableAutoScroll by viewModel.enableAutoScroll.collectAsState()
    val autoScrollLength by viewModel.autoScrollLength.collectAsState()
    val autoScrollInterval by viewModel.autoScrollInterval.collectAsState()
    
    SettingsScreen(
        title = "Reader Settings",
        onNavigateBack = onNavigateBack,
        modifier = modifier
    ) {
        ReaderSettingsContent(
            textSize = textSize,
            onTextSizeChange = viewModel::setTextSize,
            fontPath = fontPath,
            onFontPathChange = viewModel::setFontPath,
            limitImageWidth = limitImageWidth,
            onLimitImageWidthChange = viewModel::setLimitImageWidth,
            
            dayModeBackgroundColor = dayModeBackgroundColor,
            onDayModeBackgroundColorChange = viewModel::setDayModeBackgroundColor,
            nightModeBackgroundColor = nightModeBackgroundColor,
            onNightModeBackgroundColorChange = viewModel::setNightModeBackgroundColor,
            keepTextColor = keepTextColor,
            onKeepTextColorChange = viewModel::setKeepTextColor,
            alternativeTextColors = alternativeTextColors,
            onAlternativeTextColorsChange = viewModel::setAlternativeTextColors,
            
            readerMode = readerMode,
            onReaderModeChange = viewModel::setReaderMode,
            japSwipe = japSwipe,
            onJapSwipeChange = viewModel::setJapSwipe,
            showReaderScroll = showReaderScroll,
            onShowReaderScrollChange = viewModel::setShowReaderScroll,
            enableVolumeScroll = enableVolumeScroll,
            onEnableVolumeScrollChange = viewModel::setEnableVolumeScroll,
            volumeScrollLength = volumeScrollLength,
            onVolumeScrollLengthChange = viewModel::setVolumeScrollLength,
            keepScreenOn = keepScreenOn,
            onKeepScreenOnChange = viewModel::setKeepScreenOn,
            enableImmersiveMode = enableImmersiveMode,
            onEnableImmersiveModeChange = viewModel::setEnableImmersiveMode,
            
            enableAutoScroll = enableAutoScroll,
            onEnableAutoScrollChange = viewModel::setEnableAutoScroll,
            autoScrollLength = autoScrollLength,
            onAutoScrollLengthChange = viewModel::setAutoScrollLength,
            autoScrollInterval = autoScrollInterval,
            onAutoScrollIntervalChange = viewModel::setAutoScrollInterval
        )
    }
}

/**
 * Content for Reader Settings Screen.
 * 
 * Separated from the screen composable for easier testing and preview.
 * Contains all four sections with their respective settings.
 */
@Composable
private fun ColumnScope.ReaderSettingsContent(
    textSize: Int,
    onTextSizeChange: (Int) -> Unit,
    fontPath: String,
    onFontPathChange: (String) -> Unit,
    limitImageWidth: Boolean,
    onLimitImageWidthChange: (Boolean) -> Unit,
    
    dayModeBackgroundColor: Int,
    onDayModeBackgroundColorChange: (Int) -> Unit,
    nightModeBackgroundColor: Int,
    onNightModeBackgroundColorChange: (Int) -> Unit,
    keepTextColor: Boolean,
    onKeepTextColorChange: (Boolean) -> Unit,
    alternativeTextColors: Boolean,
    onAlternativeTextColorsChange: (Boolean) -> Unit,
    
    readerMode: Boolean,
    onReaderModeChange: (Boolean) -> Unit,
    japSwipe: Boolean,
    onJapSwipeChange: (Boolean) -> Unit,
    showReaderScroll: Boolean,
    onShowReaderScrollChange: (Boolean) -> Unit,
    enableVolumeScroll: Boolean,
    onEnableVolumeScrollChange: (Boolean) -> Unit,
    volumeScrollLength: Int,
    onVolumeScrollLengthChange: (Int) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    enableImmersiveMode: Boolean,
    onEnableImmersiveModeChange: (Boolean) -> Unit,
    
    enableAutoScroll: Boolean,
    onEnableAutoScrollChange: (Boolean) -> Unit,
    autoScrollLength: Int,
    onAutoScrollLengthChange: (Int) -> Unit,
    autoScrollInterval: Int,
    onAutoScrollIntervalChange: (Int) -> Unit
) {
    // Color picker dialog states
    var showDayColorPicker by remember { mutableStateOf(false) }
    var showNightColorPicker by remember { mutableStateOf(false) }
    
    // Section 1: Text & Display
    SettingsSection(title = "Text & Display") {
        SettingsSlider(
            title = "Text Size",
            description = "Adjust reading text size (${textSize}sp)",
            icon = Icons.Default.FormatSize,
            value = textSize.toFloat(),
            onValueChange = { onTextSizeChange(it.toInt()) },
            valueRange = 12f..32f,
            steps = 19 // 12, 13, 14, ..., 32 = 21 values, so 19 steps
        )
        
        SettingsDropdown(
            title = "Font",
            description = if (fontPath.isEmpty()) "System Default" else "Custom Font",
            icon = Icons.Default.FontDownload,
            selectedValue = if (fontPath.isEmpty()) "System Default" else "Custom",
            options = listOf("System Default", "Custom"),
            onOptionSelected = { selected ->
                onFontPathChange(if (selected == "System Default") "" else fontPath)
            }
        )
        
        SettingsSwitch(
            title = "Limit Image Width",
            description = "Prevent images from exceeding screen width",
            icon = Icons.Default.Image,
            checked = limitImageWidth,
            onCheckedChange = onLimitImageWidthChange
        )
    }
    
    // Section 2: Theme
    SettingsSection(title = "Theme") {
        SettingsItem(
            title = "Day Mode Background",
            description = "Customize light theme background color",
            icon = Icons.Default.LightMode,
            onClick = { showDayColorPicker = true }
        )
        
        SettingsItem(
            title = "Night Mode Background",
            description = "Customize dark theme background color",
            icon = Icons.Default.DarkMode,
            onClick = { showNightColorPicker = true }
        )
        
        SettingsSwitch(
            title = "Keep Text Color",
            description = "Preserve original text colors from web pages",
            icon = Icons.Default.Palette,
            checked = keepTextColor,
            onCheckedChange = onKeepTextColorChange
        )
        
        SettingsSwitch(
            title = "Alternative Text Colors",
            description = "Use alternative color scheme for better readability",
            icon = Icons.Default.ColorLens,
            checked = alternativeTextColors,
            onCheckedChange = onAlternativeTextColorsChange
        )
    }
    
    // Day mode color picker dialog
    if (showDayColorPicker) {
        ColorPickerDialog(
            title = "Day Mode Background",
            currentColor = dayModeBackgroundColor,
            onDismiss = { showDayColorPicker = false },
            onColorSelected = { color ->
                onDayModeBackgroundColorChange(color)
                showDayColorPicker = false
            }
        )
    }
    
    // Night mode color picker dialog
    if (showNightColorPicker) {
        ColorPickerDialog(
            title = "Night Mode Background",
            currentColor = nightModeBackgroundColor,
            onDismiss = { showNightColorPicker = false },
            onColorSelected = { color ->
                onNightModeBackgroundColorChange(color)
                showNightColorPicker = false
            }
        )
    }
    
    // Section 3: Scroll Behavior
    SettingsSection(title = "Scroll Behavior") {
        SettingsSwitch(
            title = "Reader Mode",
            description = "Enable optimized reading mode with smooth scrolling",
            icon = Icons.Default.MenuBook,
            checked = readerMode,
            onCheckedChange = onReaderModeChange
        )
        
        SettingsSwitch(
            title = "Japanese Swipe Direction",
            description = "Reverse swipe direction for right-to-left reading",
            icon = Icons.Default.SwapHoriz,
            checked = japSwipe,
            onCheckedChange = onJapSwipeChange
        )
        
        SettingsSwitch(
            title = "Show Scroll Indicator",
            description = "Display scroll position indicator while reading",
            icon = Icons.Default.LinearScale,
            checked = showReaderScroll,
            onCheckedChange = onShowReaderScrollChange
        )
        
        SettingsSwitch(
            title = "Volume Key Navigation",
            description = "Use volume keys to scroll pages",
            icon = Icons.Default.VolumeUp,
            checked = enableVolumeScroll,
            onCheckedChange = onEnableVolumeScrollChange
        )
        
        if (enableVolumeScroll) {
            SettingsSlider(
                title = "Volume Scroll Distance",
                description = "Distance to scroll per volume key press (${volumeScrollLength}px)",
                value = volumeScrollLength.toFloat(),
                onValueChange = { onVolumeScrollLengthChange(it.toInt()) },
                valueRange = 50f..500f,
                steps = 44 // 50 to 500 in steps of 10
            )
        }
        
        SettingsSwitch(
            title = "Keep Screen On",
            description = "Prevent screen from sleeping while reading",
            icon = Icons.Default.ScreenLockPortrait,
            checked = keepScreenOn,
            onCheckedChange = onKeepScreenOnChange
        )
        
        SettingsSwitch(
            title = "Immersive Mode",
            description = "Hide system bars for distraction-free reading",
            icon = Icons.Default.Fullscreen,
            checked = enableImmersiveMode,
            onCheckedChange = onEnableImmersiveModeChange
        )
    }
    
    // Section 4: Auto-Scroll (part of scroll behavior but separate for clarity)
    SettingsSection(title = "Auto-Scroll") {
        SettingsSwitch(
            title = "Enable Auto-Scroll",
            description = "Automatically scroll pages at a steady pace",
            icon = Icons.Default.PlayArrow,
            checked = enableAutoScroll,
            onCheckedChange = onEnableAutoScrollChange
        )
        
        if (enableAutoScroll) {
            SettingsSlider(
                title = "Scroll Distance",
                description = "Distance to scroll per interval (${autoScrollLength}px)",
                value = autoScrollLength.toFloat(),
                onValueChange = { onAutoScrollLengthChange(it.toInt()) },
                valueRange = 50f..500f,
                steps = 44
            )
            
            SettingsSlider(
                title = "Scroll Interval",
                description = "Time between scrolls (${autoScrollInterval}ms)",
                value = autoScrollInterval.toFloat(),
                onValueChange = { onAutoScrollIntervalChange(it.toInt()) },
                valueRange = 500f..5000f,
                steps = 44
            )
        }
    }
}


/**
 * Color picker dialog with preset color swatches.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    title: String,
    currentColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val presetColors = listOf(
        0xFFFFFFFF.toInt() to "White",
        0xFF000000.toInt() to "Black",
        0xFFF5F5DC.toInt() to "Beige",
        0xFFFAF0E6.toInt() to "Linen",
        0xFFE8E8E8.toInt() to "Light Gray",
        0xFF333333.toInt() to "Dark Gray",
        0xFFFFF8E1.toInt() to "Warm White",
        0xFFE0E0E0.toInt() to "Silver",
        0xFF1A1A2E.toInt() to "Dark Navy",
        0xFF2D2D2D.toInt() to "Charcoal",
        0xFFF5E6CA.toInt() to "Sepia",
        0xFFE8F5E9.toInt() to "Mint"
    )
    
    var selectedColor by remember { mutableIntStateOf(currentColor) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presetColors.forEach { (color, _) ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (Color(color).luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(selectedColor) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Calculate relative luminance of a color for contrast decisions.
 */
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}


// ============================================================================
// Preview Functions
// ============================================================================

@Preview(
    name = "Reader Settings - Full Screen Light",
    showBackground = true,
    heightDp = 1800
)
@Composable
private fun PreviewReaderSettingsScreenFullLight() {
    NovelLibraryBaseTheme {
        ReaderSettingsScreen(
            viewModel = createPreviewViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Reader Settings - Full Screen Dark",
    showBackground = true,
    heightDp = 1800
)
@Composable
private fun PreviewReaderSettingsScreenFullDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        ReaderSettingsScreen(
            viewModel = createPreviewViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Reader Settings Screen", showBackground = true)
@Composable
private fun PreviewReaderSettingsScreen() {
    NovelLibraryBaseTheme {
        ReaderSettingsScreen(
            viewModel = createPreviewViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Reader Settings Dark", showBackground = true)
@Composable
private fun PreviewReaderSettingsScreenDark() {
    NovelLibraryBaseTheme(darkTheme = true) {
        ReaderSettingsScreen(
            viewModel = createPreviewViewModel(),
            onNavigateBack = {}
        )
    }
}

/**
 * Creates a preview ViewModel with mock data for Compose previews.
 */
private fun createPreviewViewModel(): ReaderSettingsViewModel {
    val fakeDataStore = FakeSettingsDataStore()
    val repository = SettingsRepositoryDataStore(fakeDataStore)
    return ReaderSettingsViewModel(repository)
}
