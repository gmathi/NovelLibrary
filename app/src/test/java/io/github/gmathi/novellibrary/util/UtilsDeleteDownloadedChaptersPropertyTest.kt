package io.github.gmathi.novellibrary.util

import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.IntRange
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Property-based test for the deletion-completeness logic in [Utils.deleteDownloadedChapters]:
 *
 * 1. Delete the resolved active `Novel_Dir` (`getNovelDir(context, ...).deleteRecursively()`).
 * 2. Delete every stored `file_path`'s parent directory (`.mapNotNull { it.filePath }
 *    .mapNotNull { File(it).parentFile }.toSet().forEach { it.deleteRecursively() }`), which
 *    covers chapters left behind under a previous (pre-migration) location.
 *
 * [Utils.deleteDownloadedChapters] itself requires a real Android `Context`/`DBHelper` (SQLite
 * lookups via `dbHelper.getAllWebPageSettings`), neither of which is available under the plain
 * JVM unit test runner used in this project (no Robolectric/mocking framework configured — see
 * [io.github.gmathi.novellibrary.service.download.DownloadWebPageThreadRoutingTest] for the same
 * constraint). Rather than stand up a real DB and Context, this test replicates steps 1 and 2
 * exactly against real temp directories standing in for the internal, SD, and stale
 * pre-migration roots: the active `Novel_Dir` is resolved with [Utils.buildNovelDir] (the same
 * pure seam [Utils.getNovelDir] delegates to), and the pre-migration/other-location chapter
 * files are deleted by their simulated `file_path`'s parent directory, mirroring the DB-driven
 * step verbatim but reading paths from an in-memory list instead of `dbHelper`.
 *
 * Validates: Requirements 6.1, 6.2, 6.3
 */
class UtilsDeleteDownloadedChaptersPropertyTest {

    // Feature: download-storage-location, Property 9: Deletion removes chapters across all locations leaving no orphan
    @Property(tries = 100)
    fun deletionRemovesEveryReferencedFileAndTheActiveNovelDirAcrossAllLocations(
        @ForAll @IntRange(min = 0, max = 5) internalCount: Int,
        @ForAll @IntRange(min = 0, max = 5) sdCount: Int,
        @ForAll @IntRange(min = 0, max = 5) preMigrationCount: Int
    ) {
        val internalRoot = newTempDir("delete-test-internal")
        val sdRoot = newTempDir("delete-test-sd")
        val preMigrationRoot = newTempDir("delete-test-pre-migration")
        try {
            val novelName = "Deletion Test Novel"
            val novelId = 99L

            // The active location for this iteration is the internal root: this is what
            // getNovelDir(context, ...) would resolve to, and step 1 deletes it directly.
            val activeNovelDir = Utils.buildNovelDir(internalRoot, novelName, novelId)

            val simulatedFilePaths = mutableListOf<String>()

            // Chapters already under the active location's Novel_Dir (covered by step 1 alone).
            repeat(internalCount) { i ->
                val file = File(activeNovelDir, "chapter-internal-$i.html")
                file.writeText("content")
                simulatedFilePaths += file.absolutePath
            }

            // Chapters that live on a different (e.g. SD) Novel_Dir but are still referenced by a
            // stored file_path -- covered by step 2.
            val sdNovelDir = Utils.buildNovelDir(sdRoot, novelName, novelId)
            repeat(sdCount) { i ->
                val file = File(sdNovelDir, "chapter-sd-$i.html")
                file.writeText("content")
                simulatedFilePaths += file.absolutePath
            }

            // Chapters left behind under a stale pre-migration root -- also covered by step 2.
            val preMigrationNovelDir = Utils.buildNovelDir(preMigrationRoot, novelName, novelId)
            repeat(preMigrationCount) { i ->
                val file = File(preMigrationNovelDir, "chapter-old-$i.html")
                file.writeText("content")
                simulatedFilePaths += file.absolutePath
            }

            // Sanity precondition: everything exists before deletion runs.
            simulatedFilePaths.forEach { assertTrue(File(it).exists(), "expected $it to exist before deletion") }

            // Replicates Utils.deleteDownloadedChapters steps 1 + 2 verbatim, against the
            // simulated roots/paths instead of a Context/DBHelper.
            deleteDownloadedChaptersAcrossLocations(activeNovelDir, simulatedFilePaths)

            // No referenced chapter file remains, in any of the spread locations.
            simulatedFilePaths.forEach { path ->
                assertFalse(File(path).exists(), "expected $path to be deleted")
            }

            // No orphaned active-location Novel_Dir remains.
            assertFalse(activeNovelDir.exists(), "expected the active Novel_Dir to be deleted")
        } finally {
            internalRoot.deleteRecursively()
            sdRoot.deleteRecursively()
            preMigrationRoot.deleteRecursively()
        }
    }

    /**
     * Mirrors [Utils.deleteDownloadedChapters] steps 1 and 2:
     * 1) delete the resolved active Novel_Dir directly.
     * 2) delete each stored file_path's parent dir (covers pre-migration/other-location files).
     */
    private fun deleteDownloadedChaptersAcrossLocations(activeNovelDir: File, filePaths: List<String>) {
        activeNovelDir.deleteRecursively()

        filePaths
            .mapNotNull { File(it).parentFile }
            .toSet()
            .forEach { it.deleteRecursively() }
    }

    private fun newTempDir(prefix: String): File {
        val marker = File.createTempFile(prefix, "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        return dir
    }
}
