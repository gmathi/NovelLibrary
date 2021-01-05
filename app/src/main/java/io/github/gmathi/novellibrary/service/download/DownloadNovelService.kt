package io.github.gmathi.novellibrary.service.download

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.AppDatabase
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class DownloadNovelService : IntentService(TAG), DownloadListener {

    private var threadListMap = HashMap<String, DownloadNovelThread?>()
    private val futures = ArrayList<Future<Any>>()

    private lateinit var threadPool: ThreadPoolExecutor
    private lateinit var db: AppDatabase

    //static components
    companion object {
        const val TAG = "DownloadNovelService"
        const val QUALIFIED_NAME = "${BuildConfig.APPLICATION_ID}.service.download.DownloadNovelService"

        const val MAX_PARALLEL_DOWNLOADS = 5

        @JvmStatic
        val DOWNLOAD_NOTIFICATION_ID = Utils.getUniqueNotificationId()

        const val NOVEL_NAME = "name"
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_REMOVE = "action_remove"
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
        db = AppDatabase.createInstance(this)
        threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
    }

    //Only called when an Activity is trying to connect to this service
    override fun onBind(intent: Intent): IBinder {
        Logs.debug(TAG, "onBind")
        return binder
    }

    //Only called when the Service is called as Background Service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logs.debug(TAG, "onStartCommand")
        if (intent?.action != null && intent.action == "stop") {
            threadListMap.forEach {
                downloadListener?.handleEvent(DownloadNovelEvent(EventType.PAUSED, it.key))
                if (it.value != null && !it.value!!.isInterrupted)
                    it.value!!.interrupt()
            }
            threadListMap.clear()
            stopForeground(true)
        } else {
            startForeground(DOWNLOAD_NOTIFICATION_ID, getNotification(this, "${getString(R.string.downloading)}…"))
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onHandleIntent(intent: Intent?) {
        Logs.debug(TAG, "onHandleIntent")

        val novelName = intent?.getStringExtra(NOVEL_NAME) ?: return
        val downloadNovelThread = DownloadNovelThread(this, novelName, db, this@DownloadNovelService)
        threadListMap[novelName] = downloadNovelThread
        startForeground(DOWNLOAD_NOTIFICATION_ID, getNotification(this, "${getString(R.string.downloading)}: $novelName"))
        if (threadPool.isTerminating || threadPool.isShutdown)
            threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
        futures.add(threadPool.submit(downloadNovelThread, null as Any?))

        while (futures.isNotEmpty()) {
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

            downloadNovelThread = DownloadNovelThread(this, novelName, db, this@DownloadNovelService)
            threadListMap[novelName] = downloadNovelThread
            if (threadPool.isTerminating || threadPool.isShutdown)
                threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
            futures.add(threadPool.submit(downloadNovelThread, null as Any?))

            if (futures.size > 5)
                downloadListener?.handleEvent(DownloadNovelEvent(EventType.INSERT, novelName))
            else
                downloadListener?.handleEvent(DownloadNovelEvent(EventType.RUNNING, novelName))

            startForeground(DOWNLOAD_NOTIFICATION_ID, getNotification(this, "${getString(R.string.downloading)}: " + threadListMap.keys.joinToString(", ")))

        } else if (action == ACTION_PAUSE || action == ACTION_REMOVE) {
            val downloadNovelThread = threadListMap[novelName]
            if (downloadNovelThread != null && downloadNovelThread.isAlive) {
                downloadNovelThread.interrupt()
            }
            threadPool.remove(downloadNovelThread)
            threadPool.purge()
            threadListMap[novelName] = null
            threadListMap.remove(novelName)

            if (threadListMap.isEmpty()) {
                stopForeground(true)
                //stopSelf()
            } else {
                startForeground(DOWNLOAD_NOTIFICATION_ID, getNotification(this, "${getString(R.string.downloading)}: " + threadListMap.keys.joinToString(", ")))
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
        db.downloadDao().updateStatus(Download.STATUS_PAUSED)
        stopForeground(true)
        super.onDestroy()
    }

    //endregion

    //region DownloadListener Implementation

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        if (downloadNovelEvent.type == EventType.COMPLETE || downloadNovelEvent.type == EventType.DELETE) {
            threadListMap.remove(downloadNovelEvent.novelName)
            if (threadListMap.isNotEmpty())
                startForeground(DOWNLOAD_NOTIFICATION_ID, getNotification(this, "${getString(R.string.downloading)}: " + threadListMap.keys.joinToString(", ")))
            else
                stopForeground(true)
        }
        downloadListener?.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        downloadListener?.handleEvent(downloadWebPageEvent)
    }

    //endregion
    private fun getNotification(context: Context, status: String): Notification {

        // Add Pause button intent in notification.
        val stopIntent = Intent(this, DownloadNovelService::class.java)
        stopIntent.action = "stop"//ACTION_STOP
        //val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        val view = RemoteViews(context.packageName, R.layout.notification_download_small)
        view.setTextViewText(R.id.novelName, "Novel Library")
        view.setTextViewText(R.id.chapterName, status)
        view.setViewVisibility(R.id.containerActions, View.GONE)

        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putBoolean("showDownloads", true)
        novelDetailsIntent.putExtras(novelDetailsBundle)

        val pendingIntent = PendingIntent.getActivity(context, DOWNLOAD_NOTIFICATION_ID, novelDetailsIntent, 0)
        val channelId = getString(R.string.downloads_notification_channel_id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManagerCompat.from(applicationContext)
                .createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        getString(R.string.downloads_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = getString(R.string.downloads_notification_channel_description)
                        setSound(null, null)
                        enableVibration(false)
                    }
                )
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_file_download_white)
                .setCustomContentView(view)
                .setContentIntent(pendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_file_download_white)
                .setContent(view)
                .setContentIntent(pendingIntent)
                .build()
        }

    }

}
