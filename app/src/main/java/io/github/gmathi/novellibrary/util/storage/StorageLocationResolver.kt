package io.github.gmathi.novellibrary.util.storage

import android.content.Context
import androidx.core.content.ContextCompat
import io.github.gmathi.novellibrary.model.preference.DataCenter
import java.io.File

/**
 * Owns all preference reads and SD volume re-resolution for the download storage location feature.
 *
 * The logic-heavy parts ([resolveWritableRoot], [resolveSdWritableDir]) are exposed in two forms:
 *  - a [Context]-based overload used in production, and
 *  - a pure overload that takes the candidate volume directories (the
 *    `Context.getExternalFilesDirs()` entries) directly.
 *
 * The pure overloads are the test seam: SD presence/absence can be generated/injected without
 * real hardware by passing a synthetic candidate-dir list.
 */
object StorageLocationResolver {

    /** The location the user selected (may currently be unavailable). */
    fun getConfiguredLocation(dataCenter: DataCenter): StorageLocation =
        StorageLocation.deserialize(dataCenter.downloadStorageLocation)

    /** Persist a new active location. */
    fun setConfiguredLocation(dataCenter: DataCenter, location: StorageLocation) {
        dataCenter.downloadStorageLocation = location.serialize()
    }

    /**
     * Resolve the active writable root directory.
     *
     * Returns `context.filesDir` for [StorageLocation.Internal], or the app-specific directory on
     * the selected SD volume. Falls back to `context.filesDir` when the configured SD volume is
     * not currently available. Never throws.
     */
    fun resolveWritableRoot(context: Context, dataCenter: DataCenter): ResolvedRoot =
        resolveWritableRoot(
            configured = getConfiguredLocation(dataCenter),
            internalRoot = context.filesDir,
            candidateDirs = externalFilesDirs(context)
        )

    /**
     * Pure resolution used by both production and tests.
     *
     * @param configured the location the user selected.
     * @param internalRoot the always-available internal root (`context.filesDir`).
     * @param candidateDirs the current `getExternalFilesDirs` entries (the writable app-specific
     *        directories on each mounted volume).
     *
     * Guarantees: returns a non-null root and never throws. The root is internal storage for
     * [StorageLocation.Internal] or an unavailable SD volume; [ResolvedRoot.didFallback] is true
     * exactly when an SD volume was configured but is unavailable.
     */
    fun resolveWritableRoot(
        configured: StorageLocation,
        internalRoot: File,
        candidateDirs: List<File>
    ): ResolvedRoot = when (configured) {
        is StorageLocation.Internal ->
            ResolvedRoot(root = internalRoot, location = StorageLocation.Internal, didFallback = false)

        is StorageLocation.SdCard -> {
            val sdDir = resolveSdWritableDir(candidateDirs, configured.volumeId)
            if (sdDir != null) {
                ResolvedRoot(root = sdDir, location = configured, didFallback = false)
            } else {
                // Configured SD volume is unavailable → safe fallback to internal storage.
                ResolvedRoot(root = internalRoot, location = StorageLocation.Internal, didFallback = true)
            }
        }
    }

    /**
     * All selectable options for the settings screen, with availability + free space.
     *
     * The list always starts with [StorageLocation.Internal] followed by one option per available
     * removable SD volume. When the configured location is an SD volume that is not currently
     * mounted, that volume still appears (marked unavailable) so the UI can surface it. Exactly one
     * option is marked active: the configured location when it is available, otherwise Internal
     * (mirroring the internal-storage fallback in [resolveWritableRoot]).
     *
     * When no removable SD volume is available the result is just [StorageLocation.Internal]; the
     * UI derives its "no removable storage detected" indication from the absence of any available
     * SD option.
     */
    fun listOptions(context: Context, dataCenter: DataCenter): List<StorageOption> =
        listOptions(
            configured = getConfiguredLocation(dataCenter),
            internalRoot = context.filesDir,
            candidateDirs = externalFilesDirs(context)
        )

    /**
     * Pure option enumeration used by both production and tests.
     *
     * @param configured the location the user selected.
     * @param internalRoot the always-available internal root (`context.filesDir`).
     * @param candidateDirs the current `getExternalFilesDirs` entries; the primary emulated entry
     *        is filtered out, leaving the removable SD volumes.
     * @param freeSpace usable free bytes for a directory (injected so tests need no real device).
     * @param totalSpace total bytes for a directory (injected so tests need no real device).
     */
    fun listOptions(
        configured: StorageLocation,
        internalRoot: File,
        candidateDirs: List<File>,
        freeSpace: (File) -> Long = DiskUtil::getAvailableSpace,
        totalSpace: (File) -> Long = DiskUtil::getTotalSpace
    ): List<StorageOption> {
        // Available removable SD volumes, keyed by stable volume id (deduped, first occurrence wins).
        val availableById = LinkedHashMap<String, File>()
        for (dir in candidateDirs) {
            val id = extractVolumeId(dir) ?: continue
            if (id == EMULATED_VOLUME_ID) continue // primary emulated external storage is not removable
            if (!availableById.containsKey(id)) availableById[id] = dir
        }

        val configuredSd = configured as? StorageLocation.SdCard
        val configuredAvailable = configuredSd != null && availableById.containsKey(configuredSd.volumeId)
        // Active = configured when available, otherwise Internal (the resolveWritableRoot fallback).
        val activeIsInternal = !configuredAvailable
        val activeVolumeId = if (configuredAvailable) configuredSd.volumeId else null

        val options = ArrayList<StorageOption>(availableById.size + 2)

        // 1) Internal — always first, always available.
        options += StorageOption(
            location = StorageLocation.Internal,
            label = INTERNAL_LABEL,
            freeBytes = freeSpace(internalRoot),
            totalBytes = totalSpace(internalRoot),
            isActive = activeIsInternal,
            isAvailable = true
        )

        // 2) One option per available removable SD volume.
        for ((id, dir) in availableById) {
            val displayPath = dir.absolutePath.substringBefore("/Android/")
            options += StorageOption(
                location = StorageLocation.SdCard(id, displayPath),
                label = sdLabel(id),
                freeBytes = freeSpace(dir),
                totalBytes = totalSpace(dir),
                isActive = !activeIsInternal && activeVolumeId == id,
                isAvailable = true
            )
        }

        // 3) Configured-but-absent SD volume still appears, marked unavailable.
        if (configuredSd != null && !configuredAvailable) {
            options += StorageOption(
                location = configuredSd,
                label = sdLabel(configuredSd.volumeId),
                freeBytes = 0L,
                totalBytes = 0L,
                isActive = false,
                isAvailable = false
            )
        }

        return options
    }

    /** True if the configured SD volume is currently mounted & writable (Internal is always true). */
    fun isConfiguredLocationAvailable(context: Context, dataCenter: DataCenter): Boolean =
        isConfiguredLocationAvailable(getConfiguredLocation(dataCenter), externalFilesDirs(context))

    /** Pure availability check used by both production and tests. */
    fun isConfiguredLocationAvailable(
        configured: StorageLocation,
        candidateDirs: List<File>
    ): Boolean = when (configured) {
        is StorageLocation.Internal -> true
        is StorageLocation.SdCard -> resolveSdWritableDir(candidateDirs, configured.volumeId) != null
    }

    /** Re-resolve a volumeId to its current writable app-specific dir, or null if absent. */
    fun resolveSdWritableDir(context: Context, volumeId: String): File? =
        resolveSdWritableDir(externalFilesDirs(context), volumeId)

    /**
     * Pure volume matching used by both production and tests.
     *
     * Matches [volumeId] against the path segment after `/storage/` in each candidate directory
     * (e.g. `/storage/ABCD-1234/Android/data/<pkg>/files` → id `ABCD-1234`).
     */
    fun resolveSdWritableDir(candidateDirs: List<File>, volumeId: String): File? =
        candidateDirs.firstOrNull { extractVolumeId(it) == volumeId }

    /**
     * Extract the stable volume id from a `getExternalFilesDirs` entry: the path segment directly
     * after `/storage/`. Returns null when the path is not an external-volume path.
     */
    fun extractVolumeId(dir: File): String? {
        // Normalize to forward slashes first: java.io.File renders platform-native separators in
        // absolutePath (backslashes on Windows), but getExternalFilesDirs entries are always
        // "/storage/..." paths on-device. Normalizing keeps this matchable under Windows JVM unit
        // tests as well as on-device.
        val path = dir.absolutePath.replace('\\', '/')
        if (!path.contains("/storage/")) return null
        val id = path.substringAfter("/storage/").substringBefore("/")
        return id.ifEmpty { null }
    }

    /** The current candidate writable app-specific directories (one per mounted volume). */
    private fun externalFilesDirs(context: Context): List<File> =
        ContextCompat.getExternalFilesDirs(context, null).filterNotNull()

    /** Human-readable label for a removable SD volume, e.g. `SD card (ABCD-1234)`. */
    private fun sdLabel(volumeId: String): String = "$SD_LABEL_PREFIX ($volumeId)"

    /** Volume id of the primary emulated external storage, which is not a removable SD volume. */
    private const val EMULATED_VOLUME_ID = "emulated"

    /** Display label for internal storage. */
    const val INTERNAL_LABEL = "Internal storage"

    /** Display label prefix for a removable SD volume. */
    const val SD_LABEL_PREFIX = "SD card"
}

/** Result of resolution: the root actually used, and whether a fallback occurred. */
data class ResolvedRoot(
    val root: File,
    val location: StorageLocation,   // the location actually used
    val didFallback: Boolean         // true if configured SD was unavailable and we used Internal
)

/** A selectable location for the settings screen, with availability + free space. */
data class StorageOption(
    val location: StorageLocation,
    val label: String,
    val freeBytes: Long,
    val totalBytes: Long,
    val isActive: Boolean,
    val isAvailable: Boolean
)
