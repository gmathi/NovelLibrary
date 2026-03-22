package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the main settings screen.
 *
 * Manages the state for the main settings screen which displays the 5 main
 * settings categories: Reader, Backup & Sync, General, Advanced, and About.
 *
 * This ViewModel follows MVVM architecture with unidirectional data flow:
 * - Repository → ViewModel → UI
 * - UI events → ViewModel → Repository
 *
 * State is exposed as StateFlow for Compose UI observation.
 */
class MainSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {

    var count: Int = 0

    // Channel for one-time events like showing toasts
    private val _toastMessage = Channel<String>(Channel.BUFFERED)
    val toastMessage = _toastMessage.receiveAsFlow()

    /**
     * Dark theme setting - used to determine the app theme.
     */
    val isDarkTheme: StateFlow<Boolean> = repository.isDarkTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Developer mode setting - used to show/hide advanced options.
     */
    val isDeveloper: StateFlow<Boolean> = repository.isDeveloper
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Update the dark theme setting.
     */
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsDarkTheme(enabled)
        }
    }

    fun setDeveloper() {
        if (isDeveloper.value || count < 21) {
            count++; return
        }
        viewModelScope.launch {
            repository.setIsDeveloper(true)
            _toastMessage.send("Developer mode enabled!")
        }
    }


}
