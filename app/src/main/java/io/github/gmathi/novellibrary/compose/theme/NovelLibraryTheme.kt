package io.github.gmathi.novellibrary.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import uy.kohesive.injekt.injectLazy

/**
 * App-level theme that resolves the dark theme preference from DataCenter
 * and delegates to the shared [NovelLibraryBaseTheme] from the stubs module.
 *
 * Use this in the app module where DataCenter is available via Injekt.
 * Feature modules (settings, etc.) should use [NovelLibraryBaseTheme]
 * directly and pass the dark theme flag explicitly.
 */
@Composable
fun NovelLibraryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val dataCenter: DataCenter by injectLazy()
    val useDarkTheme = try {
        dataCenter.isDarkTheme
    } catch (e: Exception) {
        darkTheme
    }

    NovelLibraryBaseTheme(
        darkTheme = useDarkTheme,
        content = content
    )
}
