package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.compose.settings.WhatsChangedScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme

/**
 * "What's Changed" screen (About section). Now a Material 3 Compose screen that
 * renders the full version history from
 * [io.github.gmathi.novellibrary.util.changelog.Changelog] — the single source
 * of truth shared with the one-time update dialog. Add a release by prepending
 * a `ChangelogVersion` there; no assets file to edit.
 */
class WhatsChangedActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovelLibraryTheme {
                WhatsChangedScreen(onNavigateBack = { finish() })
            }
        }
    }
}
