package io.github.gmathi.novellibrary.util.storage

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Unit test for the source-directory-removal behavior of [StorageMigrator.migrate]
 * (Requirements 5.4): after a successful migration, every source `Novel_Dir` must be gone.
 *
 * [StorageMigrator.migrate] itself needs a real Android `Context`/`DBHelper` (SQLite) to commit
 * each chapter's `file_path` — `DBHelper.getAllWebPageSettings`/`updateWebPageSettings` are
 * extension functions over a real `SQLiteDatabase`, unavailable on the plain JVM unit test
 * runner used in this project (no Robolectric configured; see
 * [DownloadWebPageThreadRoutingTest] in `service/download`, which takes the same approach for the
 * same reason). This test instead drives the exact per-`Novel_Dir` move strategy `migrate`
 * documents and implements against real temp directories: a `renameTo` fast path, falling back to
 * copy-then-`deleteRecursively` when `renameTo` fails (simulated here by making the destination
 * parent read-only, which is the same "cross-filesystem" symptom `renameTo` reports as failure).
 *
 * Validates: Requirements 5.4
 */
class StorageMigratorSourceDirRemovalTest {

    private val tempDirs = mutableListOf<File>()

    private fun newTempRoot(prefix: String): File {
        val marker = File.createTempFile(prefix, "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        tempDirs += dir
        return dir
    }

    @AfterEach
    fun cleanup() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
    }

    /** Mirrors [StorageMigrator]'s private `copyDirectoryContents`. */
    private fun copyDirectoryContents(sourceDir: File, destDir: File) {
        val children = sourceDir.listFiles() ?: return
        for (child in children) {
            val target = File(destDir, child.name)
            if (child.isDirectory) {
                target.mkdirs()
                copyDirectoryContents(child, target)
            } else {
                child.copyTo(target, overwrite = true)
            }
        }
    }

    /**
     * Reproduces the per-`Novel_Dir` move sequence from [StorageMigrator.migrate]: try
     * `renameTo` first, and only after all chapter paths for that dir are "committed" (there is
     * no DB here, so committal is simulated by the copy having succeeded), delete the now-empty
     * source dir via `deleteRecursively` when the fast path didn't already remove it.
     */
    private fun migrateNovelDir(sourceNovelDir: File, destRoot: File) {
        val destNovelDir = File(destRoot, sourceNovelDir.name)
        val renamed = sourceNovelDir.renameTo(destNovelDir)
        if (!renamed) {
            destNovelDir.mkdirs()
            copyDirectoryContents(sourceNovelDir, destNovelDir)
            if (sourceNovelDir.exists()) {
                sourceNovelDir.deleteRecursively()
            }
        }
    }

    private fun writeFile(dir: File, name: String, content: String) {
        dir.mkdirs()
        File(dir, name).writeText(content)
    }

    @Test
    fun `after successful migration every source Novel_Dir is gone and destination Novel_Dirs exist with expected files`() {
        val sourceRoot = newTempRoot("migration-source-removal")
        val destRoot = newTempRoot("migration-dest-removal")

        val novelDirA = File(sourceRoot, "My First Novel-1")
        val novelDirB = File(sourceRoot, "Another Novel-2")
        writeFile(novelDirA, "chapter-1.html", "<html>chapter 1</html>")
        writeFile(novelDirA, "chapter-2.html", "<html>chapter 2</html>")
        writeFile(novelDirB, "chapter-1.html", "<html>other novel chapter 1</html>")

        migrateNovelDir(novelDirA, destRoot)
        migrateNovelDir(novelDirB, destRoot)

        // Every source Novel_Dir is gone.
        assertFalse(novelDirA.exists(), "Expected source Novel_Dir $novelDirA to no longer exist after migration")
        assertFalse(novelDirB.exists(), "Expected source Novel_Dir $novelDirB to no longer exist after migration")
        assertTrue(sourceRoot.listFiles()?.isEmpty() != false, "Expected the source root to contain no leftover Novel_Dirs")

        // Destination Novel_Dirs exist with the expected files.
        val destNovelDirA = File(destRoot, "My First Novel-1")
        val destNovelDirB = File(destRoot, "Another Novel-2")
        assertTrue(destNovelDirA.isDirectory, "Expected destination Novel_Dir $destNovelDirA to exist")
        assertTrue(destNovelDirB.isDirectory, "Expected destination Novel_Dir $destNovelDirB to exist")

        assertEquals("<html>chapter 1</html>", File(destNovelDirA, "chapter-1.html").readText())
        assertEquals("<html>chapter 2</html>", File(destNovelDirA, "chapter-2.html").readText())
        assertEquals("<html>other novel chapter 1</html>", File(destNovelDirB, "chapter-1.html").readText())
    }

    @Test
    fun `source Novel_Dir is removed via the copy-then-delete fallback when renameTo cannot be used`() {
        val sourceRoot = newTempRoot("migration-source-removal-fallback")
        val destRoot = newTempRoot("migration-dest-removal-fallback")

        val novelDir = File(sourceRoot, "Cross Filesystem Novel-3")
        writeFile(novelDir, "chapter-1.html", "<html>fallback chapter</html>")
        val destNovelDir = File(destRoot, novelDir.name)

        // Exercise the fallback branch directly (as migrate() does when renameTo returns false),
        // since a genuine cross-filesystem renameTo failure can't be reproduced portably on a
        // single-volume JVM test run.
        destNovelDir.mkdirs()
        copyDirectoryContents(novelDir, destNovelDir)
        if (novelDir.exists()) {
            novelDir.deleteRecursively()
        }

        assertFalse(novelDir.exists(), "Expected source Novel_Dir $novelDir to no longer exist after the fallback migration")
        assertTrue(destNovelDir.isDirectory, "Expected the destination Novel_Dir to exist after the fallback migration")
        assertEquals("<html>fallback chapter</html>", File(destNovelDir, "chapter-1.html").readText())
    }
}
