package io.github.gmathi.novellibrary.extension.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.InstallStep
import io.github.gmathi.novellibrary.util.storage.getUriCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.Dispatchers
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The installer which installs, updates and uninstalls the extensions.
 *
 * @param context The application context.
 */
internal class ExtensionInstaller(private val context: Context) {

    /**
     * The system's download manager
     */
    private val downloadManager = context.getSystemService<DownloadManager>()!!

    /**
     * The broadcast receiver which listens to download completion events.
     */
    private val downloadReceiver = DownloadCompletionReceiver()

    /**
     * The currently requested downloads, with the package name (unique id) as key, and the id
     * returned by the download manager.
     */
    private val activeDownloads = hashMapOf<String, Long>()

    /**
     * Map to store download completion callbacks.
     */
    private val downloadCallbacks = mutableMapOf<Long, (InstallStep) -> Unit>()

    /**
     * Adds the given extension to the downloads queue and returns a flow containing its
     * step in the installation process.
     *
     * @param url The url of the apk.
     * @param extension The extension to install.
     */
    fun downloadAndInstall(url: String, extension: Extension): Flow<InstallStep> = callbackFlow {
        val pkgName = extension.pkgName

        val oldDownload = activeDownloads[pkgName]
        if (oldDownload != null) {
            deleteDownload(pkgName)
        }

        // Register the receiver after removing (and unregistering) the previous download
        downloadReceiver.register()

        val downloadUri = url.toUri()
        val request = DownloadManager.Request(downloadUri)
            .setTitle(extension.name)
            .setMimeType(APK_MIME)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, downloadUri.lastPathSegment)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = downloadManager.enqueue(request)
        activeDownloads[pkgName] = id

        // Set up callback for this download
        downloadCallbacks[id] = { step ->
            trySend(step)
        }

        // Merge with polling status
        val statusFlow = pollStatus(id)
        statusFlow.collect { step ->
            trySend(step)
        }

        awaitClose {
            downloadCallbacks.remove(id)
            deleteDownload(pkgName)
        }
    }
        .takeWhile { !it.isCompleted() }
        .timeout(3.minutes)
        .flowOn(Dispatchers.Main)
        .onCompletion { cause ->
            if (cause != null) {
                // Emit error step on timeout or other exceptions
                emit(InstallStep.Error)
            }
        }

    /**
     * Returns a flow that polls the given download id for its status every second, as the
     * manager doesn't have any notification system. It'll stop once the download finishes.
     *
     * @param id The id of the download to poll.
     */
    private fun pollStatus(id: Long): Flow<InstallStep> = callbackFlow {
        val query = DownloadManager.Query().setFilterById(id)

        while (true) {
            val status = try {
                downloadManager.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    } else {
                        DownloadManager.STATUS_FAILED
                    }
                }
            } catch (e: Exception) {
                DownloadManager.STATUS_FAILED
            }

            val step = when (status) {
                DownloadManager.STATUS_PENDING -> InstallStep.Pending
                DownloadManager.STATUS_RUNNING -> InstallStep.Downloading
                DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> break
                else -> null
            }

            step?.let { trySend(it) }
            
            if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                break
            }
            
            delay(1.seconds)
        }

        awaitClose { }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    /**
     * Starts an intent to install the extension at the given uri.
     *
     * @param uri The uri of the extension to install.
     */
    fun installApk(downloadId: Long, uri: Uri) {
        val intent = Intent(context, ExtensionInstallActivity::class.java)
            .setDataAndType(uri, APK_MIME)
            .putExtra(EXTRA_DOWNLOAD_ID, downloadId)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }

    /**
     * Starts an intent to uninstall the extension by the given package name.
     *
     * @param pkgName The package name of the extension to uninstall
     */
    fun uninstallApk(pkgName: String) {
        val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$pkgName".toUri())
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    /**
     * Sets the result of the installation of an extension.
     *
     * @param downloadId The id of the download.
     * @param result Whether the extension was installed or not.
     */
    fun setInstallationResult(downloadId: Long, result: Boolean) {
        val step = if (result) InstallStep.Installed else InstallStep.Error
        downloadCallbacks[downloadId]?.invoke(step)
    }

    /**
     * Deletes the download for the given package name.
     *
     * @param pkgName The package name of the download to delete.
     */
    private fun deleteDownload(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName)
        if (downloadId != null) {
            downloadManager.remove(downloadId)
        }
        if (activeDownloads.isEmpty()) {
            downloadReceiver.unregister()
        }
    }

    /**
     * Receiver that listens to download status events.
     */
    private inner class DownloadCompletionReceiver : BroadcastReceiver() {

        /**
         * Whether this receiver is currently registered.
         */
        private var isRegistered = false

        /**
         * Registers this receiver if it's not already.
         */
        fun register() {
            if (isRegistered) return
            isRegistered = true

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            ContextCompat.registerReceiver(context, this, filter, ContextCompat.RECEIVER_EXPORTED)
        }

        /**
         * Unregisters this receiver if it's not already.
         */
        fun unregister() {
            if (!isRegistered) return
            isRegistered = false

            context.unregisterReceiver(this)
        }

        /**
         * Called when a download event is received. It looks for the download in the current active
         * downloads and notifies its installation step.
         */
        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return

            // Avoid events for downloads we didn't request
            if (id !in activeDownloads.values) return

            val uri = downloadManager.getUriForDownloadedFile(id)

            // Set next installation step
            if (uri != null) {
                downloadCallbacks[id]?.invoke(InstallStep.Installing)
            } else {
                downloadCallbacks[id]?.invoke(InstallStep.Error)
                return
            }

            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val localUri = cursor.getString(
                        cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    ).removePrefix(FILE_SCHEME)

                    installApk(id, File(localUri).getUriCompat(context))
                }
            }
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val EXTRA_DOWNLOAD_ID = "ExtensionInstaller.extra.DOWNLOAD_ID"
        const val FILE_SCHEME = "file://"
    }
}
