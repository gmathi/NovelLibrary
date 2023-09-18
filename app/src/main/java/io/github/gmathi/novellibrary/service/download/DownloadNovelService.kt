package io.github.gmathi.novellibrary.service.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.getRemainingDownloadsCountForNovel
import io.github.gmathi.novellibrary.database.updateDownloadStatus
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.lang.launchIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor


class DownloadNovelService : Service(), DownloadListener {

    private var threadListMap = HashMap<Long, DownloadNovelThread?>()
    private val futures = ArrayList<Future<Any>>()

    private lateinit var threadPool: ThreadPoolExecutor
    private lateinit var dbHelper: DBHelper

    //static components
    companion object {
        const val TAG = "DownloadNovelService"
        const val QUALIFIED_NAME = "${BuildConfig.APPLICATION_ID}.service.download.DownloadNovelService"

        const val MAX_PARALLEL_DOWNLOADS = 5

        const val NOVEL_ID = "novel_id"
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_REMOVE = "action_remove"

        private const val DOWNLOAD_NOTIFICATION_GROUP = "downloadNotificationGroup"
        private val NOTIFICATION_ID by lazy { Utils.getUniqueNotificationId() }
    }

    //region TemplateCode
    private val binder = DownloadNovelBinder()

    @Volatile
    var downloadListener: DownloadListener? = null

    inner class DownloadNovelBinder : Binder() {
        /** Return this instance of LocalService so clients can call public methods */
        fun getService(): DownloadNovelService = this@DownloadNovelService
    }

    override fun onBind(intent: Intent): IBinder = binder

    /** Only called when an Activity is trying to reconnect to this service */
    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    /**  Only called when an Activity is trying to disconnect to this service */
    override fun onUnbind(intent: Intent?): Boolean {
        return true //true - so it calls rebind
    }
    //endregion

    override fun onCreate() {
        super.onCreate()
        dbHelper = DBHelper.getInstance(this)
        threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        launchIO {
            val novelId = intent?.getLongExtra(NOVEL_ID, -1L) ?: return@launchIO
            addNovelToDownload(novelId)
            notifyFirst()
            withContext(Dispatchers.IO) {
                while (futures.isNotEmpty()) {
                    futures[0].get()
                    futures.removeAt(0)
                }
                threadPool.shutdown()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun addNovelToDownload(novelId: Long?) {
        novelId?.let {
            val downloadNovelThread = DownloadNovelThread(
                this@DownloadNovelService, novelId, dbHelper, this@DownloadNovelService
            )
            threadListMap[novelId] = downloadNovelThread

            if (threadPool.isTerminating || threadPool.isShutdown) threadPool = Executors.newFixedThreadPool(MAX_PARALLEL_DOWNLOADS) as ThreadPoolExecutor
            futures.add(threadPool.submit(downloadNovelThread, null as Any?))
        }
    }

    fun handleNovelDownload(novelId: Long, action: String) {
        //android.os.Debug.waitForDebugger()
        when (action) {
            ACTION_START -> {
                threadListMap[novelId]?.let {
                    if (it.isAlive) return
                }
                addNovelToDownload(novelId)
                val downloadNovelEvent = if (futures.size > 5) DownloadNovelEvent(EventType.INSERT, novelId)
                else DownloadNovelEvent(EventType.RUNNING, novelId)
                downloadListener?.handleEvent(downloadNovelEvent)
                notify(downloadNovelEvent = downloadNovelEvent)
            }

            ACTION_PAUSE, ACTION_REMOVE -> {
                threadListMap[novelId]?.let {
                    if (it.isAlive) it.interrupt()
                    threadPool.remove(it)
                    threadPool.purge()
                    threadListMap[novelId] = null
                    threadListMap.remove(novelId)
                }

                if (threadListMap.isEmpty()) {
                    stopService()
                } else {
                    notify(downloadNovelEvent = DownloadNovelEvent(EventType.PAUSED, novelId))
                }
            }

            else -> {
                // Do Nothing
            }
        }
    }

    //Called on bound (Activity) & background service
    override fun onDestroy() {
        Logs.debug(TAG, "onDestroy")
        dbHelper.updateDownloadStatus(Download.STATUS_PAUSED)
        stopService()
        super.onDestroy()
    }


    //region DownloadListener Implementation
    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        when (downloadNovelEvent.type) {
            EventType.COMPLETE, EventType.DELETE -> {
                threadListMap.remove(downloadNovelEvent.novelId)
                if (threadListMap.isNotEmpty()) notify(downloadNovelEvent = downloadNovelEvent)
                else stopService()
            }

            else -> {
                // Do Nothing
            }
        }
        downloadListener?.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        if (downloadWebPageEvent.type == EventType.RUNNING) notify(downloadWebPageEvent = downloadWebPageEvent)
        downloadListener?.handleEvent(downloadWebPageEvent)
    }

    private fun createNotificationBuilder(
        title: String, message: String, contentIntent: PendingIntent
    ): NotificationCompat.Builder {
        val largeIcon = BitmapFactory.decodeResource(
            applicationContext.resources, R.drawable.ic_library_add_white_vector
        )
        val mBuilder = NotificationCompat.Builder(
            this@DownloadNovelService, getString(R.string.downloads_notification_channel_id)
        ).setContentTitle(title).setContentText(message).setLargeIcon(largeIcon).setContentIntent(contentIntent)
            .setColor(ContextCompat.getColor(applicationContext, R.color.alice_blue)).setAutoCancel(true)
        mBuilder.setSmallIcon(R.drawable.ic_book_white_vector)
        return mBuilder
    }

    private fun createNovelDetailsPendingIntent(novel: Novel): PendingIntent {
        val novelDetailsIntent = Intent(this@DownloadNovelService, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putSerializable("novel", novel)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        val pendingIntentFlags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(
            applicationContext, novel.hashCode(), novelDetailsIntent, pendingIntentFlags
        )
    }

    private fun createNovelLibraryHomePendingIntent(): PendingIntent {
        val novelDetailsIntent = Intent(this@DownloadNovelService, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        val pendingIntentFlags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(
            applicationContext, 0, novelDetailsIntent, pendingIntentFlags
        )
    }

    private fun stopService() {
        ServiceCompat.stopForeground(this@DownloadNovelService, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun notify(downloadNovelEvent: DownloadNovelEvent? = null, downloadWebPageEvent: DownloadWebPageEvent? = null) {
        //Check Notification Post Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(this@DownloadNovelService)
        if (downloadNovelEvent?.type == EventType.PAUSED) {
            notificationManager.cancel((NOTIFICATION_ID + downloadNovelEvent.novelId + 1).toInt())
        }

        val eventNovelId = downloadNovelEvent?.novelId ?: downloadWebPageEvent?.download?.novelId
        if (eventNovelId != null) {
            val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(eventNovelId)
            val status = if (downloadWebPageEvent != null && downloadWebPageEvent.type == EventType.RUNNING) {
                "Remaining: $remainingDownloadsCount, Current: ${downloadWebPageEvent.download.chapter}"
            } else {
                "Remaining: $remainingDownloadsCount"
            }
            postNotification(notificationManager, eventNovelId, status)
        } else {
            threadListMap.keys.forEach { novelId ->
                val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(novelId)
                val status = "Remaining: $remainingDownloadsCount"
                postNotification(notificationManager, novelId, status)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(notificationManager: NotificationManagerCompat, novelId: Long, status: String) {
        val novel = dbHelper.getNovel(novelId) ?: return
        val notificationBuilder = createNotificationBuilder(
            novel.name, status, createNovelDetailsPendingIntent(novel)
        )
        notificationBuilder.setGroup(DOWNLOAD_NOTIFICATION_GROUP)
        notificationManager.notify((NOTIFICATION_ID + novel.id + 1).toInt(), notificationBuilder.build())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNotificationVisible(notificationId: Int): Boolean {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val notifications = notificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == NOTIFICATION_ID) {
                return true
            }
        }
        return false
    }

    private fun notifyFirst() {
        //Check Notification Post Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val first = createNotificationBuilder(
            getString(R.string.app_name), getString(R.string.group_download_notification_text), createNovelLibraryHomePendingIntent()
        )
        first.setGroupSummary(true).setGroup(DOWNLOAD_NOTIFICATION_GROUP)
        startForeground(NOTIFICATION_ID, first.build())
        notify()
    }

}
