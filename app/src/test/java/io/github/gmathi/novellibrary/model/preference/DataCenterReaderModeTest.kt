package io.github.gmathi.novellibrary.model.preference

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DataCenter.getReaderModeForNovel]/[DataCenter.setReaderModeForNovel].
 *
 * [DataCenter] itself requires a real Android `Context` (it calls
 * `PreferenceManager.getDefaultSharedPreferences(context)` in its constructor), which isn't
 * available on the plain JVM unit test classpath (no Robolectric/mocking framework is set up in
 * this project). Since the per-novel reader mode accessor is a thin `SharedPreferences`-backed
 * get/set pair keyed by novel id (`prefs.getBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId,
 * readerMode)` / `prefs.edit().putBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId,
 * value).apply()`), this test exercises the exact same pattern against a minimal in-memory fake
 * of `SharedPreferences`/`SharedPreferences.Editor` to validate the getter/setter contract
 * (Requirements 1.2, 1.3, 1.4, 3.2, 3.3) without needing the Android framework at runtime.
 */
class DataCenterReaderModeTest {

    private companion object {
        private const val READER_MODE = "cleanPages"
        private const val READER_MODE_PER_NOVEL_PREFIX = "reader_mode_"
    }

    /** Mirrors the exact get/set implementation of `DataCenter.readerMode`/`getReaderModeForNovel`/`setReaderModeForNovel`. */
    private class FakeReaderModeHolder(private val prefs: SharedPreferences) {
        var readerMode: Boolean
            get() = prefs.getBoolean(READER_MODE, false)
            set(value) = prefs.edit().putBoolean(READER_MODE, value).apply()

        fun getReaderModeForNovel(novelId: Long): Boolean =
            prefs.getBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId, readerMode)

        fun setReaderModeForNovel(novelId: Long, value: Boolean) {
            prefs.edit().putBoolean(READER_MODE_PER_NOVEL_PREFIX + novelId, value).apply()
        }
    }

    @Test
    fun `getReaderModeForNovel returns readerMode default when unset`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())

        assertFalse(holder.getReaderModeForNovel(1L))

        holder.readerMode = true

        assertTrue(holder.getReaderModeForNovel(1L))
    }

    @Test
    fun `setReaderModeForNovel then getReaderModeForNovel round-trips true`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())
        holder.readerMode = false

        holder.setReaderModeForNovel(42L, true)

        assertTrue(holder.getReaderModeForNovel(42L))
    }

    @Test
    fun `setReaderModeForNovel then getReaderModeForNovel round-trips false`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())
        holder.readerMode = true

        holder.setReaderModeForNovel(42L, false)

        assertFalse(holder.getReaderModeForNovel(42L))
    }

    @Test
    fun `writing for one novel id does not affect a different novel id`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())
        holder.readerMode = false

        holder.setReaderModeForNovel(1L, true)

        assertTrue(holder.getReaderModeForNovel(1L))
        assertFalse(holder.getReaderModeForNovel(2L))
    }

    @Test
    fun `changing readerMode default after a per-novel value is set does not affect that novel's stored value`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())
        holder.readerMode = false
        holder.setReaderModeForNovel(7L, true)

        holder.readerMode = true

        assertTrue(holder.getReaderModeForNovel(7L))

        holder.readerMode = false

        assertTrue(holder.getReaderModeForNovel(7L))
    }

    @Test
    fun `setReaderModeForNovel does not modify the readerMode default`() {
        val holder = FakeReaderModeHolder(FakeSharedPreferences())
        holder.readerMode = false

        holder.setReaderModeForNovel(1L, true)

        assertEquals(false, holder.readerMode)
    }
}
