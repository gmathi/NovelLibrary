package io.github.gmathi.novellibrary.service.download

import android.content.Context
import android.util.Log
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getDownloadItemInQueue
import io.github.gmathi.novellibrary.database.getRemainingDownloadsCountForNovel
import io.github.gmathi.novellibrary.model.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class DownloadNovelThread(val context: Context, val novelName: String, val dbHelper: DBHelper) : Thread() {

    private var threadPool: ThreadPoolExecutor? = null

    companion object {
        private const val TAG = "DownloadNovelThread"
    }

    override fun run() {
        try {
            var download = dbHelper.getDownloadItemInQueue(novelName)
            threadPool = Executors.newFixedThreadPool(10) as ThreadPoolExecutor

            while (download != null) {

                if (!Utils.isConnectedToNetwork(context)) throw InterruptedException(Constants.NO_NETWORK)
                threadPool?.submit(DownloadWebPageThread(context, download, dbHelper))?.get()

                download = dbHelper.getDownloadItemInQueue(novelName)
            }

            threadPool?.shutdown()
            try {
                threadPool?.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)

                if (dbHelper.getRemainingDownloadsCountForNovel(novelName) == 0)
                    EventBus.getDefault().post(DownloadNovelEvent(EventType.DELETE, novelName))
                else
                    EventBus.getDefault().post(DownloadNovelEvent(EventType.PAUSED, novelName))

            } catch (e: InterruptedException) {
                Logs.warning(TAG, "Thread pool executor interrupted~")
            }

        } catch (e: InterruptedException) {
            Logs.warning(TAG, "Interrupting the Thread: $novelName, ${e.localizedMessage}")
            threadPool?.shutdownNow()
        } catch (e: Exception) {
            Logs.error(TAG, "This is really bad!!", e)
            threadPool?.shutdownNow()
        }
    }

}