package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Property-based tests for [StorageLocationResolver.listOptions].
 */
class StorageLocationResolverListOptionsPropertyTest {

    private val internalRoot = File("/data/data/io.github.gmathi.novellibrary/files")

    /** A candidate directory (as returned by `getExternalFilesDirs`) for a given SD volume id. */
    private fun candidateDirFor(volumeId: String): File =
        File("/storage/$volumeId/Android/data/io.github.gmathi.novellibrary/files")

    /** Arbitrary volume id, e.g. "AAAA-1111". Avoids the reserved "emulated" id. */
    private fun volumeIdArbitrary(): Arbitrary<String> =
        Arbitraries.integers().between(0, 9999)
            .map { n -> "SD-%04d".format(n) }

    /** A set of distinct available volume ids, of size 0..5 (including the empty set). */
    @Provide
    fun volumeIdSets(): Arbitrary<Set<String>> =
        volumeIdArbitrary().set().ofMaxSize(5)

    /** A configured location: Internal, an available-ish SD volume, or an arbitrary SD volume. */
    @Provide
    fun configuredLocations(): Arbitrary<StorageLocation> {
        val internal = Arbitraries.just(StorageLocation.Internal as StorageLocation)
        val sdCard = volumeIdArbitrary().map { id -> StorageLocation.SdCard(id, "") as StorageLocation }
        return Arbitraries.oneOf(internal, sdCard)
    }

    // Feature: download-storage-location, Property 4: Option listing reflects volumes, active marker, and availability
    @Property(tries = 100)
    fun optionListingReflectsVolumesActiveMarkerAndAvailability(
        @ForAll("volumeIdSets") availableVolumeIds: Set<String>,
        @ForAll("configuredLocations") configured: StorageLocation
    ) {
        val candidateDirs = availableVolumeIds.map { candidateDirFor(it) }

        val options = StorageLocationResolver.listOptions(
            configured = configured,
            internalRoot = internalRoot,
            candidateDirs = candidateDirs,
            freeSpace = { 0L },
            totalSpace = { 0L }
        )

        // Internal + exactly one option per available volume, plus one more if the configured
        // SD volume is absent from the available set.
        val configuredAbsentAddsOption =
            configured is StorageLocation.SdCard && configured.volumeId !in availableVolumeIds
        val expectedSize = 1 + availableVolumeIds.size + if (configuredAbsentAddsOption) 1 else 0
        assertEquals(expectedSize, options.size, "expected Internal + one per available volume (+ absent configured)")

        val internalOptions = options.filter { it.location is StorageLocation.Internal }
        assertEquals(1, internalOptions.size, "exactly one Internal option must be present")

        val sdOptionsByVolumeId = options
            .mapNotNull { it.location as? StorageLocation.SdCard }
            .associateBy { it.volumeId }
        // One option per available volume.
        for (id in availableVolumeIds) {
            assertTrue(sdOptionsByVolumeId.containsKey(id), "expected an option for available volume $id")
        }

        // Exactly one option is marked active.
        val activeOptions = options.filter { it.isActive }
        assertEquals(1, activeOptions.size, "exactly one option must be marked active")

        val configuredAvailable =
            configured is StorageLocation.SdCard && configured.volumeId in availableVolumeIds
        if (configuredAvailable) {
            val activeVolumeId = (configured as StorageLocation.SdCard).volumeId
            assertTrue(
                activeOptions.single().location.let { it is StorageLocation.SdCard && it.volumeId == activeVolumeId },
                "the active option must match the configured, available SD volume"
            )
        } else {
            assertTrue(
                activeOptions.single().location is StorageLocation.Internal,
                "Internal must be active when the configured location is Internal or an unavailable SD volume"
            )
        }

        // A configured-but-absent SD volume still appears, marked unavailable.
        if (configured is StorageLocation.SdCard && configured.volumeId !in availableVolumeIds) {
            val absentOption = options.firstOrNull {
                val loc = it.location
                loc is StorageLocation.SdCard && loc.volumeId == configured.volumeId
            }
            assertTrue(absentOption != null, "the configured-but-absent SD volume must still appear")
            assertFalse(absentOption!!.isAvailable, "the configured-but-absent SD volume must be marked unavailable")
            assertFalse(absentOption.isActive, "the configured-but-absent SD volume must not be marked active")
        }

        // All available (non-configured-absent) options must be marked available.
        for (option in options) {
            val isConfiguredAbsent = configured is StorageLocation.SdCard &&
                configured.volumeId !in availableVolumeIds &&
                option.location.let { it is StorageLocation.SdCard && it.volumeId == configured.volumeId }
            if (!isConfiguredAbsent) {
                assertTrue(option.isAvailable, "option ${option.location} should be marked available")
            }
        }

        // Empty volume set boundary: only Internal (unless the configured-absent SD volume adds one).
        if (availableVolumeIds.isEmpty() && configured !is StorageLocation.SdCard) {
            assertEquals(1, options.size, "empty volume set with Internal configured must yield only Internal")
            assertTrue(options.single().location is StorageLocation.Internal)
        }
    }
}
