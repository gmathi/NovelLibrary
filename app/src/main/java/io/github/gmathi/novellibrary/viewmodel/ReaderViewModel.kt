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

    private var novelId: Long = -1L

    fun initialize(novelId: Long) {
        this.novelId = novelId
        _uiState.update {
            it.copy(
                isReaderMode = dataCenter.getReaderModeForNovel(novelId),
                isDarkTheme = dataCenter.getIsDarkThemeForNovel(novelId),
                isJavascriptEnabled = !dataCenter.javascriptDisabled || dataCenter.getReaderModeForNovel(novelId),
                textSize = dataCenter.getTextSizeForNovel(novelId),
                fontPath = dataCenter.getFontPathForNovel(novelId),
                fontName = extractFontName(dataCenter.getFontPathForNovel(novelId)),
                keepScreenOn = dataCenter.keepScreenOn,
                immersiveMode = dataCenter.enableImmersiveMode,
                japSwipe = dataCenter.japSwipe,
                showChapterComments = dataCenter.showChapterComments,
                enableVolumeScroll = dataCenter.enableVolumeScroll,
                showReaderScroll = dataCenter.showReaderScroll,
                keepTextColor = dataCenter.getKeepTextColorForNovel(novelId),
                alternativeTextColors = dataCenter.getAlternativeTextColorsForNovel(novelId),
                limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
                enableClusterPages = dataCenter.getEnableClusterPagesForNovel(novelId),
                enableDirectionalLinks = dataCenter.enableDirectionalLinks,
                isReaderModeButtonVisible = dataCenter.isReaderModeButtonVisible,
                showNavbarAtChapterEnd = dataCenter.showNavbarAtChapterEnd,
                dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
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
        dataCenter.setReaderModeForNovel(novelId, enabled)
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
        if (!enabled) dataCenter.setReaderModeForNovel(novelId, false)
        _uiState.update {
            it.copy(
                isJavascriptEnabled = enabled,
                isReaderMode = if (!enabled) false else it.isReaderMode
            )
        }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
    }

    fun setTextSize(size: Int) {
        dataCenter.setTextSizeForNovel(novelId, size)
        _uiState.update { it.copy(textSize = size) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.TEXT_SIZE))
    }

    fun toggleDarkTheme() {
        val newValue = !dataCenter.getIsDarkThemeForNovel(novelId)
        dataCenter.setIsDarkThemeForNovel(novelId, newValue)
        _uiState.update { it.copy(isDarkTheme = newValue) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setDayBackgroundColor(color: Int) {
        dataCenter.setDayBackgroundColorForNovel(novelId, color)
        _uiState.update { it.copy(dayBackgroundColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setDayTextColor(color: Int) {
        dataCenter.setDayTextColorForNovel(novelId, color)
        _uiState.update { it.copy(dayTextColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setNightBackgroundColor(color: Int) {
        dataCenter.setNightBackgroundColorForNovel(novelId, color)
        _uiState.update { it.copy(nightBackgroundColor = color) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setNightTextColor(color: Int) {
        dataCenter.setNightTextColorForNovel(novelId, color)
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
        dataCenter.setLimitImageWidthForNovel(novelId, enabled)
        _uiState.update { it.copy(limitImageWidth = enabled) }
        EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
    }

    fun setKeepTextColor(enabled: Boolean) {
        dataCenter.setKeepTextColorForNovel(novelId, enabled)
        _uiState.update { it.copy(keepTextColor = enabled) }
    }

    fun setAlternativeTextColors(enabled: Boolean) {
        dataCenter.setAlternativeTextColorsForNovel(novelId, enabled)
        _uiState.update { it.copy(alternativeTextColors = enabled) }
    }

    fun setEnableClusterPages(enabled: Boolean) {
        dataCenter.setEnableClusterPagesForNovel(novelId, enabled)
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
        dataCenter.setFontPathForNovel(novelId, path)
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
