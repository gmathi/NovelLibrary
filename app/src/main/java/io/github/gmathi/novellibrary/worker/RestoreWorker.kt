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
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.createNovel
import io.github.gmathi.novellibrary.database.createNovelSection
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.system.NotificationReceiver
import io.github.gmathi.novellibrary.util.view.ProgressNotificationManager
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.DATABASES_DIR
import io.github.gmathi.novellibrary.util.Constants.FILES_DIR
import io.github.gmathi.novellibrary.util.Constants.SHARED_PREFS_DIR
import io.github.gmathi.novellibrary.util.Constants.SIMPLE_NOVEL_BACKUP_FILE_NAME
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.recursiveCopy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*

internal class RestoreWorker(context: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(context, workerParameters) {

    companion object {
        internal const val KEY_URI = "restore_uri"

        internal const val KEY_SHOULD_RESTORE_SIMPLE_TEX = "shouldRestoreSimpleText"
        internal const val KEY_SHOULD_RESTORE_DATA_BASE = "shouldRestoreDatabase"
        internal const val KEY_SHOULD_RESTORE_PREFERENCES = "shouldRestorePreferences"
        internal const val KEY_SHOULD_RESTORE_FILES = "shouldRestoreFiles"

        const val UNIQUE_WORK_NAME = "restore_work"
    }

    // region context wrapper (to improve readability)
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
        ProgressNotificationManager(
            applicationContext,
            getString(R.string.backup_and_restore_notification_channel_id),
            getString(R.string.backup_and_restore_notification_channel_name),
            NotificationManagerCompat.IMPORTANCE_LOW
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                description =
                    getString(R.string.backup_and_restore_notification_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
        }.use { nm ->
            var message: String

            nm.builder
                .setLocalOnly(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_cloud_download_white_vector)
                .setTicker("${getString(R.string.app_name)} ${getString(R.string.restore)}")
                .setContentTitle("${getString(R.string.app_name)} ${getString(R.string.restore)}")

            setForeground(ForegroundInfo(nm.notificationId, nm.builder.build()))
            nm.newIndeterminateProgress()

            val shouldSimpleTextRestore = inputData.getBoolean(KEY_SHOULD_RESTORE_SIMPLE_TEX, true)
            val shouldRestoreDatabase = inputData.getBoolean(KEY_SHOULD_RESTORE_DATA_BASE, true)
            val shouldRestorePreferences =
                inputData.getBoolean(KEY_SHOULD_RESTORE_PREFERENCES, true)
            val shouldRestoreFiles = inputData.getBoolean(KEY_SHOULD_RESTORE_FILES, true)

            val dataDir = Environment.getDataDirectory()
            val baseDir = File(dataDir, Constants.DATA_SUBFOLDER)

            val currentDBsDir = File(baseDir, DATABASES_DIR)
            val currentSharedPrefsDir = File(baseDir, SHARED_PREFS_DIR)
            val currentFilesDir = File(baseDir, FILES_DIR)

            try {
                val uri: Uri = Uri.parse(inputData.getString(KEY_URI))

                nm.newIndeterminateProgress { setContentText(getString(R.string.eztracting_zip)) }
                // TODO: if possible extract selectively, directly to the data directory without using the cache directory
                Utils.unzip(contentResolver, uri, cacheDir)

                val backupDBsDir = File(cacheDir, DATABASES_DIR)
                val backupFilesDir = File(cacheDir, FILES_DIR)
                val backupSharedPrefsDir = File(cacheDir, SHARED_PREFS_DIR)

                nm.newProgress(16) { setContentText(getString(R.string.simple_text_restore)) }

                // Restore From Text File
                val simpleTextFile = File(cacheDir, SIMPLE_NOVEL_BACKUP_FILE_NAME)
                if (shouldSimpleTextRestore && simpleTextFile.exists() && simpleTextFile.canRead()) {
                    val reader = BufferedReader(InputStreamReader(FileInputStream(simpleTextFile)))
                    val jsonString = reader.readLine()
                    val jsonObject = JSONObject(jsonString)
                    nm.updateProgress(1)
                    val novelsArray = jsonObject.getJSONArray("novels")
                    val novelSectionsArray = jsonObject.getJSONArray("novelSections")
                    val oldIdMap = HashMap<Long, String>()
                    val newIdMap = HashMap<String, Long>()
                    nm.updateProgress(2)
                    for (i in 0 until novelSectionsArray.length()) {
                        val novelSection = novelSectionsArray.getJSONObject(i)
                        val name = novelSection.getString("name")
                        oldIdMap[novelSection.getLong("id")] = name
                        newIdMap[name] = dbHelper.createNovelSection(novelSection.getString("name"))
                    }
                    nm.updateProgress(3)
                    for (i in 0 until novelsArray.length()) {
                        val novelJson = novelsArray.getJSONObject(i)
                        val novel = Novel(
                            name = novelJson.getString("name"),
                            url = novelJson.getString("url")
                        )
                        if (novelJson.has("imageUrl"))
                            novel.imageUrl = novelJson.getString("imageUrl")
                        if (novelJson.has("currentlyReading"))
                            novel.currentChapterUrl = novelJson.getString("currentlyReading")
                        if (novelJson.has("metadata"))
                            novel.metadata = Gson().fromJson(
                                novelJson.getString("metadata"),
                                object : TypeToken<HashMap<String, String>>() {}.type
                            )

                        novel.novelSectionId =
                            newIdMap[oldIdMap[novelJson.getLong("novelSectionId")]]
                                ?: -1L
                        dbHelper.createNovel(novel)
                    }
                }
                nm.updateProgress(4)

                //Restore Databases
                if (shouldRestoreDatabase && backupDBsDir.exists() && backupDBsDir.isDirectory) {
                    if (!currentDBsDir.exists()) currentDBsDir.mkdir()
                    nm.updateProgress(6) { setContentText(getString(R.string.title_library)) }
                    backupDBsDir.listFiles()?.forEach {
                        Utils.copyFile(it, File(currentDBsDir, it.name))
                    }
                }
                nm.updateProgress(8)

                //Restore Shared Preferences
                if (shouldRestorePreferences && backupSharedPrefsDir.exists() && backupSharedPrefsDir.isDirectory) {
                    if (!currentSharedPrefsDir.exists()) currentSharedPrefsDir.mkdir()
                    nm.updateProgress(10) { setContentText(getString(R.string.preferences)) }
                    backupSharedPrefsDir.listFiles()?.forEach {
                        Utils.copyFile(it, File(currentSharedPrefsDir, it.name))
                    }
                }
                nm.updateProgress(12)

                //Restore Files
                if (shouldRestoreFiles && backupFilesDir.exists() && backupFilesDir.isDirectory) {
                    if (!currentFilesDir.exists()) currentFilesDir.mkdir()
                    nm.updateProgress(14) { setContentText(getString(R.string.downloaded_files)) }
                    recursiveCopy(backupFilesDir, currentFilesDir)
                }
                nm.updateProgress(16)

                message = getString(R.string.restore_success)
                nm.closeProgress { setContentText(message) }
                result = Result.success(workDataOf(WORK_KEY_RESULT to message))
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
                        getString(R.string.restore_fail)
                    }

                nm.closeProgress { setContentText(message) }
                result = Result.failure(workDataOf(WORK_KEY_RESULT to message))
            }

            if (!::result.isInitialized) { // Just in case
                message = getString(R.string.restore_fail)
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