package io.github.gmathi.novellibrary.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.google.gson.Gson
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.storage.notNullAndExists
import io.github.gmathi.novellibrary.util.system.NotificationReceiver
import io.github.gmathi.novellibrary.util.view.ProgressNotificationManager
import io.github.gmathi.novellibrary.util.Constants.DATABASES_DIR
import io.github.gmathi.novellibrary.util.Constants.DATA_SUBFOLDER
import io.github.gmathi.novellibrary.util.Constants.FILES_DIR
import io.github.gmathi.novellibrary.util.Constants.SHARED_PREFS_DIR
import io.github.gmathi.novellibrary.util.Constants.SIMPLE_NOVEL_BACKUP_FILE_NAME
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipOutputStream

internal class BackupWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val KEY_URI = "restore_uri"

        internal const val KEY_SHOULD_BACKUP_SIMPLE_TEX = "shouldBackupSimpleText"
        internal const val KEY_SHOULD_BACKUP_DATA_BASE = "shouldBackupDatabase"
        internal const val KEY_SHOULD_BACKUP_PREFERENCES = "shouldBackupPreferences"
        internal const val KEY_SHOULD_BACKUP_FILES = "shouldBackupFiles"

        const val UNIQUE_WORK_NAME = "backup_work"
    }

    // region Context wrapper (to improve readability)
    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty())
            applicationContext.getString(resId)
        else
            applicationContext.getString(resId, formatArgs)

    private val contentResolver
        get() = applicationContext.contentResolver

    private val cacheDir
        get() = applicationContext.cacheDir

    private fun sendBroadcast(intent: Intent) =
        applicationContext.sendBroadcast(intent)
    // endregion

    private lateinit var result: Result

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
                .setSmallIcon(R.drawable.ic_backup_white_24dp)
                .setTicker("${getString(R.string.app_name)} ${getString(R.string.backup)}")
                .setContentTitle("${getString(R.string.app_name)} ${getString(R.string.backup)}")

            setForeground(ForegroundInfo(nm.notificationId, nm.builder.build()))
            nm.newIndeterminateProgress()

            val shouldSimpleTextBackup = inputData.getBoolean(KEY_SHOULD_BACKUP_SIMPLE_TEX, true)
            val shouldBackupDatabase = inputData.getBoolean(KEY_SHOULD_BACKUP_DATA_BASE, true)
            val shouldBackupPreferences = inputData.getBoolean(KEY_SHOULD_BACKUP_PREFERENCES, true)
            val shouldBackupFiles = inputData.getBoolean(KEY_SHOULD_BACKUP_FILES, true)

            val dataDir = Environment.getDataDirectory()
            val baseDir = File(dataDir, DATA_SUBFOLDER)

            val currentDBsDir = File(baseDir, DATABASES_DIR)
            val currentSharedPrefsDir = File(baseDir, SHARED_PREFS_DIR)
            val currentFilesDir = File(baseDir, FILES_DIR)

            try {
                val uri: Uri? = Uri.parse(inputData.getString(KEY_URI))
                if (uri != null) contentResolver.notifyChange(uri, null)
                if (uri != null && DocumentFile.fromSingleUri(applicationContext, uri)
                        .notNullAndExists()
                ) {
                    ZipOutputStream(BufferedOutputStream(contentResolver.openOutputStream(uri)!!)).use {
                        nm.newProgress(16) { setContentText(getString(R.string.simple_text_backup)) }

                        // Backup To TextFile
                        if (shouldSimpleTextBackup) {
                            val novelsArray = dbHelper.getAllNovels()
                            val novelSectionsArray = dbHelper.getAllNovelSections()
                            val map = HashMap<String, Any>()
                            map["novels"] = novelsArray
                            map["novelSections"] = novelSectionsArray
                            nm.updateProgress(1)
                            val jsonString = Gson().toJson(map)
                            nm.updateProgress(2)
                            val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                            val writer =
                                BufferedWriter(OutputStreamWriter(FileOutputStream(simpleTextFile)))
                            writer.use { writer.write(jsonString) }
                            nm.updateProgress(3)
                            Utils.zip(simpleTextFile, it)
                        }
                        nm.updateProgress(4)

                        // Backup Databases
                        if (shouldBackupDatabase && currentDBsDir.exists() && currentDBsDir.isDirectory) {
                            nm.updateProgress(6) { setContentText(getString(R.string.title_library)) }
                            Utils.zip(currentDBsDir, it)
                        }
                        nm.updateProgress(8)

                        // Backup Shared Preferences
                        if (shouldBackupPreferences && currentSharedPrefsDir.exists() && currentSharedPrefsDir.isDirectory) {
                            nm.updateProgress(10) { setContentText(getString(R.string.preferences)) }
                            Utils.zip(currentSharedPrefsDir, it)
                        }
                        nm.updateProgress(12)

                        // Backup Files
                        if (shouldBackupFiles && currentFilesDir.exists() && currentFilesDir.isDirectory) {
                            nm.updateProgress(14) { setContentText(getString(R.string.downloaded_files)) }
                            Utils.zip(currentFilesDir, it)
                        }
                        nm.updateProgress(16)
                    }

                    message = getString(R.string.backup_success)
                    nm.closeProgress { setContentText(message) }
                    result = Result.success(workDataOf(WORK_KEY_RESULT to message))
                } else {
                    message = getString(R.string.backup_file_not_found)
                    nm.closeProgress { setContentText(message) }
                    result = Result.failure(workDataOf(WORK_KEY_RESULT to message))
                    dataCenter.backupFrequency = 0
                    WorkManager.getInstance(applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
                }
            } catch (e: Exception) {
                message =
                    if ("No space left on device" == e.localizedMessage) {
                        val databasesDirSize = Utils.getFolderSize(currentDBsDir)
                        val filesDirSize = Utils.getFolderSize(currentFilesDir)
                        val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsDir)

                        val formattedSize = Formatter.formatFileSize(
                            applicationContext,
                            databasesDirSize + filesDirSize + sharedPrefsDirSize
                        )
                        getString(R.string.need_more_space, formattedSize)
                    } else {
                        getString(R.string.backup_fail)
                    }

                nm.closeProgress { setContentText(message) }
                result = Result.failure(workDataOf(WORK_KEY_RESULT to message))
            }

            if (!::result.isInitialized) { // Just in case
                message = getString(R.string.backup_fail)
                nm.closeProgress { setContentText(message) }
                result = Result.failure(workDataOf(WORK_KEY_RESULT to message))
            }

            nm.waitForQueue()

            sendBroadcast(
                Intent(applicationContext, NotificationReceiver::class.java).setAction(
                    NotificationReceiver.ACTION_SEND_NOTIFICATION
                ).putExtra(NotificationReceiver.EXTRA_NOTIFICATION, nm.builder.build())
            )

            return@withContext result
        }
    }

}