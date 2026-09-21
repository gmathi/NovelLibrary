package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.io.IOException

/**
 * Model-based property test for [StorageMigrator]'s migration safety invariant.
 *
 * [StorageMigrator.migrate] requires a real Android [io.github.gmathi.novellibrary.database.DBHelper]
 * (a `SQLiteOpenHelper`) to commit chapter paths, which cannot be instantiated in a plain JVM unit
 * test. This test instead models the exact algorithm `migrate` implements over a real temporary
 * filesystem:
 *
 *  - iterate Novel_Dirs one at a time,
 *  - move each Novel_Dir's files to the new root (using the real, production
 *    [StorageMigrator.computeDestinationPath] for path computation, exactly as `migrate` does),
 *  - commit a chapter's stored path to the new location only after its destination file is
 *    confirmed to exist,
 *  - stop entirely at an injected failure point k, simulating an I/O error partway through.
 *
 * The property under test (Property 7 / Requirement 5.5) is a structural invariant of this
 * algorithm: no matter where k falls, every chapter's *currently stored* path resolves to a file
 * that actually exists on disk, because a path is only ever repointed to the new location after
 * its file is verified present there, and the old file is never removed before its path is
 * repointed.
 */
class StorageMigratorSafetyPropertyTest {

    private fun nameSegmentArbitrary(): Arbitrary<String> =
        Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(8)

    /** One simulated Novel_Dir: a directory name plus the chapter file names it contains. */
    data class SimulatedNovelDir(val dirName: String, val chapterFileNames: List<String>)

    @Provide
    fun migrationScenarios(): Arbitrary<MigrationScenario> {
        val novelDirArbitrary: Arbitrary<SimulatedNovelDir> = Combinators.combine(
            nameSegmentArbitrary(),
            nameSegmentArbitrary().list().ofMinSize(1).ofMaxSize(4)
        ).`as` { dirName, fileNames ->
            // De-duplicate file names within a single novel dir so each maps to a distinct file.
            SimulatedNovelDir(dirName, fileNames.distinct().ifEmpty { listOf("chapter") })
        }

        val novelDirsArbitrary: Arbitrary<List<SimulatedNovelDir>> = novelDirArbitrary
            .list()
            .ofMinSize(1)
            .ofMaxSize(6)
            .map { dirs ->
                // De-duplicate dir names across the batch so each maps to a distinct Novel_Dir.
                val seen = HashSet<String>()
                dirs.filter { seen.add(it.dirName) }.ifEmpty { listOf(SimulatedNovelDir("novel", listOf("chapter"))) }
            }

        return novelDirsArbitrary.flatMap { dirs ->
            // Injected failure point: null means "no failure, full success"; otherwise an index
            // into dirs at which processing throws before committing any of that dir's chapters.
            Arbitraries.integers().between(0, dirs.size).map { k ->
                val failAt = if (k == dirs.size) null else k
                MigrationScenario(dirs, failAt)
            }
        }
    }

    data class MigrationScenario(val novelDirs: List<SimulatedNovelDir>, val failAtIndex: Int?)

    /**
     * Runs the same commit-after-verify, stop-on-failure algorithm as
     * [StorageMigrator.migrate], driven by the real [StorageMigrator.computeDestinationPath].
     *
     * @return the map of chapter absolute file path (url key) -> currently stored path, mirroring
     *         what would be committed to `WebPageSettings.filePath` in the real DB.
     */
    private fun runModeledMigration(
        oldRoot: File,
        newRoot: File,
        scenario: MigrationScenario
    ): Map<String, String> {
        // Seed the source filesystem and the "DB": every chapter's file_path starts under oldRoot.
        val storedPaths = LinkedHashMap<String, String>()
        for (novelDir in scenario.novelDirs) {
            val sourceDir = File(oldRoot, novelDir.dirName)
            sourceDir.mkdirs()
            for (fileName in novelDir.chapterFileNames) {
                val file = File(sourceDir, fileName)
                file.writeText("content of $fileName")
                val key = "${novelDir.dirName}/$fileName"
                storedPaths[key] = file.absolutePath
            }
        }

        // Model the migrate() loop: one Novel_Dir at a time, copy, then commit each chapter path
        // only once its destination file is confirmed present, then stop entirely at failAtIndex.
        for ((index, novelDir) in scenario.novelDirs.withIndex()) {
            if (scenario.failAtIndex == index) {
                // Injected failure: an I/O error strikes before this Novel_Dir's files are copied
                // or any of its chapter paths are committed (mirrors migrate()'s try/catch break).
                break
            }

            val sourceDir = File(oldRoot, novelDir.dirName)
            val destDir = File(newRoot, novelDir.dirName)
            destDir.mkdirs()
            for (fileName in novelDir.chapterFileNames) {
                val sourceFile = File(sourceDir, fileName)
                val destFile = File(destDir, fileName)
                if (sourceFile.exists()) {
                    sourceFile.copyTo(destFile, overwrite = true)
                }

                val key = "${novelDir.dirName}/$fileName"
                val oldPath = storedPaths[key] ?: continue
                val newPath = StorageMigrator.computeDestinationPath(oldRoot, newRoot, oldPath) ?: continue

                // Commit only after the destination file is verified present (migrate()'s guard).
                if (File(newPath).exists()) {
                    storedPaths[key] = newPath
                } else {
                    throw IOException("modeled migration invariant violated: destination missing for $key")
                }
            }

            // Source dir would be deleted here in the real implementation, after paths committed.
            sourceDir.deleteRecursively()
        }

        return storedPaths
    }

    // Feature: download-storage-location, Property 7: Migration never leaves a chapter unreadable
    @Property(tries = 100)
    fun migrationNeverLeavesAChapterUnreadable(
        @ForAll("migrationScenarios") scenario: MigrationScenario
    ) {
        val oldRoot = createTempDir(prefix = "migration-safety-old-")
        val newRoot = createTempDir(prefix = "migration-safety-new-")
        try {
            val storedPaths = runModeledMigration(oldRoot, newRoot, scenario)

            // Every chapter's currently stored path must resolve to a file that actually exists,
            // whether it was committed to the new location or left untouched at the old one.
            for ((key, path) in storedPaths) {
                assertTrue(
                    File(path).exists(),
                    "chapter $key has stored path $path which does not exist on disk " +
                        "(scenario=$scenario)"
                )
            }
        } finally {
            oldRoot.deleteRecursively()
            newRoot.deleteRecursively()
        }
    }

    private fun createTempDir(prefix: String): File =
        File.createTempFile(prefix, "").let { file ->
            file.delete()
            file.mkdirs()
            file
        }
}
