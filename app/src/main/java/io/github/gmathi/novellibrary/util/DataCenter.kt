package io.github.gmathi.novellibrary.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants.DEFAULT_FONT_PATH
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import java.io.File
import java.util.*


class DataCenter(context: Context) {

    companion object {

        private const val LOCK_ROYAL_ROAD = "lockRoyalRoad"
        private const val LOCK_NOVEL_FULL = "lockNovelFull"
        private const val LOCK_SCRIBBLE = "lockScribble"

        private const val SEARCH_HISTORY_LIST = "searchHistoryList"
        private const val NOVEL_HISTORY_LIST = "novelHistoryList"
        private const val LIBRARY_HISTORY_LIST = "libraryHistoryList"

        private const val SHOW_BACKUP_HINT = "showBackupHint"
        private const val SHOW_RESTORE_HINT = "showRestoreHint"
        private const val BACKUP_DATA = "backupData"
        private const val BACKUP_FREQUENCY_HOURS = "backupFrequencyHours"
        private const val LAST_BACKUP_MILLISECONDS = "lastBackupMilliseconds"

        private const val IS_DARK_THEME = "isDarkTheme"
        private const val LOAD_LIBRARY_SCREEN = "loadLibraryScreen"
        private const val APP_VERSION_CODE = "appVersionCode"
        private const val TEXT_SIZE = "textSize"
        private const val READER_MODE = "cleanPages"
        private const val JAVASCRIPT = "javascript"
        private const val LANGUAGE = "language"
        private const val FOOLED = "wasFooled"
        private const val VERIFIED_HOSTS = "verifiedHosts"
        private const val JAP_SWIPE = "japSwipe"
        private const val SHOW_READER_SCROLL = "showReaderScroll"
        private const val SHOW_CHAPTER_COMMENTS = "showChapterComments"
        private const val VOLUME_SCROLL = "volumeScroll"
        private const val SCROLL_LENGTH = "scrollLength"
        private const val KEEP_SCREEN_ON = "keepScreenOn"
        private const val ENABLE_IMMERSIVE_MODE = "enableImmersiveMode"
        private const val SHOW_NAVBAR_AT_CHAPTER_END = "showNavbarAtChapterEnd"
        private const val KEEP_TEXT_COLOR = "keepTextColor"
        private const val ALTERNATIVE_TEXT_COLORS = "alternativeTextColors"
        private const val LIMIT_IMAGE_WIDTH = "limitImageWidth"
        private const val FONT_PATH = "fontPath"
        private const val ENABLE_CLUSTER_PAGES = "enableClusterPages"
        private const val CF_COOKIES_STRING = "cfCookiesString"
        private const val DIRECTIONAL_LINKS = "enableDirectionalLinks"
        private const val READER_MODE_BUTTON_VISIBILITY = "isReaderModeButtonVisible"
        private const val ENABLE_NOTIFICATIONS = "enableNotifications"
        private const val DEVELOPER = "developer"
        private const val DISABLE_WUXIA_DOWNLOADS = "disableWuxiaDownloads"
        private const val HAS_ALREADY_DELETED_OLD_CHANNELS = "hasAlreadyDeletedOldChannels"

        //Backup
        private const val LAST_LOCAL_BACKUP_TIMESTAMP = "lastLocalBackupTimestamp"
        private const val LAST_CLOUD_BACKUP_TIMESTAMP = "lastCloudBackupTimestamp"
        private const val LAST_BACKUP_SIZE = "lastBackupSize"

        //Google Drive Settings
        private const val GD_BACKUP_INTERVAL = "gdBackupInterval"
        private const val GD_ACCOUNT_EMAIL = "gdAccountEmail"
        private const val GD_INTERNET_TYPE = "gdInternetType"

        const val CF_COOKIES_CLEARANCE = "cf_clearance"
        const val CF_COOKIES_DUID = "__cfduid"

        //Reader mode background color
        const val DAY_MODE_BACKGROUND_COLOR = "dayModeBackgroundColor"
        const val NIGHT_MODE_BACKGROUND_COLOR = "nightModeBackgroundColor"
        const val DAY_MODE_TEXT_COLOR = "dayModeTextColor"
        const val NIGHT_MODE_TEXT_COLOR = "nightModeTextColor"

        const val READ_ALOUD_NEXT_CHAPTER = "readAloudNextChapter"

    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun loadNovelSearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(SEARCH_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)
    fun saveNovelSearchHistory(history: ArrayList<String>) = prefs.edit().putString(SEARCH_HISTORY_LIST, Gson().toJson(history)).apply()

    fun loadLibrarySearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(LIBRARY_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)
    fun saveLibrarySearchHistory(history: ArrayList<String>) = prefs.edit().putString(LIBRARY_HISTORY_LIST, Gson().toJson(history)).apply()


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

    var showBackupHint: Boolean
        get() = prefs.getBoolean(SHOW_BACKUP_HINT, true)
        set(value) = prefs.edit().putBoolean(SHOW_BACKUP_HINT, value).apply()

    var showRestoreHint: Boolean
        get() = prefs.getBoolean(SHOW_RESTORE_HINT, true)
        set(value) = prefs.edit().putBoolean(SHOW_RESTORE_HINT, value).apply()

    var backupData: ByteArray?
        get() {
            val str = prefs.getString(BACKUP_DATA, null) ?: return null
            val split = str.substring(1, str.length - 1).split(", ")
            val array = ByteArray(split.size)
            for (i in split.indices) {
                array[i] = split[i].toByte()
            }
            return array
        }
        set(value) = prefs.edit().putString(BACKUP_DATA, value?.contentToString()).apply()

    var backupFrequency: Int
        get() = prefs.getInt(BACKUP_FREQUENCY_HOURS, 0)
        set(value) = prefs.edit().putInt(BACKUP_FREQUENCY_HOURS, value).apply()

    var lastBackup: Long
        get() = prefs.getLong(LAST_BACKUP_MILLISECONDS, 0)
        set(value) = prefs.edit().putLong(LAST_BACKUP_MILLISECONDS, value).apply()

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
        get() = prefs.getString(LANGUAGE, SYSTEM_DEFAULT)!!
        @SuppressLint("ApplySharedPref")
        set(value) {
            prefs.edit().putString(LANGUAGE, value).commit()
        }

    var fooled: Boolean
        get() = prefs.getBoolean(FOOLED, false)
        set(value) = prefs.edit().putBoolean(FOOLED, value).apply()


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

    var scrollLength: Int
        get() = prefs.getInt(SCROLL_LENGTH, Constants.VOLUME_SCROLL_LENGTH_DEFAULT)
        set(value) = prefs.edit().putInt(SCROLL_LENGTH, value).apply()


    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON, true)
        set(value) = prefs.edit().putBoolean(KEEP_SCREEN_ON, value).apply()

    var enableImmersiveMode: Boolean
        get() = prefs.getBoolean(ENABLE_IMMERSIVE_MODE, true)
        set(value) = prefs.edit().putBoolean(ENABLE_IMMERSIVE_MODE, value).apply()

    var showNavbarAtChapterEnd: Boolean
        get() = prefs.getBoolean(SHOW_NAVBAR_AT_CHAPTER_END, true)
        set(value) = prefs.edit().putBoolean(SHOW_NAVBAR_AT_CHAPTER_END, value).apply()

    var keepTextColor: Boolean
        get() = prefs.getBoolean(KEEP_TEXT_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEEP_TEXT_COLOR, value).apply()

    var alternativeTextColors: Boolean
        get() = prefs.getBoolean(ALTERNATIVE_TEXT_COLORS, false)
        set(value) = prefs.edit().putBoolean(ALTERNATIVE_TEXT_COLORS, value).apply()

    var limitImageWidth: Boolean
        get() = prefs.getBoolean(LIMIT_IMAGE_WIDTH, false)
        set(value) = prefs.edit().putBoolean(LIMIT_IMAGE_WIDTH, value).apply()

    var fontPath: String
        get() {
            var path = prefs.getString(FONT_PATH, DEFAULT_FONT_PATH)!!
            if (!path.startsWith("/android_asset/fonts/") && !File(path).exists()) {
                fontPath = DEFAULT_FONT_PATH
                path = DEFAULT_FONT_PATH
            }
            return path
        }
        set(value) = prefs.edit().putString(FONT_PATH, if (value.isBlank()) DEFAULT_FONT_PATH else value ).apply()

    var enableClusterPages: Boolean
        get() = prefs.getBoolean(ENABLE_CLUSTER_PAGES, false)
        set(value) = prefs.edit().putBoolean(ENABLE_CLUSTER_PAGES, value).apply()

    var enableDirectionalLinks: Boolean
        get() = prefs.getBoolean(DIRECTIONAL_LINKS, false)
        set(value) = prefs.edit().putBoolean(DIRECTIONAL_LINKS, value).apply()

    var isReaderModeButtonVisible: Boolean
        get() = prefs.getBoolean(READER_MODE_BUTTON_VISIBILITY, true)
        set(value) = prefs.edit().putBoolean(READER_MODE_BUTTON_VISIBILITY, value).apply()

    var disableWuxiaDownloads: Boolean
        get() = prefs.getBoolean(DISABLE_WUXIA_DOWNLOADS, true)
        set(value) = prefs.edit().putBoolean(DISABLE_WUXIA_DOWNLOADS, value).apply()

    var enableNotifications: Boolean
        get() = prefs.getBoolean(ENABLE_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(ENABLE_NOTIFICATIONS, value).apply()

    var hasAlreadyDeletedOldChannels: Boolean
        get() = prefs.getBoolean(HAS_ALREADY_DELETED_OLD_CHANNELS, false)
        set(value) = prefs.edit().putBoolean(HAS_ALREADY_DELETED_OLD_CHANNELS, value).apply()


    // Verified HostNames management

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

    //Backup

    var lastLocalBackupTimestamp: String
        get() = prefs.getString(LAST_LOCAL_BACKUP_TIMESTAMP, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_LOCAL_BACKUP_TIMESTAMP, value).apply()

    var lastCloudBackupTimestamp: String
        get() = prefs.getString(LAST_CLOUD_BACKUP_TIMESTAMP, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_CLOUD_BACKUP_TIMESTAMP, value).apply()

    var lastBackupSize: String
        get() = prefs.getString(LAST_BACKUP_SIZE, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_BACKUP_SIZE, value).apply()

    //Google Settings
    var gdBackupInterval: String
        get() = prefs.getString(GD_BACKUP_INTERVAL, "Never") ?: "Never"
        set(value) = prefs.edit().putString(GD_BACKUP_INTERVAL, value).apply()

    var gdAccountEmail: String
        get() = prefs.getString(GD_ACCOUNT_EMAIL, "-") ?: "-"
        set(value) = prefs.edit().putString(GD_ACCOUNT_EMAIL, value).apply()

    var gdInternetType: String
        get() = prefs.getString(GD_INTERNET_TYPE, "WiFi or cellular") ?: "WiFi or cellular"
        set(value) = prefs.edit().putString(GD_INTERNET_TYPE, value).apply()


    var dayModeBackgroundColor: Int
        get() = prefs.getInt(DAY_MODE_BACKGROUND_COLOR, Color.WHITE)
        set(value) = prefs.edit().putInt(DAY_MODE_BACKGROUND_COLOR, value).apply()

    var nightModeBackgroundColor: Int
        get() = prefs.getInt(NIGHT_MODE_BACKGROUND_COLOR, Color.BLACK)
        set(value) = prefs.edit().putInt(NIGHT_MODE_BACKGROUND_COLOR, value).apply()

    var dayModeTextColor: Int
        get() = prefs.getInt(DAY_MODE_TEXT_COLOR, Color.BLACK)
        set(value) = prefs.edit().putInt(DAY_MODE_TEXT_COLOR, value).apply()

    var nightModeTextColor: Int
        get() = prefs.getInt(NIGHT_MODE_TEXT_COLOR, Color.WHITE)
        set(value) = prefs.edit().putInt(NIGHT_MODE_TEXT_COLOR, value).apply()

    var readAloudNextChapter: Boolean
        get() = prefs.getBoolean(READ_ALOUD_NEXT_CHAPTER, true)
        set(value) = prefs.edit().putBoolean(READ_ALOUD_NEXT_CHAPTER, value).apply()

}
