package io.github.gmathi.novellibrary.worker

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.GoogleDriveHelper
import io.github.gmathi.novellibrary.util.Constants.DATABASES_DIR
import io.github.gmathi.novellibrary.util.Constants.DATA_SUBFOLDER
import io.github.gmathi.novellibrary.util.Constants.FILES_DIR
import io.github.gmathi.novellibrary.util.Constants.SHARED_PREFS_DIR
import io.github.gmathi.novellibrary.util.Constants.SIMPLE_NOVEL_BACKUP_FILE_NAME
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.system.NotificationReceiver
import io.github.gmathi.novellibrary.util.view.ProgressNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipOutputStream

internal class GoogleDriveBackupWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val KEY_SHOULD_BACKUP_SIMPLE_TEXT = "shouldBackupSimpleText"
        internal const val KEY_SHOULD_BACKUP_DATABASE = "shouldBackupDatabase"
        internal const val KEY_SHOULD_BACKUP_PREFERENCES = "shouldBackupPreferences"
        internal const val KEY_SHOULD_BACKUP_FILES = "shouldBackupFiles"

        const val UNIQUE_WORK_NAME = "google_drive_backup_work"
    }

    private val dbHelper: DBHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()

    private val cacheDir get() = applicationContext.cacheDir

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty()) applicationContext.getString(resId)
        else applicationContext.getString(resId, *formatArgs)

    private fun sendBroadcast(intent: Intent) = applicationContext.sendBroadcast(intent)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        dataCenter.lastBackup = System.currentTimeMillis()

        ProgressNotificationManager(
            applicationContext,
            getString(R.string.backup_and_restore_notification_channel_id),
            getString(R.string.backup_and_restore_notification_channel_name),
            NotificationManagerCompat.IMPORTANCE_LOW
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                description = getString(R.string.backup_and_restore_notification_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
        }.use { nm ->
            var message: String

            nm.builder
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_cloud_upload_white_vector)
                .setTicker("${getString(R.string.app_name)} ${getString(R.string.google_drive_backup)}")
                .setContentTitle("${getString(R.string.app_name)} ${getString(R.string.google_drive_backup)}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForeground(ForegroundInfo(nm.notificationId, nm.builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE))
            } else {
                setForeground(ForegroundInfo(nm.notificationId, nm.builder.build()))
            }

            nm.newIndeterminateProgress()

            val shouldSimpleTextBackup = inputData.getBoolean(KEY_SHOULD_BACKUP_SIMPLE_TEXT, true)
            val shouldBackupDatabase = inputData.getBoolean(KEY_SHOULD_BACKUP_DATABASE, true)
            val shouldBackupPreferences = inputData.getBoolean(KEY_SHOULD_BACKUP_PREFERENCES, true)
            val shouldBackupFiles = inputData.getBoolean(KEY_SHOULD_BACKUP_FILES, true)

            val dataDir = Environment.getDataDirectory()
            val baseDir = File(dataDir, DATA_SUBFOLDER)
            val currentDBsDir = File(baseDir, DATABASES_DIR)
            val currentSharedPrefsDir = File(baseDir, SHARED_PREFS_DIR)
            val currentFilesDir = File(baseDir, FILES_DIR)

            val tempBackupFile = File(cacheDir, "google_drive_backup.zip")

            try {
                // Step 1: Create local ZIP backup
                nm.newProgress(20) { setContentText(getString(R.string.google_drive_creating_backup)) }

                ZipOutputStream(BufferedOutputStream(FileOutputStream(tempBackupFile))).use { zos ->
                    if (shouldSimpleTextBackup) {
                        val novelsArray = dbHelper.getAllNovels()
                        val novelSectionsArray = dbHelper.getAllNovelSections()
                        val map = HashMap<String, Any>()
                        map["novels"] = novelsArray
                        map["novelSections"] = novelSectionsArray
                        nm.updateProgress(2)
                        val jsonString = Gson().toJson(map)
                        val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(simpleTextFile)))
                        writer.use { it.write(jsonString) }
                        nm.updateProgress(3)
                        Utils.zip(simpleTextFile, zos)
                    }
                    nm.updateProgress(4)

                    if (shouldBackupDatabase && currentDBsDir.exists() && currentDBsDir.isDirectory) {
                        nm.updateProgress(6) { setContentText(getString(R.string.title_library)) }
                        Utils.zip(currentDBsDir, zos)
                    }
                    nm.updateProgress(8)

                    if (shouldBackupPreferences && currentSharedPrefsDir.exists() && currentSharedPrefsDir.isDirectory) {
                        nm.updateProgress(10) { setContentText(getString(R.string.preferences)) }
                        Utils.zip(currentSharedPrefsDir, zos)
                    }
                    nm.updateProgress(12)

                    if (shouldBackupFiles && currentFilesDir.exists() && currentFilesDir.isDirectory) {
                        nm.updateProgress(14) { setContentText(getString(R.string.downloaded_files)) }
                        Utils.zip(currentFilesDir, zos)
                    }
                }
                nm.updateProgress(16)

                // Step 2: Upload to Google Drive
                nm.updateProgress(17) { setContentText(getString(R.string.google_drive_uploading)) }

                val driveHelper = GoogleDriveHelper(applicationContext)
                val uploadResult = driveHelper.uploadBackup(tempBackupFile)

                if (uploadResult.isSuccess) {
                    val backupInfo = uploadResult.getOrNull()
                    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    dataCenter.lastCloudBackupTimestamp = sdf.format(Date())
                    dataCenter.lastBackupSize = backupInfo?.getFormattedSize() ?: "N/A"

                    nm.updateProgress(20)
                    message = getString(R.string.google_drive_backup_success)
                    nm.closeProgress { setContentText(message) }
                    return@withContext Result.success(workDataOf(WORK_KEY_RESULT to message))
                } else {
                    message = getString(R.string.google_drive_backup_fail)
                    nm.closeProgress { setContentText(message) }
                    return@withContext Result.failure(workDataOf(WORK_KEY_RESULT to message))
                }
            } catch (e: Exception) {
                message = if ("No space left on device" == e.localizedMessage) {
                    val databasesDirSize = Utils.getFolderSize(currentDBsDir)
                    val filesDirSize = Utils.getFolderSize(currentFilesDir)
                    val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsDir)
                    val formattedSize = Formatter.formatFileSize(
                        applicationContext,
                        databasesDirSize + filesDirSize + sharedPrefsDirSize
                    )
                    getString(R.string.need_more_space, formattedSize)
                } else {
                    getString(R.string.google_drive_backup_fail) + ": " + (e.localizedMessage ?: "Unknown error")
                }
                nm.closeProgress { setContentText(message) }
                return@withContext Result.failure(workDataOf(WORK_KEY_RESULT to message))
            } finally {
                if (tempBackupFile.exists()) tempBackupFile.delete()
            }
        }
    }
}
