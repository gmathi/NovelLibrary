package io.github.gmathi.novellibrary.settings.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceManager

/**
 * Migrates settings from SharedPreferences to DataStore.
 * 
 * This migration runs automatically the first time DataStore is accessed.
 * It copies all existing settings from SharedPreferences to DataStore,
 * preserving all user preferences during the upgrade.
 * 
 * The migration handles all setting types (boolean, int, long, string) and
 * includes special handling for dynamic sync settings that use service names.
 */
class SharedPreferencesMigration(private val context: Context) : DataMigration<Preferences> {
    
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }
    
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        // Migrate if DataStore is empty (first run with DataStore)
        return currentData.asMap().isEmpty()
    }
    
    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutablePreferences = currentData.toMutablePreferences()
        
        try {
            // Migrate all settings from SharedPreferences to DataStore
            migrateReaderSettings(mutablePreferences)
            migrateGeneralSettings(mutablePreferences)
            migrateTTSSettings(mutablePreferences)
            migrateBackupSettings(mutablePreferences)
            migrateSyncSettings(mutablePreferences)
            
            // Log successful migration
            println("Settings migration completed successfully")
        } catch (e: Exception) {
            // Log migration error but don't crash
            e.printStackTrace()
            println("Settings migration encountered an error: ${e.message}")
        }
        
        return mutablePreferences.toPreferences()
    }
    
    override suspend fun cleanUp() {
        // Optionally clear SharedPreferences after successful migration
        // For safety, we keep SharedPreferences intact in case of rollback
        // sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Migrate reader settings from SharedPreferences to DataStore.
     */
    private fun migrateReaderSettings(preferences: MutablePreferences) {
        // Boolean settings
        migrateBooleanIfExists(preferences, "cleanPages", SettingsDataStore.READER_MODE, false)
        migrateBooleanIfExists(preferences, "japSwipe", SettingsDataStore.JAP_SWIPE, true)
        migrateBooleanIfExists(preferences, "showReaderScroll", SettingsDataStore.SHOW_READER_SCROLL, true)
        migrateBooleanIfExists(preferences, "showChapterComments", SettingsDataStore.SHOW_CHAPTER_COMMENTS, false)
        migrateBooleanIfExists(preferences, "volumeScroll", SettingsDataStore.ENABLE_VOLUME_SCROLL, true)
        migrateBooleanIfExists(preferences, "keepScreenOn", SettingsDataStore.KEEP_SCREEN_ON, true)
        migrateBooleanIfExists(preferences, "enableImmersiveMode", SettingsDataStore.ENABLE_IMMERSIVE_MODE, true)
        migrateBooleanIfExists(preferences, "showNavbarAtChapterEnd", SettingsDataStore.SHOW_NAVBAR_AT_CHAPTER_END, true)
        migrateBooleanIfExists(preferences, "keepTextColor", SettingsDataStore.KEEP_TEXT_COLOR, false)
        migrateBooleanIfExists(preferences, "alternativeTextColors", SettingsDataStore.ALTERNATIVE_TEXT_COLORS, false)
        migrateBooleanIfExists(preferences, "limitImageWidth", SettingsDataStore.LIMIT_IMAGE_WIDTH, false)
        migrateBooleanIfExists(preferences, "enableClusterPages", SettingsDataStore.ENABLE_CLUSTER_PAGES, false)
        migrateBooleanIfExists(preferences, "enableDirectionalLinks", SettingsDataStore.ENABLE_DIRECTIONAL_LINKS, false)
        migrateBooleanIfExists(preferences, "isReaderModeButtonVisible", SettingsDataStore.IS_READER_MODE_BUTTON_VISIBLE, true)
        migrateBooleanIfExists(preferences, "enableAutoScroll", SettingsDataStore.ENABLE_AUTO_SCROLL, true)
        
        // Integer settings
        migrateIntIfExists(preferences, "textSize", SettingsDataStore.TEXT_SIZE, 0)
        migrateIntIfExists(preferences, "scrollLength", SettingsDataStore.VOLUME_SCROLL_LENGTH, 100)
        migrateIntIfExists(preferences, "dayModeBackgroundColor", SettingsDataStore.DAY_MODE_BACKGROUND_COLOR, -1)
        migrateIntIfExists(preferences, "nightModeBackgroundColor", SettingsDataStore.NIGHT_MODE_BACKGROUND_COLOR, -16777216)
        migrateIntIfExists(preferences, "dayModeTextColor", SettingsDataStore.DAY_MODE_TEXT_COLOR, -16777216)
        migrateIntIfExists(preferences, "nightModeTextColor", SettingsDataStore.NIGHT_MODE_TEXT_COLOR, -1)
        migrateIntIfExists(preferences, "autoScrollLength", SettingsDataStore.AUTO_SCROLL_LENGTH, 100)
        migrateIntIfExists(preferences, "autoScrollInterval", SettingsDataStore.AUTO_SCROLL_INTERVAL, 100)
        
        // String settings
        migrateStringIfExists(preferences, "fontPath", SettingsDataStore.FONT_PATH, "default")
    }
    
    /**
     * Migrate general settings from SharedPreferences to DataStore.
     */
    private fun migrateGeneralSettings(preferences: MutablePreferences) {
        // Boolean settings
        migrateBooleanIfExists(preferences, "isDarkTheme", SettingsDataStore.IS_DARK_THEME, true)
        migrateBooleanIfExists(preferences, "javascript", SettingsDataStore.JAVASCRIPT_DISABLED, false)
        migrateBooleanIfExists(preferences, "loadLibraryScreen", SettingsDataStore.LOAD_LIBRARY_SCREEN, false)
        migrateBooleanIfExists(preferences, "enableNotifications", SettingsDataStore.ENABLE_NOTIFICATIONS, true)
        migrateBooleanIfExists(preferences, "showChaptersLeftBadge", SettingsDataStore.SHOW_CHAPTERS_LEFT_BADGE, false)
        migrateBooleanIfExists(preferences, "developer", SettingsDataStore.IS_DEVELOPER, false)
        
        // String settings
        migrateStringIfExists(preferences, "language", SettingsDataStore.LANGUAGE, "System Default")
    }
    
    /**
     * Migrate TTS settings from SharedPreferences to DataStore.
     */
    private fun migrateTTSSettings(preferences: MutablePreferences) {
        migrateBooleanIfExists(preferences, "readAloudNextChapter", SettingsDataStore.READ_ALOUD_NEXT_CHAPTER, true)
        migrateBooleanIfExists(preferences, "scrollingText", SettingsDataStore.ENABLE_SCROLLING_TEXT, true)
    }
    
    /**
     * Migrate backup settings from SharedPreferences to DataStore.
     */
    private fun migrateBackupSettings(preferences: MutablePreferences) {
        // Boolean settings
        migrateBooleanIfExists(preferences, "showBackupHint", SettingsDataStore.SHOW_BACKUP_HINT, true)
        migrateBooleanIfExists(preferences, "showRestoreHint", SettingsDataStore.SHOW_RESTORE_HINT, true)
        
        // Integer settings
        migrateIntIfExists(preferences, "backupFrequencyHours", SettingsDataStore.BACKUP_FREQUENCY, 0)
        
        // Long settings
        migrateLongIfExists(preferences, "lastBackupMilliseconds", SettingsDataStore.LAST_BACKUP, 0)
        
        // String settings
        migrateStringIfExists(preferences, "lastLocalBackupTimestamp", SettingsDataStore.LAST_LOCAL_BACKUP_TIMESTAMP, "N/A")
        migrateStringIfExists(preferences, "lastCloudBackupTimestamp", SettingsDataStore.LAST_CLOUD_BACKUP_TIMESTAMP, "N/A")
        migrateStringIfExists(preferences, "lastBackupSize", SettingsDataStore.LAST_BACKUP_SIZE, "N/A")
        migrateStringIfExists(preferences, "gdBackupInterval", SettingsDataStore.GD_BACKUP_INTERVAL, "Never")
        migrateStringIfExists(preferences, "gdAccountEmail", SettingsDataStore.GD_ACCOUNT_EMAIL, "-")
        migrateStringIfExists(preferences, "gdInternetType", SettingsDataStore.GD_INTERNET_TYPE, "WiFi or cellular")
    }
    
    /**
     * Migrate sync settings from SharedPreferences to DataStore.
     * Sync settings use dynamic keys based on service names.
     */
    private fun migrateSyncSettings(preferences: MutablePreferences) {
        // Get all SharedPreferences keys
        val allKeys = sharedPreferences.all.keys
        
        // Migrate all sync-related keys
        allKeys.forEach { key ->
            when {
                key.startsWith("sync_enable_") -> {
                    val value = sharedPreferences.getBoolean(key, false)
                    preferences[booleanPreferencesKey(key)] = value
                }
                key.startsWith("sync_add_novels_") -> {
                    val value = sharedPreferences.getBoolean(key, true)
                    preferences[booleanPreferencesKey(key)] = value
                }
                key.startsWith("sync_delete_novels_") -> {
                    val value = sharedPreferences.getBoolean(key, true)
                    preferences[booleanPreferencesKey(key)] = value
                }
                key.startsWith("sync_bookmarks_") -> {
                    val value = sharedPreferences.getBoolean(key, true)
                    preferences[booleanPreferencesKey(key)] = value
                }
            }
        }
    }
    
    /**
     * Migrate a boolean preference if it exists in SharedPreferences.
     */
    private fun migrateBooleanIfExists(
        preferences: MutablePreferences,
        spKey: String,
        dsKey: Preferences.Key<Boolean>,
        defaultValue: Boolean
    ) {
        if (sharedPreferences.contains(spKey)) {
            val value = sharedPreferences.getBoolean(spKey, defaultValue)
            preferences[dsKey] = value
        }
    }
    
    /**
     * Migrate an integer preference if it exists in SharedPreferences.
     */
    private fun migrateIntIfExists(
        preferences: MutablePreferences,
        spKey: String,
        dsKey: Preferences.Key<Int>,
        defaultValue: Int
    ) {
        if (sharedPreferences.contains(spKey)) {
            val value = sharedPreferences.getInt(spKey, defaultValue)
            preferences[dsKey] = value
        }
    }
    
    /**
     * Migrate a long preference if it exists in SharedPreferences.
     */
    private fun migrateLongIfExists(
        preferences: MutablePreferences,
        spKey: String,
        dsKey: Preferences.Key<Long>,
        defaultValue: Long
    ) {
        if (sharedPreferences.contains(spKey)) {
            val value = sharedPreferences.getLong(spKey, defaultValue)
            preferences[dsKey] = value
        }
    }
    
    /**
     * Migrate a string preference if it exists in SharedPreferences.
     */
    private fun migrateStringIfExists(
        preferences: MutablePreferences,
        spKey: String,
        dsKey: Preferences.Key<String>,
        defaultValue: String
    ) {
        if (sharedPreferences.contains(spKey)) {
            val value = sharedPreferences.getString(spKey, defaultValue) ?: defaultValue
            preferences[dsKey] = value
        }
    }
}
