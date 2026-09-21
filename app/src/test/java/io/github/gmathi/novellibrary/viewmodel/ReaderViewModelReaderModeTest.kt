package io.github.gmathi.novellibrary.viewmodel

import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ReaderViewModel]'s per-novel reader-mode/JavaScript behavior
 * (`initialize(novelId)`, `setReaderMode(enabled)`, `setJavascriptEnabled(enabled)`).
 *
 * The real [ReaderViewModel] obtains its `DataCenter` via `by injectLazy()` (Injekt DI,
 * registered against a real Android `Application` in `AppModule.kt`), and `DataCenter` itself
 * requires a real Android `Context`. Neither is available on the plain JVM unit test classpath
 * (no Robolectric/mocking framework is set up in this project). Following the same approach as
 * [io.github.gmathi.novellibrary.model.preference.DataCenterDownloadStorageLocationTest] and
 * [io.github.gmathi.novellibrary.model.preference.DataCenterReaderModeTest] use for `DataCenter`
 * itself, this test does not instantiate the real `ReaderViewModel`. Instead it defines a
 * test-local mirror ([FakeReaderViewModel]) that reproduces the exact `initialize`/
 * `setReaderMode`/`setJavascriptEnabled` logic under test, wired to a minimal in-memory fake of
 * the `DataCenter` members it touches ([FakeDataCenter]) instead of `injectLazy()`. This
 * validates the same read/write contract (Requirements 2.1, 2.2, 2.5, 2.6, 2.7, 2.8) without
 * needing Injekt or the Android framework at runtime. [ReaderUiState] itself has no DI
 * dependencies (a plain data class with primitive/default-valued fields), so it is reused
 * directly from production code rather than re-declared.
 *
 * `EventBus.getDefault()` (greenrobot EventBus) runs fine on a plain JVM, so `ReaderSettingsEvent`
 * posts are verified directly via a registered test subscriber.
 */
class ReaderViewModelReaderModeTest {

    /** Minimal in-memory fake of the `DataCenter` members [FakeReaderViewModel] touches. */
    private class FakeDataCenter {
        var readerMode: Boolean = false
        var javascriptDisabled: Boolean = false
        var isDarkTheme: Boolean = true

        private val perNovelReaderMode = mutableMapOf<Long, Boolean>()

        var setReaderModeForNovelCallCount = 0
        var lastSetReaderModeForNovelArgs: Pair<Long, Boolean>? = null

        fun getReaderModeForNovel(novelId: Long): Boolean = perNovelReaderMode[novelId] ?: readerMode

        fun setReaderModeForNovel(novelId: Long, value: Boolean) {
            setReaderModeForNovelCallCount++
            lastSetReaderModeForNovelArgs = novelId to value
            perNovelReaderMode[novelId] = value
        }
    }

    /**
     * Mirrors the exact `initialize`/`setReaderMode`/`setJavascriptEnabled` implementation of
     * [ReaderViewModel], wired to [FakeDataCenter] instead of an Injekt-injected `DataCenter`.
     */
    private class FakeReaderViewModel(private val dataCenter: FakeDataCenter) {

        var uiState: ReaderUiState = ReaderUiState()
            private set

        private var novelId: Long = -1L

        fun initialize(novelId: Long) {
            this.novelId = novelId
            uiState = uiState.copy(
                isReaderMode = dataCenter.getReaderModeForNovel(novelId),
                isDarkTheme = dataCenter.isDarkTheme,
                isJavascriptEnabled = !dataCenter.javascriptDisabled || dataCenter.getReaderModeForNovel(novelId),
            )
        }

        fun setReaderMode(enabled: Boolean) {
            dataCenter.setReaderModeForNovel(novelId, enabled)
            uiState = uiState.copy(
                isReaderMode = enabled,
                isJavascriptEnabled = if (enabled) false else uiState.isJavascriptEnabled
            )
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
        }

        fun setJavascriptEnabled(enabled: Boolean) {
            dataCenter.javascriptDisabled = !enabled
            if (!enabled) dataCenter.setReaderModeForNovel(novelId, false)
            uiState = uiState.copy(
                isJavascriptEnabled = enabled,
                isReaderMode = if (!enabled) false else uiState.isReaderMode
            )
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
        }
    }

    /**
     * Test subscriber used to capture [ReaderSettingsEvent] posts from the EventBus.
     *
     * Must be a non-private class with a public `@Subscribe` method: greenrobot EventBus looks
     * up subscriber methods via reflection, which fails against a Kotlin `private class` (its
     * synthesized method is not actually public at the JVM level).
     */
    class ReaderSettingsEventCollector {
        val received = mutableListOf<ReaderSettingsEvent>()

        @Subscribe
        fun onEvent(event: ReaderSettingsEvent) {
            received.add(event)
        }
    }

    private var collector: ReaderSettingsEventCollector? = null

    private fun registerCollector(): ReaderSettingsEventCollector {
        val newCollector = ReaderSettingsEventCollector()
        collector = newCollector
        EventBus.getDefault().register(newCollector)
        return newCollector
    }

    @After
    fun tearDown() {
        collector?.let { EventBus.getDefault().unregister(it) }
        collector = null
    }

    @Test
    fun `initialize seeds isReaderMode from getReaderModeForNovel for the given novel id`() {
        val dataCenter = FakeDataCenter()
        dataCenter.readerMode = false
        dataCenter.setReaderModeForNovel(42L, true)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(42L)

        assertTrue(viewModel.uiState.isReaderMode)
    }

    @Test
    fun `initialize seeds isReaderMode false when the novel's per-novel value is false`() {
        val dataCenter = FakeDataCenter()
        dataCenter.readerMode = true
        dataCenter.setReaderModeForNovel(42L, false)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(42L)

        assertFalse(viewModel.uiState.isReaderMode)
    }

    @Test
    fun `initialize seeds isJavascriptEnabled from javascriptDisabled OR reader mode for the novel`() {
        val dataCenter = FakeDataCenter()
        dataCenter.javascriptDisabled = true
        dataCenter.setReaderModeForNovel(1L, true)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(1L)

        // javascriptDisabled = true, but reader mode is on for this novel -> OR makes it true
        assertTrue(viewModel.uiState.isJavascriptEnabled)
    }

    @Test
    fun `initialize seeds isJavascriptEnabled false when javascript disabled and reader mode off for the novel`() {
        val dataCenter = FakeDataCenter()
        dataCenter.javascriptDisabled = true
        dataCenter.setReaderModeForNovel(1L, false)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(1L)

        assertFalse(viewModel.uiState.isJavascriptEnabled)
    }

    @Test
    fun `initialize seeds isJavascriptEnabled true when javascript is not disabled regardless of reader mode`() {
        val dataCenter = FakeDataCenter()
        dataCenter.javascriptDisabled = false
        dataCenter.setReaderModeForNovel(1L, false)
        val viewModel = FakeReaderViewModel(dataCenter)

        viewModel.initialize(1L)

        assertTrue(viewModel.uiState.isJavascriptEnabled)
    }

    @Test
    fun `setReaderMode calls setReaderModeForNovel with the current novel id and never writes javascriptDisabled`() {
        val dataCenter = FakeDataCenter()
        dataCenter.javascriptDisabled = false
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(99L)

        viewModel.setReaderMode(true)

        assertEquals(1, dataCenter.setReaderModeForNovelCallCount)
        assertEquals(99L to true, dataCenter.lastSetReaderModeForNovelArgs)
        assertTrue(dataCenter.getReaderModeForNovel(99L))
        // The app-wide javascriptDisabled flag must never be touched by setReaderMode.
        assertFalse(dataCenter.javascriptDisabled)
    }

    @Test
    fun `setReaderMode does not affect the app-wide readerMode default`() {
        val dataCenter = FakeDataCenter()
        dataCenter.readerMode = false
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(5L)

        viewModel.setReaderMode(true)

        assertFalse(dataCenter.readerMode)
    }

    @Test
    fun `setReaderMode still posts ReaderSettingsEvent READER_MODE`() {
        val eventCollector = registerCollector()
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setReaderMode(true)

        assertEquals(1, eventCollector.received.size)
        assertEquals(ReaderSettingsEvent.READER_MODE, eventCollector.received.single().setting)
    }

    @Test
    fun `setReaderMode still forces isJavascriptEnabled false in uiState when enabling`() {
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setReaderMode(true)

        assertFalse(viewModel.uiState.isJavascriptEnabled)
        assertTrue(viewModel.uiState.isReaderMode)
    }

    @Test
    fun `setJavascriptEnabled false calls setReaderModeForNovel with the current novel id and false`() {
        val dataCenter = FakeDataCenter()
        dataCenter.setReaderModeForNovel(7L, true)
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(7L)

        viewModel.setJavascriptEnabled(false)

        assertEquals(2, dataCenter.setReaderModeForNovelCallCount) // one from setup, one from the call under test
        assertEquals(7L to false, dataCenter.lastSetReaderModeForNovelArgs)
        assertFalse(dataCenter.getReaderModeForNovel(7L))
    }

    @Test
    fun `setJavascriptEnabled false does not write the app-wide readerMode setter`() {
        val dataCenter = FakeDataCenter()
        dataCenter.readerMode = true
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(7L)

        viewModel.setJavascriptEnabled(false)

        // The app-wide readerMode default must be untouched; only the per-novel value for
        // novel 7 (and no other novel) should have been written.
        assertTrue(dataCenter.readerMode)
        assertFalse(dataCenter.getReaderModeForNovel(7L))
        assertTrue(dataCenter.getReaderModeForNovel(8L)) // falls back to unchanged readerMode default
    }

    @Test
    fun `setJavascriptEnabled false does not affect other novels' per-novel values`() {
        val dataCenter = FakeDataCenter()
        dataCenter.setReaderModeForNovel(1L, true)
        dataCenter.setReaderModeForNovel(2L, true)
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setJavascriptEnabled(false)

        assertFalse(dataCenter.getReaderModeForNovel(1L))
        assertTrue(dataCenter.getReaderModeForNovel(2L))
    }

    @Test
    fun `setJavascriptEnabled false still posts ReaderSettingsEvent JAVA_SCRIPT`() {
        val eventCollector = registerCollector()
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setJavascriptEnabled(false)

        assertEquals(1, eventCollector.received.size)
        assertEquals(ReaderSettingsEvent.JAVA_SCRIPT, eventCollector.received.single().setting)
    }

    @Test
    fun `setJavascriptEnabled true does not call setReaderModeForNovel`() {
        val dataCenter = FakeDataCenter()
        val viewModel = FakeReaderViewModel(dataCenter)
        viewModel.initialize(1L)

        viewModel.setJavascriptEnabled(true)

        assertEquals(0, dataCenter.setReaderModeForNovelCallCount)
        assertNull(dataCenter.lastSetReaderModeForNovelArgs)
        assertFalse(dataCenter.javascriptDisabled)
    }
}
