package io.github.gmathi.novellibrary.service.download

import android.app.IntentService
import android.content.Intent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.updateDownloadStatus
import io.github.gmathi.novellibrary.model.Download
import io.github.gmathi.novellibrary.model.DownloadActionEvent
import io.github.gmathi.novellibrary.model.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.EventType
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class DownloadNovelService : IntentService(TAG) {

    private var threadListMap = HashMap<String, DownloadNovelThread?>()
    private val futures = ArrayList<Future<Any>>()

    private lateinit var threadPool: ThreadPoolExecutor
    private lateinit var dbHelper: DBHelper

    //static components
    companion object {
        const val TAG = "DownloadNovelService"
        const val QUALIFIED_NAME = "io.github.gmathi.novellibrary.service.download.DownloadNovelService"
        const val MAX_PARALLEL_DOWNLOADS = 5

        const val NOVEL_NAME = "name"
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
    }


    override fun onHandleIntent(workIntent: Intent) {
        //android.os.Debug.waitForDebugger()


        dbHelper = DBHelper.getInstance(this)
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }


        val novelName = workIntent.getStringExtra(NOVEL_NAME)

        //Initialize Thread Pool
        threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor

        val downloadNovelThread = DownloadNovelThread(this, novelName, dbHelper)
        threadListMap[novelName] = downloadNovelThread

        futures.add(threadPool.submit(downloadNovelThread, null as Any?))

        while (!futures.isEmpty()) {
            futures[0].get()
            futures.removeAt(0)
        }

        threadPool.shutdown()
    }

    @Subscribe
    fun onActionReceived(event: DownloadActionEvent) {
        doAction(event.novelName, event.action)
    }

    private fun doAction(novelName: String, action: String) {
        if (action == ACTION_START) {
            var downloadNovelThread = threadListMap[novelName]
            if (downloadNovelThread != null && downloadNovelThread.isAlive) {
                return
            }

            downloadNovelThread = DownloadNovelThread(this, novelName, dbHelper)
            threadListMap[novelName] = downloadNovelThread
            futures.add(threadPool.submit(downloadNovelThread, null as Any?))
            if (futures.size > 5)
                EventBus.getDefault().post(DownloadNovelEvent(EventType.INSERT, novelName))
            else
                EventBus.getDefault().post(DownloadNovelEvent(EventType.RUNNING, novelName))

        } else if (action == ACTION_PAUSE) {
            val downloadNovelThread = threadListMap[novelName]
            if (downloadNovelThread != null && downloadNovelThread.isAlive) {
                downloadNovelThread.interrupt()
                threadListMap[novelName] = null
            }
        }
    }


    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        dbHelper.updateDownloadStatus(Download.STATUS_PAUSED)
        super.onDestroy()
    }


}
