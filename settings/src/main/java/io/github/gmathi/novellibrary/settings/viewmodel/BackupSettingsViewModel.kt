package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for backup settings screen.
 * 
 * Manages state for backup-related settings including:
 * - Local backup settings
 * - Google Drive backup settings
 * - Backup frequency and timestamps
 * - Backup hints
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class BackupSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    /**
     * Show backup hint.
     */
    val showBackupHint: StateFlow<Boolean> = repository.showBackupHint
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    /**
     * Show restore hint.
     */
    val showRestoreHint: StateFlow<Boolean> = repository.showRestoreHint
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    /**
     * Backup frequency in hours.
     */
    val backupFrequency: StateFlow<Int> = repository.backupFrequency
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 24
        )
    
    /**
     * Last backup timestamp in milliseconds.
     */
    val lastBackup: StateFlow<Long> = repository.lastBackup
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )
    
    /**
     * Last local backup timestamp string.
     */
    val lastLocalBackupTimestamp: StateFlow<String> = repository.lastLocalBackupTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    /**
     * Last cloud backup timestamp string.
     */
    val lastCloudBackupTimestamp: StateFlow<String> = repository.lastCloudBackupTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    /**
     * Last backup size string.
     */
    val lastBackupSize: StateFlow<String> = repository.lastBackupSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    /**
     * Google Drive backup interval.
     */
    val gdBackupInterval: StateFlow<String> = repository.gdBackupInterval
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "daily"
        )
    
    /**
     * Google Drive account email.
     */
    val gdAccountEmail: StateFlow<String> = repository.gdAccountEmail
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    /**
     * Google Drive internet type preference.
     */
    val gdInternetType: StateFlow<String> = repository.gdInternetType
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "wifi"
        )
    
    //region Update Functions
    
    fun setShowBackupHint(show: Boolean) {
        viewModelScope.launch {
            repository.setShowBackupHint(show)
        }
    }
    
    fun setShowRestoreHint(show: Boolean) {
        viewModelScope.launch {
            repository.setShowRestoreHint(show)
        }
    }
    
    fun setBackupFrequency(hours: Int) {
        viewModelScope.launch {
            repository.setBackupFrequency(hours)
        }
    }
    
    fun setLastBackup(timestamp: Long) {
        viewModelScope.launch {
            repository.setLastBackup(timestamp)
        }
    }
    
    fun setLastLocalBackupTimestamp(timestamp: String) {
        viewModelScope.launch {
            repository.setLastLocalBackupTimestamp(timestamp)
        }
    }
    
    fun setLastCloudBackupTimestamp(timestamp: String) {
        viewModelScope.launch {
            repository.setLastCloudBackupTimestamp(timestamp)
        }
    }
    
    fun setLastBackupSize(size: String) {
        viewModelScope.launch {
            repository.setLastBackupSize(size)
        }
    }
    
    fun setGdBackupInterval(interval: String) {
        viewModelScope.launch {
            repository.setGdBackupInterval(interval)
        }
    }
    
    fun setGdAccountEmail(email: String) {
        viewModelScope.launch {
            repository.setGdAccountEmail(email)
        }
    }
    
    fun setGdInternetType(type: String) {
        viewModelScope.launch {
            repository.setGdInternetType(type)
        }
    }
    
    //endregion
}
