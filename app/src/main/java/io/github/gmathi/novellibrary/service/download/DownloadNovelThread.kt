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
import io.github.gmathi.novellibrary.util.Logs
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadNovelThread(
    val context: Context,
    val novelId: Long,
    val dbHelper: DBHelper,
    private val downloadListener: DownloadListener

) : Thread(), DownloadListener {

    private var threadPool: ThreadPoolExecutor? = null

    companion object {
        private const val TAG = "DownloadNovelThread"
    }

    override fun run() {
        try {
            var download = dbHelper.getDownloadItemInQueue(novelId)
            threadPool = Executors.newFixedThreadPool(10) as ThreadPoolExecutor
//            val fasterDownloads = DataCenter(context).experimentalDownload

            while (download != null && !interrupted()) {

                if (!NetworkHelper(context).isConnectedToNetwork()) throw InterruptedException(Constants.NO_NETWORK)

//                if (!fasterDownloads)
                // .get at the end makes it a synchronous task
                threadPool?.submit(DownloadWebPageThread(context, download, dbHelper, this@DownloadNovelThread))?.get()
//                else
                // Put all in at the same time - TODO:// Problem with multitasking when adding 1000+ chapters at the same time. Need to streamline it for multitasking
//                    threadPool?.submit(DownloadWebPageThread(context, download, dbHelper, this@DownloadNovelThread))

                //Check if thread was shutdown
                if (interrupted()) {
                    threadPool?.shutdownNow(); return
                }

                download = dbHelper.getDownloadItemInQueue(novelId)
            }

            threadPool?.shutdown()
            try {
//                if (!fasterDownloads)
                threadPool?.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
//                else {
//                    if (threadPool != null)
//                        while (!threadPool!!.awaitTermination(1, TimeUnit.MINUTES) && !isInterrupted) {
//                            Thread.sleep(60000)
//                        }
//                }

                if (dbHelper.getRemainingDownloadsCountForNovel(novelId) == 0)
                    downloadListener.handleEvent(DownloadNovelEvent(EventType.DELETE, novelId))
                else
                    downloadListener.handleEvent(DownloadNovelEvent(EventType.PAUSED, novelId))

            } catch (e: InterruptedException) {
                Logs.warning(TAG, "Thread pool executor interrupted~")
            }

        } catch (e: InterruptedException) {
            threadPool?.shutdownNow()
        } catch (e: Exception) {
            threadPool?.shutdownNow()
        }
    }

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        downloadListener.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        downloadListener.handleEvent(downloadWebPageEvent)
    }

    override fun interrupt() {
        super.interrupt()
        threadPool?.shutdownNow()
    }

}