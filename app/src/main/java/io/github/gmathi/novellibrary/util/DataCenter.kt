package io.github.gmathi.novellibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.HostNames
import java.util.*


class DataCenter(context: Context) {

    companion object {

        private const val LOCK_ROYAL_ROAD = "lockRoyalRoad"
        private const val LOCK_NOVEL_FULL = "lockNovelFull"
        private const val LOCK_SCRIBBLE = "lockScribble"

        private const val SEARCH_HISTORY_LIST = "searchHistoryList"
        private const val NOVEL_HISTORY_LIST = "novelHistoryList"

        private const val IS_DARK_THEME = "isDarkTheme"
        private const val DOWNLOAD_LATEST_FIRST = "downloadLatestFirst"
        private const val EXPERIMENTAL_DOWNLOAD = "experimentalDownload"
        private const val QUEUE_NOVEL_DOWNLOADS = "queueNovelDownloads"
        private const val LOAD_LIBRARY_SCREEN = "loadLibraryScreen"
        private const val APP_VERSION_CODE = "appVersionCode"
        private const val TEXT_SIZE = "textSize"
        private const val READER_MODE = "cleanPages"
        private const val JAVASCRIPT = "javascript"
        private const val LANGUAGE = "language"
        private const val VERIFIED_HOSTS = "verifiedHosts"
        private const val JAP_SWIPE = "japSwipe"
        private const val SHOW_READER_SCROLL = "showReaderScroll"
        private const val SHOW_CHAPTER_COMMENTS = "showChapterComments"
        private const val VOLUME_SCROLL = "volumeScroll"
        private const val KEEP_SCREEN_ON = "keepScreenOn"
        private const val ENABLE_IMMERSIVE_MODE = "enableImmersiveMode"
        private const val FONT_PATH = "fontPath"
        private const val GOOGLE_ACCOUNT_NAME = "googleAccountName"
        private const val ENABLE_CLUSTER_PAGES = "enableClusterPages"
        //        private const val CF_COOKIES_USER_AGENT = "cfCookiesUserAgent"
        private const val CF_COOKIES_STRING = "cfCookiesString"
        private const val DIRECTIONAL_LINKS = "enableDirectionalLinks"
        private const val READER_MODE_BUTTON_VISIBILITY = "isReaderModeButtonVisible"
        //        private const val ENABLE_CLOUD_FLARE = "enableCloudFlare"
        private const val ENABLE_NOTIFICATIONS = "enableNotifications"
        private const val DEVELOPER = "developer"


        const val CF_COOKIES_CLEARANCE = "cf_clearance"
        const val CF_COOKIES_DUID = "__cfduid"
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun loadSearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(SEARCH_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)

    fun saveSearchHistory(history: ArrayList<String>) = prefs.edit().putString(SEARCH_HISTORY_LIST, Gson().toJson(history)).apply()

    fun loadNovelHistory(): ArrayList<Novel> = Gson().fromJson(prefs.getString(NOVEL_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<Novel>>() {}.type)
    fun removeNovelHistory() = prefs.edit().remove(NOVEL_HISTORY_LIST).apply()

    var lockRoyalRoad: Boolean
        get() = prefs.getBoolean(LOCK_ROYAL_ROAD, true)
        set(value) = prefs.edit().putBoolean(LOCK_ROYAL_ROAD, value).apply()

    var lockNovelFull: Boolean
        get() = prefs.getBoolean(LOCK_NOVEL_FULL, true)
        set(value) = prefs.edit().putBoolean(LOCK_NOVEL_FULL, value).apply()

    var lockScribble: Boolean
        get() = prefs.getBoolean(LOCK_SCRIBBLE, true)
        set(value) = prefs.edit().putBoolean(LOCK_SCRIBBLE, value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(IS_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(IS_DARK_THEME, value).apply()

    var isDeveloper: Boolean
        get() = prefs.getBoolean(DEVELOPER, false)
        set(value) = prefs.edit().putBoolean(DEVELOPER, value).apply()

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

    var loadLibraryScreen: Boolean
        get() = prefs.getBoolean(LOAD_LIBRARY_SCREEN, false)
        set(value) = prefs.edit().putBoolean(LOAD_LIBRARY_SCREEN, value).apply()

    var appVersionCode: Int
        get() = prefs.getInt(APP_VERSION_CODE, 0)
        set(value) = prefs.edit().putInt(APP_VERSION_CODE, value).apply()

    var readerMode: Boolean
        get() = prefs.getBoolean(READER_MODE, false)
        set(value) = prefs.edit().putBoolean(READER_MODE, value).apply()

    var javascriptDisabled: Boolean
        get() = prefs.getBoolean(JAVASCRIPT, false)
        set(value) = prefs.edit().putBoolean(JAVASCRIPT, value).apply()

    var language: String
        get() = prefs.getString(LANGUAGE, "systemDefault_")!!
        @SuppressLint("ApplySharedPref")
        set(value) {
            prefs.edit().putString(LANGUAGE, value).commit()
        }

    var japSwipe: Boolean
        get() = prefs.getBoolean(JAP_SWIPE, true)
        set(value) = prefs.edit().putBoolean(JAP_SWIPE, value).apply()

    var showReaderScroll: Boolean
        get() = prefs.getBoolean(SHOW_READER_SCROLL, true)
        set(value) = prefs.edit().putBoolean(SHOW_READER_SCROLL, value).apply()

    var showChapterComments: Boolean
        get() = prefs.getBoolean(SHOW_CHAPTER_COMMENTS, false)
        set(value) = prefs.edit().putBoolean(SHOW_CHAPTER_COMMENTS, value).apply()

    var volumeScroll: Boolean
        get() = prefs.getBoolean(VOLUME_SCROLL, true)
        set(value) = prefs.edit().putBoolean(VOLUME_SCROLL, value).apply()

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEEP_SCREEN_ON, value).apply()

    var enableImmersiveMode: Boolean
        get() = prefs.getBoolean(ENABLE_IMMERSIVE_MODE, true)
        set(value) = prefs.edit().putBoolean(ENABLE_IMMERSIVE_MODE, value).apply()

    var fontPath: String
        get() = prefs.getString(FONT_PATH, "")!!
        set(value) = prefs.edit().putString(FONT_PATH, value).apply()

    var googleAccountName: String
        get() = prefs.getString(GOOGLE_ACCOUNT_NAME, "")!!
        set(value) = prefs.edit().putString(GOOGLE_ACCOUNT_NAME, value).apply()

    var enableClusterPages: Boolean
        get() = prefs.getBoolean(ENABLE_CLUSTER_PAGES, false)
        set(value) = prefs.edit().putBoolean(ENABLE_CLUSTER_PAGES, value).apply()

//    var userAgent: String
//        get() = prefs.getString(CF_COOKIES_USER_AGENT, HostNames.USER_AGENT)
//        set(value) = prefs.edit().putString(CF_COOKIES_USER_AGENT, value).apply()

//    var cfClearance: String
//        get() = prefs.getString(CF_COOKIES_CLEARANCE, "")
//        set(value) = prefs.edit().putString(CF_COOKIES_CLEARANCE, value).apply()
//
//    var cfDuid: String
//        get() = prefs.getString(CF_COOKIES_DUID, "")
//        set(value) = prefs.edit().putString(CF_COOKIES_DUID, value).apply()
//
//    var cfCookiesString: String
//        get() = prefs.getString(CF_COOKIES_STRING, "")
//        set(value) = prefs.edit().putString(CF_COOKIES_STRING, value).apply()

    var enableDirectionalLinks: Boolean
        get() = prefs.getBoolean(DIRECTIONAL_LINKS, false)
        set(value) = prefs.edit().putBoolean(DIRECTIONAL_LINKS, value).apply()

    var isReaderModeButtonVisible: Boolean
        get() = prefs.getBoolean(READER_MODE_BUTTON_VISIBILITY, true)
        set(value) = prefs.edit().putBoolean(READER_MODE_BUTTON_VISIBILITY, value).apply()

//    var enableCloudFlare: Boolean
//        get() = prefs.getBoolean(ENABLE_CLOUD_FLARE, true)
//        set(value) = prefs.edit().putBoolean(ENABLE_CLOUD_FLARE, value).apply()

    var enableNotifications: Boolean
        get() = prefs.getBoolean(ENABLE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(ENABLE_NOTIFICATIONS, value).apply()

    fun getVerifiedHosts(): ArrayList<String> =
            Gson().fromJson(prefs.getString(VERIFIED_HOSTS, Gson().toJson(HostNames.defaultHostNamesList)), object : TypeToken<ArrayList<String>>() {}.type)

    fun saveVerifiedHost(host: String) {
        val hostNames = getVerifiedHosts()
        hostNames.add(host)
        prefs.edit().putString(VERIFIED_HOSTS, Gson().toJson(hostNames)).apply()
        HostNames.hostNamesList = hostNames
    }

    //CloudFlare

    fun getCFClearance(hostName: String): String {
        return prefs.getString(CF_COOKIES_CLEARANCE + hostName, "")!!
    }

    fun setCFClearance(hostName: String, value: String) {
        prefs.edit().putString(CF_COOKIES_CLEARANCE + hostName, value).apply()
    }

    fun getCFDuid(hostName: String): String {
        return prefs.getString(CF_COOKIES_DUID + hostName, "")!!
    }

    fun setCFDuid(hostName: String, value: String) {
        prefs.edit().putString(CF_COOKIES_DUID + hostName, value).apply()
    }

    fun getCFCookiesString(hostName: String): String {
        return prefs.getString(CF_COOKIES_STRING + hostName, "")!!
    }

    fun setCFCookiesString(hostName: String, value: String) {
        prefs.edit().putString(CF_COOKIES_STRING + hostName, value).apply()
    }


}
