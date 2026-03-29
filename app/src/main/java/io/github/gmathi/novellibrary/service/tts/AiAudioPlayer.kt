package io.github.gmathi.novellibrary.service.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File

/**
 * Standalone AI TTS audio player. Modeled directly on the sherpa-onnx example project.
 *
 * Design principles (learned from previous crash analysis):
 *  - Single OfflineTts instance, created once, never released until destroy().
 *  - All synthesis happens on ONE dedicated thread — no coroutines, no dispatchers.
 *  - AudioTrack is created once at init and reused.
 *  - generateWithCallback streams PCM chunks directly into AudioTrack.
 *  - Interruption via a single volatile boolean.
 *  - No interaction with TTSWrapper at all.
 */
class AiAudioPlayer(private val modelDir: String) {

    companion object {
        private const val TAG = "AiAudioPlayer"
    }

    interface Callback {
        fun onUtteranceStart(utteranceId: String)
        fun onUtteranceDone(utteranceId: String)
        fun onError(utteranceId: String, message: String)
    }

    // --- state ---
    private var tts: OfflineTts? = null
    private var track: AudioTrack? = null
    private var sampleRate: Int = 0

    @Volatile var stopped: Boolean = false
        private set
    @Volatile private var destroyed: Boolean = false
    
    // Thread synchronization to prevent multiple concurrent speak() calls
    private val speakLock = Any()
    @Volatile private var isSpeaking: Boolean = false

    var speed: Float = 1.0f
    var callback: Callback? = null

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------


    /**
     * Create the OfflineTts engine and AudioTrack.
     * Call from a background thread — model loading takes a few seconds.
     *
     * @return null on success, or an error message string on failure.
     */
    fun init(): String? {
        try {
            // Find the .onnx model file
            val onnxFile = File(modelDir).listFiles()?.find { it.name.endsWith(".onnx") }
                ?: return "No .onnx model file found in $modelDir"

            val tokensFile = File(modelDir, "tokens.txt")
            if (!tokensFile.exists()) return "Missing tokens.txt in $modelDir"

            val espeakDir = File(modelDir, "espeak-ng-data")
            if (!espeakDir.isDirectory) return "Missing espeak-ng-data in $modelDir"

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = onnxFile.absolutePath,
                        tokens = tokensFile.absolutePath,
                        dataDir = espeakDir.absolutePath,
                        lexicon = "",
                        noiseScale = 0.667f,
                        noiseScaleW = 0.8f,
                        lengthScale = 1.0f,
                    ),
                    numThreads = 2,
                    debug = false,
                    provider = "cpu",
                )
            )

            tts = OfflineTts(config = config)
            sampleRate = tts!!.sampleRate()
            Log.i(TAG, "OfflineTts created. sampleRate=$sampleRate, speakers=${tts!!.numSpeakers()}")

            // Pre-create AudioTrack so first utterance doesn't stutter
            track = createTrack(sampleRate)
            track!!.play()

            return null // success
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Native library not available", e)
            return "AI TTS native library not supported on this device"
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
            return e.message ?: "Unknown init error"
        }
    }

    /**
     * Speak a single piece of text. Blocks the calling thread until synthesis
     * is complete or [stop] is called. Designed to be called from a dedicated
     * synthesis thread — never from the main thread.
     *
     * Uses generate() (non-callback) to avoid JNI callback lambda issues with
     * R8 desugaring that cause native crashes in generateWithCallback().
     */
    fun speak(text: String, utteranceId: String) {
        // Ensure only one speak() call is active at a time
        synchronized(speakLock) {
            if (isSpeaking) {
                Log.w(TAG, "speak() called while already speaking, ignoring")
                return
            }
            isSpeaking = true
        }
        
        try {
            speakInternal(text, utteranceId)
        } finally {
            synchronized(speakLock) {
                isSpeaking = false
            }
        }
    }
    
    private fun speakInternal(text: String, utteranceId: String) {
        val engine = tts ?: run {
            Log.e(TAG, "speak() called but engine is null")
            callback?.onError(utteranceId, "Engine not initialized")
            return
        }
        val audioTrack = track ?: run {
            Log.e(TAG, "speak() called but audioTrack is null")
            callback?.onError(utteranceId, "AudioTrack not initialized")
            return
        }
        if (destroyed) {
            Log.d(TAG, "speak() called but player is destroyed")
            return
        }
        if (text.isBlank()) {
            Log.w(TAG, "speak() called with blank text, skipping")
            callback?.onUtteranceDone(utteranceId)
            return
        }
        
        stopped = false

        callback?.onUtteranceStart(utteranceId)

        try {
            // Ensure AudioTrack is in correct state before writing
            try {
                if (audioTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.flush()
                    audioTrack.play()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to prepare AudioTrack", e)
            }

            // Use generate() instead of generateWithCallback() to avoid JNI
            // callback lambda crash with R8 desugaring. For line-by-line TTS
            // the latency difference is negligible.
            Log.d(TAG, "Generating audio with speed=${speed}x")
            val audio = engine.generate(text = text, sid = 0, speed = speed)
            val samples = audio.samples

            if (stopped || destroyed) {
                Log.d(TAG, "Stopped or destroyed after generation")
                return
            }
            if (samples.isEmpty()) {
                Log.e(TAG, "Engine returned empty audio")
                callback?.onError(utteranceId, "Engine returned empty audio")
                return
            }

            val pcm = floatToPcm(samples)
            var offset = 0
            while (offset < pcm.size && !stopped && !destroyed) {
                val written = audioTrack.write(pcm, offset, pcm.size - offset)
                if (written < 0) {
                    Log.e(TAG, "AudioTrack.write error: $written")
                    break
                }
                offset += written
            }

            if (!stopped && !destroyed) {
                callback?.onUtteranceDone(utteranceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "speak() error", e)
            callback?.onError(utteranceId, e.message ?: "synthesis error")
        }
    }

    /** Stop current synthesis and flush audio. Thread-safe. */
    fun stop() {
        stopped = true
        track?.let {
            try {
                it.pause()
                it.flush()
                // Don't call play() here - let the next speak() call handle it
            } catch (e: Exception) {
                Log.w(TAG, "stop() track error", e)
            }
        }
        // Wait for any active speak() to finish
        var attempts = 0
        while (isSpeaking && attempts < 20) {
            Thread.sleep(50)
            attempts++
        }
    }

    /** Release everything. After this call the instance is unusable. */
    fun destroy() {
        destroyed = true
        stopped = true
        try { track?.stop() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        track = null
        try { tts?.free() } catch (_: Exception) {}
        tts = null
        Log.i(TAG, "destroyed")
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun createTrack(sr: Int): AudioTrack {
        val bufSize = AudioTrack.getMinBufferSize(
            sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioTrack(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sr)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufSize,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    /**
     * Float [-1,1] → 16-bit PCM little-endian.
     * Clamps to [-1,1] before conversion to guard against occasional out-of-range
     * samples from the ONNX model; uses explicit bit masking for correct sign handling.
     */
    private fun floatToPcm(audio: FloatArray): ByteArray {
        val out = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val s = (audio[i].coerceIn(-1.0f, 1.0f) * 32767).toInt()
            out[2 * i] = (s and 0xFF).toByte()
            out[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return out
    }
}
