package io.github.gmathi.novellibrary.service.download

import android.app.IntentService
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.util.Log
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.getAllReadableWebPagesCount
import io.github.gmathi.novellibrary.database.getFirstDownloadableQueueItem
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateAllDownloadQueueStatuses
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class DownloadNovelService : IntentService(TAG) {


    //static components
    companion object {
        val TAG = "DownloadNovelService"

        var isDownloading: Boolean = false
        var novelId: Long = -2L

    }

    override fun onHandleIntent(workIntent: Intent) {
        novelId = workIntent.getLongExtra(Constants.NOVEL_ID, -1L)
        isDownloading = true

        // android.os.Debug.waitForDebugger()

        if (isNetworkDown()) return

        EventBus.getDefault().register(this)
        if (novelId == -1L) {
            downloadAllNovels()
        } else {
            downloadNovel(novelId)
        }

        EventBus.getDefault().unregister(this)
        stopForeground(true)
        novelId = -2L
        isDownloading = true
    }

    private fun downloadAllNovels() {
        var downloadQueueItem = dbHelper.getFirstDownloadableQueueItem()
        while (downloadQueueItem != null) {
            novelId = downloadQueueItem.novelId
            EventBus.getDefault()?.post(NovelEvent(EventType.UPDATE, novelId, null))
            DownloadNovel(this, downloadQueueItem.novelId).start()
            downloadQueueItem = dbHelper.getFirstDownloadableQueueItem()
        }
    }

    private fun downloadNovel(novelId: Long) {
        DownloadNovel(this, novelId).start()
    }

    private fun onNoNetwork() {
        Log.e(TAG, "No Active Internet")
        dbHelper.updateAllDownloadQueueStatuses(Constants.STATUS_STOPPED)
        EventBus.getDefault().post(NovelEvent(EventType.UPDATE, -1L))
    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(this)) {
            onNoNetwork()
            return true
        }
        return false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        if (event.novelId == -1L) return
        val novel = dbHelper.getNovel(event.novelId) ?: return
        val notification = getNotification(novel)
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_DOWNLOAD_NOVEL_SERVICE,
            notification)
    }

    private fun getNotification(novel: Novel): Notification {
        // The PendingIntent to launch our activity if the user selects
        // this notification
        val readablePagesCount = dbHelper.getAllReadableWebPagesCount(novel.id)

        val title = novel.name
        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.ACTION.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val bundle = Bundle()
        bundle.putInt("currentNavId", R.id.nav_downloads)
        novelDetailsIntent.putExtras(bundle)

        val contentIntent = PendingIntent.getActivity(this, 0, novelDetailsIntent, 0)

        val builder = NotificationCompat.Builder(this, "default")
            .setContentTitle(title)
            .setOngoing(true)
            .setContentText(getString(R.string.downloading_status, getString(R.string.downloading), getString(R.string.status), getString(R.string.chapter_count, readablePagesCount, novel.chapterCount.toInt())))
            .setContentIntent(contentIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setSmallIcon(R.drawable.ic_file_download_orange_vector)
        else
            builder.setSmallIcon(R.drawable.ic_file_download_white)

        if (novel.imageFilePath != null) {
            builder.setLargeIcon(Bitmap.createScaledBitmap(BitmapFactory.decodeFile(novel.imageFilePath), 128, 192, false))
        }

        return builder.build()
    }


}
