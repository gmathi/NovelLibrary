package io.github.gmathi.novellibrary.service.sync

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterCount
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils


class SyncNovelsService : IntentService(TAG) {

    //static components
    companion object {
        val TAG = "DownloadService"
    }

    override fun onHandleIntent(workIntent: Intent) {
        //android.os.Debug.waitForDebugger()
        if (isNetworkDown()) return

        //Test Code
//        val novel = dbHelper.getNovel("Carta Visa")
//        novel?.chapterCount = 10
//        dbHelper.updateNovel(novel!!)

        //Sync Novels
        startNovelsSync()
    }

    private fun startNovelsSync() {
        val novelMap: HashMap<String, Int> = HashMap()
        dbHelper.getAllNovels().forEach {
            try {
                val totalChapters = NovelApi().getChapterCount(it.url)
                if (it.chapterCount.toInt() != totalChapters && totalChapters != 0) {
                    novelMap.put(it.name, (totalChapters - it.chapterCount).toInt())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (novelMap.size == 0) return

        val notificationText: String
        notificationText = if (novelMap.size > 4) {
            getString(R.string.new_chapters_notification_content_full, novelMap.size, novelMap.values.sum())
        } else {
            novelMap.keys.joinToString(separator = ", ") { getString(R.string.new_chapters_notification_content_single, it, novelMap[it]) }
        }

        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.ACTION.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val bundle = Bundle()
        bundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsIntent.putExtras(bundle)

        val contentIntent = PendingIntent.getActivity(this, 0, novelDetailsIntent, 0)

        val mBuilder = NotificationCompat.Builder(this, "default")
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle(getString(R.string.new_chapters_notification_title))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(notificationText))
            .setContentText(notificationText)
            .setContentIntent(contentIntent)

        val mNotifyMgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        mNotifyMgr.notify(Constants.NOTIFICATION_ID.SYNC_CHAPTERS, mBuilder.build())
    }

    private fun isNetworkDown(): Boolean {
        if (!Utils.checkNetwork(this)) {
            return true
        }
        return false
    }

}
