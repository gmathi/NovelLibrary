package io.github.gmathi.novellibrary.model.preference

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import io.github.gmathi.novellibrary.model.other.TTSFilter
import io.github.gmathi.novellibrary.model.other.TTSFilterList
import io.github.gmathi.novellibrary.util.system.getJson
import io.github.gmathi.novellibrary.util.system.putJson
import java.util.*

data class TTSPreferences(val context: Context, val prefs: SharedPreferences) {

    //#region Processing
    // Whether to attempt detection and merging of pages.
    var mergeBufferChapters: Boolean
        get() = prefs.getBoolean("ttsMergeBufferChapters", false)
        set(value) = prefs.edit().putBoolean("ttsMergeBufferChapters", value).apply()

    var discardInitialBufferPage: Boolean
        get() = prefs.getBoolean("ttsDiscardInitialBufferPage", false)
        set(value) = prefs.edit().putBoolean("ttsDiscardInitialBufferPage", value).apply()

    var useLongestPage: Boolean
        get() = prefs.getBoolean("ttsUseLongestPage", false)
        set(value) = prefs.edit().putBoolean("ttsUseLongestPage", value).apply()

    var markChaptersRead: Boolean
        get() = prefs.getBoolean("ttsMarkChaptersRead", true)
        set(value) = prefs.edit().putBoolean("ttsMarkChaptersRead", value).apply()

    var moveBookmark: Boolean
        get() = prefs.getBoolean("ttsMoveBookmark", false)
        set(value) = prefs.edit().putBoolean("ttsMoveBookmark", value).apply()

    var stripHeader: Boolean
        get() = prefs.getBoolean("ttsStripHeader", false)
        set(value) = prefs.edit().putBoolean("ttsStripHeader", value).apply()

    //#endregion

    //#region UI

    var keepScreenOn: Boolean
        get() = prefs.getBoolean("ttsKeepScreenOn", false)
        set(value) = prefs.edit().putBoolean("ttsKeepScreenOn", value).apply()

    //#endregion

    //#region FX/Playback

    var useLegacyPlayer: Boolean
        get() = prefs.getBoolean("ttsUseLegacyPlayer", false)
        set(value) = prefs.edit().putBoolean("ttsUseLegacyPlayer", value).apply()

    var language: Locale?
        get() = prefs.getString("ttsLanguage", null)?.let { Locale.forLanguageTag(it) }
        set(value) = prefs.edit().putString("ttsLanguage", value?.toLanguageTag()).apply()

    var pitch: Float
        get() = prefs.getFloat("ttsPitch", 1.0f)
        set(value) = prefs.edit().putFloat("ttsPitch", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("ttsSpeechRate", 1.0f)
        set(value) = prefs.edit().putFloat("ttsSpeechRate", value).apply()

    // TODO: 3-state: off, downpitch dialogue, downpitch speaker
    var downpitchDialogue: Boolean
        get() = prefs.getBoolean("ttsDownpitchDialogue", false)
        set(value) = prefs.edit().putBoolean("ttsDownpitchDialogue", value).apply()

    var downpitchAmount: Float
        get() = prefs.getFloat("ttsDownpitchAmount", 0.95f)
        set(value) = prefs.edit().putFloat("ttsDownpitchAmount", value).apply()

    var echoEffect: Boolean
        get() = prefs.getBoolean("ttsEchoEffect", false)
        set(value) = prefs.edit().putBoolean("ttsEchoEffect", value).apply()

    var chapterChangeSFX: Boolean
        get() = prefs.getBoolean("ttsChapterChangeSFX", true)
        set(value) = prefs.edit().putBoolean("ttsChapterChangeSFX", value).apply()

    var announceFinalChapter: Boolean
        get() = prefs.getBoolean("ttsAnnounceFinalChapter", true)
        set(value) = prefs.edit().putBoolean("ttsAnnounceFinalChapter", value).apply()

    //#endregion

    //#region Remote control

    var rewindSentences: Int
        get() = prefs.getInt("ttsRewindSentences", 1)
        set(value) = prefs.edit().putInt("ttsRewindSentences", value).apply()

    var forwardSentences: Int
        get() = prefs.getInt("ttsForwardSentences", 1)
        set(value) = prefs.edit().putInt("ttsForwardSentences", value).apply()

    var swapRewindSkip: Boolean
        get() = prefs.getBoolean("ttsSwapRewindSkip", false)
        set(value) = prefs.edit().putBoolean("ttsSwapRewindSkip", value).apply()

    var rewindToSkip: Boolean
        get() = prefs.getBoolean("ttsRewindToSkip", true)
        set(value) = prefs.edit().putBoolean("ttsRewindToSkip", value).apply()

    //#endregion

    //#region Filters

    // The names of currently active filter sets.
    var filters: List<String>
        get() = prefs.getJson("ttsFilters", "[]")
        set(value) = prefs.edit().putJson("ttsFilters", value).apply()

    // The cached filter sets. Because they are not built-in and rather sourced from outside,
    // we want to keep them cached for offline usage even if user would want to enable them.
    var filterCache: Map<String, TTSFilterList>
        get() = prefs.getJson("ttsFilterCache", Gson().toJson(emptyMap<String, TTSFilterList>()))
        set(value) = prefs.edit().putJson("ttsFilterCache", value).apply()

    // The currently active filter list composed of all enabled filter sets.
    var filterList: List<TTSFilter>
        get() = prefs.getJson("ttsFilterList", "[]")
        set(value) = prefs.edit().putJson("ttsFilterList", value).apply()

    //#endregion

    //#region Misc

    var stopTimer: Long
        get() = prefs.getLong("ttsStopTimer", 60L)
        set(value) = prefs.edit().putLong("ttsStopTimer", value).apply()

    var stopOnLoadError: Boolean
        get() = prefs.getBoolean("ttsStopOnLoadError", true)
        set(value) = prefs.edit().putBoolean("ttsStopOnLoadError", value).apply()

    //#endregion
}
