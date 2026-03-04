package io.github.gmathi.novellibrary.settings.data.datastore

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake implementation of ISettingsDataStore for testing and previews.
 * 
 * This class provides a simple in-memory implementation that can be used
 * in Compose previews and unit tests without requiring Android Context
 * or DataStore dependencies.
 */
class FakeSettingsDataStore : ISettingsDataStore {
    
    //region Reader Settings
    override val readerMode = MutableStateFlow(true)
    override val textSize = MutableStateFlow(16)
    override val japSwipe = MutableStateFlow(false)
    override val showReaderScroll = MutableStateFlow(true)
    override val showChapterComments = MutableStateFlow(true)
    override val enableVolumeScroll = MutableStateFlow(true)
    override val volumeScrollLength = MutableStateFlow(100)
    override val keepScreenOn = MutableStateFlow(false)
    override val enableImmersiveMode = MutableStateFlow(false)
    override val showNavbarAtChapterEnd = MutableStateFlow(true)
    override val keepTextColor = MutableStateFlow(false)
    override val alternativeTextColors = MutableStateFlow(false)
    override val limitImageWidth = MutableStateFlow(true)
    override val fontPath = MutableStateFlow("")
    override val enableClusterPages = MutableStateFlow(false)
    override val enableDirectionalLinks = MutableStateFlow(true)
    override val isReaderModeButtonVisible = MutableStateFlow(true)
    override val dayModeBackgroundColor = MutableStateFlow(0xFFFFFFFF.toInt())
    override val nightModeBackgroundColor = MutableStateFlow(0xFF000000.toInt())
    override val dayModeTextColor = MutableStateFlow(0xFF000000.toInt())
    override val nightModeTextColor = MutableStateFlow(0xFFFFFFFF.toInt())
    override val enableAutoScroll = MutableStateFlow(false)
    override val autoScrollLength = MutableStateFlow(100)
    override val autoScrollInterval = MutableStateFlow(1000)
    //endregion
    
    //region General Settings
    override val isDarkTheme = MutableStateFlow(false)
    override val language = MutableStateFlow("System Default")
    override val javascriptDisabled = MutableStateFlow(false)
    override val loadLibraryScreen = MutableStateFlow(false)
    override val enableNotifications = MutableStateFlow(true)
    override val showChaptersLeftBadge = MutableStateFlow(false)
    override val isDeveloper = MutableStateFlow(false)
    //endregion
    
    //region TTS Settings
    override val readAloudNextChapter = MutableStateFlow(true)
    override val enableScrollingText = MutableStateFlow(true)
    //endregion
    
    //region Backup Settings
    override val showBackupHint = MutableStateFlow(true)
    override val showRestoreHint = MutableStateFlow(true)
    override val backupFrequency = MutableStateFlow(24)
    override val lastBackup = MutableStateFlow(0L)
    override val lastLocalBackupTimestamp = MutableStateFlow("Never")
    override val lastCloudBackupTimestamp = MutableStateFlow("Never")
    override val lastBackupSize = MutableStateFlow("N/A")
    override val gdBackupInterval = MutableStateFlow("Never")
    override val gdAccountEmail = MutableStateFlow("")
    override val gdInternetType = MutableStateFlow("wifi")
    //endregion
    
    //region Sync Settings
    private val syncSettings = mutableMapOf<String, MutableStateFlow<Boolean>>()
    
    override fun getSyncEnabled(serviceName: String): Flow<Boolean> {
        return syncSettings.getOrPut("sync_enable_$serviceName") { MutableStateFlow(false) }
    }
    
    override fun getSyncAddNovels(serviceName: String): Flow<Boolean> {
        return syncSettings.getOrPut("sync_add_novels_$serviceName") { MutableStateFlow(true) }
    }
    
    override fun getSyncDeleteNovels(serviceName: String): Flow<Boolean> {
        return syncSettings.getOrPut("sync_delete_novels_$serviceName") { MutableStateFlow(true) }
    }
    
    override fun getSyncBookmarks(serviceName: String): Flow<Boolean> {
        return syncSettings.getOrPut("sync_bookmarks_$serviceName") { MutableStateFlow(true) }
    }
    //endregion
    
    //region Write Operations
    override suspend fun updateBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        // No-op for fake implementation
    }
    
    override suspend fun updateInt(key: Preferences.Key<Int>, value: Int) {
        // No-op for fake implementation
    }
    
    override suspend fun updateLong(key: Preferences.Key<Long>, value: Long) {
        // No-op for fake implementation
    }
    
    override suspend fun updateString(key: Preferences.Key<String>, value: String) {
        // No-op for fake implementation
    }
    
    override suspend fun updateSyncSetting(keyName: String, value: Boolean) {
        // No-op for fake implementation
    }
    //endregion
}
