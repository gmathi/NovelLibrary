package io.github.gmathi.novellibrary.service.ai_tts

import android.content.Context
import android.os.Build
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

private const val TAG = "AiTtsModelManager"

sealed class ModelDownloadState {
    data object NotDownloaded : ModelDownloadState()
    data class Downloading(val progress: Float) : ModelDownloadState()
    data object Downloaded : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}

/** Engine type matching the reference project's model types. */
enum class TtsEngineType { VITS, KOKORO }

data class AiTtsVoiceInfo(
    val id: String,
    val name: String,
    val language: String,
    val sizeBytes: Long,
    val engineType: TtsEngineType,
    val downloadUrl: String,
    val tokensUrl: String,
    /** Only required for Kokoro models. */
    val voicesBinUrl: String = "",
    val checksumMd5: String = "",
    val supportedAbis: Set<String> = emptySet()
)

class AiTtsModelManager(private val context: Context) {

    val primaryAbi: String = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    val isNativelySupported: Boolean = !primaryAbi.startsWith("x86")

    private val nativeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ai-tts-native").also { it.isDaemon = true }
    }
    val nativeDispatcher = nativeExecutor.asCoroutineDispatcher()

    private var currentTts: OfflineTts? = null
    private var currentVoiceId: String? = null

    fun getModelDir(voiceId: String): File =
        File(context.filesDir, "ai_tts/models/$voiceId")

    fun isModelDownloaded(voiceId: String): Boolean {
        val voice = ALL_VOICES.find { it.id == voiceId } ?: return false
        val dir = getModelDir(voiceId)
        val onnx = File(dir, "model.onnx")
        val tokens = File(dir, "tokens.txt")
        val baseOk = onnx.exists() && onnx.length() > 0 && tokens.exists() && tokens.length() > 0
        return if (voice.engineType == TtsEngineType.KOKORO) {
            val voices = File(dir, "voices.bin")
            baseOk && voices.exists() && voices.length() > 0
        } else {
            baseOk
        }
    }

    fun verifyModel(voiceId: String): Boolean = isModelDownloaded(voiceId)

    fun deleteModel(voiceId: String) {
        getModelDir(voiceId).deleteRecursively()
        if (currentVoiceId == voiceId) {
            currentTts = null
            currentVoiceId = null
        }
    }

    /** Returns the engine type for a given voice ID. */
    fun getEngineType(voiceId: String): TtsEngineType =
        ALL_VOICES.find { it.id == voiceId }?.engineType ?: TtsEngineType.VITS

    // ── Optimal thread count ─────────────────────────────────────────────────
    private fun getOptimalThreadCount(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores >= 8 -> 4
            cores >= 6 -> 3
            cores >= 4 -> 2
            else -> 1
        }
    }

    // ── espeak-ng-data extraction ────────────────────────────────────────────
    private fun extractEspeakData(): String {
        val destDir = File(context.filesDir, "espeak-ng-data")
        val existing = destDir.list()
        if (destDir.exists() && existing != null && existing.isNotEmpty()) {
            // Handle nested espeak-ng-data/espeak-ng-data structure
            val nestedDir = File(destDir, "espeak-ng-data")
            return if (File(nestedDir, "phontab").exists()) nestedDir.absolutePath
            else destDir.absolutePath
        }
        return try {
            context.assets.open("espeak-ng-data.zip").use { inputStream ->
                destDir.mkdirs()
                ZipInputStream(inputStream).use { zis ->
                    val buffer = ByteArray(32768)
                    var ze: ZipEntry? = zis.nextEntry
                    while (ze != null) {
                        val newFile = File(destDir, ze.name)
                        if (ze.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                }
                            }
                        }
                        zis.closeEntry()
                        ze = zis.nextEntry
                    }
                }
            }
            val nestedDir = File(destDir, "espeak-ng-data")
            if (File(nestedDir, "phontab").exists()) nestedDir.absolutePath
            else destDir.absolutePath
        } catch (_: Exception) { "" }
    }

    // ── Provider fallback for VITS ───────────────────────────────────────────
    private fun createVitsTtsWithFallback(modelDirPath: String, espeakDataPath: String): OfflineTts? {
        for (provider in listOf("xnnpack", "cpu")) {
            try {
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "$modelDirPath/model.onnx",
                            lexicon = "",
                            tokens = "$modelDirPath/tokens.txt",
                            dataDir = espeakDataPath,
                            dictDir = "",
                            noiseScale = 0.35f,
                            noiseScaleW = 0.667f,
                            lengthScale = 1.0f,
                        ),
                        numThreads = getOptimalThreadCount(),
                        provider = provider,
                    ),
                    maxNumSentences = 5,
                )
                val candidate = OfflineTts(config = config)
                val test = candidate.generate("ok", 0, 1.0f)
                if (test.samples.isNotEmpty()) {
                    Logs.debug(TAG, "createVitsTts: loaded with provider=$provider")
                    return candidate
                }
                try { candidate.release() } catch (_: Exception) {}
            } catch (t: Throwable) {
                Logs.debug(TAG, "createVitsTts: provider=$provider failed: ${t.message}")
            }
        }
        return null
    }

    // ── Provider fallback for Kokoro ─────────────────────────────────────────
    private fun createKokoroTtsWithFallback(
        modelDirPath: String, espeakDataPath: String, langCode: String, speakerId: Int
    ): OfflineTts? {
        for (provider in listOf("xnnpack", "cpu")) {
            try {
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        kokoro = OfflineTtsKokoroModelConfig(
                            model = "$modelDirPath/model.onnx",
                            voices = "$modelDirPath/voices.bin",
                            tokens = "$modelDirPath/tokens.txt",
                            dataDir = espeakDataPath,
                            lang = langCode,
                        ),
                        numThreads = getOptimalThreadCount(),
                        provider = provider,
                    ),
                    maxNumSentences = 3,
                    silenceScale = 0.2f,
                )
                val candidate = OfflineTts(config = config)
                // Validate with a short test
                val test = candidate.generate("ok", speakerId, 1.0f)
                if (test.samples.isNotEmpty()) {
                    Logs.debug(TAG, "createKokoroTts: loaded with provider=$provider")
                    return candidate
                }
                try { candidate.release() } catch (_: Exception) {}
            } catch (t: Throwable) {
                Logs.debug(TAG, "createKokoroTts: provider=$provider failed: ${t.message}")
            }
        }
        return null
    }

    // ── Load model (dispatches to correct engine) ────────────────────────────
    fun loadModel(voiceId: String, kokoroSpeakerId: Int = 0, kokoroLangCode: String = "en"): OfflineTts {
        if (currentVoiceId == voiceId && currentTts != null) {
            Logs.debug(TAG, "loadModel($voiceId): returning cached model")
            return currentTts!!
        }
        unloadModel()
        val modelDir = getModelDir(voiceId)
        val voice = ALL_VOICES.find { it.id == voiceId }
        val engineType = voice?.engineType ?: TtsEngineType.VITS
        Logs.debug(TAG, "loadModel($voiceId): engine=$engineType dir=${modelDir.absolutePath}")

        val espeakDataPath = extractEspeakData()
        Logs.debug(TAG, "loadModel($voiceId): espeakDataPath='$espeakDataPath'")

        val tts = when (engineType) {
            TtsEngineType.KOKORO -> createKokoroTtsWithFallback(
                modelDir.absolutePath, espeakDataPath, kokoroLangCode, kokoroSpeakerId
            )
            TtsEngineType.VITS -> createVitsTtsWithFallback(modelDir.absolutePath, espeakDataPath)
        } ?: throw IllegalStateException("Model load failed on all providers for voiceId=$voiceId ($engineType)")

        Logs.debug(TAG, "loadModel($voiceId): created, sampleRate=${tts.sampleRate()}")
        currentTts = tts
        currentVoiceId = voiceId
        return tts
    }

    fun unloadModel() {
        currentTts = null
        currentVoiceId = null
    }

    fun close() {
        nativeDispatcher.close()
    }

    // ── Download ─────────────────────────────────────────────────────────────
    suspend fun downloadModel(
        voice: AiTtsVoiceInfo,
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        val modelDir = getModelDir(voice.id)
        modelDir.mkdirs()

        val filesToDownload = mutableListOf(
            voice.downloadUrl to File(modelDir, "model.onnx"),
            voice.tokensUrl to File(modelDir, "tokens.txt")
        )
        // Kokoro models also need voices.bin
        if (voice.engineType == TtsEngineType.KOKORO && voice.voicesBinUrl.isNotEmpty()) {
            filesToDownload.add(voice.voicesBinUrl to File(modelDir, "voices.bin"))
        }
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

    fun availableVoices(): List<AiTtsVoiceInfo> =
        ALL_VOICES.filter { it.supportedAbis.isEmpty() || primaryAbi in it.supportedAbis }

    fun defaultVoiceId(): String =
        availableVoices().firstOrNull()?.id ?: ALL_VOICES.first().id

    companion object {
        private const val KOKORO_BASE =
            "https://huggingface.co/CodeBySonu95/Sherpa-onnx-models/resolve/main/kokoro-multi-lang"
        private const val VITS_BASE =
            "https://huggingface.co/CodeBySonu95/Sherpa-onnx-models/resolve/main"

        val ALL_VOICES = listOf(
            // ── Kokoro Multi-Lang (53 speakers, high quality) ─────────────────
            AiTtsVoiceInfo(
                id = "kokoro_multi_lang",
                name = "Kokoro Multi-Lang (High Quality)",
                language = "Multi-language",
                sizeBytes = 337_000_000L,
                engineType = TtsEngineType.KOKORO,
                downloadUrl = "$KOKORO_BASE/model.onnx",
                tokensUrl = "$KOKORO_BASE/tokens.txt",
                voicesBinUrl = "$KOKORO_BASE/voices.bin",
            ),

            // ── Piper VITS — English ─────────────────────────────────────────
            AiTtsVoiceInfo(
                id = "en_US-ryan-high",
                name = "Ryan (English, High)",
                language = "en-US",
                sizeBytes = 115_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-en_US-ryan-high/en_US-ryan-high.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-en_US-ryan-high/tokens.txt",
            ),
            AiTtsVoiceInfo(
                id = "en_US-ryan-medium",
                name = "Ryan (English, Medium)",
                language = "en-US",
                sizeBytes = 63_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-en_US-ryan-medium/en_US-ryan-medium.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-en_US-ryan-medium/tokens.txt",
            ),
            AiTtsVoiceInfo(
                id = "en_US-ryan-low",
                name = "Ryan (English, Low)",
                language = "en-US",
                sizeBytes = 63_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-en_US-ryan-low/en_US-ryan-low.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-en_US-ryan-low/tokens.txt",
            ),

            // ── Piper VITS — Hindi ───────────────────────────────────────────
            AiTtsVoiceInfo(
                id = "hi_IN-pratham-medium",
                name = "Pratham (Hindi, Medium)",
                language = "hi-IN",
                sizeBytes = 60_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-hi_IN-pratham-medium/hi_IN-pratham-medium.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-hi_IN-pratham-medium/tokens.txt",
            ),
            AiTtsVoiceInfo(
                id = "hi_IN-priyamvada-medium",
                name = "Priyamvada (Hindi, Medium)",
                language = "hi-IN",
                sizeBytes = 60_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-hi_IN-priyamvada-medium/hi_IN-priyamvada-medium.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-hi_IN-priyamvada-medium/tokens.txt",
            ),
            AiTtsVoiceInfo(
                id = "hi_IN-rohan-medium",
                name = "Rohan (Hindi, Medium)",
                language = "hi-IN",
                sizeBytes = 60_000_000L,
                engineType = TtsEngineType.VITS,
                downloadUrl = "$VITS_BASE/vits-piper-hi_IN-rohan-medium/hi_IN-rohan-medium.onnx",
                tokensUrl = "$VITS_BASE/vits-piper-hi_IN-rohan-medium/tokens.txt",
            ),
        )
    }
}
