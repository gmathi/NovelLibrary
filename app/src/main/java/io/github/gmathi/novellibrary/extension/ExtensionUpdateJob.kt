package io.github.gmathi.novellibrary.extension

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.util.system.notification
import kotlinx.coroutines.coroutineScope
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.TimeUnit

class ExtensionUpdateJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        val pendingUpdates = try {
            ExtensionGithubApi().checkForUpdates(context)
        } catch (e: Exception) {
            return@coroutineScope Result.failure()
        }

        if (pendingUpdates.isNotEmpty()) {
            createUpdateNotification(pendingUpdates.map { it.name })
        }

        Result.success()
    }

    private fun createUpdateNotification(names: List<String>) {
        NotificationManagerCompat.from(context).apply {
            notify(
                Notifications.ID_UPDATES_TO_EXTS,
                context.notification(Notifications.CHANNEL_UPDATES_TO_EXTS) {
                    setContentTitle(
                        context.resources.getQuantityString(
                            R.plurals.update_check_notification_ext_updates,
                            names.size,
                            names.size
                        )
                    )
                    val extNames = names.joinToString(", ")
                    setContentText(extNames)
                    setStyle(NotificationCompat.BigTextStyle().bigText(extNames))
                    setSmallIcon(R.drawable.ic_extension_white_vector)
//                    setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
                    setAutoCancel(true)
                }
            )
        }
    }

    companion object {
        private const val TAG = "ExtensionUpdate"

        fun setupTask(context: Context, forceAutoUpdateJob: Boolean? = null) {
            val dataCenter = Injekt.get<DataCenter>()
            val autoUpdateJob = forceAutoUpdateJob ?: dataCenter.automaticExtUpdates
            if (autoUpdateJob) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val request = PeriodicWorkRequestBuilder<ExtensionUpdateJob>(
                    12,
                    TimeUnit.HOURS,
                    1,
                    TimeUnit.HOURS
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.REPLACE, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
