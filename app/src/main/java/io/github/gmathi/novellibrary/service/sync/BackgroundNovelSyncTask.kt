package io.github.gmathi.novellibrary.service.sync

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.lang.launchUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class BackgroundNovelSyncTask(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val dbHelper = DBHelper.getInstance(context)

        // Enable the below line only in debug mode for triggering breakpoints
        // android.os.Debug.waitForDebugger()

        try {
            if (NetworkHelper(context).isConnectedToNetwork())
                startNovelsSync(dbHelper)
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startNovelsSync(dbHelper: DBHelper) {
        //For Testing - get a Novel and delete 5 chapters
        //dbHelper.getAllNovels().forEach { novel ->
        //            dbHelper.updateChaptersCount(novel.id, novel.chaptersCount - 5)
        //        }

        launchUI {
            Logs.debug(TAG, "start novel sync")
            val totalCountMap: HashMap<Novel, Int> = HashMap()
            val totalChaptersMap: HashMap<Novel, ArrayList<WebPage>> = HashMap()
            val sourceManager = withContext(Dispatchers.IO) { SourceManager(context) }
            val novels = dbHelper.getAllNovels()
            novels.forEach { novel ->
                try {
                    val newChaptersList = withContext(Dispatchers.IO) { sourceManager.get(novel.sourceId)?.getChapterList(novel) } ?: ArrayList()
                    val currentChaptersHashCode = dbHelper.getAllWebPages(novel.id).sumOf { it.hashCode() }
                    val newChaptersHashCode =  newChaptersList.sumOf { it.hashCode() }
                    if (newChaptersList.isNotEmpty() && newChaptersHashCode != currentChaptersHashCode) {
                        totalCountMap[novel] = newChaptersList.size
                        totalChaptersMap[novel] = ArrayList(newChaptersList)
                    }
                } catch (e: Exception) {
                    Logs.error(TAG, "Novel: $novel", e)
                    return@forEach
                }
            }

            if (totalCountMap.isEmpty()) return@launchUI

            //Update DB with new chapters
            totalChaptersMap.forEach {
                val novel = it.key
                val chapters = it.value
                novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()

                dbHelper.writableDatabase.runTransaction { writableDatabase ->
                    dbHelper.updateNovelMetaData(novel, writableDatabase)
                    dbHelper.updateChaptersAndReleasesCount(novel.id, chapters.size.toLong(), novel.newReleasesCount + (chapters.size - novel.chaptersCount), writableDatabase)
                    for (i in 0 until chapters.size) {
                        dbHelper.createWebPage(chapters[i], writableDatabase)
                        dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novel.id), writableDatabase)
                    }
                }
            }

            val novelsList: ArrayList<Novel> = ArrayList()
            totalCountMap.forEach {
                val novel = dbHelper.getNovel(it.key.id)!!
                novelsList.add(novel)
            }

            val novelDetailsIntent = Intent(context, NavDrawerActivity::class.java)
            novelDetailsIntent.action = Constants.Action.MAIN_ACTION
            novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val novelDetailsBundle = Bundle()
            novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
            novelDetailsIntent.putExtras(novelDetailsBundle)
            val contentIntent = PendingIntent.getActivity(
                context,
                0,
                novelDetailsIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
            )

            showBundledNotifications(novelsList, contentIntent)
        }
    }

//region Helper Methods

    companion object {

        private const val TAG = "BackgroundNovelSyncTask"
        private const val UPDATE_NOTIFICATION_GROUP = "updateNotificationGroup"
        private var NOTIFICATION_ID = 0

        private fun createSyncConstraints() =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(true)
                .build()

        fun scheduleRepeat(context: Context) {
            cancelAll(context)
            //in this method, single Repeating task is scheduled (the target service that will be called is MyTaskService.class)
            try {
                val constraints = createSyncConstraints()
                val periodicTask = PeriodicWorkRequestBuilder<BackgroundNovelSyncTask>(1, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()
                WorkManager.getInstance(context).enqueue(periodicTask)

                Logs.debug(TAG, "repeating task scheduled")
            } catch (e: Exception) {
                Logs.error(TAG, "scheduling failed", e)
            }

        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(TAG)
        }

    }

    private fun showBundledNotifications(novelsList: ArrayList<Novel>, contentIntent: PendingIntent) {
        if (NOTIFICATION_ID == 0) {
            NOTIFICATION_ID = Utils.getUniqueNotificationId()
        }
        val notificationManager = NotificationManagerCompat.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    context.getString(R.string.new_chapters_notification_channel_id),
                    context.getString(R.string.new_chapters_notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.new_chapters_notification_channel_description)
                })
        }

        val first = createNotificationBuilder(context.getString(R.string.app_name), context.getString(R.string.group_update_notification_text), contentIntent)
        first.setGroupSummary(true).setGroup(UPDATE_NOTIFICATION_GROUP)
        notificationManager.notify(NOTIFICATION_ID, first.build())

        novelsList.forEach { novel ->
            val notificationBuilder =
                createNotificationBuilder(novel.name, context.getString(R.string.new_chapters_notification_content_single, novel.newReleasesCount.toInt()), createNovelDetailsPendingIntent(novel))
            notificationBuilder.setGroup(UPDATE_NOTIFICATION_GROUP)
            notificationManager.notify((NOTIFICATION_ID + novel.id + 1).toInt(), notificationBuilder.build())
        }
    }


    private fun createNotificationBuilder(title: String, message: String, contentIntent: PendingIntent): NotificationCompat.Builder {
        val largeIcon = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.ic_library_add_white_vector)
        val mBuilder = NotificationCompat.Builder(context, context.getString(R.string.new_chapters_notification_channel_id))
            .setContentTitle(title)
            .setContentText(message)
            .setLargeIcon(largeIcon)
            .setContentIntent(contentIntent)
            .setColor(ContextCompat.getColor(applicationContext, R.color.alice_blue))
            .setAutoCancel(true)
        mBuilder.setSmallIcon(R.drawable.ic_book_white_vector)
        return mBuilder
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createNovelDetailsPendingIntent(novel: Novel): PendingIntent {
        val novelDetailsIntent = Intent(context, NavDrawerActivity::class.java)
        novelDetailsIntent.action = Constants.Action.MAIN_ACTION
        novelDetailsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val novelDetailsBundle = Bundle()
        novelDetailsBundle.putInt("currentNavId", R.id.nav_library)
        novelDetailsBundle.putSerializable("novel", novel)
        novelDetailsIntent.putExtras(novelDetailsBundle)
        return PendingIntent.getActivity(
            this.applicationContext,
            novel.hashCode(),
            novelDetailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

//endregion

}