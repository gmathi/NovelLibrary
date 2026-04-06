package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.gmathi.novellibrary.compose.ai_tts.AiTtsManageModelsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager

class AiTtsManageModelsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val modelManager = AiTtsModelManager(this)
        setContent {
            NovelLibraryTheme {
                AiTtsManageModelsScreen(
                    modelManager = modelManager,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
