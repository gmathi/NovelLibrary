package io.github.gmathi.novellibrary.service.sync

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.gcm.*
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPageSettings
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterCount
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils


class BackgroundNovelSyncTask : GcmTaskService() {

    override fun onRunTask(taskParams: TaskParams): Int {
        val context = this@BackgroundNovelSyncTask
        val dbHelper = DBHelper.getInstance(context)

        //android.os.Debug.waitForDebugger()

        try {
            if (Utils.isConnectedToNetwork(context))
                startNovelsSync(dbHelper)
        } catch (e: Exception) {
            return GcmNetworkManager.RESULT_RESCHEDULE
        }
        return GcmNetworkManager.RESULT_SUCCESS
    }

    private fun startNovelsSync(dbHelper: DBHelper) {
        Logs.debug(TAG, "start novel sync")

        val totalCountMap: HashMap<Novel, Int> = HashMap()

        val novels = dbHelper.getAllNovels()
        novels.forEach {
            try {
                val totalChapters = NovelApi.getChapterCount(it)
                if (totalChapters != 0 && totalChapters > it.chaptersCount.toInt()) {
                    totalCountMap[it] = totalChapters
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Novel: $it", e)
                return
            }
        }

        if (totalCountMap.isEmpty()) return

        //Update DB with new chapters
        totalCountMap.forEach {
            val novel = it.key
            novel.metaData[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
            dbHelper.updateNovelMetaData(novel)
            dbHelper.updateChaptersAndReleasesCount(novel.id, it.value.toLong(), novel.newReleasesCount + (it.value - novel.chaptersCount))
            updateChapters(novel, dbHelper)
        }

        val novelsList: ArrayList<Novel> = ArrayList()
        totalCountMap.forEach {
            val novel = dbHelper.getNovel(it.key.id)!!
            novelsList.add(novel)
        }

        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        val contentIntent = PendingIntent.getActivity(this.applicationContext, 0, novelDetailsIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        showBundledNotifications(this, novelsList, contentIntent)

    }

    /**
     * Download latest chapters from network
     */
    private fun updateChapters(novel: Novel, dbHelper: DBHelper) {
        try {
            val chapters = NovelApi.getChapterUrls(novel) ?: ArrayList()
            for (i in 0 until chapters.size) {
                if (dbHelper.getWebPage(chapters[i].url) == null)
                    dbHelper.createWebPage(chapters[i])
                if (dbHelper.getWebPageSettings(chapters[i].url) == null)
                    dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novel.id))
            }

        } catch (e: Exception) {
            Logs.error(TAG, "Novel: $novel", e)
        }
    }


//region Helper Methods

    companion object {

        private val thisClass = BackgroundNovelSyncTask::class.java
        private const val TAG = "BackgroundNovelSyncTask"
        private const val UPDATE_NOTIFICATION_GROUP = "updateNotificationGroup"

        fun scheduleRepeat(context: Context) {
            cancelAll(context)
            //in this method, single Repeating task is scheduled (the target service that will be called is MyTaskService.class)
            try {
                val periodic = PeriodicTask.Builder()
                        //specify target service - must extend GcmTaskService
                        .setService(thisClass)
                        //repeat every 60*60 seconds = 1hr
                        // .setPeriod(5)
                        .setPeriod(60 * 60)
                        //specify how much earlier the task can be executed (in seconds)
                        //.setFlex(60*60)
                        //tag that is unique to this task (can be used to cancel task)
                        .setTag(TAG)
                        //whether the task persists after device reboot
                        .setPersisted(true)
                        //if another task with same tag is already scheduled, replace it with this task
                        .setUpdateCurrent(true)
                        //set required network state, this line is optional
                        .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                        //request that charging must be connected, this line is optional
                        .setRequiresCharging(false)
                        .build()
                GcmNetworkManager.getInstance(context).schedule(periodic)
                Logs.debug(TAG, "repeating task scheduled")
            } catch (e: Exception) {
                Logs.error(TAG, "scheduling failed", e)
            }

        }

        fun cancelAll(context: Context) {
            GcmNetworkManager
                    .getInstance(context)
                    .cancelAllTasks(thisClass)
        }

    }

    private fun showBundledNotifications(context: Context, novelsList: ArrayList<Novel>, contentIntent: PendingIntent) {
        val first = createNotificationBuilder(
                context, getString(R.string.app_name), getString(R.string.group_update_notification_text), contentIntent)
        first.setGroupSummary(true).setGroup(UPDATE_NOTIFICATION_GROUP)
        showNotification(this, first.build(), 0)

        val notificationList = ArrayList<Notification>()
        novelsList.forEach { novel ->
            val notificationBuilder = createNotificationBuilder(
                    context,
                    novel.name,
                    getString(R.string.new_chapters_notification_content_single, novel.newReleasesCount.toInt()),
                    createNovelDetailsPendingIntent(novel))
            notificationBuilder.setGroup(UPDATE_NOTIFICATION_GROUP)
            notificationList.add(notificationBuilder.build())
        }
        var count = 0
        notificationList.forEach { showNotification(this, it, ++count) }
    }


    private fun createNotificationBuilder(context: Context, title: String, message: String, contentIntent: PendingIntent): NotificationCompat.Builder {

        val largeIcon = BitmapFactory.decodeResource(context.resources,
                R.drawable.ic_library_add_white_vector)
        val mBuilder = NotificationCompat.Builder(this, "default")
                .setContentTitle(title)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setContentIntent(contentIntent)
                .setColor(ContextCompat.getColor(context, R.color.alice_blue))
                .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            mBuilder.setSmallIcon(R.drawable.ic_book_white_vector)
        else
            mBuilder.setSmallIcon(R.drawable.ic_book_white)
        return mBuilder
    }

    private fun showNotification(context: Context, notification: Notification, id: Int) {
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(id, notification)
    }

    private fun createNovelDetailsPendingIntent(novel: Novel): PendingIntent {
        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putParcelable("novel", novel)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        return PendingIntent.getActivity(this.applicationContext, novel.hashCode(), novelDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

//endregion

}