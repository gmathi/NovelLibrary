package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.gmathi.novellibrary.compose.ai_tts.AiTtsManageModelsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.util.logging.Logs
import uy.kohesive.injekt.injectLazy

class AiTtsManageModelsActivity : ComponentActivity() {

    private val dataCenter: DataCenter by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val modelManager = AiTtsModelManager(this)
        val prefs = dataCenter.aiTtsPreferences
        var activeVoiceId by mutableStateOf(prefs.voiceId)
        Logs.debug("AiTtsManageModels", "onCreate: activeVoiceId='$activeVoiceId' availableVoices=${modelManager.availableVoices().size}")

        setContent {
            NovelLibraryTheme {
                AiTtsManageModelsScreen(
                    modelManager = modelManager,
                    activeVoiceId = activeVoiceId,
                    onVoiceSelected = { id ->
                        Logs.debug("AiTtsManageModels", "onVoiceSelected: id='$id'")
                        activeVoiceId = id
                        prefs.voiceId = id
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
