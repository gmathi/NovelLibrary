package io.github.gmathi.novellibrary.activity.settings

import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class AiTtsTestUiState(
    val statusText: String = "Initializing…",
    val inputText: String = "Hello! This is a test of the AI text to speech engine.",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val isSpeakEnabled: Boolean = false,
    val isStopEnabled: Boolean = false,
    val isSpeaking: Boolean = false
)

class AiTtsTestViewModel : ViewModel() {

    companion object {
        private const val TAG = "AiTtsTest"
        private const val MODEL_DIR = "vits-piper-en_US-kusal-medium"
        private const val MODEL_NAME = "en_US-kusal-medium.onnx"
        private const val MIN_ESPEAK_FILES = 50
    }

    private val _uiState = MutableStateFlow(AiTtsTestUiState())
    val uiState: StateFlow<AiTtsTestUiState> = _uiState.asStateFlow()

    private var tts: OfflineTts? = null
    private var sampleRate = 0

    @Volatile
    private var stopRequested = false
    private var speakThread: Thread? = null

    fun initEngine(filesDir: File, assetManager: AssetManager) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                updateStatus("Copying espeak-ng-data to filesystem…")
                val espeakDataPath = copyEspeakData(filesDir, assetManager)

                updateStatus("Creating OfflineTts (model from assets, espeak from filesystem)…")

                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "$MODEL_DIR/$MODEL_NAME",
                            tokens = "$MODEL_DIR/tokens.txt",
                            dataDir = espeakDataPath,
                            lexicon = "",
                            noiseScale = 0.667f,
                            noiseScaleW = 0.8f,
                            lengthScale = 1.0f,
                        ),
                        numThreads = 2,
                        debug = true,
                        provider = "cpu",
                    )
                )

                val engine = OfflineTts(assetManager = assetManager, config = config)
                tts = engine
                sampleRate = engine.sampleRate()

                Log.i(TAG, "OfflineTts ready! sampleRate=$sampleRate speakers=${engine.numSpeakers()}")

                if (sampleRate <= 0) {
                    updateStatus("ERROR: invalid sampleRate=$sampleRate")
                    return@launch
                }

                updateStatus("Ready! sampleRate=$sampleRate")
                _uiState.value = _uiState.value.copy(isSpeakEnabled = true)

            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native lib error", e)
                updateStatus("ERROR: native library — ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Init error", e)
                updateStatus("ERROR: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun updateSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(speed = speed)
    }

    fun updatePitch(pitch: Float) {
        _uiState.value = _uiState.value.copy(pitch = pitch)
    }

    fun speak() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) {
            updateStatus("Error: No text to speak")
            return
        }

        val engine = tts
        if (engine == null) {
            updateStatus("Error: TTS engine not initialized")
            return
        }
        
        // Stop any existing thread first
        if (speakThread?.isAlive == true) {
            stopRequested = true
            speakThread?.join(1000) // Wait up to 1 second for thread to finish
        }
        
        // Capture speed and pitch values before starting thread
        val currentSpeed = _uiState.value.speed
        val currentPitch = _uiState.value.pitch

        _uiState.value = _uiState.value.copy(
            isSpeakEnabled = false,
            isStopEnabled = true,
            isSpeaking = true
        )
        stopRequested = false
        updateStatus("Synthesizing…")

        speakThread = Thread({
            var audioTrack: AudioTrack? = null
            try {
                updateStatus("Generating audio (speed: ${String.format("%.1f", currentSpeed)}x)…")

                // Use currentSpeed for synthesis (not 1.0f)
                val audio = engine.generate(text = text, sid = 0, speed = currentSpeed)
                val samples = audio.samples

                if (stopRequested) {
                    updateStatus("Stopped")
                    return@Thread
                }
                if (samples.isEmpty()) {
                    updateStatus("ERROR: engine returned empty audio")
                    return@Thread
                }

                val secs = samples.size.toFloat() / sampleRate
                Log.i(TAG, "Generated ${samples.size} samples (${secs}s)")
                updateStatus("Playing ${String.format("%.1f", secs)}s of audio…")

                val pcm = floatToPcm16(samples)
                val bufSize = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                val actualBufSize = maxOf(bufSize, sampleRate * 2)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(actualBufSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                // Apply pitch adjustment via playback params (Android API 23+)
                // Note: Speed is already applied during synthesis, so we only adjust pitch here
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        val playbackParams = android.media.PlaybackParams()
                        playbackParams.pitch = currentPitch
                        audioTrack.playbackParams = playbackParams
                        Log.i(TAG, "Applied pitch: ${currentPitch}x (speed ${currentSpeed}x applied during synthesis)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to set playback parameters", e)
                    }
                }

                audioTrack.play()

                var offset = 0
                while (offset < pcm.size && !stopRequested) {
                    val written = audioTrack.write(pcm, offset, minOf(4096, pcm.size - offset))
                    if (written < 0) {
                        Log.e(TAG, "AudioTrack.write error: $written")
                        break
                    }
                    offset += written
                }

                if (!stopRequested) {
                    audioTrack.stop()
                    updateStatus("Done! (${String.format("%.1f", secs)}s)")
                } else {
                    updateStatus("Stopped")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Speak error", e)
                updateStatus("ERROR: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                try {
                    audioTrack?.release()
                } catch (_: Exception) {
                }
                _uiState.value = _uiState.value.copy(
                    isSpeakEnabled = true,
                    isStopEnabled = false,
                    isSpeaking = false
                )
            }
        }, "ai_tts_speak")
        speakThread!!.start()
    }

    fun stopSpeaking() {
        stopRequested = true
        speakThread?.interrupt()
        updateStatus("Stopping…")
        _uiState.value = _uiState.value.copy(
            isStopEnabled = false
        )
    }

    private fun updateStatus(msg: String) {
        Log.d(TAG, "Status: $msg")
        _uiState.value = _uiState.value.copy(statusText = msg)
    }

    private fun copyEspeakData(filesDir: File, assetManager: AssetManager): String {
        val targetDir = File(filesDir, "$MODEL_DIR/espeak-ng-data")
        if (targetDir.exists() && (targetDir.listFiles()?.size ?: 0) > MIN_ESPEAK_FILES) {
            Log.i(TAG, "espeak-ng-data already present at ${targetDir.absolutePath}")
            return targetDir.absolutePath
        }

        Log.i(TAG, "Copying espeak-ng-data from assets to ${targetDir.absolutePath}")
        targetDir.mkdirs()

        copyAssetDir(assetManager, "$MODEL_DIR/espeak-ng-data", targetDir)

        val count = targetDir.walkTopDown().count { it.isFile }
        Log.i(TAG, "Copied $count files to espeak-ng-data")
        return targetDir.absolutePath
    }

    private fun copyAssetDir(mgr: AssetManager, assetPath: String, targetDir: File) {
        val children = mgr.list(assetPath) ?: return
        if (children.isEmpty()) {
            val fileName = assetPath.substringAfterLast('/')
            mgr.open(assetPath).use { input ->
                FileOutputStream(File(targetDir, fileName)).use { output ->
                    input.copyTo(output)
                }
            }
            return
        }
        for (child in children) {
            val childAssetPath = "$assetPath/$child"
            val subList = mgr.list(childAssetPath)
            if (subList != null && subList.isNotEmpty()) {
                val subDir = File(targetDir, child)
                subDir.mkdirs()
                copyAssetDir(mgr, childAssetPath, subDir)
            } else {
                mgr.open(childAssetPath).use { input ->
                    FileOutputStream(File(targetDir, child)).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun floatToPcm16(audio: FloatArray): ByteArray {
        val out = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val clamped = audio[i].coerceIn(-1.0f, 1.0f)
            val s = (clamped * 32767).toInt()
            out[2 * i] = (s and 0xFF).toByte()
            out[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }

    override fun onCleared() {
        super.onCleared()
        stopRequested = true
        speakThread?.join(2000)
        try {
            tts?.free()
        } catch (_: Exception) {
        }
        tts = null
    }
}
