package io.github.gmathi.novellibrary.settings.data.repository

import android.content.Context
import io.github.gmathi.novellibrary.settings.data.datastore.ISettingsDataStore
import io.github.gmathi.novellibrary.settings.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer for settings data access using DataStore.
 * 
 * This repository wraps ISettingsDataStore and provides a clean API for accessing
 * and updating settings. It exposes settings as Flows for reactive updates and
 * provides suspend functions for write operations.
 * 
 * The repository follows the repository pattern, abstracting the data source
 * (DataStore) from the rest of the application. This allows for easier testing
 * and potential future changes to the storage mechanism.
 */
class SettingsRepositoryDataStore(private val dataStore: ISettingsDataStore) {
    
    /**
     * Constructor that creates a SettingsDataStore from a Context.
     */
    constructor(context: Context) : this(SettingsDataStore(context))
    
    //region Reader Settings
    
    /**
     * Reader mode (clean pages) enabled/disabled.
     */
    val readerMode: Flow<Boolean> = dataStore.readerMode
    
    suspend fun setReaderMode(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.READER_MODE, value)
    }
    
    /**
     * Text size for reader.
     */
    val textSize: Flow<Int> = dataStore.textSize
    
    suspend fun setTextSize(value: Int) {
        dataStore.updateInt(SettingsDataStore.TEXT_SIZE, value)
    }
    
    /**
     * Japanese swipe direction enabled/disabled.
     */
    val japSwipe: Flow<Boolean> = dataStore.japSwipe
    
    suspend fun setJapSwipe(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.JAP_SWIPE, value)
    }
    
    /**
     * Show reader scroll indicator.
     */
    val showReaderScroll: Flow<Boolean> = dataStore.showReaderScroll
    
    suspend fun setShowReaderScroll(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_READER_SCROLL, value)
    }
    
    /**
     * Show chapter comments.
     */
    val showChapterComments: Flow<Boolean> = dataStore.showChapterComments
    
    suspend fun setShowChapterComments(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_CHAPTER_COMMENTS, value)
    }
    
    /**
     * Enable volume button scrolling.
     */
    val enableVolumeScroll: Flow<Boolean> = dataStore.enableVolumeScroll
    
    suspend fun setEnableVolumeScroll(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_VOLUME_SCROLL, value)
    }
    
    /**
     * Volume scroll length.
     */
    val volumeScrollLength: Flow<Int> = dataStore.volumeScrollLength
    
    suspend fun setVolumeScrollLength(value: Int) {
        dataStore.updateInt(SettingsDataStore.VOLUME_SCROLL_LENGTH, value)
    }
    
    /**
     * Keep screen on while reading.
     */
    val keepScreenOn: Flow<Boolean> = dataStore.keepScreenOn
    
    suspend fun setKeepScreenOn(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.KEEP_SCREEN_ON, value)
    }
    
    /**
     * Enable immersive mode (hide system UI).
     */
    val enableImmersiveMode: Flow<Boolean> = dataStore.enableImmersiveMode
    
    suspend fun setEnableImmersiveMode(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_IMMERSIVE_MODE, value)
    }
    
    /**
     * Show navigation bar at chapter end.
     */
    val showNavbarAtChapterEnd: Flow<Boolean> = dataStore.showNavbarAtChapterEnd
    
    suspend fun setShowNavbarAtChapterEnd(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_NAVBAR_AT_CHAPTER_END, value)
    }
    
    /**
     * Keep original text color from web page.
     */
    val keepTextColor: Flow<Boolean> = dataStore.keepTextColor
    
    suspend fun setKeepTextColor(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.KEEP_TEXT_COLOR, value)
    }
    
    /**
     * Use alternative text colors.
     */
    val alternativeTextColors: Flow<Boolean> = dataStore.alternativeTextColors
    
    suspend fun setAlternativeTextColors(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ALTERNATIVE_TEXT_COLORS, value)
    }
    
    /**
     * Limit image width in reader.
     */
    val limitImageWidth: Flow<Boolean> = dataStore.limitImageWidth
    
    suspend fun setLimitImageWidth(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.LIMIT_IMAGE_WIDTH, value)
    }
    
    /**
     * Font path for reader.
     */
    val fontPath: Flow<String> = dataStore.fontPath
    
    suspend fun setFontPath(value: String) {
        dataStore.updateString(SettingsDataStore.FONT_PATH, value)
    }
    
    /**
     * Enable cluster pages.
     */
    val enableClusterPages: Flow<Boolean> = dataStore.enableClusterPages
    
    suspend fun setEnableClusterPages(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_CLUSTER_PAGES, value)
    }
    
    /**
     * Enable directional links.
     */
    val enableDirectionalLinks: Flow<Boolean> = dataStore.enableDirectionalLinks
    
    suspend fun setEnableDirectionalLinks(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_DIRECTIONAL_LINKS, value)
    }
    
    /**
     * Reader mode button visibility.
     */
    val isReaderModeButtonVisible: Flow<Boolean> = dataStore.isReaderModeButtonVisible
    
    suspend fun setIsReaderModeButtonVisible(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.IS_READER_MODE_BUTTON_VISIBLE, value)
    }
    
    /**
     * Day mode background color.
     */
    val dayModeBackgroundColor: Flow<Int> = dataStore.dayModeBackgroundColor
    
    suspend fun setDayModeBackgroundColor(value: Int) {
        dataStore.updateInt(SettingsDataStore.DAY_MODE_BACKGROUND_COLOR, value)
    }
    
    /**
     * Night mode background color.
     */
    val nightModeBackgroundColor: Flow<Int> = dataStore.nightModeBackgroundColor
    
    suspend fun setNightModeBackgroundColor(value: Int) {
        dataStore.updateInt(SettingsDataStore.NIGHT_MODE_BACKGROUND_COLOR, value)
    }
    
    /**
     * Day mode text color.
     */
    val dayModeTextColor: Flow<Int> = dataStore.dayModeTextColor
    
    suspend fun setDayModeTextColor(value: Int) {
        dataStore.updateInt(SettingsDataStore.DAY_MODE_TEXT_COLOR, value)
    }
    
    /**
     * Night mode text color.
     */
    val nightModeTextColor: Flow<Int> = dataStore.nightModeTextColor
    
    suspend fun setNightModeTextColor(value: Int) {
        dataStore.updateInt(SettingsDataStore.NIGHT_MODE_TEXT_COLOR, value)
    }
    
    /**
     * Enable auto scroll.
     */
    val enableAutoScroll: Flow<Boolean> = dataStore.enableAutoScroll
    
    suspend fun setEnableAutoScroll(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_AUTO_SCROLL, value)
    }
    
    /**
     * Auto scroll length.
     */
    val autoScrollLength: Flow<Int> = dataStore.autoScrollLength
    
    suspend fun setAutoScrollLength(value: Int) {
        dataStore.updateInt(SettingsDataStore.AUTO_SCROLL_LENGTH, value)
    }
    
    /**
     * Auto scroll interval.
     */
    val autoScrollInterval: Flow<Int> = dataStore.autoScrollInterval
    
    suspend fun setAutoScrollInterval(value: Int) {
        dataStore.updateInt(SettingsDataStore.AUTO_SCROLL_INTERVAL, value)
    }
    
    //endregion
    
    //region General Settings
    
    /**
     * Dark theme enabled/disabled.
     */
    val isDarkTheme: Flow<Boolean> = dataStore.isDarkTheme
    
    suspend fun setIsDarkTheme(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.IS_DARK_THEME, value)
    }
    
    /**
     * App language.
     */
    val language: Flow<String> = dataStore.language
    
    suspend fun setLanguage(value: String) {
        dataStore.updateString(SettingsDataStore.LANGUAGE, value)
    }
    
    /**
     * JavaScript enabled/disabled.
     */
    val javascriptDisabled: Flow<Boolean> = dataStore.javascriptDisabled
    
    suspend fun setJavascriptDisabled(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.JAVASCRIPT_DISABLED, value)
    }
    
    /**
     * Load library screen on startup.
     */
    val loadLibraryScreen: Flow<Boolean> = dataStore.loadLibraryScreen
    
    suspend fun setLoadLibraryScreen(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.LOAD_LIBRARY_SCREEN, value)
    }
    
    /**
     * Enable notifications.
     */
    val enableNotifications: Flow<Boolean> = dataStore.enableNotifications
    
    suspend fun setEnableNotifications(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_NOTIFICATIONS, value)
    }
    
    /**
     * Show chapters left badge.
     */
    val showChaptersLeftBadge: Flow<Boolean> = dataStore.showChaptersLeftBadge
    
    suspend fun setShowChaptersLeftBadge(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_CHAPTERS_LEFT_BADGE, value)
    }
    
    /**
     * Developer mode enabled/disabled.
     */
    val isDeveloper: Flow<Boolean> = dataStore.isDeveloper
    
    suspend fun setIsDeveloper(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.IS_DEVELOPER, value)
    }
    
    /**
     * Network timeout in seconds.
     */
    val networkTimeoutSeconds: Flow<Int> = dataStore.networkTimeoutSeconds
    
    suspend fun setNetworkTimeoutSeconds(value: Int) {
        dataStore.updateInt(SettingsDataStore.NETWORK_TIMEOUT_SECONDS, value)
    }
    
    //endregion
    
    //region TTS Settings
    
    /**
     * Read aloud next chapter automatically.
     */
    val readAloudNextChapter: Flow<Boolean> = dataStore.readAloudNextChapter
    
    suspend fun setReadAloudNextChapter(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.READ_ALOUD_NEXT_CHAPTER, value)
    }
    
    /**
     * Enable scrolling text during TTS.
     */
    val enableScrollingText: Flow<Boolean> = dataStore.enableScrollingText
    
    suspend fun setEnableScrollingText(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.ENABLE_SCROLLING_TEXT, value)
    }
    
    //endregion
    
    //region Backup Settings
    
    /**
     * Show backup hint.
     */
    val showBackupHint: Flow<Boolean> = dataStore.showBackupHint
    
    suspend fun setShowBackupHint(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_BACKUP_HINT, value)
    }
    
    /**
     * Show restore hint.
     */
    val showRestoreHint: Flow<Boolean> = dataStore.showRestoreHint
    
    suspend fun setShowRestoreHint(value: Boolean) {
        dataStore.updateBoolean(SettingsDataStore.SHOW_RESTORE_HINT, value)
    }
    
    /**
     * Backup frequency in hours.
     */
    val backupFrequency: Flow<Int> = dataStore.backupFrequency
    
    suspend fun setBackupFrequency(value: Int) {
        dataStore.updateInt(SettingsDataStore.BACKUP_FREQUENCY, value)
    }
    
    /**
     * Last backup timestamp in milliseconds.
     */
    val lastBackup: Flow<Long> = dataStore.lastBackup
    
    suspend fun setLastBackup(value: Long) {
        dataStore.updateLong(SettingsDataStore.LAST_BACKUP, value)
    }
    
    /**
     * Last local backup timestamp string.
     */
    val lastLocalBackupTimestamp: Flow<String> = dataStore.lastLocalBackupTimestamp
    
    suspend fun setLastLocalBackupTimestamp(value: String) {
        dataStore.updateString(SettingsDataStore.LAST_LOCAL_BACKUP_TIMESTAMP, value)
    }
    
    /**
     * Last cloud backup timestamp string.
     */
    val lastCloudBackupTimestamp: Flow<String> = dataStore.lastCloudBackupTimestamp
    
    suspend fun setLastCloudBackupTimestamp(value: String) {
        dataStore.updateString(SettingsDataStore.LAST_CLOUD_BACKUP_TIMESTAMP, value)
    }
    
    /**
     * Last backup size string.
     */
    val lastBackupSize: Flow<String> = dataStore.lastBackupSize
    
    suspend fun setLastBackupSize(value: String) {
        dataStore.updateString(SettingsDataStore.LAST_BACKUP_SIZE, value)
    }
    
    /**
     * Google Drive backup interval.
     */
    val gdBackupInterval: Flow<String> = dataStore.gdBackupInterval
    
    suspend fun setGdBackupInterval(value: String) {
        dataStore.updateString(SettingsDataStore.GD_BACKUP_INTERVAL, value)
    }
    
    /**
     * Google Drive account email.
     */
    val gdAccountEmail: Flow<String> = dataStore.gdAccountEmail
    
    suspend fun setGdAccountEmail(value: String) {
        dataStore.updateString(SettingsDataStore.GD_ACCOUNT_EMAIL, value)
    }
    
    /**
     * Google Drive internet type preference.
     */
    val gdInternetType: Flow<String> = dataStore.gdInternetType
    
    suspend fun setGdInternetType(value: String) {
        dataStore.updateString(SettingsDataStore.GD_INTERNET_TYPE, value)
    }
    
    //endregion
    
    //region Sync Settings
    
    /**
     * Get sync enabled status for a specific service.
     */
    fun getSyncEnabled(serviceName: String): Flow<Boolean> {
        return dataStore.getSyncEnabled(serviceName)
    }
    
    /**
     * Set sync enabled status for a specific service.
     */
    suspend fun setSyncEnabled(serviceName: String, enabled: Boolean) {
        dataStore.updateSyncSetting("sync_enable_$serviceName", enabled)
    }
    
    /**
     * Get sync add novels setting for a specific service.
     */
    fun getSyncAddNovels(serviceName: String): Flow<Boolean> {
        return dataStore.getSyncAddNovels(serviceName)
    }
    
    /**
     * Set sync add novels setting for a specific service.
     */
    suspend fun setSyncAddNovels(serviceName: String, enabled: Boolean) {
        dataStore.updateSyncSetting("sync_add_novels_$serviceName", enabled)
    }
    
    /**
     * Get sync delete novels setting for a specific service.
     */
    fun getSyncDeleteNovels(serviceName: String): Flow<Boolean> {
        return dataStore.getSyncDeleteNovels(serviceName)
    }
    
    /**
     * Set sync delete novels setting for a specific service.
     */
    suspend fun setSyncDeleteNovels(serviceName: String, enabled: Boolean) {
        dataStore.updateSyncSetting("sync_delete_novels_$serviceName", enabled)
    }
    
    /**
     * Get sync bookmarks setting for a specific service.
     */
    fun getSyncBookmarks(serviceName: String): Flow<Boolean> {
        return dataStore.getSyncBookmarks(serviceName)
    }
    
    /**
     * Set sync bookmarks setting for a specific service.
     */
    suspend fun setSyncBookmarks(serviceName: String, enabled: Boolean) {
        dataStore.updateSyncSetting("sync_bookmarks_$serviceName", enabled)
    }
    
    //endregion
}
