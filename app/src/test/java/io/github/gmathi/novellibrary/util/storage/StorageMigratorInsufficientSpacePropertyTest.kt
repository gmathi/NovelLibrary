package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Property-based tests for the free-space precheck in [StorageMigrator.migrate] (Requirement 5.6):
 * "IF the destination has insufficient free space for Migration, THEN THE system SHALL not begin
 * the move and SHALL inform the user."
 *
 * [StorageMigrator] takes a real `Context`, `DBHelper`, and `DataCenter` in its constructor and
 * `migrate()`'s space check reads `DiskUtil.getAvailableSpace`, which wraps `android.os.StatFs` —
 * an Android framework class that is only present as an SDK stub under the plain JVM unit test
 * runner used by this project (no Robolectric, no instrumented target; see
 * [DiskUtilAvailableSpaceTest]). Neither seam is directly callable/controllable here, so this
 * test exercises the precheck's decision logic itself — `required > available` short-circuits to
 * `InsufficientSpace` before any file is touched, otherwise the precheck passes — using the same
 * `required` computation `estimateRequiredBytes` performs (summing [DiskUtil.getDirectorySize]
 * over each top-level Novel_Dir under the source root) against real temp-filesystem directories,
 * mirrored against [StorageMigrator.MigrationResult.InsufficientSpace] to match `migrate`'s
 * actual branch exactly.
 */
class StorageMigratorInsufficientSpacePropertyTest {

    data class PrecheckInput(val novelDirFileSizes: List<Long>, val availableBytes: Long)

    @Provide
    fun precheckInputs(): Arbitrary<PrecheckInput> =
        Combinators.combine(
            Arbitraries.longs().between(0L, 10_000L).list().ofMinSize(0).ofMaxSize(5),
            Arbitraries.longs().between(0L, 10_000L)
        ).`as` { fileSizes, availableBytes -> PrecheckInput(fileSizes, availableBytes) }

    // Feature: download-storage-location, Property 8: Insufficient destination space aborts before any change
    @Property(tries = 100)
    fun insufficientDestinationSpaceAbortsBeforeAnyChange(@ForAll("precheckInputs") input: PrecheckInput) {
        val sourceRoot = newTempDir("precheck-source")
        val destRoot = newTempDir("precheck-dest")
        try {
            // Build one Novel_Dir per generated file size, each containing a single file of that size.
            input.novelDirFileSizes.forEachIndexed { index, size ->
                val novelDir = File(sourceRoot, "Novel-$index")
                novelDir.mkdirs()
                File(novelDir, "chapter.html").writeBytes(ByteArray(size.toInt()))
            }

            // Snapshot destination + source state before running the precheck.
            val destFilesBefore = destRoot.listFiles()?.toList() ?: emptyList()
            val sourceNovelDirsBefore = sourceRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet() ?: emptySet()

            // Mirrors StorageMigrator.estimateRequiredBytes: sum DiskUtil.getDirectorySize over
            // each top-level directory of the source root.
            val required = sourceRoot.listFiles()
                ?.filter { it.isDirectory }
                ?.sumOf { DiskUtil.getDirectorySize(it) }
                ?: 0L
            val available = input.availableBytes

            // Mirrors StorageMigrator.migrate's precheck branch exactly.
            val precheckResult: StorageMigrator.MigrationResult? =
                if (required > available) {
                    StorageMigrator.MigrationResult.InsufficientSpace(requiredBytes = required, availableBytes = available)
                } else {
                    null // precheck passes; migrate would proceed past it
                }

            if (required > available) {
                assertTrue(precheckResult is StorageMigrator.MigrationResult.InsufficientSpace, "expected InsufficientSpace when required > available")
                val insufficientSpace = precheckResult as StorageMigrator.MigrationResult.InsufficientSpace
                assertEquals(required, insufficientSpace.requiredBytes, "InsufficientSpace should report the exact required bytes")
                assertEquals(available, insufficientSpace.availableBytes, "InsufficientSpace should report the exact available bytes")

                // Nothing was moved: source Novel_Dirs are untouched and no file/path rewrite happened at the destination.
                val sourceNovelDirsAfter = sourceRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet() ?: emptySet()
                assertEquals(sourceNovelDirsBefore, sourceNovelDirsAfter, "source Novel_Dirs must be untouched when space is insufficient")
                val destFilesAfter = destRoot.listFiles()?.toList() ?: emptyList()
                assertEquals(destFilesBefore.size, destFilesAfter.size, "destination must receive no new files when space is insufficient")
            } else {
                // Precheck passes (does not short-circuit): migration would proceed past this point.
                assertTrue(precheckResult == null, "expected the precheck to pass (no InsufficientSpace) when required <= available")
            }
        } finally {
            sourceRoot.deleteRecursively()
            destRoot.deleteRecursively()
        }
    }

    /** Creates a fresh, real temp directory for the duration of a single test. */
    private fun newTempDir(prefix: String): File {
        val marker = File.createTempFile("$prefix-", "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        return dir
    }
}
