package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.gmathi.novellibrary.compose.ai_tts.AiTtsControlsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme

class AiTtsControlsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val novelTitle = intent.getStringExtra(EXTRA_NOVEL_TITLE) ?: ""
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE) ?: ""

        setContent {
            NovelLibraryTheme {
                AiTtsControlsScreen(
                    novelTitle = novelTitle,
                    chapterTitle = chapterTitle,
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_NOVEL_TITLE = "extra_novel_title"
        const val EXTRA_CHAPTER_TITLE = "extra_chapter_title"

        fun createIntent(context: Context, novelTitle: String = "", chapterTitle: String = ""): Intent {
            return Intent(context, AiTtsControlsActivity::class.java).apply {
                putExtra(EXTRA_NOVEL_TITLE, novelTitle)
                putExtra(EXTRA_CHAPTER_TITLE, chapterTitle)
            }
        }
    }
}
