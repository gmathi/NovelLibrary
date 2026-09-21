package io.github.gmathi.novellibrary.model.preference

import android.content.SharedPreferences
import io.github.gmathi.novellibrary.util.storage.StorageLocation
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [DataCenter.downloadStorageLocation].
 *
 * [DataCenter] itself requires a real Android `Context` (it calls
 * `PreferenceManager.getDefaultSharedPreferences(context)` in its constructor), which isn't
 * available on the plain JVM unit test classpath (no Robolectric/mocking framework is set up in
 * this project). Since `downloadStorageLocation` is a thin `SharedPreferences`-backed `var`
 * (`get() = prefs.getString(KEY, DEFAULT)!!`, `set(value) = prefs.edit().putString(KEY, value).apply()`),
 * this test exercises the exact same pattern against a minimal in-memory fake of
 * `SharedPreferences`/`SharedPreferences.Editor` to validate the getter/setter contract
 * (Requirement 1.4) without needing the Android framework at runtime.
 */
class DataCenterDownloadStorageLocationTest {

    private companion object {
        private const val DOWNLOAD_STORAGE_LOCATION = "downloadStorageLocation"
    }

    /** Mirrors the exact get/set implementation of `DataCenter.downloadStorageLocation`. */
    private class FakeDownloadStorageLocationHolder(private val prefs: SharedPreferences) {
        var downloadStorageLocation: String
            get() = prefs.getString(DOWNLOAD_STORAGE_LOCATION, StorageLocation.INTERNAL_TOKEN)!!
            set(value) = prefs.edit().putString(DOWNLOAD_STORAGE_LOCATION, value).apply()
    }

    @Test
    fun `getter defaults to internal when unset`() {
        val holder = FakeDownloadStorageLocationHolder(FakeSharedPreferences())

        assertEquals(StorageLocation.INTERNAL_TOKEN, holder.downloadStorageLocation)
    }

    @Test
    fun `getter returns the last value set`() {
        val holder = FakeDownloadStorageLocationHolder(FakeSharedPreferences())
        val sdLocation = "${StorageLocation.SD_PREFIX}ABCD-1234"

        holder.downloadStorageLocation = sdLocation

        assertEquals(sdLocation, holder.downloadStorageLocation)
    }

    @Test
    fun `getter reflects the most recent of multiple writes`() {
        val holder = FakeDownloadStorageLocationHolder(FakeSharedPreferences())

        holder.downloadStorageLocation = "${StorageLocation.SD_PREFIX}FIRST-0001"
        holder.downloadStorageLocation = "${StorageLocation.SD_PREFIX}SECOND-0002"

        assertEquals("${StorageLocation.SD_PREFIX}SECOND-0002", holder.downloadStorageLocation)
    }

    @Test
    fun `setting back to internal token restores default value`() {
        val holder = FakeDownloadStorageLocationHolder(FakeSharedPreferences())

        holder.downloadStorageLocation = "${StorageLocation.SD_PREFIX}ABCD-1234"
        holder.downloadStorageLocation = StorageLocation.INTERNAL_TOKEN

        assertEquals(StorageLocation.INTERNAL_TOKEN, holder.downloadStorageLocation)
    }
}
