package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for sync settings screen.
 * 
 * Manages state for sync-related settings including:
 * - Sync enabled status for different services
 * - Sync add novels setting
 * - Sync delete novels setting
 * - Sync bookmarks setting
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class SyncSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    /**
     * Get sync enabled status for a specific service.
     * 
     * @param serviceName The name of the sync service (e.g., "google", "dropbox")
     * @return StateFlow of the sync enabled status
     */
    fun getSyncEnabled(serviceName: String): StateFlow<Boolean> {
        return repository.getSyncEnabled(serviceName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }
    
    /**
     * Get sync add novels setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @return StateFlow of the sync add novels setting
     */
    fun getSyncAddNovels(serviceName: String): StateFlow<Boolean> {
        return repository.getSyncAddNovels(serviceName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )
    }
    
    /**
     * Get sync delete novels setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @return StateFlow of the sync delete novels setting
     */
    fun getSyncDeleteNovels(serviceName: String): StateFlow<Boolean> {
        return repository.getSyncDeleteNovels(serviceName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false
            )
    }
    
    /**
     * Get sync bookmarks setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @return StateFlow of the sync bookmarks setting
     */
    fun getSyncBookmarks(serviceName: String): StateFlow<Boolean> {
        return repository.getSyncBookmarks(serviceName)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )
    }
    
    //region Update Functions
    
    /**
     * Set sync enabled status for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @param enabled Whether sync is enabled
     */
    fun setSyncEnabled(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncEnabled(serviceName, enabled)
        }
    }
    
    /**
     * Set sync add novels setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @param enabled Whether to sync adding novels
     */
    fun setSyncAddNovels(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncAddNovels(serviceName, enabled)
        }
    }
    
    /**
     * Set sync delete novels setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @param enabled Whether to sync deleting novels
     */
    fun setSyncDeleteNovels(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncDeleteNovels(serviceName, enabled)
        }
    }
    
    /**
     * Set sync bookmarks setting for a specific service.
     * 
     * @param serviceName The name of the sync service
     * @param enabled Whether to sync bookmarks
     */
    fun setSyncBookmarks(serviceName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setSyncBookmarks(serviceName, enabled)
        }
    }
    
    //endregion
}
