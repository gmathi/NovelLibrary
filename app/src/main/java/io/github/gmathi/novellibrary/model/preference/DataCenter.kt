package io.github.gmathi.novellibrary.model.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.model.other.TTSFilter
import io.github.gmathi.novellibrary.model.other.TTSFilterList
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.PREF_DOH_CLOUDFLARE
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.DEFAULT_FONT_PATH
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import io.github.gmathi.novellibrary.util.storage.StorageLocation
import io.github.gmathi.novellibrary.util.system.getJson
import io.github.gmathi.novellibrary.util.system.putJson
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
        private const val READER_MODE_PER_NOVEL_PREFIX = "reader_mode_"
        private const val IS_DARK_THEME_PER_NOVEL_PREFIX = "is_dark_theme_"
        private const val FONT_PATH_PER_NOVEL_PREFIX = "font_path_"
        private const val TEXT_SIZE_PER_NOVEL_PREFIX = "text_size_"
        private const val DAY_BG_COLOR_PER_NOVEL_PREFIX = "day_bg_color_"
        private const val NIGHT_BG_COLOR_PER_NOVEL_PREFIX = "night_bg_color_"
        private const val DAY_TEXT_COLOR_PER_NOVEL_PREFIX = "day_text_color_"
        private const val NIGHT_TEXT_COLOR_PER_NOVEL_PREFIX = "night_text_color_"
        private const val LIMIT_IMAGE_WIDTH_PER_NOVEL_PREFIX = "limit_image_width_"
        private const val KEEP_TEXT_COLOR_PER_NOVEL_PREFIX = "keep_text_color_"
        private const val ALT_TEXT_COLORS_PER_NOVEL_PREFIX = "alt_text_colors_"
        private const val ENABLE_CLUSTER_PAGES_PER_NOVEL_PREFIX = "enable_cluster_pages_"
        private const val JAVASCRIPT = "javascript"
        private const val LANGUAGE = "language"
        private const val FOOLED = "wasFooled"
        private const val VERIFIED_HOSTS = "verifiedHosts"
        private const val JAP_SWIPE = "japSwipe"
        private const val SHOW_READER_SCROLL = "showReaderScroll"
        private const val SHOW_CHAPTER_COMMENTS = "showChapterComments"
        private const val ENABLE_VOLUME_SCROLL = "volumeScroll"
        private const val SCROLL_LENGTH = "scrollLength"
        private const val KEEP_SCREEN_ON = "keepScreenOn"
        private const val ENABLE_IMMERSIVE_MODE = "enableImmersiveMode"
        private const val SHOW_NAVBAR_AT_CHAPTER_END = "showNavbarAtChapterEnd"
        private const val KEEP_TEXT_COLOR = "keepTextColor"
        private const val ALTERNATIVE_TEXT_COLORS = "alternativeTextColors"
        private const val LIMIT_IMAGE_WIDTH = "limitImageWidth"
        private const val FONT_PATH = "fontPath"
        private const val ENABLE_CLUSTER_PAGES = "enableClusterPages"
        private const val DIRECTIONAL_LINKS = "enableDirectionalLinks"
        private const val READER_MODE_BUTTON_VISIBILITY = "isReaderModeButtonVisible"
        private const val ENABLE_NOTIFICATIONS = "enableNotifications"
        private const val DEVELOPER = "developer"
        private const val DISABLE_WUXIA_DOWNLOADS = "disableWuxiaDownloads"
        private const val HAS_ALREADY_DELETED_OLD_CHANNELS = "hasAlreadyDeletedOldChannels"
        private const val LOGIN_COOKIES_STRING = "loginCookiesString"
        private const val CUSTOM_QUERY_LOOKUPS = "customQueryLookups"
        private const val SHOW_CHAPTERS_LEFT_BADGE = "showChaptersLeftBadge"
        private const val LIBRARY_MANUAL_ORDER = "libraryManualOrder"
        private const val USER_SPECIFIED_SELECTOR_QUERIES = "userSpecifiedSelectorQueries"
        private const val AUTO_SCROLL_LENGTH = "autoScrollLength"
        private const val AUTO_SCROLL_INTERVAL = "autoScrollInterval"
        private const val ENABLE_AUTO_SCROLL = "enableAutoScroll"
        private const val USE_NU_API_FETCH = "useNUAPIFetch"

        //Download storage location
        private const val DOWNLOAD_STORAGE_LOCATION = "downloadStorageLocation"

        //Backup
        private const val LAST_LOCAL_BACKUP_TIMESTAMP = "lastLocalBackupTimestamp"
        private const val LAST_CLOUD_BACKUP_TIMESTAMP = "lastCloudBackupTimestamp"
        private const val LAST_BACKUP_SIZE = "lastBackupSize"

        //Google Drive Settings
        private const val GD_BACKUP_INTERVAL = "gdBackupInterval"
        private const val GD_ACCOUNT_EMAIL = "gdAccountEmail"
        private const val GD_INTERNET_TYPE = "gdInternetType"

//      private const val CF_COOKIES_STRING = "cfCookiesString"
//      const val CF_COOKIES_CLEARANCE = "cf_clearance"
//      const val CF_COOKIES_DUID = "__cfduid"


        //Reader mode background color
        const val DAY_MODE_BACKGROUND_COLOR = "dayModeBackgroundColor"
        const val NIGHT_MODE_BACKGROUND_COLOR = "nightModeBackgroundColor"
        const val DAY_MODE_TEXT_COLOR = "dayModeTextColor"
        const val NIGHT_MODE_TEXT_COLOR = "nightModeTextColor"

        const val READ_ALOUD_NEXT_CHAPTER = "readAloudNextChapter"
        const val SCROLLING_TEXT = "scrollingText"

        // Sync
        const val SYNC_ENABLE = "sync_enable_"
        const val SYNC_ADD_NOVELS = "sync_add_novels_"
        const val SYNC_DELETE_NOVELS = "sync_delete_novels_"
        const val SYNC_BOOKMARKS = "sync_bookmarks_"

        //DNS over HTTPS
        const val ENABLE_DOH = "enable_doh"
        const val DOH_PROVIDER = "doh_provider"

        //Cloudflare
        const val USE_WEBVIEW_FETCHER_FOR_CLOUDFLARE = "use_webview_fetcher_for_cloudflare"

        //Content Selectors List
        const val SELECTOR_QUERIES = "selectorsQueries"

        //Extensions
        const val AUTOMATIC_EXT_UPDATES = "automaticExtUpdates"

        //App Auto Update
        private const val ENABLE_AUTO_APP_UPDATE = "enableAutoAppUpdate"

        // App Night Mode (separate from reader isDarkTheme)
        private const val APP_NIGHT_MODE = "appNightMode"

        // Set when startup DB cleanup detects corruption. The UI uses this to offer
        // user-initiated recovery instead of wiping the library automatically.
        private const val DATABASE_CORRUPTION_DETECTED = "databaseCorruptionDetected"
    }
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val ttsPreferences = TTSPreferences(context, prefs)
    val aiTtsPreferences = AiTtsPreferences(context, prefs)


    fun internalGet(closure: SharedPreferences.()->Unit) = closure(prefs)
    fun internalPut(closure: SharedPreferences.Editor.()->Unit) {
        val editor = prefs.edit()
        closure(editor)
        editor.apply()
    }

    fun loadNovelSearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(SEARCH_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)
    fun saveNovelSearchHistory(history: ArrayList<String>) = prefs.edit().putString(SEARCH_HISTORY_LIST, Gson().toJson(history)).apply()

    fun loadLibrarySearchHistory(): ArrayList<String> = Gson().fromJson(prefs.getString(LIBRARY_HISTORY_LIST, "[]"), object : TypeToken<ArrayList<String>>() {}.type)
    fun saveLibrarySearchHistory(history: ArrayList<String>) = prefs.edit().putString(LIBRARY_HISTORY_LIST, Gson().toJson(history)).apply()

    //region Library manual order snapshot

    // Stores a snapshot of the user's manual novel ordering, keyed by novel-section id.
    // The value is the ordered list of novel ids as they appeared before the first sort.
    // This lets the user revert a sort back to their last manual arrangement.
    private fun loadManualOrderMap(): HashMap<Long, ArrayList<Long>> =
        Gson().fromJson(
            prefs.getString(LIBRARY_MANUAL_ORDER, "{}"),
            object : TypeToken<HashMap<Long, ArrayList<Long>>>() {}.type
        )

    private fun saveManualOrderMap(map: HashMap<Long, ArrayList<Long>>) =
        prefs.edit().putString(LIBRARY_MANUAL_ORDER, Gson().toJson(map)).apply()

    /** True if a manual-order snapshot exists for the given section. */
    fun hasManualOrderSnapshot(novelSectionId: Long): Boolean =
        loadManualOrderMap().containsKey(novelSectionId)

    /**
     * Saves [novelIds] as the manual-order snapshot for [novelSectionId], but only if one
     * isn't already saved. This captures the arrangement just before the first sort so that
     * repeated sorts don't clobber the original manual order.
     */
    fun saveManualOrderSnapshotIfAbsent(novelSectionId: Long, novelIds: List<Long>) {
        val map = loadManualOrderMap()
        if (!map.containsKey(novelSectionId)) {
            map[novelSectionId] = ArrayList(novelIds)
            saveManualOrderMap(map)
        }
    }

    /** Returns the saved manual-order snapshot for [novelSectionId], or null if none exists. */
    fun getManualOrderSnapshot(novelSectionId: Long): ArrayList<Long>? =
        loadManualOrderMap()[novelSectionId]

    /** Clears the manual-order snapshot for [novelSectionId] (e.g. after reverting or a manual drag). */
    fun clearManualOrderSnapshot(novelSectionId: Long) {
        val map = loadManualOrderMap()
        if (map.remove(novelSectionId) != null) {
            saveManualOrderMap(map)
        }
    }

    //endregion


    var lockRoyalRoad: Boolean
        get() = prefs.getBoolean(LOCK_ROYAL_ROAD, true)
        set(value) = prefs.edit().putBoolean(LOCK_ROYAL_ROAD, value).apply()

    var lockNovelFull: Boolean
        get() = prefs.getBoolean(LOCK_NOVEL_FULL, true)
        set(value) = prefs.edit().putBoolean(LOCK_NOVEL_FULL, value).apply()

    var lockScribble: Boolean
        get() = prefs.getBoolean(LOCK_SCRIBBLE, true)
        set(value) = prefs.edit().putBoolean(LOCK_SCRIBBLE, value).apply()

    /**
     * The default reader theme (dark vs light) applied to novels that have no per-novel
     * value stored yet (see [getIsDarkThemeForNovel]).
     */
    var isDarkTheme: Boolean
        get() = prefs.getBoolean(IS_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(IS_DARK_THEME, value).apply()

    /**
     * Returns the dark-theme preference for the novel identified by [novelId]. Falls back to
     * the app-wide default ([isDarkTheme]) when no per-novel value has been stored yet.
     */
    fun getIsDarkThemeForNovel(novelId: Long): Boolean =
        prefs.getBoolean(IS_DARK_THEME_PER_NOVEL_PREFIX + novelId, isDarkTheme)

    /**
     * Persists the dark-theme preference for the novel identified by [novelId]. Does not
     * affect the app-wide default ([isDarkTheme]) or any other novel's stored preference.
     */
    fun setIsDarkThemeForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(IS_DARK_THEME_PER_NOVEL_PREFIX + novelId, value).apply()
    }

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

    /**
     * The default reader text size applied to novels that have no per-novel value stored yet
     * (see [getTextSizeForNovel]).
     */
    var textSize: Int
        get() = prefs.getInt(TEXT_SIZE, 0)
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()

    /**
     * Returns the text-size preference for the novel identified by [novelId]. Falls back to
     * the app-wide default ([textSize]) when no per-novel value has been stored yet.
     */
    fun getTextSizeForNovel(novelId: Long): Int =
        prefs.getInt(TEXT_SIZE_PER_NOVEL_PREFIX + novelId, textSize)

    /**
     * Persists the text-size preference for the novel identified by [novelId]. Does not
     * affect the app-wide default ([textSize]) or any other novel's stored preference.
     */
    fun setTextSizeForNovel(novelId: Long, value: Int) {
        prefs.edit().putInt(TEXT_SIZE_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    var loadLibraryScreen: Boolean
        get() = prefs.getBoolean(LOAD_LIBRARY_SCREEN, false)
        set(value) = prefs.edit().putBoolean(LOAD_LIBRARY_SCREEN, value).apply()

    var appVersionCode: Int
        get() = prefs.getInt(APP_VERSION_CODE, 0)
        set(value) = prefs.edit().putInt(APP_VERSION_CODE, value).apply()

    /**
     * The default Reader Mode value applied to novels that have no per-novel
     * Reader_Mode_Preference stored yet (see [getReaderModeForNovel]). Configured via the
     * "Reader Mode" toggle in ReaderSettingsActivity.
     */
    var readerMode: Boolean
        get() = prefs.getBoolean(READER_MODE, true)
        set(value) = prefs.edit().putBoolean(READER_MODE, value).apply()

    /**
     * Returns the Reader Mode preference for the novel identified by [novelId]. Falls back to
     * the app-wide default ([readerMode]) when no per-novel value has been stored yet.
     */
    fun getReaderModeForNovel(novelId: Long): Boolean =
        prefs.getBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId, readerMode)

    /**
     * Persists the Reader Mode preference for the novel identified by [novelId]. Does not
     * affect the app-wide default ([readerMode]) or any other novel's stored preference.
     */
    fun setReaderModeForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    var javascriptDisabled: Boolean
        get() = prefs.getBoolean(JAVASCRIPT, true)
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

    var enableVolumeScroll: Boolean
        get() = prefs.getBoolean(ENABLE_VOLUME_SCROLL, true)
        set(value) = prefs.edit().putBoolean(ENABLE_VOLUME_SCROLL, value).apply()

    var volumeScrollLength: Int
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

    /**
     * The default "keep text color" preference applied to novels that have no per-novel value
     * stored yet (see [getKeepTextColorForNovel]).
     */
    var keepTextColor: Boolean
        get() = prefs.getBoolean(KEEP_TEXT_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEEP_TEXT_COLOR, value).apply()

    /**
     * Returns the "keep text color" preference for the novel identified by [novelId]. Falls
     * back to the app-wide default ([keepTextColor]) when no per-novel value has been stored yet.
     */
    fun getKeepTextColorForNovel(novelId: Long): Boolean =
        prefs.getBoolean(KEEP_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, keepTextColor)

    /**
     * Persists the "keep text color" preference for the novel identified by [novelId]. Does
     * not affect the app-wide default ([keepTextColor]) or any other novel's stored preference.
     */
    fun setKeepTextColorForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(KEEP_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default "alternative text colors" preference applied to novels that have no
     * per-novel value stored yet (see [getAlternativeTextColorsForNovel]).
     */
    var alternativeTextColors: Boolean
        get() = prefs.getBoolean(ALTERNATIVE_TEXT_COLORS, false)
        set(value) = prefs.edit().putBoolean(ALTERNATIVE_TEXT_COLORS, value).apply()

    /**
     * Returns the "alternative text colors" preference for the novel identified by [novelId].
     * Falls back to the app-wide default ([alternativeTextColors]) when no per-novel value has
     * been stored yet.
     */
    fun getAlternativeTextColorsForNovel(novelId: Long): Boolean =
        prefs.getBoolean(ALT_TEXT_COLORS_PER_NOVEL_PREFIX + novelId, alternativeTextColors)

    /**
     * Persists the "alternative text colors" preference for the novel identified by [novelId].
     * Does not affect the app-wide default ([alternativeTextColors]) or any other novel's
     * stored preference.
     */
    fun setAlternativeTextColorsForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(ALT_TEXT_COLORS_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default "limit image width" preference applied to novels that have no per-novel
     * value stored yet (see [getLimitImageWidthForNovel]).
     */
    var limitImageWidth: Boolean
        get() = prefs.getBoolean(LIMIT_IMAGE_WIDTH, false)
        set(value) = prefs.edit().putBoolean(LIMIT_IMAGE_WIDTH, value).apply()

    /**
     * Returns the "limit image width" preference for the novel identified by [novelId]. Falls
     * back to the app-wide default ([limitImageWidth]) when no per-novel value has been stored yet.
     */
    fun getLimitImageWidthForNovel(novelId: Long): Boolean =
        prefs.getBoolean(LIMIT_IMAGE_WIDTH_PER_NOVEL_PREFIX + novelId, limitImageWidth)

    /**
     * Persists the "limit image width" preference for the novel identified by [novelId]. Does
     * not affect the app-wide default ([limitImageWidth]) or any other novel's stored preference.
     */
    fun setLimitImageWidthForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(LIMIT_IMAGE_WIDTH_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default reader font path applied to novels that have no per-novel value stored yet
     * (see [getFontPathForNovel]).
     */
    var fontPath: String
        get() {
            var path = prefs.getString(FONT_PATH, DEFAULT_FONT_PATH)!!
            if (!path.startsWith("/android_asset/fonts/") && !File(path).exists()) {
                fontPath = DEFAULT_FONT_PATH
                path = DEFAULT_FONT_PATH
            }
            return path
        }
        set(value) = prefs.edit().putString(FONT_PATH, if (value.isBlank()) DEFAULT_FONT_PATH else value).apply()

    /**
     * Returns the font-path preference for the novel identified by [novelId]. Falls back to
     * the app-wide default ([fontPath]) when no per-novel value has been stored yet.
     */
    fun getFontPathForNovel(novelId: Long): String =
        prefs.getString(FONT_PATH_PER_NOVEL_PREFIX + novelId, null) ?: fontPath

    /**
     * Persists the font-path preference for the novel identified by [novelId]. Does not
     * affect the app-wide default ([fontPath]) or any other novel's stored preference.
     */
    fun setFontPathForNovel(novelId: Long, value: String) {
        prefs.edit().putString(FONT_PATH_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default "merge pages" preference applied to novels that have no per-novel value
     * stored yet (see [getEnableClusterPagesForNovel]).
     */
    var enableClusterPages: Boolean
        get() = prefs.getBoolean(ENABLE_CLUSTER_PAGES, false)
        set(value) = prefs.edit().putBoolean(ENABLE_CLUSTER_PAGES, value).apply()

    /**
     * Returns the "merge pages" preference for the novel identified by [novelId]. Falls back to
     * the app-wide default ([enableClusterPages]) when no per-novel value has been stored yet.
     */
    fun getEnableClusterPagesForNovel(novelId: Long): Boolean =
        prefs.getBoolean(ENABLE_CLUSTER_PAGES_PER_NOVEL_PREFIX + novelId, enableClusterPages)

    /**
     * Persists the "merge pages" preference for the novel identified by [novelId]. Does not
     * affect the app-wide default ([enableClusterPages]) or any other novel's stored preference.
     */
    fun setEnableClusterPagesForNovel(novelId: Long, value: Boolean) {
        prefs.edit().putBoolean(ENABLE_CLUSTER_PAGES_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    var enableDirectionalLinks: Boolean
        get() = prefs.getBoolean(DIRECTIONAL_LINKS, false)
        set(value) = prefs.edit().putBoolean(DIRECTIONAL_LINKS, value).apply()

    var linkifyText: Boolean
        get() = prefs.getBoolean("linkifyText", false)
        set(value) = prefs.edit().putBoolean("linkifyText", value).apply()

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


    var showChaptersLeftBadge: Boolean
        get() = prefs.getBoolean(SHOW_CHAPTERS_LEFT_BADGE, false)
        set(value) = prefs.edit().putBoolean(SHOW_CHAPTERS_LEFT_BADGE, value).apply()

    // Verified HostNames management

    fun getVerifiedHosts(): ArrayList<String> =
        Gson().fromJson(prefs.getString(VERIFIED_HOSTS, Gson().toJson(HostNames.defaultHostNamesList)), object : TypeToken<ArrayList<String>>() {}.type)

    fun saveVerifiedHost(host: String) {
        val hostNames = getVerifiedHosts()
        hostNames.add(host)
        prefs.edit().putString(VERIFIED_HOSTS, Gson().toJson(hostNames)).apply()
        HostNames.hostNamesList = hostNames
    }

    //region CloudFlare - Not used anymore from here

//    fun getCFClearance(hostName: String): String {
//        return prefs.getString(CF_COOKIES_CLEARANCE + hostName, "")!!
//    }
//
//    fun setCFClearance(hostName: String, value: String) {
//        prefs.edit().putString(CF_COOKIES_CLEARANCE + hostName, value).apply()
//    }
//
//    fun getCFDuid(hostName: String): String {
//        return prefs.getString(CF_COOKIES_DUID + hostName, "")!!
//    }
//
//    fun setCFDuid(hostName: String, value: String) {
//        prefs.edit().putString(CF_COOKIES_DUID + hostName, value).apply()
//    }
//
//    fun getCFCookiesString(hostName: String): String {
//        return prefs.getString(CF_COOKIES_STRING + hostName, "")!!
//    }
//
//    fun setCFCookiesString(hostName: String, value: String) {
//        prefs.edit().putString(CF_COOKIES_STRING + hostName, value).apply()
//    }
    //endregion

    //region Novel Sync
    fun getLoginCookiesString(hostName: String): String {
        return prefs.getString(LOGIN_COOKIES_STRING + hostName, "")!!;
    }

    fun setLoginCookiesString(hostName: String, value: String) {
        prefs.edit().putString(LOGIN_COOKIES_STRING + hostName, value).apply()
    }

    fun deleteLoginCookieString(hostName: String) {
        prefs.edit().remove(LOGIN_COOKIES_STRING + hostName).apply()
    }

    fun getSyncEnabled(name: String): Boolean {
        return prefs.getBoolean(SYNC_ENABLE + name, false)
    }

    fun setSyncEnabled(name: String, value: Boolean) {
        prefs.edit().putBoolean(SYNC_ENABLE + name, value).apply()
    }

    fun getSyncAddNovels(name: String): Boolean {
        return prefs.getBoolean(SYNC_ADD_NOVELS + name, true)
    }

    fun setSyncAddNovels(name: String, value: Boolean) {
        prefs.edit().putBoolean(SYNC_ADD_NOVELS + name, value).apply()
    }

    fun getSyncDeleteNovels(name: String): Boolean {
        return prefs.getBoolean(SYNC_DELETE_NOVELS + name, true)
    }

    fun setSyncDeleteNovels(name: String, value: Boolean) {
        prefs.edit().putBoolean(SYNC_DELETE_NOVELS + name, value).apply()
    }

    fun getSyncBookmarks(name: String): Boolean {
        return prefs.getBoolean(SYNC_BOOKMARKS + name, true)
    }

    fun setSyncBookmarks(name: String, value: Boolean) {
        prefs.edit().putBoolean(SYNC_BOOKMARKS + name, value).apply()
    }

    //endregion

    //region Backup

    var lastLocalBackupTimestamp: String
        get() = prefs.getString(LAST_LOCAL_BACKUP_TIMESTAMP, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_LOCAL_BACKUP_TIMESTAMP, value).apply()

    var lastCloudBackupTimestamp: String
        get() = prefs.getString(LAST_CLOUD_BACKUP_TIMESTAMP, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_CLOUD_BACKUP_TIMESTAMP, value).apply()

    var lastBackupSize: String
        get() = prefs.getString(LAST_BACKUP_SIZE, "N/A") ?: "N/A"
        set(value) = prefs.edit().putString(LAST_BACKUP_SIZE, value).apply()

    //endregion

    //region Google Settings
    var gdBackupInterval: String
        get() = prefs.getString(GD_BACKUP_INTERVAL, "Never") ?: "Never"
        set(value) = prefs.edit().putString(GD_BACKUP_INTERVAL, value).apply()

    var gdAccountEmail: String
        get() = prefs.getString(GD_ACCOUNT_EMAIL, "-") ?: "-"
        set(value) = prefs.edit().putString(GD_ACCOUNT_EMAIL, value).apply()

    var gdInternetType: String
        get() = prefs.getString(GD_INTERNET_TYPE, "WiFi or cellular") ?: "WiFi or cellular"
        set(value) = prefs.edit().putString(GD_INTERNET_TYPE, value).apply()

    //endregion

    /**
     * The default day-mode background color applied to novels that have no per-novel value
     * stored yet (see [getDayBackgroundColorForNovel]).
     */
    var dayModeBackgroundColor: Int
        get() = prefs.getInt(DAY_MODE_BACKGROUND_COLOR, Color.WHITE)
        set(value) = prefs.edit().putInt(DAY_MODE_BACKGROUND_COLOR, value).apply()

    /**
     * Returns the day-mode background color for the novel identified by [novelId]. Falls back
     * to the app-wide default ([dayModeBackgroundColor]) when no per-novel value has been stored yet.
     */
    fun getDayBackgroundColorForNovel(novelId: Long): Int =
        prefs.getInt(DAY_BG_COLOR_PER_NOVEL_PREFIX + novelId, dayModeBackgroundColor)

    /**
     * Persists the day-mode background color for the novel identified by [novelId]. Does not
     * affect the app-wide default ([dayModeBackgroundColor]) or any other novel's stored preference.
     */
    fun setDayBackgroundColorForNovel(novelId: Long, value: Int) {
        prefs.edit().putInt(DAY_BG_COLOR_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default night-mode background color applied to novels that have no per-novel value
     * stored yet (see [getNightBackgroundColorForNovel]).
     */
    var nightModeBackgroundColor: Int
        get() = prefs.getInt(NIGHT_MODE_BACKGROUND_COLOR, Color.BLACK)
        set(value) = prefs.edit().putInt(NIGHT_MODE_BACKGROUND_COLOR, value).apply()

    /**
     * Returns the night-mode background color for the novel identified by [novelId]. Falls back
     * to the app-wide default ([nightModeBackgroundColor]) when no per-novel value has been stored yet.
     */
    fun getNightBackgroundColorForNovel(novelId: Long): Int =
        prefs.getInt(NIGHT_BG_COLOR_PER_NOVEL_PREFIX + novelId, nightModeBackgroundColor)

    /**
     * Persists the night-mode background color for the novel identified by [novelId]. Does not
     * affect the app-wide default ([nightModeBackgroundColor]) or any other novel's stored preference.
     */
    fun setNightBackgroundColorForNovel(novelId: Long, value: Int) {
        prefs.edit().putInt(NIGHT_BG_COLOR_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default day-mode text color applied to novels that have no per-novel value stored
     * yet (see [getDayTextColorForNovel]).
     */
    var dayModeTextColor: Int
        get() = prefs.getInt(DAY_MODE_TEXT_COLOR, Color.BLACK)
        set(value) = prefs.edit().putInt(DAY_MODE_TEXT_COLOR, value).apply()

    /**
     * Returns the day-mode text color for the novel identified by [novelId]. Falls back to the
     * app-wide default ([dayModeTextColor]) when no per-novel value has been stored yet.
     */
    fun getDayTextColorForNovel(novelId: Long): Int =
        prefs.getInt(DAY_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, dayModeTextColor)

    /**
     * Persists the day-mode text color for the novel identified by [novelId]. Does not affect
     * the app-wide default ([dayModeTextColor]) or any other novel's stored preference.
     */
    fun setDayTextColorForNovel(novelId: Long, value: Int) {
        prefs.edit().putInt(DAY_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    /**
     * The default night-mode text color applied to novels that have no per-novel value stored
     * yet (see [getNightTextColorForNovel]).
     */
    var nightModeTextColor: Int
        get() = prefs.getInt(NIGHT_MODE_TEXT_COLOR, Color.WHITE)
        set(value) = prefs.edit().putInt(NIGHT_MODE_TEXT_COLOR, value).apply()

    /**
     * Returns the night-mode text color for the novel identified by [novelId]. Falls back to
     * the app-wide default ([nightModeTextColor]) when no per-novel value has been stored yet.
     */
    fun getNightTextColorForNovel(novelId: Long): Int =
        prefs.getInt(NIGHT_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, nightModeTextColor)

    /**
     * Persists the night-mode text color for the novel identified by [novelId]. Does not
     * affect the app-wide default ([nightModeTextColor]) or any other novel's stored preference.
     */
    fun setNightTextColorForNovel(novelId: Long, value: Int) {
        prefs.edit().putInt(NIGHT_TEXT_COLOR_PER_NOVEL_PREFIX + novelId, value).apply()
    }

    var readAloudNextChapter: Boolean
        get() = prefs.getBoolean(READ_ALOUD_NEXT_CHAPTER, true)
        set(value) = prefs.edit().putBoolean(READ_ALOUD_NEXT_CHAPTER, value).apply()

    var enableScrollingText: Boolean
        get() = prefs.getBoolean(SCROLLING_TEXT, true)
        set(value) = prefs.edit().putBoolean(SCROLLING_TEXT, value).apply()

    var userSpecifiedSelectorQueries: String
        get() = prefs.getString(USER_SPECIFIED_SELECTOR_QUERIES, "") ?: ""
        set(value) = prefs.edit().putString(USER_SPECIFIED_SELECTOR_QUERIES, value).apply()

    var dohProvider: Int
        get() = prefs.getInt(DOH_PROVIDER, PREF_DOH_CLOUDFLARE)
        set(value) = prefs.edit().putInt(DOH_PROVIDER, value).apply()

    /**
     * When true, once a cf_clearance cookie exists for a host, Cloudflare-gated requests are
     * fetched via a WebView instead of being replayed through OkHttp. This avoids the TLS
     * fingerprint mismatch between OkHttp (Java SSLSocket) and WebView (Chromium BoringSSL)
     * that otherwise causes Cloudflare to reject a cookie obtained via manual verification.
     * Slower per-request, but far more likely to succeed. Defaults to on.
     */
    var useWebViewFetcherForCloudflare: Boolean
        get() = prefs.getBoolean(USE_WEBVIEW_FETCHER_FOR_CLOUDFLARE, true)
        set(value) = prefs.edit().putBoolean(USE_WEBVIEW_FETCHER_FOR_CLOUDFLARE, value).apply()

    var htmlCleanerSelectorQueries: ArrayList<SelectorQuery>
        get() = Gson().fromJson(prefs.getString(SELECTOR_QUERIES, "[]"), object : TypeToken<ArrayList<SelectorQuery>>() {}.type)
        set(value) = prefs.edit().putString(SELECTOR_QUERIES, Gson().toJson(value)).apply()

    var automaticExtUpdates: Boolean
        get() = prefs.getBoolean(AUTOMATIC_EXT_UPDATES, false)
        set(value) = prefs.edit().putBoolean(AUTOMATIC_EXT_UPDATES, value).apply()

    var extensionUpdatesCount: Int
        get() = prefs.getInt("extensionUpdatesCount", Color.WHITE)
        set(value) = prefs.edit().putInt("extensionUpdatesCount", value).apply()

    var showNSFWSource: Boolean
        get() = prefs.getBoolean("showNSFWSource", false)
        set(value) = prefs.edit().putBoolean("showNSFWSource", value).apply()

    var trustedSignatures: MutableSet<String>
        get() = prefs.getStringSet("trustedSignatures", emptySet())!!
        set(value) = prefs.edit().putStringSet("trustedSignatures", value).apply()

    var lastExtCheck: Long
        get() = prefs.getLong("lastExtCheck", Date().time)
        set(value) = prefs.edit().putLong("lastExtCheck", value).apply()

    fun isSourceEnabled(sourceKey: String): Boolean = prefs.getBoolean(sourceKey, true)
    fun enableSource(sourceKey: String, enable: Boolean) = prefs.edit().putBoolean(sourceKey, enable).apply()

    var enableAutoScroll: Boolean
        get() = prefs.getBoolean(ENABLE_AUTO_SCROLL, true)
        set(value) = prefs.edit().putBoolean(ENABLE_AUTO_SCROLL, value).apply()

    var autoScrollLength: Int
        get() = prefs.getInt(AUTO_SCROLL_LENGTH, Constants.AUTO_SCROLL_LENGTH_DEFAULT)
        set(value) = prefs.edit().putInt(AUTO_SCROLL_LENGTH, value).apply()

    var autoScrollInterval: Int
        get() = prefs.getInt(AUTO_SCROLL_INTERVAL, Constants.AUTO_SCROLL_INTERVAL_DEFAULT)
        set(value) = prefs.edit().putInt(AUTO_SCROLL_INTERVAL, value).apply()

    var useNUAPIFetch: Boolean
        get() = prefs.getBoolean(USE_NU_API_FETCH, true)
        set(value) = prefs.edit().putBoolean(USE_NU_API_FETCH, value).apply()

    /**
     * The user-selected root under which downloaded chapters are stored.
     * Either [StorageLocation.INTERNAL_TOKEN] ("internal") or a serialized SD volume
     * ("sd:<volumeId>"). Defaults to internal storage when unset.
     */
    var downloadStorageLocation: String
        get() = prefs.getString(DOWNLOAD_STORAGE_LOCATION, StorageLocation.INTERNAL_TOKEN)!!
        set(value) = prefs.edit().putString(DOWNLOAD_STORAGE_LOCATION, value).apply()

    var useAiTts: Boolean
        get() = prefs.getBoolean("useAiTts", false)
        set(value) = prefs.edit().putBoolean("useAiTts", value).apply()

    var enableAutoAppUpdate: Boolean
        get() = prefs.getBoolean(ENABLE_AUTO_APP_UPDATE, true)
        set(value) = prefs.edit().putBoolean(ENABLE_AUTO_APP_UPDATE, value).apply()

    var appNightMode: Boolean
        get() = prefs.getBoolean(APP_NIGHT_MODE, true)
        set(value) = prefs.edit().putBoolean(APP_NIGHT_MODE, value).apply()

    /**
     * True when startup database cleanup hit a corruption error. The launcher screen reads
     * this to offer the user an explicit recovery (reset) option, then clears it once handled.
     */
    var databaseCorruptionDetected: Boolean
        get() = prefs.getBoolean(DATABASE_CORRUPTION_DETECTED, false)
        set(value) = prefs.edit().putBoolean(DATABASE_CORRUPTION_DETECTED, value).apply()
}
