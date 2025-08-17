package io.github.gmathi.novellibrary.service.download

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class DownloadNovelService : Service(), DownloadListener {

    private var downloadServiceMap = HashMap<Long, CoroutineDownloadService?>()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _downloadProgressFlow = MutableSharedFlow<DownloadWebPageEvent>()
    val downloadProgressFlow: SharedFlow<DownloadWebPageEvent> = _downloadProgressFlow.asSharedFlow()

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val novelId = intent?.getLongExtra(NOVEL_ID, -1L) ?: return@launch
            addNovelToDownload(novelId)
            notifyFirst()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private suspend fun addNovelToDownload(novelId: Long?) {
        novelId?.let {
            val downloadService = CoroutineDownloadService(
                this@DownloadNovelService, novelId, dbHelper, this@DownloadNovelService
            )
            downloadServiceMap[novelId] = downloadService

            // Collect download progress events
            serviceScope.launch {
                downloadService.getDownloadEventsFlow().collect { event ->
                    _downloadProgressFlow.emit(event)
                }
            }

            // Start the download process
            downloadService.startDownload()
        }
    }

    fun handleNovelDownload(novelId: Long, action: String) {
        serviceScope.launch {
            when (action) {
                ACTION_START -> {
                    downloadServiceMap[novelId]?.let {
                        if (it.isDownloading()) return@launch
                    }
                    addNovelToDownload(novelId)
                    val downloadNovelEvent = if (downloadServiceMap.size > MAX_PARALLEL_DOWNLOADS) 
                        DownloadNovelEvent(EventType.INSERT, novelId)
                    else 
                        DownloadNovelEvent(EventType.RUNNING, novelId)
                    downloadListener?.handleEvent(downloadNovelEvent)
                    notify(downloadNovelEvent = downloadNovelEvent)
                }

                ACTION_PAUSE, ACTION_REMOVE -> {
                    downloadServiceMap[novelId]?.let { downloadService ->
                        downloadService.cancelAllDownloads()
                        downloadService.cleanup()
                        downloadServiceMap[novelId] = null
                        downloadServiceMap.remove(novelId)
                    }

                    if (downloadServiceMap.isEmpty()) {
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
    }

    //Called on bound (Activity) & background service
    override fun onDestroy() {
        Logs.debug(TAG, "onDestroy")
        dbHelper.updateDownloadStatus(Download.STATUS_PAUSED)
        
        // Cancel all download services and cleanup
        downloadServiceMap.values.forEach { downloadService ->
            downloadService?.cancelAllDownloads()
            downloadService?.cleanup()
        }
        downloadServiceMap.clear()
        
        // Cancel the service scope
        serviceScope.cancel()
        
        stopService()
        super.onDestroy()
    }


    //region DownloadListener Implementation
    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        when (downloadNovelEvent.type) {
            EventType.COMPLETE, EventType.DELETE -> {
                serviceScope.launch {
                    downloadServiceMap[downloadNovelEvent.novelId]?.cleanup()
                    downloadServiceMap.remove(downloadNovelEvent.novelId)
                    if (downloadServiceMap.isNotEmpty()) notify(downloadNovelEvent = downloadNovelEvent)
                    else stopService()
                }
            }

            else -> {
                // Do Nothing
            }
        }
        downloadListener?.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        if (downloadWebPageEvent.type == EventType.RUNNING) {
            serviceScope.launch {
                notify(downloadWebPageEvent = downloadWebPageEvent)
            }
        }
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
            downloadServiceMap.keys.forEach { novelId ->
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

    private fun notifyFirst() {
        //Check Notification Post Permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val first = createNotificationBuilder(
            getString(R.string.app_name), getString(R.string.group_download_notification_text), createNovelLibraryHomePendingIntent()
        )
        first.setGroupSummary(true).setGroup(DOWNLOAD_NOTIFICATION_GROUP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, first.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, first.build())
        }
        notify()
    }

    /**
     * Get download status for a specific novel
     */
    suspend fun getDownloadStatus(novelId: Long): DownloadStatus? {
        return downloadServiceMap[novelId]?.getDownloadStatus()
    }

    /**
     * Check if any downloads are currently running
     */
    fun hasActiveDownloads(): Boolean {
        return downloadServiceMap.values.any { it?.isDownloading() == true }
    }

    /**
     * Get all active novel IDs being downloaded
     */
    fun getActiveNovelIds(): Set<Long> {
        return downloadServiceMap.keys.toSet()
    }

    /**
     * Cancel download for a specific web page URL
     */
    suspend fun cancelWebPageDownload(novelId: Long, webPageUrl: String) {
        downloadServiceMap[novelId]?.cancelDownload(webPageUrl)
    }

}
