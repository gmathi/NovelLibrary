package io.github.gmathi.novellibrary.util.storage

/**
 * A sealed representation of where downloaded chapters are rooted.
 *
 * The active value is persisted in [io.github.gmathi.novellibrary.model.preference.DataCenter]
 * via [serialize] and restored via [deserialize].
 */
sealed class StorageLocation {

    /** App internal storage: context.filesDir. Always available. */
    object Internal : StorageLocation()

    /**
     * A removable volume identified by its stable volume id (e.g. "XXXX-XXXX").
     * [displayPath] is the volume root for display only.
     */
    data class SdCard(val volumeId: String, val displayPath: String) : StorageLocation()

    /** Serialized form persisted in DataCenter. */
    fun serialize(): String = when (this) {
        Internal -> INTERNAL_TOKEN
        is SdCard -> "$SD_PREFIX$volumeId"
    }

    companion object {
        const val INTERNAL_TOKEN = "internal"
        const val SD_PREFIX = "sd:"

        fun deserialize(value: String?): StorageLocation =
            when {
                value == null || value == INTERNAL_TOKEN -> Internal
                value.startsWith(SD_PREFIX) -> SdCard(value.removePrefix(SD_PREFIX), "")
                else -> Internal // unknown/corrupt → safe default
            }
    }
}
