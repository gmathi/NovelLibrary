package io.github.gmathi.novellibrary.util.storage

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Property-based tests for [StorageLocation] serialization.
 */
class StorageLocationTest {

    /**
     * Feature: download-storage-location, Property 1: Storage location serialization round trip
     *
     * For any StorageLocation value (Internal, or SdCard with any volume id), deserializing its
     * serialized form produces an equal value.
     *
     * Validates: Requirements 1.1, 1.3
     */
    @Property(tries = 100)
    fun `serialize then deserialize round trips to the original value`(
        @ForAll("storageLocations") location: StorageLocation
    ) {
        val roundTripped = StorageLocation.deserialize(location.serialize())
        assertEquals(location, roundTripped)
    }

    /**
     * Feature: download-storage-location, Property 1: Storage location serialization round trip
     *
     * Any null or unrecognized stored value deserializes to Internal.
     *
     * Validates: Requirements 1.1, 1.3
     */
    @Property(tries = 100)
    fun `null or garbage values deserialize to Internal`(
        @ForAll("nullOrGarbageValues") value: String?
    ) {
        assertEquals(StorageLocation.Internal, StorageLocation.deserialize(value))
    }

    @Provide
    fun storageLocations(): Arbitrary<StorageLocation> {
        val internal: Arbitrary<StorageLocation> = Arbitraries.just(StorageLocation.Internal)
        val sdCard: Arbitrary<StorageLocation> = Arbitraries.strings()
            .ofMinLength(0)
            .ofMaxLength(32)
            // displayPath is not part of the serialized form, so deserialize() always
            // reconstructs it as "". Only volumeId round trips.
            .map { volumeId -> StorageLocation.SdCard(volumeId, "") }
        return Arbitraries.oneOf(internal, sdCard)
    }

    @Provide
    fun nullOrGarbageValues(): Arbitrary<String?> {
        val garbage: Arbitrary<String> = Arbitraries.strings()
            .ofMinLength(0)
            .ofMaxLength(32)
            .filter { it != StorageLocation.INTERNAL_TOKEN && !it.startsWith(StorageLocation.SD_PREFIX) }
        return garbage.injectNull(0.2)
    }
}
