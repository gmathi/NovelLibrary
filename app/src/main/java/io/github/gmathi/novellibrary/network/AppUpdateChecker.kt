package io.github.gmathi.novellibrary.network

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.withIOContext
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.util.system.notificationManager
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Request
import uy.kohesive.injekt.injectLazy
import java.io.File

/**
 * Checks for app updates from the GitHub releases branch,
 * downloads the APK, and shows a notification to install it.
 */
class AppUpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateChecker"
    }

    private val networkHelper: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val updateApi = AppUpdateGithubApi()

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun checkAndPromptUpdate(force: Boolean = false) {
        if (!force && !dataCenter.enableAutoAppUpdate) return
        if (!networkHelper.isConnectedToNetwork()) return

        try {
            val latestUpdate = updateApi.checkForUpdates(context)
            if (!latestUpdate.hasUpdate) return

            Logs.info(TAG, "Update available: ${latestUpdate.versionName} (${latestUpdate.versionCode})")
            val apkUrl = updateApi.getApkUrl(latestUpdate)
            downloadAndNotify(apkUrl, latestUpdate.versionName)
        } catch (e: Exception) {
            Logs.error(TAG, "Failed to check for updates: ${e.localizedMessage}", e)
        }
    }

    private suspend fun downloadAndNotify(apkUrl: String, versionName: String) {
        withIOContext {
            try {
                showProgressNotification(versionName)

                val request = Request.Builder().url(apkUrl).build()
                val response = networkHelper.client.newCall(request).execute()

                if (!response.isSuccessful) {
                    response.close()
                    Logs.error(TAG, "Download failed: HTTP ${response.code}")
                    showFailedNotification()
                    return@withIOContext
                }

                val externalDir = context.getExternalFilesDir(null)
                if (externalDir == null) {
                    Logs.error(TAG, "Download failed: external files directory unavailable")
                    showFailedNotification()
                    return@withIOContext
                }
                val apkFile = File(externalDir, "app_update.apk")
                val body = response.body
                if (body == null) {
                    Logs.error(TAG, "Download failed: empty response body")
                    showFailedNotification()
                    return@withIOContext
                }
                body.byteStream().use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                showInstallNotification(apkFile, versionName)
            } catch (e: Exception) {
                Logs.error(TAG, "Download failed: ${e.localizedMessage}", e)
                showFailedNotification()
            }
        }
    }

    private fun showProgressNotification(versionName: String) {
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_APP_UPDATE)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle(context.getString(R.string.app_update_available, versionName))
            .setContentText(context.getString(R.string.app_update_downloading))
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        context.notificationManager.notify(Notifications.ID_APP_UPDATE, notification)
    }

    private fun showInstallNotification(apkFile: File, versionName: String) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT

        val pendingIntent = PendingIntent.getActivity(context, 0, installIntent, flags)

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_APP_UPDATE)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle(context.getString(R.string.app_update_available, versionName))
            .setContentText(context.getString(R.string.app_update_tap_to_install, versionName))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        context.notificationManager.notify(Notifications.ID_APP_UPDATE, notification)
    }

    private fun showFailedNotification() {
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_APP_UPDATE)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle(context.getString(R.string.check_for_updates))
            .setContentText(context.getString(R.string.app_update_download_failed))
            .setAutoCancel(true)
            .build()
        context.notificationManager.notify(Notifications.ID_APP_UPDATE, notification)
    }
}
