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
 */
class DownloadNovelThread(
    val context: Context,
    val novelId: Long,
    val dbHelper: DBHelper,
    private val downloadListener: DownloadListener

) : Thread(), DownloadListener {

    companion object {
        private const val TAG = "DownloadNovelThread"
    }

    override fun run() {
        try {
            var download = dbHelper.getDownloadItemInQueue(novelId)

            while (download != null && !interrupted()) {

                if (!NetworkHelper(context).isConnectedToNetwork())
                    throw InterruptedException(Constants.NO_NETWORK)

                // Run the chapter download directly on this thread (no pool needed)
                DownloadWebPageThread(context, download, dbHelper, this@DownloadNovelThread).run()

                // Check if thread was interrupted during the download
                if (interrupted()) return

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