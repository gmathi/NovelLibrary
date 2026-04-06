package io.github.gmathi.novellibrary.service.ai_tts

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

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
    val checksumMd5: String
)

class AiTtsModelManager(private val context: Context) {

    private var currentTts: OfflineTts? = null
    private var currentVoiceId: String? = null

    fun getModelDir(voiceId: String): File =
        File(context.filesDir, "ai_tts/models/$voiceId")

    fun isModelDownloaded(voiceId: String): Boolean {
        val dir = getModelDir(voiceId)
        return File(dir, "model.onnx").exists() && File(dir, "model.onnx.json").exists()
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
            return currentTts!!
        }
        unloadModel()
        val modelDir = getModelDir(voiceId).absolutePath
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$modelDir/model.onnx",
                    lexicon = "",
                    tokens = "$modelDir/tokens.txt",
                    dataDir = "",
                    dictDir = "",
                )
            )
        )
        val tts = OfflineTts(config = config)
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
            "${voice.downloadUrl}.json" to File(modelDir, "model.onnx.json")
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
            checksumMd5 = ""
        )
    )
}
