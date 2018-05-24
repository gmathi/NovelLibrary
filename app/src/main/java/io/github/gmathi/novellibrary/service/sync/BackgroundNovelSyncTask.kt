package io.github.gmathi.novellibrary.service.sync

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.android.gms.gcm.*
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterCount
import io.github.gmathi.novellibrary.network.getChapterUrls
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils


class BackgroundNovelSyncTask : GcmTaskService() {

    override fun onRunTask(taskParams: TaskParams): Int {
        val context = this@BackgroundNovelSyncTask
        val dbHelper = DBHelper.getInstance(context)
//
//        //TODO: DELETE Test Code
//        val novellist = dbHelper.getAllNovels();
//        if (novellist != null) {
//            novellist.forEach({
//                it?.newReleasesCount = 400
//                it?.chaptersCount = 400
//                dbHelper.updateNovel(it!!)
//            })
//        }

        try {
            if (Utils.isConnectedToNetwork(context))
                startNovelsSync(dbHelper)
        } catch (e: Exception) {
            return GcmNetworkManager.RESULT_RESCHEDULE
        }
        return GcmNetworkManager.RESULT_SUCCESS
    }

    private fun startNovelsSync(dbHelper: DBHelper) {
        Log.e(TAG, "start novel sync")

        val totalCountMap: HashMap<Novel, Int> = HashMap()

        val novels = dbHelper.getAllNovels()
        novels.forEach {
            try {
                val totalChapters = NovelApi.getChapterCount(it)
                if (totalChapters != 0 && totalChapters > it.chaptersCount.toInt()) {
                    totalCountMap[it] = totalChapters
                }
            } catch (e: Exception) {
                Crashlytics.log("Novel: $it")
                Crashlytics.logException(e)
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

        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putSerializable("novelsChapMap", totalCountMap)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        val contentIntent = PendingIntent.getActivity(this.applicationContext, 0, novelDetailsIntent, PendingIntent.FLAG_CANCEL_CURRENT)


        showBundledNotifications(this, totalCountMap, contentIntent)

    }

    private fun updateChapters(novel: Novel, dbHelper: DBHelper) {
        if (!Utils.isConnectedToNetwork(this@BackgroundNovelSyncTask) || novel.id == -1L)
            return

        //Download latest chapters from network
        try {
            val chapterList = NovelApi.getChapterUrls(novel)?.reversed()
            chapterList?.let {

                //We start the insertion from the last since that is faster, instead of checking the 1st 1000 chaps.
                for (i in it.size - 1 downTo 0) {
                    val webPage = dbHelper.getWebPage(novel.id, i.toLong())
                    if (webPage == null) {
                        it[i].orderId = i.toLong()
                        it[i].novelId = novel.id
                        dbHelper.createWebPage(it[i])
                    } else
                        return@let
                }
            }
        } catch (e: Exception) {
            Crashlytics.log("Novel: $novel")
            Crashlytics.logException(e)
        }
    }

//region Helper Methods

    companion object {

        private val thisClass = BackgroundNovelSyncTask::class.java
        private const val TAG = "BackgroundNovelSyncTask"
        private const val KEY_NOTIFICATION_GROUP = "KEY_NOTIFICATION_GROUP"

        fun scheduleRepeat(context: Context) {
            cancelAll(context)
            //in this method, single Repeating task is scheduled (the target service that will be called is MyTaskService.class)
            try {
                val periodic = PeriodicTask.Builder()
                        //specify target service - must extend GcmTaskService
                        .setService(thisClass)
                        //repeat every 60 seconds
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
                Log.v(TAG, "repeating task scheduled")
            } catch (e: Exception) {
                Log.e(TAG, "scheduling failed")
                e.printStackTrace()
            }

        }

        fun cancelAll(context: Context) {
            GcmNetworkManager
                    .getInstance(context)
                    .cancelAllTasks(thisClass)
        }

    }

    private fun showBundledNotifications(context: Context, novelMap: HashMap<Novel, Int>, contentIntent: PendingIntent) {
        val first = createNotificationBuilder(
                context, getString(R.string.app_name), getString(R.string.group_notification_text), contentIntent)
        first.setGroupSummary(true).setGroup(KEY_NOTIFICATION_GROUP)

        val notificationList = ArrayList<Notification>()

        novelMap.forEach { singleNovelMap ->
            val notificationBuilder = createNotificationBuilder(
                    context,
                    singleNovelMap.key.name,
                    getString(R.string.new_chapters_notification_content_single, singleNovelMap.key.newReleasesCount.toInt()),
                    createNovelDetailsPendingIntent(novelMap, singleNovelMap.key))
            notificationBuilder.setGroup(KEY_NOTIFICATION_GROUP)
            notificationList.add(notificationBuilder.build())
        }

        showNotification(this, first.build(), 0)

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

    private fun createNovelDetailsPendingIntent(novelsMap: HashMap<Novel, Int>, novel: Novel): PendingIntent {
        val novelDetailsIntent = Intent(this, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putSerializable("novelsChapMap", novelsMap)
        novelDetailsBundle.putSerializable("novel", novel)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        return PendingIntent.getActivity(this.applicationContext, novel.hashCode(), novelDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

//endregion

}