package io.github.gmathi.novellibrary.settings.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore-based settings storage with Flow-based accessors.
 * Provides type-safe, reactive access to all application settings.
 * 
 * This class defines preference keys for all settings and implements Flow-based
 * accessors that emit updates whenever settings change. Errors during read/write
 * operations are handled gracefully with fallback to default values.
 */
class SettingsDataStore(private val context: Context) {
    
    companion object {
        // DataStore instance
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        
        //region Reader Settings Keys
        val READER_MODE = booleanPreferencesKey("cleanPages")
        val TEXT_SIZE = intPreferencesKey("textSize")
        val JAP_SWIPE = booleanPreferencesKey("japSwipe")
        val SHOW_READER_SCROLL = booleanPreferencesKey("showReaderScroll")
        val SHOW_CHAPTER_COMMENTS = booleanPreferencesKey("showChapterComments")
        val ENABLE_VOLUME_SCROLL = booleanPreferencesKey("volumeScroll")
        val VOLUME_SCROLL_LENGTH = intPreferencesKey("scrollLength")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keepScreenOn")
        val ENABLE_IMMERSIVE_MODE = booleanPreferencesKey("enableImmersiveMode")
        val SHOW_NAVBAR_AT_CHAPTER_END = booleanPreferencesKey("showNavbarAtChapterEnd")
        val KEEP_TEXT_COLOR = booleanPreferencesKey("keepTextColor")
        val ALTERNATIVE_TEXT_COLORS = booleanPreferencesKey("alternativeTextColors")
        val LIMIT_IMAGE_WIDTH = booleanPreferencesKey("limitImageWidth")
        val FONT_PATH = stringPreferencesKey("fontPath")
        val ENABLE_CLUSTER_PAGES = booleanPreferencesKey("enableClusterPages")
        val ENABLE_DIRECTIONAL_LINKS = booleanPreferencesKey("enableDirectionalLinks")
        val IS_READER_MODE_BUTTON_VISIBLE = booleanPreferencesKey("isReaderModeButtonVisible")
        val DAY_MODE_BACKGROUND_COLOR = intPreferencesKey("dayModeBackgroundColor")
        val NIGHT_MODE_BACKGROUND_COLOR = intPreferencesKey("nightModeBackgroundColor")
        val DAY_MODE_TEXT_COLOR = intPreferencesKey("dayModeTextColor")
        val NIGHT_MODE_TEXT_COLOR = intPreferencesKey("nightModeTextColor")
        val ENABLE_AUTO_SCROLL = booleanPreferencesKey("enableAutoScroll")
        val AUTO_SCROLL_LENGTH = intPreferencesKey("autoScrollLength")
        val AUTO_SCROLL_INTERVAL = intPreferencesKey("autoScrollInterval")
        //endregion
        
        //region General Settings Keys
        val IS_DARK_THEME = booleanPreferencesKey("isDarkTheme")
        val LANGUAGE = stringPreferencesKey("language")
        val JAVASCRIPT_DISABLED = booleanPreferencesKey("javascript")
        val LOAD_LIBRARY_SCREEN = booleanPreferencesKey("loadLibraryScreen")
        val ENABLE_NOTIFICATIONS = booleanPreferencesKey("enableNotifications")
        val SHOW_CHAPTERS_LEFT_BADGE = booleanPreferencesKey("showChaptersLeftBadge")
        val IS_DEVELOPER = booleanPreferencesKey("developer")
        //endregion
        
        //region TTS Settings Keys
        val READ_ALOUD_NEXT_CHAPTER = booleanPreferencesKey("readAloudNextChapter")
        val ENABLE_SCROLLING_TEXT = booleanPreferencesKey("scrollingText")
        //endregion
        
        //region Backup Settings Keys
        val SHOW_BACKUP_HINT = booleanPreferencesKey("showBackupHint")
        val SHOW_RESTORE_HINT = booleanPreferencesKey("showRestoreHint")
        val BACKUP_FREQUENCY = intPreferencesKey("backupFrequencyHours")
        val LAST_BACKUP = longPreferencesKey("lastBackupMilliseconds")
        val LAST_LOCAL_BACKUP_TIMESTAMP = stringPreferencesKey("lastLocalBackupTimestamp")
        val LAST_CLOUD_BACKUP_TIMESTAMP = stringPreferencesKey("lastCloudBackupTimestamp")
        val LAST_BACKUP_SIZE = stringPreferencesKey("lastBackupSize")
        val GD_BACKUP_INTERVAL = stringPreferencesKey("gdBackupInterval")
        val GD_ACCOUNT_EMAIL = stringPreferencesKey("gdAccountEmail")
        val GD_INTERNET_TYPE = stringPreferencesKey("gdInternetType")
        //endregion
        
        // Sync settings use dynamic keys based on service name
        // Format: "sync_enable_{serviceName}", "sync_add_novels_{serviceName}", etc.
    }
    
    //region Reader Settings Flows
    
    /**
     * Reader mode (clean pages) enabled/disabled.
     */
    val readerMode: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[READER_MODE] ?: false }
    
    /**
     * Text size for reader.
     */
    val textSize: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[TEXT_SIZE] ?: 0 }
    
    /**
     * Japanese swipe direction enabled/disabled.
     */
    val japSwipe: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[JAP_SWIPE] ?: true }
    
    /**
     * Show reader scroll indicator.
     */
    val showReaderScroll: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_READER_SCROLL] ?: true }
    
    /**
     * Show chapter comments.
     */
    val showChapterComments: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_CHAPTER_COMMENTS] ?: false }
    
    /**
     * Enable volume button scrolling.
     */
    val enableVolumeScroll: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_VOLUME_SCROLL] ?: true }
    
    /**
     * Volume scroll length.
     */
    val volumeScrollLength: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[VOLUME_SCROLL_LENGTH] ?: 100 }
    
    /**
     * Keep screen on while reading.
     */
    val keepScreenOn: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[KEEP_SCREEN_ON] ?: true }
    
    /**
     * Enable immersive mode (hide system UI).
     */
    val enableImmersiveMode: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_IMMERSIVE_MODE] ?: true }
    
    /**
     * Show navigation bar at chapter end.
     */
    val showNavbarAtChapterEnd: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_NAVBAR_AT_CHAPTER_END] ?: true }
    
    /**
     * Keep original text color from web page.
     */
    val keepTextColor: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[KEEP_TEXT_COLOR] ?: false }
    
    /**
     * Use alternative text colors.
     */
    val alternativeTextColors: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ALTERNATIVE_TEXT_COLORS] ?: false }
    
    /**
     * Limit image width in reader.
     */
    val limitImageWidth: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LIMIT_IMAGE_WIDTH] ?: false }
    
    /**
     * Font path for reader.
     */
    val fontPath: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[FONT_PATH] ?: "default" }
    
    /**
     * Enable cluster pages.
     */
    val enableClusterPages: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_CLUSTER_PAGES] ?: false }
    
    /**
     * Enable directional links.
     */
    val enableDirectionalLinks: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_DIRECTIONAL_LINKS] ?: false }
    
    /**
     * Reader mode button visibility.
     */
    val isReaderModeButtonVisible: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[IS_READER_MODE_BUTTON_VISIBLE] ?: true }
    
    /**
     * Day mode background color.
     */
    val dayModeBackgroundColor: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[DAY_MODE_BACKGROUND_COLOR] ?: -1 }
    
    /**
     * Night mode background color.
     */
    val nightModeBackgroundColor: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[NIGHT_MODE_BACKGROUND_COLOR] ?: -16777216 }
    
    /**
     * Day mode text color.
     */
    val dayModeTextColor: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[DAY_MODE_TEXT_COLOR] ?: -16777216 }
    
    /**
     * Night mode text color.
     */
    val nightModeTextColor: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[NIGHT_MODE_TEXT_COLOR] ?: -1 }
    
    /**
     * Enable auto scroll.
     */
    val enableAutoScroll: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_AUTO_SCROLL] ?: true }
    
    /**
     * Auto scroll length.
     */
    val autoScrollLength: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[AUTO_SCROLL_LENGTH] ?: 100 }
    
    /**
     * Auto scroll interval.
     */
    val autoScrollInterval: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[AUTO_SCROLL_INTERVAL] ?: 100 }
    
    //endregion
    
    //region General Settings Flows
    
    /**
     * Dark theme enabled/disabled.
     */
    val isDarkTheme: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[IS_DARK_THEME] ?: true }
    
    /**
     * App language.
     */
    val language: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LANGUAGE] ?: "System Default" }
    
    /**
     * JavaScript enabled/disabled.
     */
    val javascriptDisabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[JAVASCRIPT_DISABLED] ?: false }
    
    /**
     * Load library screen on startup.
     */
    val loadLibraryScreen: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LOAD_LIBRARY_SCREEN] ?: false }
    
    /**
     * Enable notifications.
     */
    val enableNotifications: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_NOTIFICATIONS] ?: true }
    
    /**
     * Show chapters left badge.
     */
    val showChaptersLeftBadge: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_CHAPTERS_LEFT_BADGE] ?: false }
    
    /**
     * Developer mode enabled/disabled.
     */
    val isDeveloper: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[IS_DEVELOPER] ?: false }
    
    //endregion
    
    //region TTS Settings Flows
    
    /**
     * Read aloud next chapter automatically.
     */
    val readAloudNextChapter: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[READ_ALOUD_NEXT_CHAPTER] ?: true }
    
    /**
     * Enable scrolling text during TTS.
     */
    val enableScrollingText: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[ENABLE_SCROLLING_TEXT] ?: true }
    
    //endregion
    
    //region Backup Settings Flows
    
    /**
     * Show backup hint.
     */
    val showBackupHint: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_BACKUP_HINT] ?: true }
    
    /**
     * Show restore hint.
     */
    val showRestoreHint: Flow<Boolean> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[SHOW_RESTORE_HINT] ?: true }
    
    /**
     * Backup frequency in hours.
     */
    val backupFrequency: Flow<Int> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[BACKUP_FREQUENCY] ?: 0 }
    
    /**
     * Last backup timestamp in milliseconds.
     */
    val lastBackup: Flow<Long> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LAST_BACKUP] ?: 0 }
    
    /**
     * Last local backup timestamp string.
     */
    val lastLocalBackupTimestamp: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LAST_LOCAL_BACKUP_TIMESTAMP] ?: "N/A" }
    
    /**
     * Last cloud backup timestamp string.
     */
    val lastCloudBackupTimestamp: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LAST_CLOUD_BACKUP_TIMESTAMP] ?: "N/A" }
    
    /**
     * Last backup size string.
     */
    val lastBackupSize: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[LAST_BACKUP_SIZE] ?: "N/A" }
    
    /**
     * Google Drive backup interval.
     */
    val gdBackupInterval: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[GD_BACKUP_INTERVAL] ?: "Never" }
    
    /**
     * Google Drive account email.
     */
    val gdAccountEmail: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[GD_ACCOUNT_EMAIL] ?: "-" }
    
    /**
     * Google Drive internet type preference.
     */
    val gdInternetType: Flow<String> = context.dataStore.data
        .catch { exception -> handleException(exception) }
        .map { preferences -> preferences[GD_INTERNET_TYPE] ?: "WiFi or cellular" }
    
    //endregion
    
    //region Sync Settings Flows
    
    /**
     * Get sync enabled status for a specific service.
     */
    fun getSyncEnabled(serviceName: String): Flow<Boolean> {
        val key = booleanPreferencesKey("sync_enable_$serviceName")
        return context.dataStore.data
            .catch { exception -> handleException(exception) }
            .map { preferences -> preferences[key] ?: false }
    }
    
    /**
     * Get sync add novels setting for a specific service.
     */
    fun getSyncAddNovels(serviceName: String): Flow<Boolean> {
        val key = booleanPreferencesKey("sync_add_novels_$serviceName")
        return context.dataStore.data
            .catch { exception -> handleException(exception) }
            .map { preferences -> preferences[key] ?: true }
    }
    
    /**
     * Get sync delete novels setting for a specific service.
     */
    fun getSyncDeleteNovels(serviceName: String): Flow<Boolean> {
        val key = booleanPreferencesKey("sync_delete_novels_$serviceName")
        return context.dataStore.data
            .catch { exception -> handleException(exception) }
            .map { preferences -> preferences[key] ?: true }
    }
    
    /**
     * Get sync bookmarks setting for a specific service.
     */
    fun getSyncBookmarks(serviceName: String): Flow<Boolean> {
        val key = booleanPreferencesKey("sync_bookmarks_$serviceName")
        return context.dataStore.data
            .catch { exception -> handleException(exception) }
            .map { preferences -> preferences[key] ?: true }
    }
    
    //endregion
    
    //region Write Operations
    
    /**
     * Update a boolean setting.
     */
    suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            // Handle error gracefully - log but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Update an integer setting.
     */
    suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            // Handle error gracefully - log but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Update a long setting.
     */
    suspend fun updateLong(key: Preferences.Key<Long>, value: Long) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            // Handle error gracefully - log but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Update a string setting.
     */
    suspend fun updateString(key: Preferences.Key<String>, value: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            // Handle error gracefully - log but don't crash
            e.printStackTrace()
        }
    }
    
    /**
     * Update a sync setting for a specific service.
     */
    suspend fun updateSyncSetting(keyName: String, value: Boolean) {
        try {
            val key = booleanPreferencesKey(keyName)
            context.dataStore.edit { preferences ->
                preferences[key] = value
            }
        } catch (e: IOException) {
            // Handle error gracefully - log but don't crash
            e.printStackTrace()
        }
    }
    
    //endregion
    
    /**
     * Handle exceptions during DataStore operations.
     * Logs the error and allows the Flow to continue with default values.
     */
    private fun handleException(exception: Throwable) {
        if (exception is IOException) {
            // Log IO errors but don't crash
            exception.printStackTrace()
        } else {
            // Re-throw unexpected exceptions
            throw exception
        }
    }
}
