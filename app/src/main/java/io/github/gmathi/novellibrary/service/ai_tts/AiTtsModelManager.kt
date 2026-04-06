package io.github.gmathi.novellibrary.service.ai_tts

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

private const val TAG = "AiTtsModelManager"

sealed class ModelDownloadState {
    data object NotDownloaded : ModelDownloadState()
    data class Downloading(val progress: Float) : ModelDownloadState()
    data object Downloaded : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

data class AiTtsVoiceInfo(
    val id: String,
    val name: String,
    val language: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val tokensUrl: String,
    val checksumMd5: String
)

class AiTtsModelManager(private val context: Context) {

    private var currentTts: OfflineTts? = null
    private var currentVoiceId: String? = null

    fun getModelDir(voiceId: String): File =
        File(context.filesDir, "ai_tts/models/$voiceId")

    fun isModelDownloaded(voiceId: String): Boolean {
        val dir = getModelDir(voiceId)
        val onnx = File(dir, "model.onnx")
        val json = File(dir, "model.onnx.json")
        val tokens = File(dir, "tokens.txt")
        Logs.debug(TAG, "isModelDownloaded($voiceId): dir=${dir.absolutePath}")
        Logs.debug(TAG, "  model.onnx    exists=${onnx.exists()} size=${if (onnx.exists()) onnx.length() else -1}")
        Logs.debug(TAG, "  model.onnx.json exists=${json.exists()} size=${if (json.exists()) json.length() else -1}")
        Logs.debug(TAG, "  tokens.txt    exists=${tokens.exists()} size=${if (tokens.exists()) tokens.length() else -1}")
        val result = onnx.exists() && json.exists() && tokens.exists()
        Logs.debug(TAG, "  -> isModelDownloaded=$result")
        return result
    }

    fun verifyModel(voiceId: String): Boolean = isModelDownloaded(voiceId)

    fun deleteModel(voiceId: String) {
        getModelDir(voiceId).deleteRecursively()
        if (currentVoiceId == voiceId) {
            unloadModel()
        }
    }

    fun loadModel(voiceId: String): OfflineTts {
        if (currentVoiceId == voiceId && currentTts != null) {
            Logs.debug(TAG, "loadModel($voiceId): returning cached model")
            return currentTts!!
        }
        unloadModel()
        val modelDir = getModelDir(voiceId)
        Logs.debug(TAG, "loadModel($voiceId): loading from ${modelDir.absolutePath}")

        // Log each required file before attempting to load
        listOf("model.onnx", "model.onnx.json", "tokens.txt").forEach { name ->
            val f = File(modelDir, name)
            Logs.debug(TAG, "  $name: exists=${f.exists()} size=${if (f.exists()) f.length() else -1}")
        }

        val modelDirPath = modelDir.absolutePath
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$modelDirPath/model.onnx",
                    lexicon = "",
                    tokens = "$modelDirPath/tokens.txt",
                    dataDir = "",
                    dictDir = "",
                )
            )
        )
        Logs.debug(TAG, "loadModel($voiceId): creating OfflineTts with config")
        val tts = OfflineTts(config = config)
        Logs.debug(TAG, "loadModel($voiceId): OfflineTts created, sampleRate=${tts.sampleRate()}")
        currentTts = tts
        currentVoiceId = voiceId
        return tts
    }

    fun unloadModel() {
        currentTts = null
        currentVoiceId = null
    }

    suspend fun downloadModel(
        voice: AiTtsVoiceInfo,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val modelDir = getModelDir(voice.id)
        modelDir.mkdirs()

        val filesToDownload = listOf(
            voice.downloadUrl to File(modelDir, "model.onnx"),
            "${voice.downloadUrl}.json" to File(modelDir, "model.onnx.json"),
            voice.tokensUrl to File(modelDir, "tokens.txt")
        )
        val totalFiles = filesToDownload.size

        try {
            filesToDownload.forEachIndexed { fileIndex, (url, destFile) ->
                val connection = URL(url).openConnection().apply { connect() }
                val contentLength = connection.contentLengthLong
                connection.getInputStream().use { input ->
                    destFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead = 0L
                        var n: Int
                        while (input.read(buffer).also { n = it } != -1) {
                            output.write(buffer, 0, n)
                            bytesRead += n
                            if (contentLength > 0) {
                                val fileProgress = bytesRead.toFloat() / contentLength
                                val overall = (fileIndex + fileProgress) / totalFiles
                                withContext(Dispatchers.Main) { onProgress(overall) }
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onComplete() }
        } catch (e: Exception) {
            modelDir.deleteRecursively()
            withContext(Dispatchers.Main) { onError(e.message ?: "Download failed") }
        }
    }

    fun availableVoices(): List<AiTtsVoiceInfo> = listOf(
        AiTtsVoiceInfo(
            id = "en_US-ryan-high",
            name = "Ryan (US English, High Quality)",
            language = "en-US",
            sizeBytes = 63_000_000L,
            downloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/high/en_US-ryan-high.onnx",
            tokensUrl = "https://huggingface.co/csukuangfj/vits-piper-en_US-ryan-high/resolve/main/tokens.txt",
            checksumMd5 = ""
        )
    )
}
