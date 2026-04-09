package io.github.gmathi.novellibrary.model.preference

import android.content.Context
import android.content.SharedPreferences

data class AiTtsPreferences(val context: Context, val prefs: SharedPreferences) {

    var voiceId: String
        get() {
            val default = "kokoro_multi_lang"
            return prefs.getString("ai_tts_voice_id", default) ?: default
        }
        set(value) = prefs.edit().putString("ai_tts_voice_id", value).apply()

    /** Kokoro speaker ID (0–52). Default 0 (af_alloy). See KokoroVoiceHelper for the full list. */
    var kokoroSpeakerId: Int
        get() = prefs.getInt("ai_tts_kokoro_speaker_id", 0)
        set(value) = prefs.edit().putInt("ai_tts_kokoro_speaker_id", value).apply()

    /** Kokoro language code for the active speaker (e.g. "en", "hi", "ja"). */
    var kokoroLangCode: String
        get() = prefs.getString("ai_tts_kokoro_lang_code", "en") ?: "en"
        set(value) = prefs.edit().putString("ai_tts_kokoro_lang_code", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("ai_tts_speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("ai_tts_speech_rate", value).apply()

    var pitch: Float
        get() = prefs.getFloat("ai_tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("ai_tts_pitch", value).apply()

    var volumeNormalization: Boolean
        get() = prefs.getBoolean("ai_tts_volume_normalization", true)
        set(value) = prefs.edit().putBoolean("ai_tts_volume_normalization", value).apply()

    /** When enabled, punctuation-based silence is injected between sentences for natural pacing. */
    var smartPunctuation: Boolean
        get() = prefs.getBoolean("ai_tts_smart_punct", true)
        set(value) = prefs.edit().putBoolean("ai_tts_smart_punct", value).apply()

    /** When enabled, emotion tags like [sad], [angry], [whisper] in text are processed. Beta feature. */
    var emotionTags: Boolean
        get() = prefs.getBoolean("ai_tts_emotion_tags", false)
        set(value) = prefs.edit().putBoolean("ai_tts_emotion_tags", value).apply()

    var autoReadNextChapter: Boolean
        get() = prefs.getBoolean("ai_tts_auto_read_next_chapter", true)
        set(value) = prefs.edit().putBoolean("ai_tts_auto_read_next_chapter", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("ai_tts_keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("ai_tts_keep_screen_on", value).apply()
}
