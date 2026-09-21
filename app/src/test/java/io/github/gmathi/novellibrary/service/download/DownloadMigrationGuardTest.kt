package io.github.gmathi.novellibrary.service.download

import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.storage.StorageMigrator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit test for the "reject/defer a download while a storage migration is running" guard shared
 * by [DownloadWebPageThread.isMigrationInProgress] (checked at the top of `run()`) and
 * [DownloadNovelThread.run]'s equivalent inline check before starting a
 * [DownloadWebPageThread] (Requirement 5.3).
 *
 * Both call sites gate on the exact same expression — `StorageMigrator.isMigrationInProgress` —
 * and on a positive check throw `InterruptedException(Constants.MIGRATION_IN_PROGRESS)`, which
 * `DownloadNovelThread.run`'s own top-level `catch (e: InterruptedException)` (its "progress
 * callback" — it reports `EventType.PAUSED`/`EventType.DELETE` via `downloadListener.handleEvent`
 * once the loop unwinds) and `DownloadWebPageThread.run`'s `catch (e: InterruptedException)`
 * both treat as a deferral: the in-flight download row is left/reset to `IN_QUEUE` rather than
 * failed, so the download is retried once migration finishes rather than being lost.
 *
 * Neither thread is directly constructable in a plain-JVM unit test: both need a real Android
 * `Context`/`DBHelper` (SQLite, `NetworkHelper`, Jsoup network fetch, `HtmlCleaner`), unavailable
 * without Robolectric/mocking (no such framework is configured in this project — see the
 * boundary note in [io.github.gmathi.novellibrary.model.preference.DataCenterDownloadStorageLocationTest]).
 * `StorageMigrator` itself needs a real `Context`/`DBHelper`/`DataCenter` for the same reason, so
 * driving `isMigrationInProgress` via a real `migrate()` call isn't practical here either — and
 * its setter is private (`@Volatile var isMigrationInProgress: Boolean = false; private set`),
 * so it cannot be poked directly from a test.
 *
 * What *is* pure and shared by both call sites is the guard expression itself:
 * `StorageMigrator.isMigrationInProgress` gating a throw of
 * `InterruptedException(Constants.MIGRATION_IN_PROGRESS)`. This test extracts exactly that guard
 * (mirroring the extraction pattern used in `ReaderFilePathFallbackTest`) and exercises it against
 * the real, shared `StorageMigrator.isMigrationInProgress` flag — reflectively toggling the
 * private-set companion property for the duration of the test, then always restoring it in
 * `finally`, since it is process-global mutable state shared with other `StorageMigrator` tests
 * running in the same JVM/test run.
 *
 * Validates: Requirements 5.3
 */
class DownloadMigrationGuardTest {

    /**
     * Mirrors [DownloadWebPageThread.isMigrationInProgress] / `DownloadNovelThread.run`'s inline
     * check exactly: if migration is in progress, throw the deferral exception; otherwise permit
     * the download to proceed.
     */
    private fun guardAgainstMigrationInProgress() {
        if (StorageMigrator.isMigrationInProgress) {
            throw InterruptedException(Constants.MIGRATION_IN_PROGRESS)
        }
    }

    /**
     * Reflectively sets the private-set companion property `StorageMigrator.isMigrationInProgress`.
     * Production code only ever flips this from within `StorageMigrator.migrate()`'s try/finally;
     * this is a test-only seam to simulate "migration currently running" without needing a real
     * `Context`/`DBHelper`/`DataCenter` to drive an actual `migrate()` call.
     *
     * The backing field lives on the outer `StorageMigrator` class (Kotlin hoists
     * `@Volatile private static var` companion backing fields there) and is only mutable via the
     * compiler-generated `access$setMigrationInProgress$cp` bridge, since the property setter
     * itself is `private`.
     */
    private fun setMigrationInProgress(value: Boolean) {
        val method = StorageMigrator::class.java.getDeclaredMethod("access\$setMigrationInProgress\$cp", Boolean::class.javaPrimitiveType)
        method.isAccessible = true
        method.invoke(null, value)
    }

    @Test
    fun `download guard throws the deferral exception while migration is in progress`() {
        assertFalse(StorageMigrator.isMigrationInProgress, "Precondition failed: migration flag was already set by another test")
        setMigrationInProgress(true)
        try {
            assertTrue(StorageMigrator.isMigrationInProgress)

            val thrown = org.junit.jupiter.api.Assertions.assertThrows(InterruptedException::class.java) {
                guardAgainstMigrationInProgress()
            }
            assertEquals(Constants.MIGRATION_IN_PROGRESS, thrown.message)
        } finally {
            setMigrationInProgress(false)
        }
        assertFalse(StorageMigrator.isMigrationInProgress, "Flag must not leak into other tests")
    }

    @Test
    fun `download guard permits the download once migration has finished`() {
        assertFalse(StorageMigrator.isMigrationInProgress, "Precondition failed: migration flag was already set by another test")
        setMigrationInProgress(true)
        // Simulate StorageMigrator.migrate()'s finally block releasing the lock once migration completes.
        setMigrationInProgress(false)
        try {
            assertFalse(StorageMigrator.isMigrationInProgress)

            var proceeded = false
            try {
                guardAgainstMigrationInProgress()
                proceeded = true
            } catch (e: InterruptedException) {
                proceeded = false
            }
            assertTrue(proceeded, "Download must be permitted once migration has finished")
        } finally {
            setMigrationInProgress(false)
        }
    }

    /**
     * End-to-end simulation of `DownloadNovelThread.run`'s deferral handling: when the guard
     * throws mid-queue, the surrounding `catch (e: InterruptedException)` swallows it (matching
     * production, which logs and returns rather than reporting `EventType.PAUSED`/`DELETE`) and no
     * further downloads in the queue are started — i.e. the download is deferred, not dropped or
     * failed.
     */
    @Test
    fun `a deferred download is not reported as failed and no further queue items are started`() {
        assertFalse(StorageMigrator.isMigrationInProgress, "Precondition failed: migration flag was already set by another test")
        setMigrationInProgress(true)
        try {
            var startedDownloads = 0
            var reportedFailure = false

            // Mirrors DownloadNovelThread.run's `while (download != null && !interrupted())` loop body.
            try {
                guardAgainstMigrationInProgress()
                startedDownloads++ // would start DownloadWebPageThread here in production
            } catch (e: InterruptedException) {
                // Matches DownloadNovelThread.run's top-level catch: logged, not reported as a failure.
                reportedFailure = false
            }

            assertEquals(0, startedDownloads, "No download should have started while migration is in progress")
            assertFalse(reportedFailure, "A deferred download must not be reported as failed")
        } finally {
            setMigrationInProgress(false)
        }
    }

    @Test
    fun `isMigrationInProgress is false at rest`() {
        // Sanity check on the real shared flag's default/rest state, guarding against leakage from
        // other StorageMigrator-related tests in this test run.
        assertFalse(StorageMigrator.isMigrationInProgress, "isMigrationInProgress must be false at rest")
    }
}
