package io.github.gmathi.novellibrary.service.download

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getDownloadItemInQueue
import io.github.gmathi.novellibrary.database.getRemainingDownloadsCountForNovel
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.logging.Logs


/**
 * Downloads chapters for a single novel sequentially.
 *
 * The previous implementation created a 10-thread pool but called `.get()` on each
 * submitted future, making it effectively single-threaded. This version runs each
 * chapter download directly on the current thread, avoiding the unnecessary pool overhead.
 *
 * Includes rate limiting: a base delay between every chapter download, plus exponential
 * backoff when Cloudflare challenges are detected, to avoid IP bans.
 */
class DownloadNovelThread(
    val context: Context,
    val novelId: Long,
    val dbHelper: DBHelper,
    private val downloadListener: DownloadListener

) : Thread(), DownloadListener {

    companion object {
        private const val TAG = "DownloadNovelThread"

        /** Base delay between chapter downloads (ms). Prevents rapid-fire requests. */
        private const val BASE_DELAY_MS = 500L

        /** Initial backoff delay when Cloudflare is detected (ms). */
        private const val CF_INITIAL_BACKOFF_MS = 5_000L

        /** Maximum backoff delay (ms). Caps at ~80 seconds. */
        private const val CF_MAX_BACKOFF_MS = 80_000L

        /** After this many consecutive Cloudflare hits, pause the novel download. */
        private const val CF_MAX_CONSECUTIVE_FAILURES = 5
    }

    override fun run() {
        try {
            var download = dbHelper.getDownloadItemInQueue(novelId)
            var consecutiveCfFailures = 0
            var cfBackoffMs = CF_INITIAL_BACKOFF_MS

            while (download != null && !interrupted()) {

                if (!NetworkHelper(context).isConnectedToNetwork())
                    throw InterruptedException(Constants.NO_NETWORK)

                // Run the chapter download directly on this thread (no pool needed)
                val webPageThread = DownloadWebPageThread(context, download, dbHelper, this@DownloadNovelThread)
                webPageThread.run()

                // Check if thread was interrupted during the download
                if (interrupted()) return

                // Rate limiting: if Cloudflare was detected, back off exponentially
                if (webPageThread.cloudflareDetected) {
                    consecutiveCfFailures++
                    Logs.warning(TAG, "Cloudflare detected for novelId=$novelId " +
                            "(consecutive=$consecutiveCfFailures, backoff=${cfBackoffMs}ms)")

                    if (consecutiveCfFailures >= CF_MAX_CONSECUTIVE_FAILURES) {
                        Logs.warning(TAG, "Too many consecutive Cloudflare failures for novelId=$novelId, pausing download")
                        break
                    }

                    // Sleep with exponential backoff; check for interruption
                    sleep(cfBackoffMs)
                    cfBackoffMs = (cfBackoffMs * 2).coerceAtMost(CF_MAX_BACKOFF_MS)
                } else {
                    // Successful download — reset backoff and apply base rate limit
                    consecutiveCfFailures = 0
                    cfBackoffMs = CF_INITIAL_BACKOFF_MS
                    sleep(BASE_DELAY_MS)
                }

                download = dbHelper.getDownloadItemInQueue(novelId)
            }

            if (dbHelper.getRemainingDownloadsCountForNovel(novelId) == 0)
                downloadListener.handleEvent(DownloadNovelEvent(EventType.DELETE, novelId))
            else
                downloadListener.handleEvent(DownloadNovelEvent(EventType.PAUSED, novelId))

        } catch (e: InterruptedException) {
            Logs.warning(TAG, "Thread interrupted: novelId=$novelId")
        } catch (e: Exception) {
            Logs.error(TAG, "Unexpected error downloading novel $novelId", e)
        }
    }

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        downloadListener.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        downloadListener.handleEvent(downloadWebPageEvent)
    }

}