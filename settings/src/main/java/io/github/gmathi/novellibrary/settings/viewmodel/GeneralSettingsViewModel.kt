package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for general settings screen.
 * 
 * Manages state for general app settings including:
 * - App theme (light/dark)
 * - Language selection
 * - Notification preferences
 * - JavaScript settings
 * - Library screen preferences
 * - Developer mode
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class GeneralSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    /**
     * Dark theme enabled/disabled.
     */
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * App language setting.
     */
    val language: StateFlow<String> = repository.language
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "en"
        )
    
    /**
     * JavaScript disabled setting.
     */
    val javascriptDisabled: StateFlow<Boolean> = repository.javascriptDisabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Load library screen on startup.
     */
    val loadLibraryScreen: StateFlow<Boolean> = repository.loadLibraryScreen
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Enable notifications.
     */
    val enableNotifications: StateFlow<Boolean> = repository.enableNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    /**
     * Show chapters left badge.
     */
    val showChaptersLeftBadge: StateFlow<Boolean> = repository.showChaptersLeftBadge
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    /**
     * Developer mode enabled/disabled.
     */
    val isDeveloper: StateFlow<Boolean> = repository.isDeveloper
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    //region Update Functions
    
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsDarkTheme(enabled)
        }
    }
    
    fun setLanguage(language: String) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }
    
    fun setJavascriptDisabled(disabled: Boolean) {
        viewModelScope.launch {
            repository.setJavascriptDisabled(disabled)
        }
    }
    
    fun setLoadLibraryScreen(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLoadLibraryScreen(enabled)
        }
    }
    
    fun setEnableNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableNotifications(enabled)
        }
    }
    
    fun setShowChaptersLeftBadge(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowChaptersLeftBadge(enabled)
        }
    }
    
    fun setIsDeveloper(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsDeveloper(enabled)
        }
    }
    
    //endregion
}
