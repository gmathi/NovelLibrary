package io.github.gmathi.novellibrary.stubs.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shared color schemes for the Novel Library app.
 *
 * This is the single source of truth for all color definitions.
 * Both the app module's NovelLibraryTheme and the settings module's
 * SettingsTheme consume these schemes, ensuring visual consistency.
 */

// Dark color scheme aligned with the app's existing dark theme palette:
// colorDarkKnight (#182128), colorLightKnight (#252e39), accent teal (#009688)
val NovelLibraryDarkColorScheme = darkColorScheme(
    primary = Color(0xFF009688),           // Teal accent matching app's colorAccent
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = Color(0xFF80CBC4),          // Lighter teal for secondary
    onSecondary = Color(0xFF00332E),
    secondaryContainer = Color(0xFF1A3F3B),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFF90CAF9),           // Light blue for tertiary accents
    onTertiary = Color(0xFF003258),
    tertiaryContainer = Color(0xFF00497D),
    onTertiaryContainer = Color(0xFFCCE5FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF182128),         // colorDarkKnight - app's main dark bg
    onBackground = Color(0xFFE2E6EA),
    surface = Color(0xFF182128),            // colorDarkKnight - consistent surface
    onSurface = Color(0xFFE2E6EA),
    surfaceVariant = Color(0xFF2E3A42),
    onSurfaceVariant = Color(0xFFBDC7CF),
    outline = Color(0xFF87919A),
    outlineVariant = Color(0xFF2E3A42),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2E6EA),
    inverseOnSurface = Color(0xFF2E3133),
    inversePrimary = Color(0xFF00796B),
    surfaceDim = Color(0xFF141C22),
    surfaceBright = Color(0xFF3A4550),
    surfaceContainerLowest = Color(0xFF0F161C),
    surfaceContainerLow = Color(0xFF182128),   // colorDarkKnight
    surfaceContainer = Color(0xFF1E2830),
    surfaceContainerHigh = Color(0xFF252E39),  // colorLightKnight - elevated surfaces
    surfaceContainerHighest = Color(0xFF2E3A44)
)

val NovelLibraryLightColorScheme = lightColorScheme(
    primary = Color(0xFF00639B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCE5FF),
    onPrimaryContainer = Color(0xFF001D32),
    secondary = Color(0xFF526070),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E4F7),
    onSecondaryContainer = Color(0xFF0E1D2A),
    tertiary = Color(0xFF6A5677),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F),
    outlineVariant = Color(0xFFC2C7CF),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3133),
    inverseOnSurface = Color(0xFFF0F0F4),
    inversePrimary = Color(0xFF90CAF9),
    surfaceDim = Color(0xFFD9D9DD),
    surfaceBright = Color(0xFFFCFCFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F7),
    surfaceContainer = Color(0xFFEDEDF1),
    surfaceContainerHigh = Color(0xFFE7E8EC),
    surfaceContainerHighest = Color(0xFFE2E2E6)
)