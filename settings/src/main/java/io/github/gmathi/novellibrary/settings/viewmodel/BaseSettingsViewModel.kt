package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Base ViewModel for settings screens.
 * 
 * Provides common state management patterns used across all settings ViewModels:
 * - Converting Flow to StateFlow with proper lifecycle handling
 * - Launching coroutines in viewModelScope for updates
 * - Standard sharing configuration for StateFlow
 * 
 * All settings ViewModels should extend this base class to ensure consistent
 * state management patterns.
 */
abstract class BaseSettingsViewModel(
    protected val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    /**
     * Standard sharing configuration for StateFlow.
     * 
     * - WhileSubscribed(5000): Keep upstream flow active for 5 seconds after
     *   last subscriber unsubscribes, allowing for quick resubscription without
     *   restarting the flow
     * - This is the recommended configuration for UI state in Android
     */
    protected val sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000)
    
    /**
     * Converts a Flow to StateFlow with standard configuration.
     * 
     * @param flow Source flow from repository
     * @param initialValue Initial value for the StateFlow
     * @return StateFlow that can be collected by UI
     */
    protected fun <T> Flow<T>.asStateFlow(initialValue: T): StateFlow<T> {
        return stateIn(
            scope = viewModelScope,
            started = sharingStarted,
            initialValue = initialValue
        )
    }
    
    /**
     * Launches a coroutine to update a setting value.
     * 
     * @param block Suspend function to execute (typically a repository update)
     */
    protected fun updateSetting(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
    
    /**
     * Launches a coroutine to update a setting value with validation.
     * 
     * @param value Value to validate and update
     * @param validator Function to validate the value
     * @param updater Suspend function to update the repository
     */
    protected fun <T> updateSettingWithValidation(
        value: T,
        validator: (T) -> T,
        updater: suspend (T) -> Unit
    ) {
        viewModelScope.launch {
            val validatedValue = validator(value)
            updater(validatedValue)
        }
    }
}
