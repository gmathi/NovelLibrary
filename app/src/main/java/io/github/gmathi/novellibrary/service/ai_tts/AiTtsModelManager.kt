package io.github.gmathi.novellibrary.service.ai_tts

import android.content.Context
import android.os.Build
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

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
    val checksumMd5: String,
    /**
     * ABIs this model is compatible with (e.g. "arm64-v8a", "x86_64").
     * Empty set means the model works on all architectures.
     */
    val supportedAbis: Set<String> = emptySet()
)

class AiTtsModelManager(private val context: Context) {

    /** The primary ABI this process is running under (e.g. "arm64-v8a", "x86_64"). */
    val primaryAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

    /**
     * True when running on a native ARM device.
     * False on x86/x86_64 (Android emulator), where the ONNX Runtime x86 JNI library
     * has known thread-pool initialization races that crash the process.
     */
    val isNativelySupported: Boolean = !primaryAbi.startsWith("x86")

    // Single dedicated thread for ALL native sherpa-onnx / ONNX-Runtime calls.
    // ORT's global inter-op thread pool is initialized once on first session creation;
    // dispatching that call (or any subsequent generate() call) from different
    // Dispatchers.IO threads can race on ORT's internal pthread_mutex structures
    // and cause a FORTIFY abort.  A single-thread executor eliminates that race.
    private val nativeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ai-tts-native").also { it.isDaemon = true }
    }
    val nativeDispatcher = nativeExecutor.asCoroutineDispatcher()

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
                ),
                // numThreads defaults to 0 (= all CPU cores). With many cores the ONNX Runtime
                // thread pool has a race during initialization that destroys a pthread mutex while
                // another thread is still trying to lock it → FORTIFY abort. Force single-threaded.
                numThreads = 1,
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
        currentTts?.release()  // Free native resources synchronously; prevents GC finalizer race
        currentTts = null
        currentVoiceId = null
    }

    /** Call from [AiTtsPlayer.destroy] after all native work has completed. */
    fun close() {
        nativeDispatcher.close()
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

    /**
     * Returns voices compatible with the current device ABI.
     * Voices with an empty [AiTtsVoiceInfo.supportedAbis] run on all architectures.
     */
    fun availableVoices(): List<AiTtsVoiceInfo> =
        ALL_VOICES.filter { it.supportedAbis.isEmpty() || primaryAbi in it.supportedAbis }

    /** The default voice ID to use for the current device ABI. */
    fun defaultVoiceId(): String =
        availableVoices().firstOrNull()?.id ?: ALL_VOICES.first().id

    companion object {
        // Shared tokens file for all English piper/VITS models.
        private const val EN_TOKENS_URL =
            "https://huggingface.co/csukuangfj/vits-piper-en_US-ryan-high/resolve/main/tokens.txt"

        private val ALL_VOICES = listOf(

            // ── ARM64 / physical devices ──────────────────────────────────────────────
            // High-quality 120 MB VITS model.  The ORT x86_64 JNI library has a
            // thread-pool init race that crashes the process when loading this model,
            // so it is restricted to native ARM builds only.
            AiTtsVoiceInfo(
                id = "en_US-ryan-high",
                name = "Ryan (US English, High Quality)",
                language = "en-US",
                sizeBytes = 120_786_792L,
                downloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/high/en_US-ryan-high.onnx",
                tokensUrl = EN_TOKENS_URL,
                checksumMd5 = "",
                supportedAbis = setOf("arm64-v8a", "armeabi-v7a")
            ),

            // ── x86 / x86_64 (Android emulator) ─────────────────────────────────────
            // Lightweight ~24 MB model.  Smaller graph → less ORT thread-pool pressure,
            // making it safer on the emulator's x86_64 ORT build.
            AiTtsVoiceInfo(
                id = "en_US-amy-low",
                name = "Amy (US English, Low Quality)",
                language = "en-US",
                sizeBytes = 24_000_000L,
                downloadUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/low/en_US-amy-low.onnx",
                tokensUrl = EN_TOKENS_URL,
                checksumMd5 = "",
                supportedAbis = setOf("x86_64", "x86")
            )
        )
    }
}
