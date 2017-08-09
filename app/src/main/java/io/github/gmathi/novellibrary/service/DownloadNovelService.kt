package io.github.gmathi.novellibrary.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getFirstDownloadableQueueItem
import io.github.gmathi.novellibrary.database.updateAllDownloadQueueStatuses
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import org.greenrobot.eventbus.EventBus


class DownloadNovelService : IntentService(TAG) {

    lateinit var dbHelper: DBHelper

    //static components
    companion object {
        val TAG = "DownloadNovelService"

        var isDownloading: Boolean = false
        var novelId: Long = -2L

    }

    override fun onHandleIntent(workIntent: Intent) {
        dbHelper = DBHelper(applicationContext)
        novelId = workIntent.getLongExtra(Constants.NOVEL_ID, -1L)
        isDownloading = true

        // android.os.Debug.waitForDebugger()

        if (isNetworkDown()) return

        if (novelId == -1L) {
            downloadAllNovels()
        } else {
            downloadNovel(novelId)
        }

        novelId = -2L
        isDownloading = true

    }

    private fun downloadAllNovels() {
        var downloadQueueItem = dbHelper.getFirstDownloadableQueueItem()
        while (downloadQueueItem != null) {
            novelId = downloadQueueItem.novelId
            EventBus.getDefault()?.post(NovelEvent(EventType.UPDATE, novelId, null))
            DownloadNovel(this, downloadQueueItem.novelId, dbHelper).start()
            downloadQueueItem = dbHelper.getFirstDownloadableQueueItem()
        }
    }

    private fun downloadNovel(novelId: Long) {
        DownloadNovel(this, novelId, dbHelper).start()
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

}
