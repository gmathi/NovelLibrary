package io.github.gmathi.novellibrary.service.ai_tts

/**
 * Kokoro multi-lang voice catalog (53 speakers).
 * Mirrors the reference project's KokoroVoiceHelper.java.
 */
data class KokoroVoice(
    val speakerId: Int,
    val voiceKey: String,
    val displayName: String,
    val language: String,
    val languageCode: String,
    val gender: String,
    val flag: String
) {
    val subtitle: String get() = "$language • $gender"
    val fullLabel: String get() = "$flag $displayName ($language $gender)"
}

object KokoroVoiceHelper {

    val ALL_VOICES: List<KokoroVoice> = listOf(
        // American Female
        KokoroVoice(0, "af_alloy", "Alloy", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(1, "af_aoede", "Aoede", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(2, "af_bella", "Bella", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(3, "af_heart", "Heart", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(4, "af_jessica", "Jessica", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(5, "af_kore", "Kore", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(6, "af_nicole", "Nicole", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(7, "af_nova", "Nova", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(8, "af_river", "River", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(9, "af_sarah", "Sarah", "English", "en", "Female", "🇺🇸"),
        KokoroVoice(10, "af_sky", "Sky", "English", "en", "Female", "🇺🇸"),
        // American Male
        KokoroVoice(11, "am_adam", "Adam", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(12, "am_echo", "Echo", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(13, "am_eric", "Eric", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(14, "am_fenrir", "Fenrir", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(15, "am_liam", "Liam", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(16, "am_michael", "Michael", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(17, "am_onyx", "Onyx", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(18, "am_puck", "Puck", "English", "en", "Male", "🇺🇸"),
        KokoroVoice(19, "am_santa", "Santa", "English", "en", "Male", "🇺🇸"),
        // British Female
        KokoroVoice(20, "bf_alice", "Alice", "English", "en", "Female", "🇬🇧"),
        KokoroVoice(21, "bf_emma", "Emma", "English", "en", "Female", "🇬🇧"),
        KokoroVoice(22, "bf_isabella", "Isabella", "English", "en", "Female", "🇬🇧"),
        KokoroVoice(23, "bf_lily", "Lily", "English", "en", "Female", "🇬🇧"),
        // British Male
        KokoroVoice(24, "bm_daniel", "Daniel", "English", "en", "Male", "🇬🇧"),
        KokoroVoice(25, "bm_fable", "Fable", "English", "en", "Male", "🇬🇧"),
        KokoroVoice(26, "bm_george", "George", "English", "en", "Male", "🇬🇧"),
        KokoroVoice(27, "bm_lewis", "Lewis", "English", "en", "Male", "🇬🇧"),
        // Spanish
        KokoroVoice(28, "ef_dora", "Dora", "Spanish", "es", "Female", "🇪🇸"),
        KokoroVoice(29, "em_alex", "Alex", "Spanish", "es", "Male", "🇪🇸"),
        // French
        KokoroVoice(30, "ff_siwis", "Siwis", "French", "fr", "Female", "🇫🇷"),
        // Hindi
        KokoroVoice(31, "hf_alpha", "Alpha", "Hindi", "hi", "Female", "🇮🇳"),
        KokoroVoice(32, "hf_beta", "Beta", "Hindi", "hi", "Female", "🇮🇳"),
        KokoroVoice(33, "hm_omega", "Omega", "Hindi", "hi", "Male", "🇮🇳"),
        KokoroVoice(34, "hm_psi", "Psi", "Hindi", "hi", "Male", "🇮🇳"),
        // Italian
        KokoroVoice(35, "if_sara", "Sara", "Italian", "it", "Female", "🇮🇹"),
        KokoroVoice(36, "im_nicola", "Nicola", "Italian", "it", "Male", "🇮🇹"),
        // Japanese
        KokoroVoice(37, "jf_alpha", "Alpha", "Japanese", "ja", "Female", "🇯🇵"),
        KokoroVoice(38, "jf_gongitsune", "Gongitsune", "Japanese", "ja", "Female", "🇯🇵"),
        KokoroVoice(39, "jf_nezumi", "Nezumi", "Japanese", "ja", "Female", "🇯🇵"),
        KokoroVoice(40, "jf_tebukuro", "Tebukuro", "Japanese", "ja", "Female", "🇯🇵"),
        KokoroVoice(41, "jm_kumo", "Kumo", "Japanese", "ja", "Male", "🇯🇵"),
        // Portuguese
        KokoroVoice(42, "pf_dora", "Dora", "Portuguese", "pt", "Female", "🇵🇹"),
        KokoroVoice(43, "pm_alex", "Alex", "Portuguese", "pt", "Male", "🇵🇹"),
        KokoroVoice(44, "pm_santa", "Santa", "Portuguese", "pt", "Male", "🇵🇹"),
        // Chinese
        KokoroVoice(45, "zf_xiaobei", "Xiaobei", "Chinese", "zh", "Female", "🇨🇳"),
        KokoroVoice(46, "zf_xiaoni", "Xiaoni", "Chinese", "zh", "Female", "🇨🇳"),
        KokoroVoice(47, "zf_xiaoxiao", "Xiaoxiao", "Chinese", "zh", "Female", "🇨🇳"),
        KokoroVoice(48, "zf_xiaoyi", "Xiaoyi", "Chinese", "zh", "Female", "🇨🇳"),
        KokoroVoice(49, "zm_yunjian", "Yunjian", "Chinese", "zh", "Male", "🇨🇳"),
        KokoroVoice(50, "zm_yunxi", "Yunxi", "Chinese", "zh", "Male", "🇨🇳"),
        KokoroVoice(51, "zm_yunxia", "Yunxia", "Chinese", "zh", "Male", "🇨🇳"),
        KokoroVoice(52, "zm_yunyang", "Yunyang", "Chinese", "zh", "Male", "🇨🇳"),
    )

    fun getById(speakerId: Int): KokoroVoice? = ALL_VOICES.find { it.speakerId == speakerId }
    fun getByKey(voiceKey: String): KokoroVoice? = ALL_VOICES.find { it.voiceKey == voiceKey }
    fun getByLanguage(langCode: String): List<KokoroVoice> = ALL_VOICES.filter { it.languageCode == langCode }
    fun getDefaultVoice(): KokoroVoice = ALL_VOICES[0]
    fun availableLanguages(): List<String> = ALL_VOICES.map { it.language }.distinct()
}
