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
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.createNovel
import io.github.gmathi.novellibrary.database.createNovelSection
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.GoogleDriveHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.DATABASES_DIR
import io.github.gmathi.novellibrary.util.Constants.FILES_DIR
import io.github.gmathi.novellibrary.util.Constants.SHARED_PREFS_DIR
import io.github.gmathi.novellibrary.util.Constants.SIMPLE_NOVEL_BACKUP_FILE_NAME
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.recursiveCopy
import io.github.gmathi.novellibrary.util.system.NotificationReceiver
import io.github.gmathi.novellibrary.util.view.ProgressNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uy.kohesive.injekt.injectLazy
import java.io.*

internal class GoogleDriveRestoreWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val KEY_SHOULD_RESTORE_SIMPLE_TEXT = "shouldRestoreSimpleText"
        internal const val KEY_SHOULD_RESTORE_DATABASE = "shouldRestoreDatabase"
        internal const val KEY_SHOULD_RESTORE_PREFERENCES = "shouldRestorePreferences"
        internal const val KEY_SHOULD_RESTORE_FILES = "shouldRestoreFiles"

        const val UNIQUE_WORK_NAME = "google_drive_restore_work"
    }

    private val dbHelper: DBHelper by injectLazy()
    private val cacheDir get() = applicationContext.cacheDir

    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty()) applicationContext.getString(resId)
        else applicationContext.getString(resId, *formatArgs)

    private fun sendBroadcast(intent: Intent) = applicationContext.sendBroadcast(intent)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
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
                .setSmallIcon(R.drawable.ic_cloud_download_white_vector)
                .setTicker("${getString(R.string.app_name)} ${getString(R.string.google_drive_restore)}")
                .setContentTitle("${getString(R.string.app_name)} ${getString(R.string.google_drive_restore)}")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setForeground(ForegroundInfo(nm.notificationId, nm.builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE))
            } else {
                setForeground(ForegroundInfo(nm.notificationId, nm.builder.build()))
            }

            nm.newIndeterminateProgress()

            val shouldSimpleTextRestore = inputData.getBoolean(KEY_SHOULD_RESTORE_SIMPLE_TEXT, true)
            val shouldRestoreDatabase = inputData.getBoolean(KEY_SHOULD_RESTORE_DATABASE, true)
            val shouldRestorePreferences = inputData.getBoolean(KEY_SHOULD_RESTORE_PREFERENCES, true)
            val shouldRestoreFiles = inputData.getBoolean(KEY_SHOULD_RESTORE_FILES, true)

            val dataDir = Environment.getDataDirectory()
            val baseDir = File(dataDir, Constants.DATA_SUBFOLDER)
            val currentDBsDir = File(baseDir, DATABASES_DIR)
            val currentSharedPrefsDir = File(baseDir, SHARED_PREFS_DIR)
            val currentFilesDir = File(baseDir, FILES_DIR)

            val tempBackupFile = File(cacheDir, "google_drive_restore.zip")

            try {
                // Step 1: Download from Google Drive
                nm.newProgress(20) { setContentText(getString(R.string.google_drive_downloading)) }

                val driveHelper = GoogleDriveHelper(applicationContext)
                val downloadResult = driveHelper.downloadBackup(tempBackupFile)

                if (downloadResult.isFailure) {
                    message = getString(R.string.google_drive_restore_no_backup)
                    nm.closeProgress { setContentText(message) }
                    return@withContext Result.failure(workDataOf(WORK_KEY_RESULT to message))
                }
                nm.updateProgress(6)

                // Step 2: Extract ZIP
                nm.updateProgress(7) { setContentText(getString(R.string.eztracting_zip)) }
                Utils.unzip(tempBackupFile, cacheDir)
                nm.updateProgress(8)

                val backupDBsDir = File(cacheDir, DATABASES_DIR)
                val backupFilesDir = File(cacheDir, FILES_DIR)
                val backupSharedPrefsDir = File(cacheDir, SHARED_PREFS_DIR)

                // Step 3: Restore simple text
                nm.updateProgress(9) { setContentText(getString(R.string.simple_text_restore)) }
                val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                if (shouldSimpleTextRestore && simpleTextFile.exists() && simpleTextFile.canRead()) {
                    val reader = BufferedReader(InputStreamReader(FileInputStream(simpleTextFile)))
                    val jsonString = reader.readLine()
                    val jsonObject = JSONObject(jsonString)
                    val novelsArray = jsonObject.getJSONArray("novels")
                    val novelSectionsArray = jsonObject.getJSONArray("novelSections")
                    val oldIdMap = HashMap<Long, String>()
                    val newIdMap = HashMap<String, Long>()
                    for (i in 0 until novelSectionsArray.length()) {
                        val novelSection = novelSectionsArray.getJSONObject(i)
                        val name = novelSection.getString("name")
                        oldIdMap[novelSection.getLong("id")] = name
                        newIdMap[name] = dbHelper.createNovelSection(novelSection.getString("name"))
                    }
                    for (i in 0 until novelsArray.length()) {
                        val novelJson = novelsArray.getJSONObject(i)
                        val novelUrl = novelJson.getString("url")
                        val sourceId = try {
                            novelJson.getLong("sourceId")
                        } catch (e: Exception) {
                            dbHelper.getSourceId(novelUrl)
                        }
                        val novel = Novel(
                            name = novelJson.getString("name"),
                            url = novelUrl,
                            sourceId = sourceId
                        )
                        if (novelJson.has("externalNovelId"))
                            novel.externalNovelId = novelJson.getString("externalNovelId")
                        if (novelJson.has("imageUrl"))
                            novel.imageUrl = novelJson.getString("imageUrl")
                        if (novelJson.has("currentlyReading"))
                            novel.currentChapterUrl = novelJson.getString("currentlyReading")
                        if (novelJson.has("metadata"))
                            novel.metadata = Gson().fromJson(
                                novelJson.getString("metadata"),
                                object : TypeToken<HashMap<String, String>>() {}.type
                            )
                        novel.novelSectionId = newIdMap[oldIdMap[novelJson.getLong("novelSectionId")]] ?: -1L
                        dbHelper.createNovel(novel)
                    }
                }
                nm.updateProgress(11)

                // Step 4: Restore databases
                if (shouldRestoreDatabase && backupDBsDir.exists() && backupDBsDir.isDirectory) {
                    if (!currentDBsDir.exists()) currentDBsDir.mkdir()
                    nm.updateProgress(12) { setContentText(getString(R.string.title_library)) }
                    backupDBsDir.listFiles()?.forEach {
                        Utils.copyFile(it, File(currentDBsDir, it.name))
                    }
                }
                nm.updateProgress(14)

                // Step 5: Restore shared preferences
                if (shouldRestorePreferences && backupSharedPrefsDir.exists() && backupSharedPrefsDir.isDirectory) {
                    if (!currentSharedPrefsDir.exists()) currentSharedPrefsDir.mkdir()
                    nm.updateProgress(15) { setContentText(getString(R.string.preferences)) }
                    backupSharedPrefsDir.listFiles()?.forEach {
                        Utils.copyFile(it, File(currentSharedPrefsDir, it.name))
                    }
                }
                nm.updateProgress(17)

                // Step 6: Restore files
                if (shouldRestoreFiles && backupFilesDir.exists() && backupFilesDir.isDirectory) {
                    if (!currentFilesDir.exists()) currentFilesDir.mkdir()
                    nm.updateProgress(18) { setContentText(getString(R.string.downloaded_files)) }
                    recursiveCopy(backupFilesDir, currentFilesDir)
                }
                nm.updateProgress(20)

                message = getString(R.string.google_drive_restore_success)
                nm.closeProgress { setContentText(message) }

                sendBroadcast(
                    Intent(applicationContext, NotificationReceiver::class.java).setAction(
                        NotificationReceiver.ACTION_SEND_NOTIFICATION
                    ).putExtra(NotificationReceiver.EXTRA_NOTIFICATION, nm.builder.build())
                )

                return@withContext Result.success(workDataOf(WORK_KEY_RESULT to message))
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
                    getString(R.string.google_drive_restore_fail) + ": " + (e.localizedMessage ?: "Unknown error")
                }
                nm.closeProgress { setContentText(message) }
                return@withContext Result.failure(workDataOf(WORK_KEY_RESULT to message))
            } finally {
                if (tempBackupFile.exists()) tempBackupFile.delete()
            }
        }
    }
}
