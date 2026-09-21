package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Property-based tests for [StorageLocationResolver.resolveWritableRoot].
 *
 * Uses the pure overload of `resolveWritableRoot`, which takes the configured [StorageLocation]
 * and the candidate SD volume directories directly, so SD presence/absence can be generated
 * without real hardware.
 */
class StorageLocationResolverPropertyTest {

    private val internalRoot = File("/data/data/io.github.gmathi.novellibrary/files")

    /** Arbitrary volume id, e.g. "AAAA-1111". */
    private fun volumeIdArbitrary(): Arbitrary<String> =
        Arbitraries.integers().between(0, 9999).map { n -> "SD-%04d".format(n) }

    /**
     * A candidate directory (as returned by `getExternalFilesDirs`) for a given SD volume id.
     *
     * [StorageLocationResolver.extractVolumeId] matches the literal `/storage/` path segment
     * against `File.absolutePath`. On real Android devices paths always use `/`, but a plain
     * `java.io.File` built on a Windows JVM normalizes to `\` and drops the leading slash
     * (`/storage/X` becomes `C:\storage\X`), which would never match and would make every SD
     * volume look unavailable regardless of host OS. [AndroidStylePathFile] overrides the path
     * accessors to always return the Android-style forward-slash form, so this test exercises
     * the real matching logic the same way on every host OS.
     */
    private fun candidateDirFor(volumeId: String): File =
        AndroidStylePathFile("/storage/$volumeId/Android/data/io.github.gmathi.novellibrary/files")

    /** A [File] whose path accessors always return [androidPath] verbatim, bypassing host-OS normalization. */
    private class AndroidStylePathFile(private val androidPath: String) : File(androidPath) {
        override fun getPath(): String = androidPath
        override fun getAbsolutePath(): String = androidPath
        override fun getCanonicalPath(): String = androidPath
        override fun equals(other: Any?): Boolean = other is File && other.path == androidPath
        override fun hashCode(): Int = androidPath.hashCode()
        override fun toString(): String = androidPath
    }

    @Provide
    fun configuredLocations(): Arbitrary<StorageLocation> {
        val internal = Arbitraries.just(StorageLocation.Internal as StorageLocation)
        val sdCard = volumeIdArbitrary().map { id -> StorageLocation.SdCard(id, "") as StorageLocation }
        return Arbitraries.oneOf(internal, sdCard)
    }

    /** A set of distinct available volume ids, of size 0..5 (including the empty set). */
    @Provide
    fun availableVolumeIdSets(): Arbitrary<Set<String>> =
        volumeIdArbitrary().set().ofMaxSize(5)

    // Feature: download-storage-location, Property 2: Configured location resolves to a valid writable root with safe fallback
    @Property(tries = 100)
    fun resolveWritableRootAlwaysReturnsAWritableRootWithSafeFallback(
        @ForAll("configuredLocations") configured: StorageLocation,
        @ForAll("availableVolumeIdSets") availableVolumeIds: Set<String>
    ) {
        val candidateDirs = availableVolumeIds.map { candidateDirFor(it) }

        val resolved = StorageLocationResolver.resolveWritableRoot(
            configured = configured,
            internalRoot = internalRoot,
            candidateDirs = candidateDirs
        )

        // Always resolves to a non-null, usable root. resolveWritableRoot must never throw, which
        // is implicitly asserted by reaching this line without an exception propagating.
        assertNotNull(resolved.root, "resolveWritableRoot returned a null root")

        val configuredSdVolumeUnavailable =
            configured is StorageLocation.SdCard && configured.volumeId !in availableVolumeIds

        // didFallback is true exactly when a configured SD volume is unavailable.
        assertEquals(
            configuredSdVolumeUnavailable,
            resolved.didFallback,
            "didFallback should be true exactly when the configured SD volume is unavailable " +
                "(configured=$configured, availableVolumeIds=$availableVolumeIds)"
        )

        if (configured is StorageLocation.Internal || configuredSdVolumeUnavailable) {
            // Internal, or an unavailable/unknown SD volume -> root is internal storage.
            assertEquals(internalRoot, resolved.root, "expected fallback to the internal root")
            assertTrue(resolved.location is StorageLocation.Internal, "expected resolved location to be Internal")
        } else {
            // Configured SD volume is available -> root is its writable app-specific directory.
            val expectedSdDir = candidateDirFor((configured as StorageLocation.SdCard).volumeId)
            assertEquals(expectedSdDir, resolved.root, "expected the SD volume's writable directory")
            assertEquals(configured, resolved.location, "expected resolved location to match configured")
        }
    }
}
