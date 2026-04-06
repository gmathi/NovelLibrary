package io.github.gmathi.novellibrary.service.ai_tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.HandlerThread
import android.os.Process
import io.github.gmathi.novellibrary.model.preference.AiTtsPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var sentences: List<String> = emptyList()
    private var title: String = ""
    private var linkedPages: ArrayList<String> = arrayListOf()
    private var chapterIndex: Int = 0

    // --- Coroutines ---
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playbackJob: Job? = null

    // --- Audio thread ---
    private val audioThread = HandlerThread("ai_tts_audio", Process.THREAD_PRIORITY_AUDIO).also { it.start() }
    private val audioHandler = android.os.Handler(audioThread.looper)

    fun setData(text: String, title: String, linkedPages: ArrayList<String>, chapterIndex: Int) {
        stop()
        this.title = title
        this.linkedPages = linkedPages
        this.chapterIndex = chapterIndex
        this.sentences = splitIntoSentences(text)
        _currentSentenceIndex.value = 0
        _playbackState.value = AiTtsPlaybackState.Idle
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
        _playbackState.value = AiTtsPlaybackState.Paused
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Paused)
    }

    fun stop() {
        playbackJob?.cancel()
        _playbackState.value = AiTtsPlaybackState.Stopped
        _currentSentenceIndex.value = 0
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Stopped)
    }

    fun nextSentence() {
        val next = _currentSentenceIndex.value + 1
        if (next < sentences.size) {
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
        audioThread.quit()
        modelManager.unloadModel()
    }

    // --- Internal ---

    private suspend fun playSentencesFrom(startIndex: Int) {
        _playbackState.value = AiTtsPlaybackState.LoadingModel
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.LoadingModel)

        val tts = withContext(Dispatchers.IO) {
            modelManager.loadModel(preferences.voiceId)
        }

        _playbackState.value = AiTtsPlaybackState.Playing
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Playing)

        for (index in startIndex until sentences.size) {
            if (!currentCoroutineContext().isActive) break
            _currentSentenceIndex.value = index
            val sentence = sentences[index]
            eventListener?.onSentenceChanged(index, sentence)
            synthesizeAndPlay(tts, sentence)
        }

        if (currentCoroutineContext().isActive) {
            if (preferences.autoReadNextChapter) {
                eventListener?.onChapterChanged(chapterIndex + 1)
            } else {
                _playbackState.value = AiTtsPlaybackState.Idle
                eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.Idle)
            }
        }
    }

    private suspend fun synthesizeAndPlay(
        tts: com.k2fsa.sherpa.onnx.OfflineTts,
        sentence: String
    ) = withContext(Dispatchers.IO) {
        if (sentence.isBlank()) return@withContext
        val audio = tts.generate(
            text = sentence,
            sid = 0,
            speed = preferences.speechRate
        )
        playPcmOnAudioTrack(audio.samples, audio.sampleRate)
    }

    private fun playPcmOnAudioTrack(samples: FloatArray, sampleRate: Int) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()
        audioTrack.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        audioTrack.stop()
        audioTrack.release()
    }

    private fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
