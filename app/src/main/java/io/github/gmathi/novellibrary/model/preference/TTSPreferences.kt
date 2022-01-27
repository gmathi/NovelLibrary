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

    var pitch: Float
        get() = prefs.getFloat("ttsPitch", 1.0f)
        set(value) = prefs.edit().putFloat("ttsPitch", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("ttsSpeechRate", 1.0f)
        set(value) = prefs.edit().putFloat("ttsSpeechRate", value).apply()

    var markChaptersRead: Boolean
        get() = prefs.getBoolean("ttsMarkChaptersRead", true)
        set(value) = prefs.edit().putBoolean("ttsMarkChaptersRead", value).apply()

    var moveBookmark: Boolean
        get() = prefs.getBoolean("ttsMoveBookmark", false)
        set(value) = prefs.edit().putBoolean("ttsMoveBookmark", value).apply()

    var language: Locale?
        get() = prefs.getString("ttsLanguage", null)?.let { Locale.forLanguageTag(it) }
        set(value) = prefs.edit().putString("ttsLanguage", value?.toLanguageTag()).apply()

    var stripHeader: Boolean
        get() = prefs.getBoolean("ttsStripHeader", false)
        set(value) = prefs.edit().putBoolean("ttsStripHeader", value).apply()

    var chapterChangeSFX: Boolean
        get() = prefs.getBoolean("ttsChapterChangeSFX", true)
        set(value) = prefs.edit().putBoolean("ttsChapterChangeSFX", value).apply()

    var stopTimer: Long
        get() = prefs.getLong("ttsStopTimer", 60L)
        set(value) = prefs.edit().putLong("ttsStopTimer", value).apply()

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

}
