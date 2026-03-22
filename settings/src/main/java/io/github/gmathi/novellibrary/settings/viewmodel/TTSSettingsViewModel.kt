package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for TTS (Text-to-Speech) settings screen.
 * 
 * Manages state for TTS-related settings including:
 * - Read aloud next chapter automatically
 * - Enable scrolling text during TTS
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class TTSSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    /**
     * Read aloud next chapter automatically.
     */
    val readAloudNextChapter: StateFlow<Boolean> = repository.readAloudNextChapter
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    /**
     * Enable scrolling text during TTS.
     */
    val enableScrollingText: StateFlow<Boolean> = repository.enableScrollingText
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    //region Update Functions
    
    fun setReadAloudNextChapter(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReadAloudNextChapter(enabled)
        }
    }
    
    fun setEnableScrollingText(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableScrollingText(enabled)
        }
    }
    
    //endregion
}
