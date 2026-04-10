package io.github.gmathi.novellibrary.activity.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import uy.kohesive.injekt.injectLazy

class BackupRestoreActivity : ComponentActivity() {

    private val dataCenter: DataCenter by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            NovelLibraryTheme {
                BackupRestoreScreen(
                    onBack = { finish() },
                    dataCenter = dataCenter
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }
}
