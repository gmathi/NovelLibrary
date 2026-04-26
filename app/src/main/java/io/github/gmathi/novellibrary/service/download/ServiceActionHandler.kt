package io.github.gmathi.novellibrary.service.download

/**
 * Abstraction that lets the ViewModel trigger download-service actions
 * without holding a direct reference to the service or the Activity.
 * The Activity implements this and sets it on the ViewModel after creation.
 */
interface ServiceActionHandler {
    /** Forward an action (ACTION_START, ACTION_PAUSE, ACTION_REMOVE) to the service for [novelId]. */
    fun handleAction(novelId: Long, action: String)

    /** Start the download service for [novelId] when it is not already running. */
    fun startService(novelId: Long)

    /** Check whether the [DownloadNovelService] is currently running. */
    fun isServiceRunning(): Boolean
}
