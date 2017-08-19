package io.github.gmathi.novellibrary.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.HostNames
import java.util.*


class DataCenter(context: Context) {

    private val BOOKMARKS_LIST = "bookmarksList"
    private val CACHE_LIST = "cacheList"
    private val SEARCH_HISTORY_LIST = "searchHistoryList"

    private val IS_DARK_THEME = "isDarkTheme"
    private val DOWNLOAD_LATEST_FIRST = "downloadLatestFirst"
    private val EXPERIMENTAL_DOWNLOAD = "experimentalDownload"
    private val QUEUE_NOVEL_DOWNLOADS = "queueNovelDownloads"
    private val LOCK_ROYAL_ROAD = "lockRoyalRoad"
    private val LOAD_LIBRARY_SCREEN = "loadLibraryScreen"
    private val APP_VERSION_CODE = "appVersionCode"
    private val TEXT_SIZE = "textSize"
    private val CLEAN_PAGES = "cleanPages"
    private val JAVASCRIPT = "javascript"
    private val LANGUAGE = "language"
    private val VERIFIED_HOSTS = "verifiedHosts"
    private val JAP_SWIPE = "japSwipe"

    val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var bookmarksJson: String
        get() = prefs.getString(BOOKMARKS_LIST, "[]")
        set(value) = prefs.edit().putString(BOOKMARKS_LIST, value).apply()

    var cacheJson: String
        get() = prefs.getString(CACHE_LIST, "{}")
        set(value) = prefs.edit().putString(CACHE_LIST, value).apply()

    var cacheMap: HashMap<String, ArrayList<WebPage>> = HashMap()

    fun loadCacheMap() {
        cacheMap = Gson().fromJson(cacheJson, object : TypeToken<HashMap<String, ArrayList<WebPage>>>() {}.type)
    }

    fun saveCacheMap() {
        cacheJson = Gson().toJson(cacheMap)
    }

    fun loadSearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(SEARCH_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)

    fun saveSearchHistory(history: ArrayList<String>) = prefs.edit().putString(SEARCH_HISTORY_LIST, Gson().toJson(history)).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(IS_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(IS_DARK_THEME, value).apply()

    var downloadLatestFirst: Boolean
        get() = prefs.getBoolean(DOWNLOAD_LATEST_FIRST, false)
        set(value) = prefs.edit().putBoolean(DOWNLOAD_LATEST_FIRST, value).apply()

    var experimentalDownload: Boolean
        get() = prefs.getBoolean(EXPERIMENTAL_DOWNLOAD, false)
        set(value) = prefs.edit().putBoolean(EXPERIMENTAL_DOWNLOAD, value).apply()

    var queueNovelDownloads: Boolean
        get() = prefs.getBoolean(QUEUE_NOVEL_DOWNLOADS, true)
        set(value) = prefs.edit().putBoolean(QUEUE_NOVEL_DOWNLOADS, value).apply()

    var textSize: Int
        get() = prefs.getInt(TEXT_SIZE, 0)
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()

    var lockRoyalRoad: Boolean
        get() = prefs.getBoolean(LOCK_ROYAL_ROAD, true)
        set(value) = prefs.edit().putBoolean(LOCK_ROYAL_ROAD, value).apply()

    var loadLibraryScreen: Boolean
        get() = prefs.getBoolean(LOAD_LIBRARY_SCREEN, false)
        set(value) = prefs.edit().putBoolean(LOAD_LIBRARY_SCREEN, value).apply()

    var appVersionCode: Int
        get() = prefs.getInt(APP_VERSION_CODE, 0)
        set(value) = prefs.edit().putInt(APP_VERSION_CODE, value).apply()

    var cleanChapters: Boolean
        get() = prefs.getBoolean(CLEAN_PAGES, false)
        set(value) = prefs.edit().putBoolean(CLEAN_PAGES, value).apply()

    var javascriptDisabled: Boolean
        get() = prefs.getBoolean(JAVASCRIPT, false)
        set(value) = prefs.edit().putBoolean(JAVASCRIPT, value).apply()

    var language: String
        get() = prefs.getString(LANGUAGE, Locale.getDefault().toString())
        set(value) = prefs.edit().putString(LANGUAGE, value).apply()

    var japSwipe: Boolean
        get() = prefs.getBoolean(JAP_SWIPE, true)
        set(value) = prefs.edit().putBoolean(JAP_SWIPE, value).apply()



    fun getVerifiedHosts(): ArrayList<String> {
        return Gson().fromJson(prefs.getString(VERIFIED_HOSTS, Gson().toJson(HostNames.getDefaultHostNamesList())), object : TypeToken<ArrayList<String>>() {}.type)
    }

    fun saveVerifiedHost(host: String) {
        if (!HostNames.isVerifiedHost(host))
            HostNames.addHost(host)
        prefs.edit().putString(VERIFIED_HOSTS, Gson().toJson(HostNames.getHostNamesList())).apply()
    }

    fun clearVerifiedHosts() {
        prefs.edit().putString(VERIFIED_HOSTS, "[]").apply()
    }
}