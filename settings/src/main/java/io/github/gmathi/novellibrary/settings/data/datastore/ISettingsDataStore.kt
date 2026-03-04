package io.github.gmathi.novellibrary.settings.data.datastore

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Interface for settings data storage.
 * 
 * Defines the contract for accessing and updating application settings.
 * This interface allows for easy mocking in tests and previews.
 */
interface ISettingsDataStore {
    
    //region Reader Settings
    val readerMode: Flow<Boolean>
    val textSize: Flow<Int>
    val japSwipe: Flow<Boolean>
    val showReaderScroll: Flow<Boolean>
    val showChapterComments: Flow<Boolean>
    val enableVolumeScroll: Flow<Boolean>
    val volumeScrollLength: Flow<Int>
    val keepScreenOn: Flow<Boolean>
    val enableImmersiveMode: Flow<Boolean>
    val showNavbarAtChapterEnd: Flow<Boolean>
    val keepTextColor: Flow<Boolean>
    val alternativeTextColors: Flow<Boolean>
    val limitImageWidth: Flow<Boolean>
    val fontPath: Flow<String>
    val enableClusterPages: Flow<Boolean>
    val enableDirectionalLinks: Flow<Boolean>
    val isReaderModeButtonVisible: Flow<Boolean>
    val dayModeBackgroundColor: Flow<Int>
    val nightModeBackgroundColor: Flow<Int>
    val dayModeTextColor: Flow<Int>
    val nightModeTextColor: Flow<Int>
    val enableAutoScroll: Flow<Boolean>
    val autoScrollLength: Flow<Int>
    val autoScrollInterval: Flow<Int>
    //endregion
    
    //region General Settings
    val isDarkTheme: Flow<Boolean>
    val language: Flow<String>
    val javascriptDisabled: Flow<Boolean>
    val loadLibraryScreen: Flow<Boolean>
    val enableNotifications: Flow<Boolean>
    val showChaptersLeftBadge: Flow<Boolean>
    val isDeveloper: Flow<Boolean>
    //endregion
    
    //region TTS Settings
    val readAloudNextChapter: Flow<Boolean>
    val enableScrollingText: Flow<Boolean>
    //endregion
    
    //region Backup Settings
    val showBackupHint: Flow<Boolean>
    val showRestoreHint: Flow<Boolean>
    val backupFrequency: Flow<Int>
    val lastBackup: Flow<Long>
    val lastLocalBackupTimestamp: Flow<String>
    val lastCloudBackupTimestamp: Flow<String>
    val lastBackupSize: Flow<String>
    val gdBackupInterval: Flow<String>
    val gdAccountEmail: Flow<String>
    val gdInternetType: Flow<String>
    //endregion
    
    //region Sync Settings
    fun getSyncEnabled(serviceName: String): Flow<Boolean>
    fun getSyncAddNovels(serviceName: String): Flow<Boolean>
    fun getSyncDeleteNovels(serviceName: String): Flow<Boolean>
    fun getSyncBookmarks(serviceName: String): Flow<Boolean>
    //endregion
    
    //region Write Operations
    suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean)
    suspend fun updateInt(key: Preferences.Key<Int>, value: Int)
    suspend fun updateLong(key: Preferences.Key<Long>, value: Long)
    suspend fun updateString(key: Preferences.Key<String>, value: String)
    suspend fun updateSyncSetting(keyName: String, value: Boolean)
    //endregion
}
