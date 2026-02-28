package io.github.gmathi.novellibrary.service.tts

/**
 * Voice presets for AI TTS models.
 * 
 * NOTE: Current VITS-Piper models (kusal, lessac) only support sid=0.
 * This enum is retained for future multi-speaker model support.
 * Each preset corresponds to a speaker ID (sid) in multi-speaker models.
 */
enum class AiVoicePreset(val sid: Int, val displayName: String, val gender: String) {
    EXPR_VOICE_2_M(0, "expr-voice-2-m", "Male"),
    EXPR_VOICE_2_F(1, "expr-voice-2-f", "Female"),
    EXPR_VOICE_3_M(2, "expr-voice-3-m", "Male"),
    EXPR_VOICE_3_F(3, "expr-voice-3-f", "Female"),
    EXPR_VOICE_4_M(4, "expr-voice-4-m", "Male"),
    EXPR_VOICE_4_F(5, "expr-voice-4-f", "Female"),
    EXPR_VOICE_5_M(6, "expr-voice-5-m", "Male"),
    EXPR_VOICE_5_F(7, "expr-voice-5-f", "Female");

    companion object {
        /**
         * Get voice preset by speaker ID.
         * @param sid Speaker ID (0-7)
         * @return Corresponding AiVoicePreset, defaults to EXPR_VOICE_2_M if invalid
         */
        fun fromSid(sid: Int): AiVoicePreset = 
            entries.firstOrNull { it.sid == sid } ?: EXPR_VOICE_2_M
    }
}
