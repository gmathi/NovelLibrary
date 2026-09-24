package io.github.gmathi.novellibrary.network

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.withIOContext
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.util.system.notificationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.Request
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** What a check for an app update came to. */
sealed class UpdateCheckResult {
    /** Automatic update checks are turned off. */
    object Disabled : UpdateCheckResult()

    object Offline : UpdateCheckResult()

    /** This is the latest version. */
    object UpToDate : UpdateCheckResult()

    /** A newer version is downloading; a notification will offer to install it. */
    data class Downloading(val versionName: String) : UpdateCheckResult()

    /** A download started earlier is still running. */
    object AlreadyDownloading : UpdateCheckResult()

    /**
     * A newer version is out but was not downloaded: notifications are off, so the download could
     * neither show its progress nor offer to install it. [apkUrl] can be opened in a browser instead.
     */
    data class NotificationsOff(val versionName: String, val apkUrl: String) : UpdateCheckResult()

    data class Failed(val error: Exception) : UpdateCheckResult()
}

/**
 * Checks for app updates from the GitHub releases branch,
 * downloads the APK, and shows a notification to install it.
 */
class AppUpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateChecker"

        // The APK is around 150 MB: its download outlives the screen that started it, and a check
        // made while it runs does not start a second download into the same file.
        private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val downloading = AtomicBoolean(false)
    }

    private val networkHelper: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val updateApi = AppUpdateGithubApi()

    /**
     * Checks for a newer version and, if there is one, starts downloading it; a notification offers
     * to install it once it is in. Returns what the check came to, for a check the user asked for.
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun checkAndPromptUpdate(force: Boolean = false): UpdateCheckResult {
        if (!force && !dataCenter.enableAutoAppUpdate) return UpdateCheckResult.Disabled
        if (!networkHelper.isConnectedToNetwork()) return UpdateCheckResult.Offline

        val latestUpdate =
            try {
                updateApi.checkForUpdates(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logs.error(TAG, "Failed to check for updates: ${e.localizedMessage}", e)
                return UpdateCheckResult.Failed(e)
            }
        if (!latestUpdate.hasUpdate) return UpdateCheckResult.UpToDate

        Logs.info(TAG, "Update available: ${latestUpdate.versionName} (${latestUpdate.versionCode})")
        val apkUrl = updateApi.getApkUrl(latestUpdate)
        // The download reports only through notifications. Without them (Android 13+ blocks them
        // until the user allows them) it would finish unseen, with no way to install it.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return UpdateCheckResult.NotificationsOff(latestUpdate.versionName, apkUrl)
        }
        if (!downloading.compareAndSet(false, true)) return UpdateCheckResult.AlreadyDownloading
        downloadScope.launch {
            try {
                downloadAndNotify(apkUrl, latestUpdate.versionName)
            } finally {
                downloading.set(false)
            }
        }
        return UpdateCheckResult.Downloading(latestUpdate.versionName)
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
