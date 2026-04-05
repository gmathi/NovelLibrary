package io.github.gmathi.novellibrary.model.preference

import android.content.Context
import android.content.SharedPreferences

data class AiTtsPreferences(val context: Context, val prefs: SharedPreferences) {

    var voiceId: String
        get() = prefs.getString("ai_tts_voice_id", "en_US-ryan-high") ?: "en_US-ryan-high"
        set(value) = prefs.edit().putString("ai_tts_voice_id", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("ai_tts_speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("ai_tts_speech_rate", value).apply()

    var pitch: Float
        get() = prefs.getFloat("ai_tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("ai_tts_pitch", value).apply()

    var volumeNormalization: Boolean
        get() = prefs.getBoolean("ai_tts_volume_normalization", true)
        set(value) = prefs.edit().putBoolean("ai_tts_volume_normalization", value).apply()

    var autoReadNextChapter: Boolean
        get() = prefs.getBoolean("ai_tts_auto_read_next_chapter", true)
        set(value) = prefs.edit().putBoolean("ai_tts_auto_read_next_chapter", value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("ai_tts_keep_screen_on", false)
        set(value) = prefs.edit().putBoolean("ai_tts_keep_screen_on", value).apply()
}
