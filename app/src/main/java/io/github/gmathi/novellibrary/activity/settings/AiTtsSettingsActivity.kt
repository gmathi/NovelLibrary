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
import io.github.gmathi.novellibrary.util.logging.Logs
import uy.kohesive.injekt.injectLazy

class AiTtsSettingsActivity : ComponentActivity() {

    private val dataCenter: DataCenter by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = dataCenter.aiTtsPreferences
        val modelManager = AiTtsModelManager(this)
        Logs.debug("AiTtsSettings", "onCreate: voiceId='${prefs.voiceId}' speechRate=${prefs.speechRate} pitch=${prefs.pitch} keepScreenOn=${prefs.keepScreenOn}")

        var speechRate by mutableFloatStateOf(prefs.speechRate)
        var pitch by mutableFloatStateOf(prefs.pitch)
        var autoReadNextChapter by mutableStateOf(prefs.autoReadNextChapter)
        var keepScreenOn by mutableStateOf(prefs.keepScreenOn)
        var volumeNormalization by mutableStateOf(prefs.volumeNormalization)
        var smartPunctuation by mutableStateOf(prefs.smartPunctuation)
        var emotionTags by mutableStateOf(prefs.emotionTags)
        var activeVoiceId by mutableStateOf(prefs.voiceId)
        var kokoroSpeakerId by mutableStateOf(prefs.kokoroSpeakerId)
        val availableVoices: List<AiTtsVoiceInfo> = modelManager.availableVoices()

        setContent {
            NovelLibraryTheme {
                AiTtsSettingsScreen(
                    speechRate = speechRate,
                    pitch = pitch,
                    autoReadNextChapter = autoReadNextChapter,
                    keepScreenOn = keepScreenOn,
                    volumeNormalization = volumeNormalization,
                    smartPunctuation = smartPunctuation,
                    emotionTags = emotionTags,
                    activeVoiceId = activeVoiceId,
                    availableVoices = availableVoices,
                    modelManager = modelManager,
                    kokoroSpeakerId = kokoroSpeakerId,
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
                    onSmartPunctuationChange = { value ->
                        smartPunctuation = value
                        prefs.smartPunctuation = value
                    },
                    onEmotionTagsChange = { value ->
                        emotionTags = value
                        prefs.emotionTags = value
                    },
                    onVoiceSelected = { voice ->
                        activeVoiceId = voice.id
                        prefs.voiceId = voice.id
                    },
                    onKokoroVoiceSelected = { kokoroVoice ->
                        kokoroSpeakerId = kokoroVoice.speakerId
                        prefs.kokoroSpeakerId = kokoroVoice.speakerId
                        prefs.kokoroLangCode = kokoroVoice.languageCode
                    },
                    onManageModels = {
                        startActivity(android.content.Intent(this, AiTtsManageModelsActivity::class.java))
                    },
                    onClearCache = {
                        Logs.info("AiTtsSettings", "onClearCache: deleting all models and resetting preferences")
                        // Delete all downloaded models
                        modelManager.availableVoices().forEach { voice ->
                            modelManager.deleteModel(voice.id)
                        }
                        // Reset preferences to defaults
                        speechRate = 1.0f; prefs.speechRate = 1.0f
                        pitch = 1.0f; prefs.pitch = 1.0f
                        smartPunctuation = true; prefs.smartPunctuation = true
                        emotionTags = false; prefs.emotionTags = false
                        autoReadNextChapter = true; prefs.autoReadNextChapter = true
                        keepScreenOn = false; prefs.keepScreenOn = false
                        volumeNormalization = true; prefs.volumeNormalization = true
                        activeVoiceId = modelManager.defaultVoiceId()
                        prefs.voiceId = activeVoiceId
                        kokoroSpeakerId = 0
                        prefs.kokoroSpeakerId = 0
                        prefs.kokoroLangCode = "en"
                    },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}
