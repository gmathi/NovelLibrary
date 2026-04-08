package io.github.gmathi.novellibrary.service.ai_tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import io.github.gmathi.novellibrary.model.preference.AiTtsPreferences
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "AiTtsPlayer"

class AiTtsPlayer(
    private val modelManager: AiTtsModelManager,
    private val preferences: AiTtsPreferences,
    private val eventListener: AiTtsEventListener? = null
) {
    // --- State ---
    private val _playbackState = MutableStateFlow<AiTtsPlaybackState>(AiTtsPlaybackState.Idle)
    val playbackState: StateFlow<AiTtsPlaybackState> = _playbackState.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    // --- Data ---
    private val _sentences = MutableStateFlow<List<String>>(emptyList())
    val sentences: StateFlow<List<String>> = _sentences.asStateFlow()

    private var title: String = ""
    private var linkedPages: ArrayList<String> = arrayListOf()
    private var chapterIndex: Int = 0

    // --- Coroutines ---
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null
    // Serializes native TTS synthesis so a new job always waits for the current
    // generate() call to finish before loading/releasing the model.
    private val synthesisLock = Mutex()

    // --- Audio ---
    // Reused across sentences to avoid per-sentence AudioTrack allocation/release overhead,
    // which causes native OOM on the emulator (x86_64) with many sentences.
    private var audioTrack: AudioTrack? = null
    private var audioTrackSampleRate: Int = -1

    fun setData(text: String, title: String, linkedPages: ArrayList<String>, chapterIndex: Int) {
        stop()
        this.title = title
        this.linkedPages = linkedPages
        this.chapterIndex = chapterIndex
        _sentences.value = splitIntoSentences(text)
        _currentSentenceIndex.value = 0
        _playbackState.value = AiTtsPlaybackState.Idle
        Logs.debug(TAG, "setData: title='$title' chapterIndex=$chapterIndex sentences=${_sentences.value.size} textLength=${text.length}")
    }

    fun start() {
        if (_playbackState.value is AiTtsPlaybackState.Playing) return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            playSentencesFrom(_currentSentenceIndex.value)
        }
    }

    fun pause() {
        if (_playbackState.value !is AiTtsPlaybackState.Playing) return
        playbackJob?.cancel()
        releaseAudioTrack()
        _playbackState.value = AiTtsPlaybackState.Paused
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Paused)
    }

    fun stop() {
        playbackJob?.cancel()
        releaseAudioTrack()
        _playbackState.value = AiTtsPlaybackState.Stopped
        _currentSentenceIndex.value = 0
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Stopped)
    }

    /** Called by the service while the model files are being downloaded from the network.
     *  [progress] is 0–100, or -1 if unknown. */
    fun setDownloadingModel(progress: Int = -1) {
        val state = AiTtsPlaybackState.DownloadingModel(progress)
        if (progress == -1) playbackJob?.cancel()
        _playbackState.value = state
        eventListener?.onPlaybackStateChanged(state)
    }

    fun nextSentence() {
        val next = _currentSentenceIndex.value + 1
        if (next < _sentences.value.size) {
            _currentSentenceIndex.value = next
            if (_playbackState.value is AiTtsPlaybackState.Playing) {
                playbackJob?.cancel()
                playbackJob = scope.launch { playSentencesFrom(next) }
            }
        }
    }

    fun prevSentence() {
        val prev = (_currentSentenceIndex.value - 1).coerceAtLeast(0)
        _currentSentenceIndex.value = prev
        if (_playbackState.value is AiTtsPlaybackState.Playing) {
            playbackJob?.cancel()
            playbackJob = scope.launch { playSentencesFrom(prev) }
        }
    }

    fun nextChapter() {
        eventListener?.onChapterChanged(chapterIndex + 1)
    }

    fun prevChapter() {
        if (chapterIndex > 0) eventListener?.onChapterChanged(chapterIndex - 1)
    }

    fun destroy() {
        scope.cancel()
        releaseAudioTrack()
        // scope.cancel() marks coroutines for cancellation but does NOT wait for any
        // in-progress blocking native call (generate / loadModel) on nativeDispatcher to
        // finish.  Acquiring synthesisLock here blocks until the current native call
        // returns and releases the lock, guaranteeing the nativeDispatcher thread is idle
        // before we release the OfflineTts native object.
        runBlocking { synthesisLock.withLock { modelManager.unloadModel() } }
        modelManager.close()
    }

    private fun releaseAudioTrack() {
        audioTrack?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        audioTrack = null
        audioTrackSampleRate = -1
    }

    // --- Internal ---

    private suspend fun playSentencesFrom(startIndex: Int) {
        Logs.debug(TAG, "playSentencesFrom: startIndex=$startIndex totalSentences=${_sentences.value.size} voiceId=${preferences.voiceId}")
        _playbackState.value = AiTtsPlaybackState.LoadingModel
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.LoadingModel)

        val tts = try {
            // Hold synthesisLock while loading the model so we wait for any in-progress
            // generate() call on the old instance to finish before we (potentially) release it.
            // nativeDispatcher is a single-thread executor — all ORT calls share one thread,
            // preventing the global inter-op thread-pool initialization race.
            synthesisLock.withLock {
                withContext(modelManager.nativeDispatcher) {
                    modelManager.loadModel(preferences.voiceId)
                }
            }
        } catch (e: Exception) {
            Logs.error(TAG, "playSentencesFrom: loadModel failed: ${e.message}", e)
            _playbackState.value = AiTtsPlaybackState.Error(e.message ?: "Failed to load model")
            eventListener?.onPlaybackStateChanged(_playbackState.value)
            eventListener?.onError(e.message ?: "Failed to load model")
            return
        }

        Logs.debug(TAG, "playSentencesFrom: model loaded, starting playback")
        _playbackState.value = AiTtsPlaybackState.Playing
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Playing)

        val sentenceList = _sentences.value
        for (index in startIndex until sentenceList.size) {
            if (!currentCoroutineContext().isActive) break
            _currentSentenceIndex.value = index
            val sentence = sentenceList[index]
            Logs.debug(TAG, "playSentencesFrom: synthesizing sentence $index/${sentenceList.size}: '${sentence.take(60)}'")
            eventListener?.onSentenceChanged(index, sentence)
            try {
                synthesizeAndPlay(tts, sentence)
            } catch (e: Exception) {
                Logs.error(TAG, "playSentencesFrom: synthesizeAndPlay failed at index $index: ${e.message}", e)
                _playbackState.value = AiTtsPlaybackState.Error(e.message ?: "Synthesis failed")
                eventListener?.onPlaybackStateChanged(_playbackState.value)
                eventListener?.onError(e.message ?: "Synthesis failed")
                return
            }
        }

        if (currentCoroutineContext().isActive) {
            if (preferences.autoReadNextChapter) {
                Logs.debug(TAG, "playSentencesFrom: chapter complete, advancing to next chapter")
                eventListener?.onChapterChanged(chapterIndex + 1)
            } else {
                Logs.debug(TAG, "playSentencesFrom: playback complete, going idle")
                _playbackState.value = AiTtsPlaybackState.Idle
                eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Idle)
            }
        }
    }

    private suspend fun synthesizeAndPlay(
        tts: com.k2fsa.sherpa.onnx.OfflineTts,
        sentence: String
    ) {
        if (sentence.isBlank()) return
        synthesisLock.withLock {
            withContext(modelManager.nativeDispatcher) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                Logs.debug(TAG, "synthesizeAndPlay: generating audio for sentence length=${sentence.length} speed=${preferences.speechRate}")
                val audio = tts.generate(
                    text = sentence,
                    sid = 0,
                    speed = preferences.speechRate
                )
                Logs.debug(TAG, "synthesizeAndPlay: generated ${audio.samples.size} samples at ${audio.sampleRate}Hz")

                // Convert float samples to PCM bytes and play
                val pcmBytes = floatsToPcm16(audio.samples)
                playPcmOnAudioTrack(pcmBytes, audio.sampleRate)

                // Inject punctuation-based silence after the sentence
                val silenceMs = getSentenceTrailingSilenceMs(sentence)
                if (silenceMs > 0) {
                    val silenceBytes = createSilence(silenceMs, audio.sampleRate)
                    playPcmOnAudioTrack(silenceBytes, audio.sampleRate)
                }
            }
        }
    }

    /** Converts float PCM samples [-1,1] to 16-bit little-endian PCM bytes. */
    private fun floatsToPcm16(samples: FloatArray): ByteArray {
        val pcm = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            var v = (samples[i] * 32767f).toInt()
            if (v > 32767) v = 32767
            if (v < -32768) v = -32768
            pcm[i * 2] = (v and 0xff).toByte()
            pcm[i * 2 + 1] = (v shr 8).toByte()
        }
        return pcm
    }

    /**
     * Returns silence duration in ms based on the trailing punctuation of a sentence.
     * Mirrors the reference project's AudioEmotionHelper punctuation logic.
     */
    private fun getSentenceTrailingSilenceMs(sentence: String): Int {
        val trimmed = sentence.trimEnd()
        return when {
            trimmed.endsWith("...") -> 380
            trimmed.endsWith(".") || trimmed.endsWith("।") -> 280
            trimmed.endsWith("?") -> 230
            trimmed.endsWith("!") -> 190
            trimmed.endsWith(",") -> 140
            else -> 80 // small natural gap between sentences
        }
    }

    /** Creates a byte array of silence (zero PCM) for the given duration. */
    private fun createSilence(durationMs: Int, sampleRate: Int): ByteArray {
        if (durationMs <= 0) return ByteArray(0)
        val bytesPerSecond = sampleRate * 2 // 16-bit mono
        var bytes = (bytesPerSecond * durationMs) / 1000
        if (bytes % 2 != 0) bytes++ // keep 16-bit aligned
        return ByteArray(bytes)
    }

    private fun playPcmOnAudioTrack(pcmBytes: ByteArray, sampleRate: Int) {
        if (sampleRate <= 0) {
            Logs.warning(TAG, "playPcmOnAudioTrack: invalid sampleRate=$sampleRate, skipping")
            return
        }
        if (pcmBytes.isEmpty()) return

        // Reuse the existing AudioTrack if the sample rate hasn't changed.
        // Creating a new AudioTrack per sentence causes native OOM on x86_64 emulator.
        if (audioTrack == null || audioTrackSampleRate != sampleRate) {
            releaseAudioTrack()
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize <= 0) {
                Logs.warning(TAG, "playPcmOnAudioTrack: getMinBufferSize returned $bufferSize for sampleRate=$sampleRate, skipping")
                return
            }
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrackSampleRate = sampleRate
            Logs.debug(TAG, "playPcmOnAudioTrack: created AudioTrack sampleRate=$sampleRate bufferSize=$bufferSize")
        }

        val track = audioTrack ?: return
        try {
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) {
                track.play()
            }
            track.write(pcmBytes, 0, pcmBytes.size)
            // Flush remaining buffered audio before returning so the next sentence
            // starts cleanly without overlap.
            track.stop()
        } catch (e: Exception) {
            Logs.error(TAG, "playPcmOnAudioTrack: error during playback: ${e.message}", e)
            releaseAudioTrack()
            throw e
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Match reference project: split on sentence-ending punctuation including
        // newlines, pipe separators, and Hindi danda (।)
        return text.split(Regex("(?<=[.!?\\n|।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
