package io.github.gmathi.novellibrary.activity.settings

import io.github.gmathi.novellibrary.util.storage.StorageLocation
import io.github.gmathi.novellibrary.util.storage.StorageOption
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse

/**
 * Property-based tests for the click-handling guard in [StorageSettingsActivity.onItemClick].
 *
 * [StorageSettingsActivity] is a real Android `Activity` and can't be instantiated in a plain
 * JVM unit test, so this test mirrors the exact guard logic of `onItemClick` in a small,
 * directly testable function. The mirrored logic must stay in sync with:
 *
 * ```
 * override fun onItemClick(item: StorageOption, position: Int) {
 *     if (isMigrating) return
 *     // Selecting the already-active location is a no-op (Req 2.6).
 *     if (item.isActive) return
 *     confirmLocationChange(item)
 * }
 * ```
 */
class StorageSettingsActivityItemClickPropertyTest {

    /**
     * Mirrors [StorageSettingsActivity.onItemClick]'s guard logic. [onConfirm] stands in for
     * `confirmLocationChange`, which is what eventually triggers [StorageMigrator][io.github.gmathi.novellibrary.util.storage.StorageMigrator.migrate]
     * and persists the new active location.
     */
    private fun handleItemClick(item: StorageOption, isMigrating: Boolean, onConfirm: (StorageOption) -> Unit) {
        if (isMigrating) return
        if (item.isActive) return
        onConfirm(item)
    }

    /** Arbitrary volume id, e.g. "SD-1234". */
    private fun volumeIdArbitrary(): Arbitrary<String> =
        Arbitraries.integers().between(0, 9999).map { n -> "SD-%04d".format(n) }

    /** Arbitrary [StorageLocation], covering both Internal and SdCard. */
    private fun storageLocationArbitrary(): Arbitrary<StorageLocation> {
        val internal = Arbitraries.just(StorageLocation.Internal as StorageLocation)
        val sdCard = volumeIdArbitrary().map { id -> StorageLocation.SdCard(id, "") as StorageLocation }
        return Arbitraries.oneOf(internal, sdCard)
    }

    /** An arbitrary [StorageOption] with [StorageOption.isActive] fixed to `true`. */
    @Provide
    fun activeStorageOptions(): Arbitrary<StorageOption> =
        Combinators.combine(
            storageLocationArbitrary(),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            Arbitraries.longs().between(0, Long.MAX_VALUE / 2),
            Arbitraries.longs().between(0, Long.MAX_VALUE / 2)
        ).`as` { location, label, freeBytes, totalBytes ->
            StorageOption(
                location = location,
                label = label,
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                isActive = true,
                isAvailable = true
            )
        }

    /** An arbitrary [StorageOption] with [StorageOption.isActive] fixed to `false`. */
    @Provide
    fun inactiveStorageOptions(): Arbitrary<StorageOption> =
        Combinators.combine(
            storageLocationArbitrary(),
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20),
            Arbitraries.longs().between(0, Long.MAX_VALUE / 2),
            Arbitraries.longs().between(0, Long.MAX_VALUE / 2),
            Arbitraries.of(true, false)
        ).`as` { location, label, freeBytes, totalBytes, isAvailable ->
            StorageOption(
                location = location,
                label = label,
                freeBytes = freeBytes,
                totalBytes = totalBytes,
                isActive = false,
                isAvailable = isAvailable
            )
        }

    // Feature: download-storage-location, Property 5: Selecting the already-active location is a no-op
    @Property(tries = 100)
    fun selectingTheActiveLocationIsANoOp(@ForAll("activeStorageOptions") activeOption: StorageOption) {
        var confirmCallCount = 0
        var persistedPreference = activeOption.location.serialize()

        val migrator = FakeMigrator()

        handleItemClick(activeOption, isMigrating = false) { target ->
            confirmCallCount++
            // What confirmLocationChange would eventually do: trigger migration and persist
            // the newly selected location. Neither must happen for an already-active option.
            migrator.migrate()
            persistedPreference = target.location.serialize()
        }

        assertEquals(0, confirmCallCount, "confirmLocationChange must never be invoked for the active option")
        assertFalse(migrator.migrateCalled, "no migration must start when the active option is re-selected")
        assertEquals(
            activeOption.location.serialize(),
            persistedPreference,
            "the persisted preference must remain unchanged when the active option is re-selected"
        )
    }

    // Contrast case: a non-active option (and isMigrating == false) must trigger the callback,
    // confirming the guard is specific to `isActive` rather than always short-circuiting.
    @Property(tries = 100)
    fun selectingANonActiveLocationTriggersConfirmation(@ForAll("inactiveStorageOptions") inactiveOption: StorageOption) {
        var confirmCallCount = 0

        handleItemClick(inactiveOption, isMigrating = false) { confirmCallCount++ }

        assertEquals(1, confirmCallCount, "confirmLocationChange must be invoked exactly once for a non-active option")
    }

    /** A minimal migration stand-in used only to observe whether migration was triggered. */
    private class FakeMigrator {
        var migrateCalled = false
            private set

        fun migrate() {
            migrateCalled = true
        }
    }
}
