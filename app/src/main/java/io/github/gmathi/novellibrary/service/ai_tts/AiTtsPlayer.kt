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
import java.util.Random
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "AiTtsPlayer"

/**
 * Max number of pre-synthesized audio chunks to hold in the buffer.
 * Producer pauses when the queue reaches this size and resumes when
 * the player thread drains a slot.
 */
private const val MAX_BUFFER_CHUNKS = 10

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

    /** True while the producer is synthesizing audio before it reaches the playback queue. */
    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    /** True when the player thread is actively writing PCM to AudioTrack. */
    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    // --- Data ---
    private val _sentences = MutableStateFlow<List<String>>(emptyList())
    val sentences: StateFlow<List<String>> = _sentences.asStateFlow()

    private var title: String = ""
    private var linkedPages: ArrayList<String> = arrayListOf()
    private var chapterIndex: Int = 0

    // --- Coroutines ---
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null
    private val synthesisLock = Mutex()

    // --- Audio ---
    private var audioTrack: AudioTrack? = null
    private var audioTrackSampleRate: Int = -1
    private val silenceJitterRandom = Random()

    // --- Producer-Consumer streaming ---
    /** Sentinel chunk pushed to signal the player thread that no more data is coming. */
    private val SENTINEL = AudioChunk(-1, ByteArray(0), 0)

    /**
     * A chunk of synthesized PCM audio tagged with its sentence index so the
     * player thread can update [_currentSentenceIndex] at the right moment.
     */
    private data class AudioChunk(
        val sentenceIndex: Int,
        val pcmBytes: ByteArray,
        val sampleRate: Int
    )

    /** The bounded queue between the producer (synthesis) and consumer (playback). */
    private var audioQueue: LinkedBlockingQueue<AudioChunk>? = null

    /** Flag the player thread checks to know it should exit. */
    private val stopRequested = AtomicBoolean(false)

    /** The player thread handle so we can join on it during pause/stop. */
    private var playerThread: Thread? = null

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
        Logs.debug(TAG, "start: resuming playback from sentence ${_currentSentenceIndex.value}")
        playbackJob?.cancel()
        playbackJob = scope.launch {
            playSentencesFrom(_currentSentenceIndex.value)
        }
    }

    fun pause() {
        if (_playbackState.value !is AiTtsPlaybackState.Playing) return
        Logs.debug(TAG, "pause: pausing at sentence ${_currentSentenceIndex.value}")
        // Cancel the producer coroutine first
        playbackJob?.cancel()
        // Signal the player thread to stop and drain
        stopPlayerThread()
        _isSynthesizing.value = false
        _isAudioPlaying.value = false
        _playbackState.value = AiTtsPlaybackState.Paused
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Paused)
    }

    fun stop() {
        Logs.debug(TAG, "stop: stopping playback")
        playbackJob?.cancel()
        stopPlayerThread()
        _isSynthesizing.value = false
        _isAudioPlaying.value = false
        _playbackState.value = AiTtsPlaybackState.Stopped
        _currentSentenceIndex.value = 0
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Stopped)
    }

    fun setDownloadingModel(progress: Int = -1) {
        val state = AiTtsPlaybackState.DownloadingModel(progress)
        if (progress == -1) playbackJob?.cancel()
        _playbackState.value = state
        eventListener?.onPlaybackStateChanged(state)
    }

    fun nextSentence() {
        val next = _currentSentenceIndex.value + 1
        if (next < _sentences.value.size) {
            Logs.debug(TAG, "nextSentence: advancing to $next/${_sentences.value.size}")
            seekToSentence(next)
        }
    }

    fun prevSentence() {
        val prev = (_currentSentenceIndex.value - 1).coerceAtLeast(0)
        Logs.debug(TAG, "prevSentence: going back to $prev")
        seekToSentence(prev)
    }

    fun seekToSentence(index: Int) {
        val clamped = index.coerceIn(0, (_sentences.value.size - 1).coerceAtLeast(0))
        Logs.debug(TAG, "seekToSentence: seeking to $clamped/${_sentences.value.size}")
        _currentSentenceIndex.value = clamped
        if (_playbackState.value is AiTtsPlaybackState.Playing) {
            playbackJob?.cancel()
            stopPlayerThread()
            playbackJob = scope.launch { playSentencesFrom(clamped) }
        }
    }

    fun nextChapter() {
        Logs.debug(TAG, "nextChapter: requesting chapter ${chapterIndex + 1}")
        eventListener?.onChapterChanged(chapterIndex + 1)
    }

    fun prevChapter() {
        Logs.debug(TAG, "prevChapter: requesting chapter ${chapterIndex - 1}")
        if (chapterIndex > 0) eventListener?.onChapterChanged(chapterIndex - 1)
    }

    fun destroy() {
        Logs.debug(TAG, "destroy: releasing resources")
        scope.cancel()
        stopPlayerThread()
        runBlocking { synthesisLock.withLock { modelManager.unloadModel() } }
        modelManager.close()
    }

    // ── Player thread management ─────────────────────────────────────────────

    /** Stops the player thread, drains the queue, and releases AudioTrack. */
    private fun stopPlayerThread() {
        stopRequested.set(true)
        // Unblock the player thread if it's waiting on an empty queue
        audioQueue?.let { q ->
            q.clear()
            q.offer(SENTINEL)
        }
        playerThread?.let { t ->
            try { t.join(2000) } catch (_: InterruptedException) {}
        }
        playerThread = null
        audioQueue = null
        releaseAudioTrack()
    }

    private fun releaseAudioTrack() {
        audioTrack?.let {
            try { it.stop() } catch (_: Exception) {}
            it.release()
        }
        audioTrack = null
        audioTrackSampleRate = -1
    }

    private fun activeSpeakerId(): Int {
        return if (modelManager.getEngineType(preferences.voiceId) == TtsEngineType.KOKORO) {
            preferences.kokoroSpeakerId
        } else {
            0
        }
    }

    // ── Core producer-consumer pipeline ──────────────────────────────────────

    private suspend fun playSentencesFrom(startIndex: Int) {
        Logs.debug(TAG, "playSentencesFrom: startIndex=$startIndex totalSentences=${_sentences.value.size}")
        _playbackState.value = AiTtsPlaybackState.LoadingModel
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.LoadingModel)

        val tts = try {
            synthesisLock.withLock {
                withContext(modelManager.nativeDispatcher) {
                    modelManager.loadModel(
                        preferences.voiceId,
                        kokoroSpeakerId = preferences.kokoroSpeakerId,
                        kokoroLangCode = preferences.kokoroLangCode
                    )
                }
            }
        } catch (e: Exception) {
            Logs.error(TAG, "playSentencesFrom: loadModel failed: ${e.message}", e)
            _playbackState.value = AiTtsPlaybackState.Error(e.message ?: "Failed to load model")
            eventListener?.onPlaybackStateChanged(_playbackState.value)
            eventListener?.onError(e.message ?: "Failed to load model")
            return
        }

        Logs.debug(TAG, "playSentencesFrom: model loaded, starting streaming pipeline")

        // Set up the bounded queue and player thread
        stopRequested.set(false)
        val queue = LinkedBlockingQueue<AudioChunk>(MAX_BUFFER_CHUNKS)
        audioQueue = queue

        // Start the consumer (player) thread — keeps AudioTrack playing continuously
        val consumer = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            try {
                while (!stopRequested.get()) {
                    val chunk = try {
                        queue.take() // blocks until a chunk is available
                    } catch (_: InterruptedException) {
                        break
                    }
                    if (chunk === SENTINEL || stopRequested.get()) break

                    // Update current sentence on the main thread
                    scope.launch(Dispatchers.Main) {
                        _currentSentenceIndex.value = chunk.sentenceIndex
                        eventListener?.onSentenceChanged(chunk.sentenceIndex, _sentences.value.getOrElse(chunk.sentenceIndex) { "" })
                    }

                    // Write PCM to AudioTrack (blocks until written — this is the streaming magic)
                    _isAudioPlaying.value = true
                    writeToAudioTrack(chunk.pcmBytes, chunk.sampleRate)
                }
            } catch (e: Exception) {
                if (!stopRequested.get()) {
                    Logs.error(TAG, "playerThread: error: ${e.message}", e)
                }
            }
            _isAudioPlaying.value = false
            Logs.debug(TAG, "playerThread: exiting")
        }, "ai-tts-player")
        playerThread = consumer
        consumer.start()

        _playbackState.value = AiTtsPlaybackState.Playing
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Playing)

        // Producer: synthesize sentences and push chunks into the bounded queue
        val sentenceList = _sentences.value
        try {
            for (index in startIndex until sentenceList.size) {
                if (!currentCoroutineContext().isActive || stopRequested.get()) break
                val sentence = sentenceList[index]
                Logs.debug(TAG, "producer: synthesizing sentence $index/${sentenceList.size}: '${sentence.take(60)}'")

                _isSynthesizing.value = true
                val chunks = synthesizeSentence(tts, sentence, index)
                _isSynthesizing.value = false

                for (chunk in chunks) {
                    if (!currentCoroutineContext().isActive || stopRequested.get()) break
                    // put() blocks if the queue is full — this is the back-pressure mechanism
                    withContext(Dispatchers.IO) { queue.put(chunk) }
                }
            }
        } catch (_: CancellationException) {
            _isSynthesizing.value = false
            Logs.debug(TAG, "producer: cancelled")
        } catch (e: Exception) {
            _isSynthesizing.value = false
            Logs.error(TAG, "producer: synthesis error: ${e.message}", e)
            _playbackState.value = AiTtsPlaybackState.Error(e.message ?: "Synthesis failed")
            eventListener?.onPlaybackStateChanged(_playbackState.value)
            eventListener?.onError(e.message ?: "Synthesis failed")
            stopRequested.set(true)
            queue.clear()
            queue.offer(SENTINEL)
            return
        }

        if (currentCoroutineContext().isActive && !stopRequested.get()) {
            // Signal end-of-stream and wait for the player thread to finish draining
            queue.put(SENTINEL)
            withContext(Dispatchers.IO) {
                try { consumer.join() } catch (_: InterruptedException) {}
            }
            releaseAudioTrack()

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

    /**
     * Synthesizes a single sentence into one or more [AudioChunk]s.
     * For emotion-tag mode, each sub-chunk (text between tags/punctuation) becomes
     * its own chunk with silence injected as separate zero-PCM chunks.
     * For normal mode, the whole sentence is one chunk plus optional trailing silence.
     */
    private suspend fun synthesizeSentence(
        tts: com.k2fsa.sherpa.onnx.OfflineTts,
        sentence: String,
        sentenceIndex: Int
    ): List<AudioChunk> {
        if (sentence.isBlank()) return emptyList()

        return if (preferences.emotionTags) {
            synthesizeWithEmotionTags(tts, sentence, sentenceIndex)
        } else {
            val pcm = synthesizeToPcm(tts, sentence) ?: return emptyList()
            val chunks = mutableListOf(AudioChunk(sentenceIndex, pcm.first, pcm.second))
            if (preferences.smartPunctuation) {
                val silenceMs = getSentenceTrailingSilenceMs(sentence)
                if (silenceMs > 0) {
                    chunks.add(AudioChunk(sentenceIndex, createSilence(silenceMs, pcm.second), pcm.second))
                }
            }
            chunks
        }
    }

    private suspend fun synthesizeToPcm(
        tts: com.k2fsa.sherpa.onnx.OfflineTts,
        sentence: String
    ): Pair<ByteArray, Int>? {
        if (sentence.isBlank()) return null
        return synthesisLock.withLock {
            withContext(modelManager.nativeDispatcher) {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                val sid = activeSpeakerId()
                val audio = tts.generate(text = sentence, sid = sid, speed = preferences.speechRate)
                if (audio.samples.isEmpty()) return@withContext null
                var pcmBytes = floatsToPcm16(audio.samples)
                if (preferences.volumeNormalization) {
                    pcmBytes = normalizePcmVolume(pcmBytes)
                }
                Pair(pcmBytes, audio.sampleRate)
            }
        }
    }

    // ── Emotion tags ─────────────────────────────────────────────────────────

    private data class EmotionProfile(
        val volume: Float = 1.0f,
        val speed: Float = 1.0f,
        val attackTimeMs: Int = 1500
    )

    private suspend fun synthesizeWithEmotionTags(
        tts: com.k2fsa.sherpa.onnx.OfflineTts,
        sentence: String,
        sentenceIndex: Int
    ): List<AudioChunk> {
        val chunks = mutableListOf<AudioChunk>()
        val baseSpeed = preferences.speechRate
        val sid = activeSpeakerId()
        var currentProfile = EmotionProfile(speed = baseSpeed)
        var lastVolume = 1.0f

        val regex = Regex("""(\[[a-zA-Z]+]|\.\.\.|\.|,|!|\?|।)""")
        var lastEnd = 0

        for (match in regex.findAll(sentence)) {
            val textChunk = sentence.substring(lastEnd, match.range.first).trim()
            if (textChunk.isNotEmpty() && !textChunk.matches(Regex("""\[[a-zA-Z]+]"""))) {
                val audio = synthesisLock.withLock {
                    withContext(modelManager.nativeDispatcher) {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                        tts.generate(text = textChunk, sid = sid, speed = currentProfile.speed)
                    }
                }
                if (audio.samples.isNotEmpty()) {
                    var pcm = floatsToPcm16(audio.samples)
                    if (lastVolume != 1.0f || currentProfile.volume != 1.0f) {
                        pcm = applyVolumeEnvelope(pcm, lastVolume, currentProfile.volume, currentProfile.attackTimeMs, audio.sampleRate)
                    }
                    chunks.add(AudioChunk(sentenceIndex, pcm, audio.sampleRate))
                    lastVolume = currentProfile.volume
                }
            }

            val token = match.value
            if (token.startsWith("[")) {
                currentProfile = when (token.lowercase()) {
                    "[whisper]", "[whispers]" -> EmotionProfile(volume = 0.65f, speed = baseSpeed * 0.95f, attackTimeMs = 2500)
                    "[angry]" -> EmotionProfile(volume = 1.15f, speed = baseSpeed * 1.05f, attackTimeMs = 1500)
                    "[sad]" -> EmotionProfile(volume = 0.80f, speed = baseSpeed * 0.92f, attackTimeMs = 2500)
                    "[sarcastic]", "[sarcastically]" -> EmotionProfile(volume = 1.0f, speed = baseSpeed * 1.02f, attackTimeMs = 1500)
                    "[giggle]", "[giggles]" -> EmotionProfile(volume = 1.10f, speed = baseSpeed * 1.05f, attackTimeMs = 1000)
                    "[normal]", "[]" -> EmotionProfile(speed = baseSpeed)
                    else -> EmotionProfile(speed = baseSpeed)
                }
            } else if (preferences.smartPunctuation) {
                val baseMs = when (token) {
                    "," -> 140; "!" -> 190; "?" -> 230
                    ".", "।" -> 280; "..." -> 380
                    else -> 0
                }
                if (baseMs > 0) {
                    val speedAdjusted = (baseMs / currentProfile.speed).toInt()
                    val jittered = applyJitter(speedAdjusted)
                    val sampleRate = tts.sampleRate()
                    if (sampleRate > 0) {
                        chunks.add(AudioChunk(sentenceIndex, createSilence(jittered, sampleRate), sampleRate))
                    }
                }
            }
            lastEnd = match.range.last + 1
        }

        val remaining = sentence.substring(lastEnd).trim()
        if (remaining.isNotEmpty() && !remaining.matches(Regex("""\[[a-zA-Z]+]"""))) {
            val audio = synthesisLock.withLock {
                withContext(modelManager.nativeDispatcher) {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                    tts.generate(text = remaining, sid = sid, speed = currentProfile.speed)
                }
            }
            if (audio.samples.isNotEmpty()) {
                var pcm = floatsToPcm16(audio.samples)
                if (lastVolume != 1.0f || currentProfile.volume != 1.0f) {
                    pcm = applyVolumeEnvelope(pcm, lastVolume, currentProfile.volume, currentProfile.attackTimeMs, audio.sampleRate)
                }
                chunks.add(AudioChunk(sentenceIndex, pcm, audio.sampleRate))
            }
        }

        return chunks
    }

    // ── AudioTrack streaming (never stops between chunks) ────────────────────

    /**
     * Writes PCM data to the AudioTrack in streaming mode. The track is created
     * once and kept playing — successive writes flow seamlessly without gaps.
     * Called only from the player thread.
     */
    private fun writeToAudioTrack(pcmBytes: ByteArray, sampleRate: Int) {
        if (sampleRate <= 0 || pcmBytes.isEmpty()) return

        if (audioTrack == null || audioTrackSampleRate != sampleRate) {
            releaseAudioTrack()
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (bufferSize <= 0) {
                Logs.warning(TAG, "writeToAudioTrack: getMinBufferSize=$bufferSize for sampleRate=$sampleRate, skipping")
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
            audioTrack!!.play()
            Logs.debug(TAG, "writeToAudioTrack: created & started AudioTrack sampleRate=$sampleRate")
        }

        val track = audioTrack ?: return
        // write() blocks until all bytes are consumed — this is the natural back-pressure
        track.write(pcmBytes, 0, pcmBytes.size)
    }

    // ── DSP utilities ────────────────────────────────────────────────────────

    private fun applyVolumeEnvelope(
        pcm: ByteArray, startVol: Float, targetVol: Float, attackTimeMs: Int, sampleRate: Int
    ): ByteArray {
        if (startVol == 1.0f && targetVol == 1.0f) return pcm
        val totalSamples = pcm.size / 2
        val transitionSamples = ((sampleRate * attackTimeMs) / 1000).coerceAtMost(totalSamples)
        val volumeStep = if (transitionSamples > 0) (targetVol - startVol) / transitionSamples else 0f

        for (i in 0 until pcm.size step 2) {
            val lower = pcm[i].toInt() and 0xFF
            val upper = pcm[i + 1].toInt() shl 8
            var sample = (lower or upper).toShort().toInt()
            val sampleIndex = i / 2
            val vol = if (sampleIndex < transitionSamples) startVol + volumeStep * sampleIndex else targetVol
            sample = (sample * vol).toInt().coerceIn(-32768, 32767)
            pcm[i] = (sample and 0xFF).toByte()
            pcm[i + 1] = (sample shr 8).toByte()
        }
        return pcm
    }

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

    private fun normalizePcmVolume(pcm: ByteArray): ByteArray {
        if (pcm.size < 2) return pcm
        var peak = 0
        for (i in 0 until pcm.size step 2) {
            val lower = pcm[i].toInt() and 0xFF
            val upper = pcm[i + 1].toInt() shl 8
            val sample = kotlin.math.abs((lower or upper).toShort().toInt())
            if (sample > peak) peak = sample
        }
        if (peak == 0) return pcm
        val targetPeak = (32767 * 0.95f)
        val gain = targetPeak / peak
        if (gain in 0.99f..1.01f) return pcm

        for (i in 0 until pcm.size step 2) {
            val lower = pcm[i].toInt() and 0xFF
            val upper = pcm[i + 1].toInt() shl 8
            var sample = (lower or upper).toShort().toInt()
            sample = (sample * gain).toInt().coerceIn(-32768, 32767)
            pcm[i] = (sample and 0xFF).toByte()
            pcm[i + 1] = (sample shr 8).toByte()
        }
        return pcm
    }

    private fun getSentenceTrailingSilenceMs(sentence: String): Int {
        val trimmed = sentence.trimEnd()
        val baseMs = when {
            trimmed.endsWith("...") -> 380
            trimmed.endsWith(".") || trimmed.endsWith("।") -> 280
            trimmed.endsWith("?") -> 230
            trimmed.endsWith("!") -> 190
            trimmed.endsWith(",") -> 140
            else -> 80
        }
        return applyJitter(baseMs)
    }

    private fun applyJitter(baseMs: Int): Int {
        val jitterRange = (baseMs * 0.10f).toInt()
        var result = baseMs
        if (jitterRange > 0) {
            result += silenceJitterRandom.nextInt(jitterRange * 2) - jitterRange
        }
        return result.coerceIn(60, 600)
    }

    private fun createSilence(durationMs: Int, sampleRate: Int): ByteArray {
        if (durationMs <= 0) return ByteArray(0)
        val bytesPerSecond = sampleRate * 2
        var bytes = (bytesPerSecond * durationMs) / 1000
        if (bytes % 2 != 0) bytes++
        return ByteArray(bytes)
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?\\n|।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
