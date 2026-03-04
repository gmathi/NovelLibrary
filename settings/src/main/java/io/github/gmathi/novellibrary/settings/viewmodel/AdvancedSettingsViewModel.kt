package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for advanced settings screen.
 * 
 * Manages state for advanced/technical settings including:
 * - Network settings (JavaScript, Cloudflare bypass)
 * - Cache management
 * - Debug settings (developer mode, logging)
 * - Data management (migration tools, reset settings)
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class AdvancedSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    //region Network Settings
    
    /**
     * JavaScript disabled setting.
     */
    val javascriptDisabled: StateFlow<Boolean> = repository.javascriptDisabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    //endregion
    
    //region Debug Settings
    
    /**
     * Developer mode enabled/disabled.
     */
    val isDeveloper: StateFlow<Boolean> = repository.isDeveloper
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    //endregion
    
    //region Update Functions
    
    fun setJavascriptDisabled(disabled: Boolean) {
        viewModelScope.launch {
            repository.setJavascriptDisabled(disabled)
        }
    }
    
    fun setIsDeveloper(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsDeveloper(enabled)
        }
    }
    
    //endregion
}
