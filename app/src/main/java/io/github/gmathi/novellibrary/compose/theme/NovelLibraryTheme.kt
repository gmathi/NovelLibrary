package io.github.gmathi.novellibrary.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.gmathi.novellibrary.model.preference.DataCenter
import uy.kohesive.injekt.injectLazy

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFFBCC7DC),
    onSecondary = Color(0xFF263141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
    outlineVariant = Color(0xFF42474E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2E3133),
    inversePrimary = Color(0xFF00639B),
    surfaceDim = Color(0xFF1A1C1E),
    surfaceBright = Color(0xFF404244),
    surfaceContainerLowest = Color(0xFF0F1113),
    surfaceContainerLow = Color(0xFF1A1C1E),
    surfaceContainer = Color(0xFF1E2022),
    surfaceContainerHigh = Color(0xFF282A2D),
    surfaceContainerHighest = Color(0xFF333538)
)

private val LightColorScheme = lightColorScheme(
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

@Composable
fun NovelLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    overrideDarkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val dataCenter: DataCenter by injectLazy()
    val useDarkTheme = overrideDarkTheme ?: try {
        dataCenter.appNightMode
    } catch (e: Exception) {
        // Fallback to system theme if DataCenter fails
        darkTheme
    }
    
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val context = view.context
                if (context is Activity) {
                    val window = context.window
                    window.statusBarColor = colorScheme.surface.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
                }
            } catch (e: Exception) {
                // Ignore status bar color errors
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
