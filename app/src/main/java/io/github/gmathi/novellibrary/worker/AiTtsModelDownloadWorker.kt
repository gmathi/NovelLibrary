package io.github.gmathi.novellibrary.worker

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.service.ai_tts.TtsEngineType
import io.github.gmathi.novellibrary.util.notification.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AiTtsModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val voiceId = inputData.getString(KEY_VOICE_ID) ?: return@withContext Result.failure()
        val modelManager = AiTtsModelManager(applicationContext)

        // Find voice info
        val voiceInfo = modelManager.availableVoices().find { it.id == voiceId }
            ?: return@withContext Result.failure(
                workDataOf("error" to "Unknown voice: $voiceId")
            )

        val modelDir = modelManager.getModelDir(voiceId)
        val tempDir = File(applicationContext.cacheDir, "ai_tts_download/$voiceId")
        tempDir.mkdirs()

        try {
            // Build the list of files to download based on engine type
            val filesToDownload = mutableListOf(
                voiceInfo.downloadUrl to "model.onnx",
                voiceInfo.tokensUrl to "tokens.txt"
            )
            if (voiceInfo.engineType == TtsEngineType.KOKORO && voiceInfo.voicesBinUrl.isNotEmpty()) {
                filesToDownload.add(voiceInfo.voicesBinUrl to "voices.bin")
            }
            val totalFiles = filesToDownload.size

            filesToDownload.forEachIndexed { fileIndex, (url, fileName) ->
                val basePercent = (fileIndex * 100) / totalFiles
                val filePercent = 100 / totalFiles
                postProgress(voiceId, basePercent, "Downloading $fileName...")
                downloadFile(
                    url = url,
                    dest = File(tempDir, fileName)
                ) { progress ->
                    postProgress(voiceId, basePercent + (progress * filePercent / 100), "Downloading $fileName...")
                }
            }

            // Move atomically to final location
            modelDir.mkdirs()
            tempDir.listFiles()?.forEach { file ->
                file.copyTo(File(modelDir, file.name), overwrite = true)
            }

            // Broadcast model ready.
            // setPackage() is required on Android 14+ to deliver to RECEIVER_NOT_EXPORTED
            // dynamically-registered receivers within the same app.
            applicationContext.sendBroadcast(
                Intent(ACTION_MODEL_READY)
                    .setPackage(applicationContext.packageName)
                    .putExtra(KEY_VOICE_ID, voiceId)
            )

            postComplete(voiceId)
            Result.success(workDataOf(KEY_VOICE_ID to voiceId))
        } catch (e: Exception) {
            tempDir.deleteRecursively()
            postError(voiceId, e.message ?: "Download failed")
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private fun downloadFile(url: String, dest: File, onProgress: (Int) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connect()
        val total = connection.contentLengthLong
        var downloaded = 0L
        connection.inputStream.use { input ->
            dest.outputStream().use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    if (total > 0) onProgress((downloaded * 100 / total).toInt())
                }
            }
        }
    }

    private fun postProgress(voiceId: String, percent: Int, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_AI_TTS_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading AI TTS model")
            .setContentText("$voiceId — $message")
            .setProgress(100, percent, false)
            .setOngoing(true)
            .build()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(Notifications.ID_AI_TTS_DOWNLOAD, notification)
        }
        // Broadcast progress so the service can update its playback state UI
        applicationContext.sendBroadcast(
            Intent(ACTION_MODEL_DOWNLOAD_PROGRESS)
                .setPackage(applicationContext.packageName)
                .putExtra(KEY_VOICE_ID, voiceId)
                .putExtra(KEY_PROGRESS, percent)
        )
    }

    private fun postComplete(voiceId: String) {
        val notification = NotificationCompat.Builder(applicationContext, Notifications.CHANNEL_AI_TTS_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AI TTS model ready")
            .setContentText(voiceId)
            .setAutoCancel(true)
            .build()
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(Notifications.ID_AI_TTS_DOWNLOAD, notification)
        }
    }

    private fun postError(voiceId: String, message: String) {
        notificationManager.cancel(Notifications.ID_AI_TTS_DOWNLOAD)
    }

    companion object {
        const val KEY_VOICE_ID = "voice_id"
        const val KEY_PROGRESS = "progress"
        const val ACTION_MODEL_READY = "io.github.gmathi.novellibrary.AI_TTS_MODEL_READY"
        const val ACTION_MODEL_DOWNLOAD_PROGRESS = "io.github.gmathi.novellibrary.AI_TTS_MODEL_DOWNLOAD_PROGRESS"

        fun enqueue(context: Context, voiceId: String): androidx.work.Operation {
            val request = OneTimeWorkRequestBuilder<AiTtsModelDownloadWorker>()
                .setInputData(workDataOf(KEY_VOICE_ID to voiceId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            return WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "ai_tts_download_$voiceId",
                    ExistingWorkPolicy.KEEP,
                    request
                )
        }
    }
}
