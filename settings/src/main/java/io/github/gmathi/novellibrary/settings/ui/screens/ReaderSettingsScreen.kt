package io.github.gmathi.novellibrary.settings.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
            onClick = {
                // TODO: Open color picker dialog
                // For now, this is a placeholder
            }
        )
        
        SettingsItem(
            title = "Night Mode Background",
            description = "Customize dark theme background color",
            icon = Icons.Default.DarkMode,
            onClick = {
                // TODO: Open color picker dialog
                // For now, this is a placeholder
            }
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
    MaterialTheme {
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
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            ReaderSettingsScreen(
                viewModel = createPreviewViewModel(),
                onNavigateBack = {}
            )
        }
    }
}

@Preview(name = "Reader Settings Screen", showBackground = true)
@Composable
private fun PreviewReaderSettingsScreen() {
    MaterialTheme {
        ReaderSettingsScreen(
            viewModel = createPreviewViewModel(),
            onNavigateBack = {}
        )
    }
}

@Preview(name = "Reader Settings Dark", showBackground = true)
@Composable
private fun PreviewReaderSettingsScreenDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            ReaderSettingsScreen(
                viewModel = createPreviewViewModel(),
                onNavigateBack = {}
            )
        }
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
