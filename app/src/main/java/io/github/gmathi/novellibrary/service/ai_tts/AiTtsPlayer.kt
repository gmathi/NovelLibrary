package io.github.gmathi.novellibrary.service.ai_tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.HandlerThread
import android.os.Process
import io.github.gmathi.novellibrary.model.preference.AiTtsPreferences
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    // --- Audio thread ---
    private val audioThread = HandlerThread("ai_tts_audio", Process.THREAD_PRIORITY_AUDIO).also { it.start() }
    private val audioHandler = android.os.Handler(audioThread.looper)

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
        audioThread.quit()
        modelManager.unloadModel()
    }

    // --- Internal ---

    private suspend fun playSentencesFrom(startIndex: Int) {
        Logs.debug(TAG, "playSentencesFrom: startIndex=$startIndex totalSentences=${_sentences.value.size} voiceId=${preferences.voiceId}")
        _playbackState.value = AiTtsPlaybackState.LoadingModel
        eventListener?.onPlaybackStateChanged(AiTtsPlaybackState.LoadingModel)

        val tts = try {
            withContext(Dispatchers.IO) {
                modelManager.loadModel(preferences.voiceId)
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
            synthesizeAndPlay(tts, sentence)
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
    ) = withContext(Dispatchers.IO) {
        if (sentence.isBlank()) return@withContext
        Logs.debug(TAG, "synthesizeAndPlay: generating audio for sentence length=${sentence.length} speed=${preferences.speechRate}")
        val audio = tts.generate(
            text = sentence,
            sid = 0,
            speed = preferences.speechRate
        )
        Logs.debug(TAG, "synthesizeAndPlay: generated ${audio.samples.size} samples at ${audio.sampleRate}Hz")
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
