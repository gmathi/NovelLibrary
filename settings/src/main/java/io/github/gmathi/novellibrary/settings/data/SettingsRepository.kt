package io.github.gmathi.novellibrary.settings.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

/**
 * Centralized access to settings data.
 * Uses the same default SharedPreferences instance as the app module to maintain compatibility.
 * Provides type-safe accessors for reader, general, TTS, and sync settings.
 */
class SettingsRepository(context: Context) {
    
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    //region Reader Settings
    
    /**
     * Reader mode (clean pages) enabled/disabled.
     */
    var readerMode: Boolean
        get() = prefs.getBoolean("cleanPages", false)
        set(value) = prefs.edit().putBoolean("cleanPages", value).apply()
    
    /**
     * Text size for reader.
     */
    var textSize: Int
        get() = prefs.getInt("textSize", 0)
        set(value) = prefs.edit().putInt("textSize", value).apply()
    
    /**
     * Japanese swipe direction enabled/disabled.
     */
    var japSwipe: Boolean
        get() = prefs.getBoolean("japSwipe", true)
        set(value) = prefs.edit().putBoolean("japSwipe", value).apply()
    
    /**
     * Show reader scroll indicator.
     */
    var showReaderScroll: Boolean
        get() = prefs.getBoolean("showReaderScroll", true)
        set(value) = prefs.edit().putBoolean("showReaderScroll", value).apply()
    
    /**
     * Show chapter comments.
     */
    var showChapterComments: Boolean
        get() = prefs.getBoolean("showChapterComments", false)
        set(value) = prefs.edit().putBoolean("showChapterComments", value).apply()
    
    /**
     * Enable volume button scrolling.
     */
    var enableVolumeScroll: Boolean
        get() = prefs.getBoolean("volumeScroll", true)
        set(value) = prefs.edit().putBoolean("volumeScroll", value).apply()
    
    /**
     * Volume scroll length.
     */
    var volumeScrollLength: Int
        get() = prefs.getInt("scrollLength", 100)
        set(value) = prefs.edit().putInt("scrollLength", value).apply()
    
    /**
     * Keep screen on while reading.
     */
    var keepScreenOn: Boolean
        get() = prefs.getBoolean("keepScreenOn", true)
        set(value) = prefs.edit().putBoolean("keepScreenOn", value).apply()
    
    /**
     * Enable immersive mode (hide system UI).
     */
    var enableImmersiveMode: Boolean
        get() = prefs.getBoolean("enableImmersiveMode", true)
        set(value) = prefs.edit().putBoolean("enableImmersiveMode", value).apply()
    
    /**
     * Show navigation bar at chapter end.
     */
    var showNavbarAtChapterEnd: Boolean
        get() = prefs.getBoolean("showNavbarAtChapterEnd", true)
        set(value) = prefs.edit().putBoolean("showNavbarAtChapterEnd", value).apply()
    
    /**
     * Keep original text color from web page.
     */
    var keepTextColor: Boolean
        get() = prefs.getBoolean("keepTextColor", false)
        set(value) = prefs.edit().putBoolean("keepTextColor", value).apply()
    
    /**
     * Use alternative text colors.
     */
    var alternativeTextColors: Boolean
        get() = prefs.getBoolean("alternativeTextColors", false)
        set(value) = prefs.edit().putBoolean("alternativeTextColors", value).apply()
    
    /**
     * Limit image width in reader.
     */
    var limitImageWidth: Boolean
        get() = prefs.getBoolean("limitImageWidth", false)
        set(value) = prefs.edit().putBoolean("limitImageWidth", value).apply()
    
    /**
     * Font path for reader.
     */
    var fontPath: String
        get() = prefs.getString("fontPath", "default") ?: "default"
        set(value) = prefs.edit().putString("fontPath", value).apply()
    
    /**
     * Enable cluster pages.
     */
    var enableClusterPages: Boolean
        get() = prefs.getBoolean("enableClusterPages", false)
        set(value) = prefs.edit().putBoolean("enableClusterPages", value).apply()
    
    /**
     * Enable directional links.
     */
    var enableDirectionalLinks: Boolean
        get() = prefs.getBoolean("enableDirectionalLinks", false)
        set(value) = prefs.edit().putBoolean("enableDirectionalLinks", value).apply()
    
    /**
     * Reader mode button visibility.
     */
    var isReaderModeButtonVisible: Boolean
        get() = prefs.getBoolean("isReaderModeButtonVisible", true)
        set(value) = prefs.edit().putBoolean("isReaderModeButtonVisible", value).apply()
    
    /**
     * Day mode background color.
     */
    var dayModeBackgroundColor: Int
        get() = prefs.getInt("dayModeBackgroundColor", -1)
        set(value) = prefs.edit().putInt("dayModeBackgroundColor", value).apply()
    
    /**
     * Night mode background color.
     */
    var nightModeBackgroundColor: Int
        get() = prefs.getInt("nightModeBackgroundColor", -16777216)
        set(value) = prefs.edit().putInt("nightModeBackgroundColor", value).apply()
    
    /**
     * Day mode text color.
     */
    var dayModeTextColor: Int
        get() = prefs.getInt("dayModeTextColor", -16777216)
        set(value) = prefs.edit().putInt("dayModeTextColor", value).apply()
    
    /**
     * Night mode text color.
     */
    var nightModeTextColor: Int
        get() = prefs.getInt("nightModeTextColor", -1)
        set(value) = prefs.edit().putInt("nightModeTextColor", value).apply()
    
    /**
     * Enable auto scroll.
     */
    var enableAutoScroll: Boolean
        get() = prefs.getBoolean("enableAutoScroll", true)
        set(value) = prefs.edit().putBoolean("enableAutoScroll", value).apply()
    
    /**
     * Auto scroll length.
     */
    var autoScrollLength: Int
        get() = prefs.getInt("autoScrollLength", 100)
        set(value) = prefs.edit().putInt("autoScrollLength", value).apply()
    
    /**
     * Auto scroll interval.
     */
    var autoScrollInterval: Int
        get() = prefs.getInt("autoScrollInterval", 100)
        set(value) = prefs.edit().putInt("autoScrollInterval", value).apply()
    
    //endregion
    
    //region General Settings
    
    /**
     * Dark theme enabled/disabled.
     */
    var isDarkTheme: Boolean
        get() = prefs.getBoolean("isDarkTheme", true)
        set(value) = prefs.edit().putBoolean("isDarkTheme", value).apply()
    
    /**
     * App language.
     */
    var language: String
        get() = prefs.getString("language", "System Default") ?: "System Default"
        set(value) = prefs.edit().putString("language", value).apply()
    
    /**
     * JavaScript enabled/disabled.
     */
    var javascriptDisabled: Boolean
        get() = prefs.getBoolean("javascript", false)
        set(value) = prefs.edit().putBoolean("javascript", value).apply()
    
    /**
     * Load library screen on startup.
     */
    var loadLibraryScreen: Boolean
        get() = prefs.getBoolean("loadLibraryScreen", false)
        set(value) = prefs.edit().putBoolean("loadLibraryScreen", value).apply()
    
    /**
     * Enable notifications.
     */
    var enableNotifications: Boolean
        get() = prefs.getBoolean("enableNotifications", true)
        set(value) = prefs.edit().putBoolean("enableNotifications", value).apply()
    
    /**
     * Show chapters left badge.
     */
    var showChaptersLeftBadge: Boolean
        get() = prefs.getBoolean("showChaptersLeftBadge", false)
        set(value) = prefs.edit().putBoolean("showChaptersLeftBadge", value).apply()
    
    /**
     * Developer mode enabled/disabled.
     */
    var isDeveloper: Boolean
        get() = prefs.getBoolean("developer", false)
        set(value) = prefs.edit().putBoolean("developer", value).apply()
    
    //endregion
    
    //region TTS Settings
    
    /**
     * Read aloud next chapter automatically.
     */
    var readAloudNextChapter: Boolean
        get() = prefs.getBoolean("readAloudNextChapter", true)
        set(value) = prefs.edit().putBoolean("readAloudNextChapter", value).apply()
    
    /**
     * Enable scrolling text during TTS.
     */
    var enableScrollingText: Boolean
        get() = prefs.getBoolean("scrollingText", true)
        set(value) = prefs.edit().putBoolean("scrollingText", value).apply()
    
    //endregion
    
    //region Sync Settings
    
    /**
     * Get sync enabled status for a specific service.
     */
    fun getSyncEnabled(serviceName: String): Boolean {
        return prefs.getBoolean("sync_enable_$serviceName", false)
    }
    
    /**
     * Set sync enabled status for a specific service.
     */
    fun setSyncEnabled(serviceName: String, enabled: Boolean) {
        prefs.edit().putBoolean("sync_enable_$serviceName", enabled).apply()
    }
    
    /**
     * Get sync add novels setting for a specific service.
     */
    fun getSyncAddNovels(serviceName: String): Boolean {
        return prefs.getBoolean("sync_add_novels_$serviceName", true)
    }
    
    /**
     * Set sync add novels setting for a specific service.
     */
    fun setSyncAddNovels(serviceName: String, enabled: Boolean) {
        prefs.edit().putBoolean("sync_add_novels_$serviceName", enabled).apply()
    }
    
    /**
     * Get sync delete novels setting for a specific service.
     */
    fun getSyncDeleteNovels(serviceName: String): Boolean {
        return prefs.getBoolean("sync_delete_novels_$serviceName", true)
    }
    
    /**
     * Set sync delete novels setting for a specific service.
     */
    fun setSyncDeleteNovels(serviceName: String, enabled: Boolean) {
        prefs.edit().putBoolean("sync_delete_novels_$serviceName", enabled).apply()
    }
    
    /**
     * Get sync bookmarks setting for a specific service.
     */
    fun getSyncBookmarks(serviceName: String): Boolean {
        return prefs.getBoolean("sync_bookmarks_$serviceName", true)
    }
    
    /**
     * Set sync bookmarks setting for a specific service.
     */
    fun setSyncBookmarks(serviceName: String, enabled: Boolean) {
        prefs.edit().putBoolean("sync_bookmarks_$serviceName", enabled).apply()
    }
    
    //endregion
    
    //region Backup Settings
    
    /**
     * Show backup hint.
     */
    var showBackupHint: Boolean
        get() = prefs.getBoolean("showBackupHint", true)
        set(value) = prefs.edit().putBoolean("showBackupHint", value).apply()
    
    /**
     * Show restore hint.
     */
    var showRestoreHint: Boolean
        get() = prefs.getBoolean("showRestoreHint", true)
        set(value) = prefs.edit().putBoolean("showRestoreHint", value).apply()
    
    /**
     * Backup frequency in hours.
     */
    var backupFrequency: Int
        get() = prefs.getInt("backupFrequencyHours", 0)
        set(value) = prefs.edit().putInt("backupFrequencyHours", value).apply()
    
    /**
     * Last backup timestamp in milliseconds.
     */
    var lastBackup: Long
        get() = prefs.getLong("lastBackupMilliseconds", 0)
        set(value) = prefs.edit().putLong("lastBackupMilliseconds", value).apply()
    
    /**
     * Last local backup timestamp string.
     */
    var lastLocalBackupTimestamp: String
        get() = prefs.getString("lastLocalBackupTimestamp", "N/A") ?: "N/A"
        set(value) = prefs.edit().putString("lastLocalBackupTimestamp", value).apply()
    
    /**
     * Last cloud backup timestamp string.
     */
    var lastCloudBackupTimestamp: String
        get() = prefs.getString("lastCloudBackupTimestamp", "N/A") ?: "N/A"
        set(value) = prefs.edit().putString("lastCloudBackupTimestamp", value).apply()
    
    /**
     * Last backup size string.
     */
    var lastBackupSize: String
        get() = prefs.getString("lastBackupSize", "N/A") ?: "N/A"
        set(value) = prefs.edit().putString("lastBackupSize", value).apply()
    
    /**
     * Google Drive backup interval.
     */
    var gdBackupInterval: String
        get() = prefs.getString("gdBackupInterval", "Never") ?: "Never"
        set(value) = prefs.edit().putString("gdBackupInterval", value).apply()
    
    /**
     * Google Drive account email.
     */
    var gdAccountEmail: String
        get() = prefs.getString("gdAccountEmail", "-") ?: "-"
        set(value) = prefs.edit().putString("gdAccountEmail", value).apply()
    
    /**
     * Google Drive internet type preference.
     */
    var gdInternetType: String
        get() = prefs.getString("gdInternetType", "WiFi or cellular") ?: "WiFi or cellular"
        set(value) = prefs.edit().putString("gdInternetType", value).apply()
    
    //endregion
    
    /**
     * Provides direct access to the underlying SharedPreferences for advanced use cases.
     */
    fun getSharedPreferences(): SharedPreferences = prefs
}
