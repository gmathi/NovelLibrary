package io.github.gmathi.novellibrary.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [ReaderViewModel]'s per-novel display-setting behavior: dark theme, font,
 * text size, the four reader-mode colors, limit image width, keep text color, alternative
 * text colors, and merge (cluster) pages.
 *
 * Follows the same approach as [ReaderViewModelReaderModeTest]: the real [ReaderViewModel]
 * depends on Injekt-injected `DataCenter`, which requires a real Android `Context` and isn't
 * available on the plain JVM unit test classpath. This test defines a test-local mirror
 * ([FakeReaderViewModel]) of `initialize`/setter logic, wired to a minimal in-memory
 * [FakeDataCenter] instead of `injectLazy()`.
 */
class ReaderViewModelDisplaySettingsTest {

    /** Minimal in-memory fake of the `DataCenter` members [FakeReaderViewModel] touches. */
    private class FakeDataCenter {
        var isDarkTheme: Boolean = true
        var fontPath: String = "/android_asset/fonts/default.ttf"
        var textSize: Int = 0
        var dayModeBackgroundColor: Int = -0x1
        var dayModeTextColor: Int = -0x1000000
        var nightModeBackgroundColor: Int = -0x1000000
        var nightModeTextColor: Int = -0x1
        var limitImageWidth: Boolean = false
        var keepTextColor: Boolean = false
        var alternativeTextColors: Boolean = false
        var enableClusterPages: Boolean = false

        private val perNovelIsDarkTheme = mutableMapOf<Long, Boolean>()
        private val perNovelFontPath = mutableMapOf<Long, String>()
        private val perNovelTextSize = mutableMapOf<Long, Int>()
        private val perNovelDayBackgroundColor = mutableMapOf<Long, Int>()
        private val perNovelDayTextColor = mutableMapOf<Long, Int>()
        private val perNovelNightBackgroundColor = mutableMapOf<Long, Int>()
        private val perNovelNightTextColor = mutableMapOf<Long, Int>()
        private val perNovelLimitImageWidth = mutableMapOf<Long, Boolean>()
        private val perNovelKeepTextColor = mutableMapOf<Long, Boolean>()
        private val perNovelAlternativeTextColors = mutableMapOf<Long, Boolean>()
        private val perNovelEnableClusterPages = mutableMapOf<Long, Boolean>()

        fun getIsDarkThemeForNovel(novelId: Long): Boolean = perNovelIsDarkTheme[novelId] ?: isDarkTheme
        fun setIsDarkThemeForNovel(novelId: Long, value: Boolean) { perNovelIsDarkTheme[novelId] = value }

        fun getFontPathForNovel(novelId: Long): String = perNovelFontPath[novelId] ?: fontPath
        fun setFontPathForNovel(novelId: Long, value: String) { perNovelFontPath[novelId] = value }

        fun getTextSizeForNovel(novelId: Long): Int = perNovelTextSize[novelId] ?: textSize
        fun setTextSizeForNovel(novelId: Long, value: Int) { perNovelTextSize[novelId] = value }

        fun getDayBackgroundColorForNovel(novelId: Long): Int = perNovelDayBackgroundColor[novelId] ?: dayModeBackgroundColor
        fun setDayBackgroundColorForNovel(novelId: Long, value: Int) { perNovelDayBackgroundColor[novelId] = value }

        fun getDayTextColorForNovel(novelId: Long): Int = perNovelDayTextColor[novelId] ?: dayModeTextColor
        fun setDayTextColorForNovel(novelId: Long, value: Int) { perNovelDayTextColor[novelId] = value }

        fun getNightBackgroundColorForNovel(novelId: Long): Int = perNovelNightBackgroundColor[novelId] ?: nightModeBackgroundColor
        fun setNightBackgroundColorForNovel(novelId: Long, value: Int) { perNovelNightBackgroundColor[novelId] = value }

        fun getNightTextColorForNovel(novelId: Long): Int = perNovelNightTextColor[novelId] ?: nightModeTextColor
        fun setNightTextColorForNovel(novelId: Long, value: Int) { perNovelNightTextColor[novelId] = value }

        fun getLimitImageWidthForNovel(novelId: Long): Boolean = perNovelLimitImageWidth[novelId] ?: limitImageWidth
        fun setLimitImageWidthForNovel(novelId: Long, value: Boolean) { perNovelLimitImageWidth[novelId] = value }

        fun getKeepTextColorForNovel(novelId: Long): Boolean = perNovelKeepTextColor[novelId] ?: keepTextColor
        fun setKeepTextColorForNovel(novelId: Long, value: Boolean) { perNovelKeepTextColor[novelId] = value }

        fun getAlternativeTextColorsForNovel(novelId: Long): Boolean = perNovelAlternativeTextColors[novelId] ?: alternativeTextColors
        fun setAlternativeTextColorsForNovel(novelId: Long, value: Boolean) { perNovelAlternativeTextColors[novelId] = value }

        fun getEnableClusterPagesForNovel(novelId: Long): Boolean = perNovelEnableClusterPages[novelId] ?: enableClusterPages
        fun setEnableClusterPagesForNovel(novelId: Long, value: Boolean) { perNovelEnableClusterPages[novelId] = value }
    }

    /** Mirrors the relevant subset of [ReaderViewModel]'s `initialize`/setter implementation. */
    private class FakeReaderViewModel(private val dataCenter: FakeDataCenter) {

        var uiState: ReaderUiState = ReaderUiState()
            private set

        private var novelId: Long = -1L

        fun initialize(novelId: Long) {
            this.novelId = novelId
            uiState = uiState.copy(
                isDarkTheme = dataCenter.getIsDarkThemeForNovel(novelId),
                textSize = dataCenter.getTextSizeForNovel(novelId),
                fontPath = dataCenter.getFontPathForNovel(novelId),
                keepTextColor = dataCenter.getKeepTextColorForNovel(novelId),
                alternativeTextColors = dataCenter.getAlternativeTextColorsForNovel(novelId),
                limitImageWidth = dataCenter.getLimitImageWidthForNovel(novelId),
                enableClusterPages = dataCenter.getEnableClusterPagesForNovel(novelId),
                dayBackgroundColor = dataCenter.getDayBackgroundColorForNovel(novelId),
                dayTextColor = dataCenter.getDayTextColorForNovel(novelId),
                nightBackgroundColor = dataCenter.getNightBackgroundColorForNovel(novelId),
                nightTextColor = dataCenter.getNightTextColorForNovel(novelId),
            )
        }

        fun setTextSize(size: Int) {
            dataCenter.setTextSizeForNovel(novelId, size)
            uiState = uiState.copy(textSize = size)
        }

        fun toggleDarkTheme() {
            val newValue = !dataCenter.getIsDarkThemeForNovel(novelId)
            dataCenter.setIsDarkThemeForNovel(novelId, newValue)
            uiState = uiState.copy(isDarkTheme = newValue)
        }

        fun setDayBackgroundColor(color: Int) {
            dataCenter.setDayBackgroundColorForNovel(novelId, color)
            uiState = uiState.copy(dayBackgroundColor = color)
        }

        fun setNightBackgroundColor(color: Int) {
            dataCenter.setNightBackgroundColorForNovel(novelId, color)
            uiState = uiState.copy(nightBackgroundColor = color)
        }

        fun setLimitImageWidth(enabled: Boolean) {
            dataCenter.setLimitImageWidthForNovel(novelId, enabled)
            uiState = uiState.copy(limitImageWidth = enabled)
        }

        fun setEnableClusterPages(enabled: Boolean) {
            dataCenter.setEnableClusterPagesForNovel(novelId, enabled)
            uiState = uiState.copy(enableClusterPages = enabled)
        }

        fun onFontChanged(path: String) {
            dataCenter.setFontPathForNovel(novelId, path)
            uiState = uiState.copy(fontPath = path)
        }
    }

    @Test
    fun `initialize seeds isDarkTheme from the per-novel accessor`() {
        val dataCenter = FakeDataCenter()
        dataCenter.isDarkTheme = false
        dataCenter.setIsDarkThemeForNovel(42L, true)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(42L)

        assertEquals(true, viewModel.uiState.isDarkTheme)
    }

    @Test
    fun `initialize seeds textSize font colors and flags from their per-novel accessors`() {
        val dataCenter = FakeDataCenter()
        dataCenter.setTextSizeForNovel(1L, 8)
        dataCenter.setFontPathForNovel(1L, "/android_asset/fonts/mono.ttf")
        dataCenter.setDayBackgroundColorForNovel(1L, -0x111112)
        dataCenter.setNightTextColorForNovel(1L, -0x765434)
        dataCenter.setLimitImageWidthForNovel(1L, true)
        dataCenter.setEnableClusterPagesForNovel(1L, true)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(1L)

        assertEquals(8, viewModel.uiState.textSize)
        assertEquals("/android_asset/fonts/mono.ttf", viewModel.uiState.fontPath)
        assertEquals(-0x111112, viewModel.uiState.dayBackgroundColor)
        assertEquals(-0x765434, viewModel.uiState.nightTextColor)
        assertEquals(true, viewModel.uiState.limitImageWidth)
        assertEquals(true, viewModel.uiState.enableClusterPages)
    }

    @Test
    fun `setTextSize writes the per-novel accessor and not the app-wide property`() {
        val dataCenter = FakeDataCenter()
        dataCenter.textSize = 0
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(99L)

        viewModel.setTextSize(10)

        assertEquals(10, dataCenter.getTextSizeForNovel(99L))
        assertEquals(0, dataCenter.textSize)
        assertEquals(0, dataCenter.getTextSizeForNovel(100L))
    }

    @Test
    fun `toggleDarkTheme writes the per-novel accessor and not the app-wide property`() {
        val dataCenter = FakeDataCenter()
        dataCenter.isDarkTheme = true
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(5L)

        viewModel.toggleDarkTheme()

        assertEquals(false, dataCenter.getIsDarkThemeForNovel(5L))
        assertEquals(true, dataCenter.isDarkTheme)
        assertEquals(true, dataCenter.getIsDarkThemeForNovel(6L))
    }

    @Test
    fun `setDayBackgroundColor and setNightBackgroundColor write independent per-novel keys`() {
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setDayBackgroundColor(-0x111112)
        viewModel.setNightBackgroundColor(-0x654322)

        assertEquals(-0x111112, dataCenter.getDayBackgroundColorForNovel(1L))
        assertEquals(-0x654322, dataCenter.getNightBackgroundColorForNovel(1L))
        assertEquals(-0x1, dataCenter.dayModeBackgroundColor)
        assertEquals(-0x1000000, dataCenter.nightModeBackgroundColor)
    }

    @Test
    fun `setLimitImageWidth and setEnableClusterPages write per-novel and do not affect other novels`() {
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setLimitImageWidth(true)
        viewModel.setEnableClusterPages(true)

        assertEquals(true, dataCenter.getLimitImageWidthForNovel(1L))
        assertEquals(true, dataCenter.getEnableClusterPagesForNovel(1L))
        assertEquals(false, dataCenter.getLimitImageWidthForNovel(2L))
        assertEquals(false, dataCenter.getEnableClusterPagesForNovel(2L))
    }

    @Test
    fun `onFontChanged writes the per-novel font path and not the app-wide default`() {
        val dataCenter = FakeDataCenter()
        dataCenter.fontPath = "/android_asset/fonts/default.ttf"
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(7L)

        viewModel.onFontChanged("/android_asset/fonts/serif.ttf")

        assertEquals("/android_asset/fonts/serif.ttf", dataCenter.getFontPathForNovel(7L))
        assertEquals("/android_asset/fonts/default.ttf", dataCenter.fontPath)
        assertEquals("/android_asset/fonts/default.ttf", dataCenter.getFontPathForNovel(8L))
    }
}
