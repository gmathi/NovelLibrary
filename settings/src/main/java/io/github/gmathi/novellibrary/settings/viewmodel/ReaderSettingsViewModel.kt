package io.github.gmathi.novellibrary.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for reader settings screen.
 * 
 * Manages state for all reader-related settings including:
 * - Text size and font
 * - Theme and colors
 * - Scroll behavior
 * - Volume key navigation
 * - Auto-scroll settings
 * - TTS settings
 * 
 * This ViewModel follows MVVM architecture with unidirectional data flow.
 */
class ReaderSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {
    
    //region Text & Display Settings
    
    val textSize: StateFlow<Int> = repository.textSize
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 16
        )
    
    val fontPath: StateFlow<String> = repository.fontPath
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )
    
    val limitImageWidth: StateFlow<Boolean> = repository.limitImageWidth
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    //endregion
    
    //region Theme Settings
    
    val dayModeBackgroundColor: StateFlow<Int> = repository.dayModeBackgroundColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFFFFFFFF.toInt()
        )
    
    val nightModeBackgroundColor: StateFlow<Int> = repository.nightModeBackgroundColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFF000000.toInt()
        )
    
    val dayModeTextColor: StateFlow<Int> = repository.dayModeTextColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFF000000.toInt()
        )
    
    val nightModeTextColor: StateFlow<Int> = repository.nightModeTextColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0xFFFFFFFF.toInt()
        )
    
    val keepTextColor: StateFlow<Boolean> = repository.keepTextColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val alternativeTextColors: StateFlow<Boolean> = repository.alternativeTextColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    //endregion
    
    //region Scroll Behavior Settings
    
    val readerMode: StateFlow<Boolean> = repository.readerMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    val japSwipe: StateFlow<Boolean> = repository.japSwipe
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val showReaderScroll: StateFlow<Boolean> = repository.showReaderScroll
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    val enableVolumeScroll: StateFlow<Boolean> = repository.enableVolumeScroll
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val volumeScrollLength: StateFlow<Int> = repository.volumeScrollLength
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 100
        )
    
    val keepScreenOn: StateFlow<Boolean> = repository.keepScreenOn
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val enableImmersiveMode: StateFlow<Boolean> = repository.enableImmersiveMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val showNavbarAtChapterEnd: StateFlow<Boolean> = repository.showNavbarAtChapterEnd
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    val enableAutoScroll: StateFlow<Boolean> = repository.enableAutoScroll
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val autoScrollLength: StateFlow<Int> = repository.autoScrollLength
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 100
        )
    
    val autoScrollInterval: StateFlow<Int> = repository.autoScrollInterval
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 1000
        )
    
    //endregion
    
    //region Advanced Reader Settings
    
    val showChapterComments: StateFlow<Boolean> = repository.showChapterComments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    val enableClusterPages: StateFlow<Boolean> = repository.enableClusterPages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    
    val enableDirectionalLinks: StateFlow<Boolean> = repository.enableDirectionalLinks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    val isReaderModeButtonVisible: StateFlow<Boolean> = repository.isReaderModeButtonVisible
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    //endregion
    
    //region Update Functions
    
    fun setTextSize(size: Int) {
        viewModelScope.launch {
            repository.setTextSize(size)
        }
    }
    
    fun setFontPath(path: String) {
        viewModelScope.launch {
            repository.setFontPath(path)
        }
    }
    
    fun setLimitImageWidth(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLimitImageWidth(enabled)
        }
    }
    
    fun setDayModeBackgroundColor(color: Int) {
        viewModelScope.launch {
            repository.setDayModeBackgroundColor(color)
        }
    }
    
    fun setNightModeBackgroundColor(color: Int) {
        viewModelScope.launch {
            repository.setNightModeBackgroundColor(color)
        }
    }
    
    fun setDayModeTextColor(color: Int) {
        viewModelScope.launch {
            repository.setDayModeTextColor(color)
        }
    }
    
    fun setNightModeTextColor(color: Int) {
        viewModelScope.launch {
            repository.setNightModeTextColor(color)
        }
    }
    
    fun setKeepTextColor(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeepTextColor(enabled)
        }
    }
    
    fun setAlternativeTextColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAlternativeTextColors(enabled)
        }
    }
    
    fun setReaderMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReaderMode(enabled)
        }
    }
    
    fun setJapSwipe(enabled: Boolean) {
        viewModelScope.launch {
            repository.setJapSwipe(enabled)
        }
    }
    
    fun setShowReaderScroll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowReaderScroll(enabled)
        }
    }
    
    fun setEnableVolumeScroll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableVolumeScroll(enabled)
        }
    }
    
    fun setVolumeScrollLength(length: Int) {
        viewModelScope.launch {
            repository.setVolumeScrollLength(length)
        }
    }
    
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKeepScreenOn(enabled)
        }
    }
    
    fun setEnableImmersiveMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableImmersiveMode(enabled)
        }
    }
    
    fun setShowNavbarAtChapterEnd(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowNavbarAtChapterEnd(enabled)
        }
    }
    
    fun setEnableAutoScroll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableAutoScroll(enabled)
        }
    }
    
    fun setAutoScrollLength(length: Int) {
        viewModelScope.launch {
            repository.setAutoScrollLength(length)
        }
    }
    
    fun setAutoScrollInterval(interval: Int) {
        viewModelScope.launch {
            repository.setAutoScrollInterval(interval)
        }
    }
    
    fun setShowChapterComments(enabled: Boolean) {
        viewModelScope.launch {
            repository.setShowChapterComments(enabled)
        }
    }
    
    fun setEnableClusterPages(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableClusterPages(enabled)
        }
    }
    
    fun setEnableDirectionalLinks(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableDirectionalLinks(enabled)
        }
    }
    
    fun setIsReaderModeButtonVisible(enabled: Boolean) {
        viewModelScope.launch {
            repository.setIsReaderModeButtonVisible(enabled)
        }
    }
    
    //endregion
}
