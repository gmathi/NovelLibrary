package io.github.gmathi.novellibrary.service.tts

/**
 * TTS engine mode selection.
 */
enum class TtsEngineMode(val key: String) {
    /** Android system TextToSpeech engine */
    SYSTEM("system"),
    
    /** AI TTS using VITS-Piper via sherpa-onnx */
    AI_VITS("ai_vits");

    companion object {
        /**
         * Get engine mode from preference key.
         * @param key Preference key string
         * @return Corresponding TtsEngineMode, defaults to AI_VITS if key is invalid
         */
        fun fromKey(key: String): TtsEngineMode =
            entries.firstOrNull { it.key == key } ?: AI_VITS
    }
}
