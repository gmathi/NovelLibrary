package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Property-based tests for [StorageMigrator.computeDestinationPath].
 */
class StorageMigratorDestinationPathPropertyTest {

    /** Arbitrary filesystem-safe path segment (root/dir/file name component). */
    private fun pathSegmentArbitrary(): Arbitrary<String> =
        Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(12)

    @Provide
    fun destinationPathInputs(): Arbitrary<DestinationPathInput> =
        Combinators.combine(
            pathSegmentArbitrary(), // oldRootName
            pathSegmentArbitrary(), // newRootName
            pathSegmentArbitrary(), // novelDirName
            pathSegmentArbitrary()  // fileName
        ).`as` { oldRootName, newRootName, novelDirName, fileName ->
            DestinationPathInput(oldRootName, newRootName, novelDirName, fileName)
        }

    data class DestinationPathInput(
        val oldRootName: String,
        val newRootName: String,
        val novelDirName: String,
        val fileName: String
    )

    // Feature: download-storage-location, Property 6: Migration rewrites every path under the new root preserving structure
    @Property(tries = 100)
    fun computeDestinationPathMapsEveryChapterUnderTheNewRootPreservingStructure(
        @ForAll("destinationPathInputs") input: DestinationPathInput
    ) {
        val oldRoot = File(System.getProperty("java.io.tmpdir"), "migration-old-${input.oldRootName}")
        val newRoot = File(System.getProperty("java.io.tmpdir"), "migration-new-${input.newRootName}")

        val oldFilePath = File(File(oldRoot, input.novelDirName), input.fileName).absolutePath

        val destinationPath = StorageMigrator.computeDestinationPath(oldRoot, newRoot, oldFilePath)

        assertNotNull(destinationPath, "expected a destination path for a file located under oldRoot")

        val destinationFile = File(destinationPath!!)
        val newRootWithSeparator = if (newRoot.absolutePath.endsWith(File.separator)) {
            newRoot.absolutePath
        } else {
            newRoot.absolutePath + File.separator
        }

        // Every computed destination is located under the new root.
        assertTrue(
            destinationFile.absolutePath.startsWith(newRootWithSeparator),
            "expected $destinationPath to be located under $newRootWithSeparator"
        )

        // The "<novelDir>/<fileName>" suffix relative to the root is preserved.
        val expectedSuffix = File(input.novelDirName, input.fileName).path
        val actualSuffix = destinationFile.absolutePath.substring(newRootWithSeparator.length)
        assertEquals(
            expectedSuffix,
            actualSuffix,
            "expected the <novelDir>/<fileName> suffix to be preserved relative to the new root"
        )
    }
}
