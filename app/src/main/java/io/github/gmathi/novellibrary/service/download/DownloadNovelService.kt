package io.github.gmathi.novellibrary.service.download

import android.app.IntentService
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NovelDownloadsActivity
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.updateDownloadStatus
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.service.TTSService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class DownloadNovelService : IntentService(TAG), DownloadListener {

    private var threadListMap = HashMap<String, DownloadNovelThread?>()
    private val futures = ArrayList<Future<Any>>()

    private lateinit var threadPool: ThreadPoolExecutor
    private lateinit var dbHelper: DBHelper

    //static components
    companion object {
        const val TAG = "DownloadNovelService"
        private const val DOWNLOAD_NOTIFICATION_GROUP = "downloadNotificationGroup"
        const val QUALIFIED_NAME = "io.github.gmathi.novellibrary.service.download.DownloadNovelService"

        const val MAX_PARALLEL_DOWNLOADS = 5

        const val NOVEL_NAME = "name"
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
    }

    private val binder = DownloadNovelBinder()
    @Volatile
    var downloadListener: DownloadListener? = null

    inner class DownloadNovelBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): DownloadNovelService = this@DownloadNovelService
    }

    //region Lifecycle functions

    override fun onCreate() {
        super.onCreate()
        Logs.debug(TAG, "onCreate")
        dbHelper = DBHelper.getInstance(this)
        threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
    }

    //Only called when an Activity is trying to connect to this service
    override fun onBind(intent: Intent): IBinder {
        Logs.debug(TAG, "onBind")
        return binder
    }

    //Only called when the Service is called as Background Service
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Logs.debug(TAG, "onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent) {
        Logs.debug(TAG, "onHandleIntent")
        val novelName = intent.getStringExtra(NOVEL_NAME)
        val downloadNovelThread = DownloadNovelThread(this, novelName, dbHelper, this@DownloadNovelService)
        threadListMap[novelName] = downloadNovelThread

        futures.add(threadPool.submit(downloadNovelThread, null as Any?))

        while (!futures.isEmpty()) {
            futures[0].get()
            futures.removeAt(0)
        }
        threadPool.shutdown()
    }


    fun handleNovelDownload(novelName: String, action: String) {
        Logs.debug(TAG, "handleNovelDownload")

        //android.os.Debug.waitForDebugger()
        if (action == ACTION_START) {
            var downloadNovelThread = threadListMap[novelName]
            if (downloadNovelThread != null && downloadNovelThread.isAlive) {
                return
            }

            downloadNovelThread = DownloadNovelThread(this, novelName, dbHelper, this@DownloadNovelService)
            threadListMap[novelName] = downloadNovelThread
            futures.add(threadPool.submit(downloadNovelThread, null as Any?))

            if (futures.size > 5)
                downloadListener?.handleEvent(DownloadNovelEvent(EventType.INSERT, novelName))
            else
                downloadListener?.handleEvent(DownloadNovelEvent(EventType.RUNNING, novelName))

        } else if (action == ACTION_PAUSE) {
            val downloadNovelThread = threadListMap[novelName]
            if (downloadNovelThread != null && downloadNovelThread.isAlive) {
                downloadNovelThread.interrupt()
                threadListMap[novelName] = null
            }
        }
    }


    //Only called when an Activity is trying to reconnect to this service
    override fun onRebind(intent: Intent?) {
        Logs.debug(TAG, "onRebind")
        super.onRebind(intent)
    }

    //Only called when an Activity is trying to disconnect to this service
    override fun onUnbind(intent: Intent?): Boolean {
        Logs.debug(TAG, "onUnbind")
        return true //true - so it calls rebind
    }

    //Called on bound (Activity) & background service
    override fun onDestroy() {
        Logs.debug(TAG, "onDestroy")
        dbHelper.updateDownloadStatus(Download.STATUS_PAUSED)
        super.onDestroy()
    }

    //endregion

    //region DownloadListener Implementation

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        downloadListener?.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        downloadListener?.handleEvent(downloadWebPageEvent)
    }

    //endregion

}
