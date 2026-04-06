package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.gmathi.novellibrary.compose.ai_tts.AiTtsSettingsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsVoiceInfo

class AiTtsSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataCenter = DataCenter(this)
        val prefs = dataCenter.aiTtsPreferences
        val modelManager = AiTtsModelManager(this)

        var speechRate by mutableFloatStateOf(prefs.speechRate)
        var pitch by mutableFloatStateOf(prefs.pitch)
        var autoReadNextChapter by mutableStateOf(prefs.autoReadNextChapter)
        var keepScreenOn by mutableStateOf(prefs.keepScreenOn)
        var volumeNormalization by mutableStateOf(prefs.volumeNormalization)
        var activeVoiceId by mutableStateOf(prefs.voiceId)
        val availableVoices: List<AiTtsVoiceInfo> = modelManager.availableVoices()

        setContent {
            NovelLibraryTheme {
                AiTtsSettingsScreen(
                    speechRate = speechRate,
                    pitch = pitch,
                    autoReadNextChapter = autoReadNextChapter,
                    keepScreenOn = keepScreenOn,
                    volumeNormalization = volumeNormalization,
                    activeVoiceId = activeVoiceId,
                    availableVoices = availableVoices,
                    onSpeechRateChange = { value ->
                        speechRate = value
                        prefs.speechRate = value
                    },
                    onPitchChange = { value ->
                        pitch = value
                        prefs.pitch = value
                    },
                    onAutoReadNextChapterChange = { value ->
                        autoReadNextChapter = value
                        prefs.autoReadNextChapter = value
                    },
                    onKeepScreenOnChange = { value ->
                        keepScreenOn = value
                        prefs.keepScreenOn = value
                    },
                    onVolumeNormalizationChange = { value ->
                        volumeNormalization = value
                        prefs.volumeNormalization = value
                    },
                    onVoiceSelected = { voice ->
                        activeVoiceId = voice.id
                        prefs.voiceId = voice.id
                    },
                    onManageModels = {
                        startActivity(android.content.Intent(this, AiTtsManageModelsActivity::class.java))
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
