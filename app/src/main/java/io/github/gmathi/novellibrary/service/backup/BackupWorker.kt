package io.github.gmathi.novellibrary.service.backup

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.service.ProgressNotificationManager
import io.github.gmathi.novellibrary.util.Constants.DATABASES_DIR
import io.github.gmathi.novellibrary.util.Constants.DATA_SUBFOLDER
import io.github.gmathi.novellibrary.util.Constants.FILES_DIR
import io.github.gmathi.novellibrary.util.Constants.SHARED_PREFS_DIR
import io.github.gmathi.novellibrary.util.Constants.SIMPLE_NOVEL_BACKUP_FILE_NAME
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipOutputStream

internal class BackupWorker(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val KEY_SHOULD_BACKUP_SIMPLE_TEX = "shouldBackupSimpleText"
        internal const val KEY_SHOULD_BACKUP_DATA_BASE = "shouldBackupDatabase"
        internal const val KEY_SHOULD_BACKUP_PREFERENCES = "shouldBackupPreferences"
        internal const val KEY_SHOULD_BACKUP_FILES = "shouldBackupFiles"
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
    // endregion

    private lateinit var result: Result

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        ProgressNotificationManager(applicationContext).use { nm ->
            var message: String

            nm.builder
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_backup_white_24dp)
                .setTicker("NovelLibrary Backup")

            setForeground(ForegroundInfo(nm.notificationId, nm.builder.build()))
            nm.newIndeterminateProgress()

            val uri: Uri = dataCenter.backupUri!!

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
                        val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(simpleTextFile)))
                        writer.use { writer.write(jsonString) }
                        nm.updateProgress(3)
                        Utils.zip(simpleTextFile, it)
                    }
                    nm.updateProgress(4)

                    // Backup Databases
                    if (shouldBackupDatabase && currentDBsDir.exists() && currentDBsDir.isDirectory) {
                        nm.updateProgress(6) { setContentText(getString(R.string.title_library)) }
                        delay(1000)
                        Utils.zip(currentDBsDir, it)
                    }
                    nm.updateProgress(8)

                    // Backup Shared Preferences
                    if (shouldBackupPreferences && currentSharedPrefsDir.exists() && currentSharedPrefsDir.isDirectory) {
                        nm.updateProgress(10) { setContentText(getString(R.string.preferences)) }
                        delay(1000)
                        Utils.zip(currentSharedPrefsDir, it)
                    }
                    nm.updateProgress(12)

                    // Backup Files
                    if (shouldBackupFiles && currentFilesDir.exists() && currentFilesDir.isDirectory) {
                        nm.updateProgress(14) { setContentText(getString(R.string.downloaded_files)) }
                        delay(1000)
                        Utils.zip(currentFilesDir, it)
                    }
                    nm.updateProgress(16)
                }

                message = getString(R.string.backup_success)
                nm.closeProgress { setContentText(message) }
                result = Result.success(workDataOf(WORK_KEY_RESULT to message))
            } catch (e: Exception) {
                message =
                    if ("No space left on device" == e.localizedMessage) {
                        val databasesDirSize = Utils.getFolderSize(currentDBsDir)
                        val filesDirSize = Utils.getFolderSize(currentFilesDir)
                        val sharedPrefsDirSize = Utils.getFolderSize(currentSharedPrefsDir)

                        val formattedSize = Formatter.formatFileSize(applicationContext, databasesDirSize + filesDirSize + sharedPrefsDirSize)
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

            // Keep notification alive for the user to see the result
            delay(10000)

            return@withContext result
        }
    }

}