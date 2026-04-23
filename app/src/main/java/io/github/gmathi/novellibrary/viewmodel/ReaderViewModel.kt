package io.github.gmathi.novellibrary.viewmodel

import android.graphics.Color
import androidx.lifecycle.ViewModel
import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import io.github.gmathi.novellibrary.model.preference.DataCenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.greenrobot.eventbus.EventBus
import uy.kohesive.injekt.injectLazy

data class ReaderUiState(
    val isReaderMode: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isJavascriptEnabled: Boolean = true,
    val textSize: Int = 0,
    val fontName: String = "Default",
    val fontPath: String = "",
    val keepScreenOn: Boolean = true,
    val immersiveMode: Boolean = true,
    val japSwipe: Boolean = true,
    val showChapterComments: Boolean = false,
    val enableVolumeScroll: Boolean = true,
    val showReaderScroll: Boolean = true,
    val keepTextColor: Boolean = false,
    val alternativeTextColors: Boolean = false,
    val limitImageWidth: Boolean = false,
    val enableClusterPages: Boolean = false,
    val enableDirectionalLinks: Boolean = false,
    val isReaderModeButtonVisible: Boolean = false,
    val showNavbarAtChapterEnd: Boolean = true,
    val dayBackgroundColor: Int = Color.WHITE,
    val dayTextColor: Int = Color.BLACK,
    val nightBackgroundColor: Int = Color.BLACK,
    val nightTextColor: Int = Color.WHITE,
    // Bottom bar state
    val isSettingsPanelVisible: Boolean = false,
    val currentChapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val chapterTitle: String = "",
)

class ReaderViewModel : ViewModel() {

    private val dataCenter: DataCenter by injectLazy()

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    fun initialize() {
        _uiState.update {
            it.copy(
                isReaderMode = dataCenter.readerMode,
                isDarkTheme = dataCenter.isDarkTheme,
                isJavascriptEnabled = !dataCenter.javascriptDisabled,
                textSize = dataCenter.textSize,
                fontPath = dataCenter.fontPath,
                fontName = extractFontName(dataCenter.fontPath),
                keepScreenOn = dataCenter.keepScreenOn,
                immersiveMode = dataCenter.enableImmersiveMode,
                japSwipe = dataCenter.japSwipe,
                showChapterComments = dataCenter.showChapterComments,
                enableVolumeScroll = dataCenter.enableVolumeScroll,
                showReaderScroll = dataCenter.showReaderScroll,
                keepTextColor = dataCenter.keepTextColor,
                alternativeTextColors = dataCenter.alternativeTextColors,
                limitImageWidth = dataCenter.limitImageWidth,
                enableClusterPages = dataCenter.enableClusterPages,
                enableDirectionalLinks = dataCenter.enableDirectionalLinks,
                isReaderModeButtonVisible = dataCenter.isReaderModeButtonVisible,
                showNavbarAtChapterEnd = dataCenter.showNavbarAtChapterEnd,
                dayBackgroundColor = dataCenter.dayModeBackgroundColor,
                dayTextColor = dataCenter.dayModeTextColor,
                nightBackgroundColor = dataCenter.nightModeBackgroundColor,
                nightTextColor = dataCenter.nightModeTextColor,
            )
        }
    }

    fun toggleSettingsPanel() {
        _uiState.update { it.copy(isSettingsPanelVisible = !it.isSettingsPanelVisible) }
    }

    fun hideSettingsPanel() {
        _uiState.update { it.copy(isSettingsPanelVisible = false) }
    }

    fun setReaderMode(enabled: Boolean) {
        dataCenter.readerMode = enabled
        if (enabled) dataCenter.javascriptDisabled = true
        _uiState.update {
            it.copy(
                isReaderMode = enabled,
                isJavascriptEnabled = if (enabled) false else it.isJavascriptEnabled
            )
        }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
    }

    fun setJavascriptEnabled(enabled: Boolean) {
        dataCenter.javascriptDisabled = !enabled
        if (!enabled) dataCenter.readerMode = false
        _uiState.update {
            it.copy(
                isJavascriptEnabled = enabled,
                isReaderMode = if (!enabled) false else it.isReaderMode
            )
        }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
    }

    fun setTextSize(size: Int) {
        dataCenter.textSize = size
        _uiState.update { it.copy(textSize = size) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.TEXT_SIZE))
    }

    fun toggleDarkTheme() {
        val newValue = !dataCenter.isDarkTheme
        dataCenter.isDarkTheme = newValue
        _uiState.update { it.copy(isDarkTheme = newValue) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setDayBackgroundColor(color: Int) {
        dataCenter.dayModeBackgroundColor = color
        _uiState.update { it.copy(dayBackgroundColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setDayTextColor(color: Int) {
        dataCenter.dayModeTextColor = color
        _uiState.update { it.copy(dayTextColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setNightBackgroundColor(color: Int) {
        dataCenter.nightModeBackgroundColor = color
        _uiState.update { it.copy(nightBackgroundColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setNightTextColor(color: Int) {
        dataCenter.nightModeTextColor = color
        _uiState.update { it.copy(nightTextColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setKeepScreenOn(enabled: Boolean) {
        dataCenter.keepScreenOn = enabled
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun setImmersiveMode(enabled: Boolean) {
        dataCenter.enableImmersiveMode = enabled
        _uiState.update { it.copy(immersiveMode = enabled) }
    }

    fun setLimitImageWidth(enabled: Boolean) {
        dataCenter.limitImageWidth = enabled
        _uiState.update { it.copy(limitImageWidth = enabled) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setKeepTextColor(enabled: Boolean) {
        dataCenter.keepTextColor = enabled
        _uiState.update { it.copy(keepTextColor = enabled) }
    }

    fun setAlternativeTextColors(enabled: Boolean) {
        dataCenter.alternativeTextColors = enabled
        _uiState.update { it.copy(alternativeTextColors = enabled) }
    }

    fun setEnableClusterPages(enabled: Boolean) {
        dataCenter.enableClusterPages = enabled
        _uiState.update { it.copy(enableClusterPages = enabled) }
    }

    fun setShowNavbarAtChapterEnd(enabled: Boolean) {
        dataCenter.showNavbarAtChapterEnd = enabled
        _uiState.update { it.copy(showNavbarAtChapterEnd = enabled) }
    }

    fun updateChapterInfo(index: Int, total: Int, title: String) {
        _uiState.update {
            it.copy(currentChapterIndex = index, totalChapters = total, chapterTitle = title)
        }
    }

    fun onFontChanged(path: String) {
        dataCenter.fontPath = path
        _uiState.update { it.copy(fontPath = path, fontName = extractFontName(path)) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
    }

    private fun extractFontName(path: String): String {
        if (path.isBlank()) return "Default"
        return path.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')
            .ifBlank { "Default" }
    }
}
