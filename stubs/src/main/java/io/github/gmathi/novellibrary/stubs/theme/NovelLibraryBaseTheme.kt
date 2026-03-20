package io.github.gmathi.novellibrary.stubs.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Base theme composable shared across all modules.
 *
 * Applies the Novel Library color schemes and configures the status bar.
 * Feature modules should use this directly. The app module's
 * NovelLibraryTheme wraps this and resolves the dark theme preference
 * from DataCenter.
 *
 * @param darkTheme Whether to use the dark color scheme.
 * @param content The composable content to theme.
 */
@Composable
fun NovelLibraryBaseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        NovelLibraryDarkColorScheme
    } else {
        NovelLibraryLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val context = view.context
                if (context is Activity) {
                    val window = context.window
                    window.statusBarColor = colorScheme.primary.toArgb()
                    WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = !darkTheme
                }
            } catch (_: Exception) { }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
