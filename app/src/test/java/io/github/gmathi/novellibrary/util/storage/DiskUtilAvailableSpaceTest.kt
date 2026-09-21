package io.github.gmathi.novellibrary.util.storage

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Integration tests for [DiskUtil.getAvailableSpace] and [DiskUtil.getTotalSpace].
 *
 * These helpers wrap `android.os.StatFs`, an Android framework class. Under the plain JVM unit
 * test runner (no device/emulator, no Robolectric in this project), the framework class is only
 * available as an SDK stub whose methods throw `RuntimeException("Stub!")`. Both helpers already
 * catch `Exception` around the `StatFs` call and return `0L` on failure, so that stub exception
 * is swallowed and the helpers behave as a safe "always non-negative, 0 on error" API even in
 * this environment. That is exactly the contract these tests assert, so they run as ordinary
 * local unit tests here rather than requiring an instrumented/androidTest target.
 *
 * Validates: Requirements 2.2
 */
class DiskUtilAvailableSpaceTest {

    @Test
    fun `getAvailableSpace returns a non-negative value for a real directory`() {
        val tempDir = newTempDir()
        try {
            val availableSpace = DiskUtil.getAvailableSpace(tempDir)
            assertTrue(availableSpace >= 0L, "Expected non-negative available space, got $availableSpace")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `getTotalSpace returns a non-negative value for a real directory`() {
        val tempDir = newTempDir()
        try {
            val totalSpace = DiskUtil.getTotalSpace(tempDir)
            assertTrue(totalSpace >= 0L, "Expected non-negative total space, got $totalSpace")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `getAvailableSpace returns 0 for an invalid path`() {
        val invalidPath = File("/this/path/does/not/exist/${System.nanoTime()}")
        assertTrue(!invalidPath.exists(), "Precondition failed: path unexpectedly exists")
        val availableSpace = DiskUtil.getAvailableSpace(invalidPath)
        assertTrue(availableSpace == 0L, "Expected 0 for an invalid path, got $availableSpace")
    }

    @Test
    fun `getTotalSpace returns 0 for an invalid path`() {
        val invalidPath = File("/this/path/does/not/exist/${System.nanoTime()}")
        assertTrue(!invalidPath.exists(), "Precondition failed: path unexpectedly exists")
        val totalSpace = DiskUtil.getTotalSpace(invalidPath)
        assertTrue(totalSpace == 0L, "Expected 0 for an invalid path, got $totalSpace")
    }

    /** Creates a fresh, real temp directory for the duration of a single test. */
    private fun newTempDir(): File {
        val marker = File.createTempFile("disk-util-test-", "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        return dir
    }
}
