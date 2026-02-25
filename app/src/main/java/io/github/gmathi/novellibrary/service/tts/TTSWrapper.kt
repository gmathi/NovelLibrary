package io.github.gmathi.novellibrary.service.tts

import android.content.Context
import android.media.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.collection.CircularArray
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

/**
 * The wrapper around TTS engine that does playback from the app, not TTS engine.
 *
 * @param legacyMode If set, will use default TTS playback features, i.e. it won't play audio from the app but instead from the TTS engine.
 */
class TTSWrapper(val context: Context, var callback: TTSWrapperCallback, private val legacyMode: Boolean) :
    TextToSpeech.OnInitListener, UtteranceProgressListener(), AudioTrack.OnPlaybackPositionUpdateListener, Handler.Callback {

    companion object {
        const val SENTENCE_CACHE_SIZE = 8
    }

    interface TTSWrapperCallback {
        fun onInit(status: Int)

        fun onError(utteranceId: String, errorCode: Int)
        fun onStart(utteranceId: String)
        fun onDone(utteranceId: String)
        fun onRangeStart(utteranceId: String, start: Int, end: Int)
        fun onStop(utteranceId: String, interrupted: Boolean)
    }

    private enum class TTSQueueType {
        Setup, // Apply changes for TTS playback.
        Speak, // Speak text.
        Earcon, // Play earcon.
        Silence, // Play silence.
    }

    private class TTSConfig() {
        var pitch = 1.0f
        var speed = 1.0f
        var language: Locale? = null
        var voice: Voice? = null

        fun apply(tts: TextToSpeech) {
            tts.setPitch(pitch)
            tts.setSpeechRate(speed)
            if (language != null) tts.language = language
            if (voice != null) tts.voice = voice
        }
    }

    private abstract class TTSQueueCommand(val utteranceId: String) {
        companion object {
            // In order to avoid data pollution when TTS is stopped but events still trickle in, use incremental utterance IDs internally
            var uidCounter: Long = 0
        }

        /** Internal ID used to distinguish this particular queue item during TTS callback events. */
        var internalId: String = "uid_${TTSQueueCommand.uidCounter++}"

        // If returns true - system should move on and consume next command.
        abstract fun execute(tts: TTSWrapper): Boolean
    }

    private class SetPitchCommand(val pitch: Float) : TTSQueueCommand("") {
        override fun execute(tts: TTSWrapper): Boolean {
            if (tts.activeConfig.pitch != pitch) tts.tts.setPitch(pitch)
            tts.activeConfig.pitch = pitch
            return true
        }

        override fun toString(): String {
            return "SetPitch($pitch)"
        }
    }

    private class SetSpeedCommand(val speed: Float) : TTSQueueCommand("") {
        override fun execute(tts: TTSWrapper): Boolean {
            if (tts.activeConfig.speed != speed) tts.activeConfig.speed = speed
            tts.tts.setSpeechRate(speed)
            return true
        }

        override fun toString(): String {
            return "SetSpeechRate($speed)"
        }
    }

    private class SetLanguageCommand(val language: Locale) : TTSQueueCommand("") {
        override fun execute(tts: TTSWrapper): Boolean {
            tts.activeConfig.language = language
            tts.tts.language = language
            return true
        }

        override fun toString(): String {
            return "SetLanguage($language)"
        }
    }

    private class SetVoiceCommand(val voice: Voice) : TTSQueueCommand("") {
        override fun execute(tts: TTSWrapper): Boolean {
            tts.activeConfig.voice = voice
            tts.tts.voice = voice
            return true
        }

        override fun toString(): String {
            return "SetVoice(${voice.name})"
        }
    }

    private abstract class PlaybackCommand(utteranceId: String) : TTSQueueCommand(utteranceId) {
        var begin: Int = 0
        var frames: Int = 0
        var rangeOffset: Int = 0
        var markers = mutableListOf<TTSMarker>()
    }

    private class SilenceCommand(val durationMs: Long, utteranceId: String) : PlaybackCommand(utteranceId) {
        override fun execute(tts: TTSWrapper): Boolean {
            return if (tts.legacyMode) {
                tts.playSilentUtterance(durationMs, TextToSpeech.QUEUE_ADD, utteranceId)
                false
            } else {
                tts.initDefaultTrack(internalId)
                val samples = tts.writer.addSilence(durationMs.toInt())
                begin = tts.frame
                frames = samples
                tts.addMarkerUnsafe(TTSMarker(TTSMarkerType.Start, this, 0))
                tts.addMarkerUnsafe(TTSMarker(TTSMarkerType.Done, this, samples))
                tts.frame += samples
                true
            }
        }

        override fun toString(): String {
            return "PlaySilence(${durationMs}ms)"
        }
    }

    private class EarconCommand(val earconId: String, val params: Bundle, utteranceId: String) : PlaybackCommand(utteranceId) {
        override fun execute(tts: TTSWrapper): Boolean {
            if (tts.legacyMode) {
                tts.tts.playEarcon(earconId, TextToSpeech.QUEUE_ADD, params, utteranceId)
                return false
            } else {
                val earcon = tts.earcons[earconId] ?: return true
                tts.initDefaultTrack(internalId)
                val samples = tts.writer.addSilence(earcon.duration)
                begin = tts.frame
                frames = samples
                tts.addMarkerUnsafe(TTSMarker(this, earcon, 0))
                tts.addMarkerUnsafe(TTSMarker(this, 0, earconId.length, 0))
                tts.addMarkerUnsafe(TTSMarker(TTSMarkerType.Done, this, samples))
                tts.frame += samples
                return true
            }
        }

        override fun toString(): String {
            return "PlayEarcon($earconId)"
        }
    }

    private class SpeakCommand(val text: String, rangeOffset: Int, val params: Bundle, utteranceId: String) : PlaybackCommand(utteranceId) {
        var audio = mutableListOf<ByteArray>()
        var cached = false
        init { this.rangeOffset = rangeOffset }

        override fun execute(tts: TTSWrapper): Boolean {
//            Log.d(TTSPlayer.TAG, "Speaking text: \"${cmd.text}\"")
            tts.synthesizing = this
            if (tts.legacyMode) {
                tts.tts.speak(text, TextToSpeech.QUEUE_ADD, params, utteranceId)
                return false
            } else {
                if (cached) {
                    tts.initDefaultTrack(internalId)
                    begin = tts.frame
                    markers.forEach { tts.addMarkerUnsafe(it) }
                    tts.frame += frames
                    tts.writer.addList(audio)
                    tts.synthesizing = null
                    return true
                } else {
                    var retries = 0
                    while (retries < 3) {
                        try {
                            if (tts.tts.synthesizeToFile(text, params, tts.devNull(), internalId) == TextToSpeech.ERROR) {
                                // TTS engine bug: Randomly disconnect service for no reason
                                tts.queue.addFirst(this)
                                tts.synthesizing = null
                                tts.restartTTS()
                                return false
                            }
                            break
                        } catch (crash: DeadObjectException) {
                            // TTS engine bug: Randomly complain about dead objects
                        }
                        retries++
                    }
                    return false
                }
            }
        }

        fun clone(utteranceId: String, params: Bundle, rangeOffset: Int): SpeakCommand {
            val cmd = SpeakCommand(text, rangeOffset, params, utteranceId)
            cmd.begin = this.begin
            cmd.frames = this.frames
            cmd.audio = this.audio
            cmd.markers.addAll(this.markers.map { it.clone(this, false) })
            cmd.cached = this.cached
            return cmd
        }

        override fun toString(): String {
            return "SpeakText($text)"
        }
    }

    private val queue = CircularArray<TTSQueueCommand>(8)
    private var synthesizing: SpeakCommand? = null // Currently synthesizing sentence
    private var playing: PlaybackCommand? = null // Currently playing sentence
    // TODO: Reintroduce sentence cache
    // Need to account for config changes

    private data class Earcon(val player: MediaPlayer) {
        val duration = player.duration
    }
    private val earcons = mutableMapOf<String, Earcon>()

    var tts: TextToSpeech = TextToSpeech(context, this)
    // In case of mismatch - ignore the event.
    private var disposed = false
    private val sessionId = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
    private val earconId = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()

    private enum class TTSMarkerType {
        RangeStart,
        Start,
        StartEarcon,
        Done,
    }
    private class TTSMarkerS(val type: TTSMarkerType, val frame: Int) {
        var start: Int = 0
        var end: Int = 0
    }
    private class TTSMarker(val type: TTSMarkerType, val owner: PlaybackCommand, val frame: Int, selfAdd: Boolean = true) {

        var start: Int = 0
        var end: Int = 0
        lateinit var earcon: Earcon
        init {
            if (selfAdd) owner.markers.add(this)
        }

        constructor(owner: PlaybackCommand, start: Int, end: Int, frame: Int, selfAdd: Boolean = true) : this(TTSMarkerType.RangeStart, owner, frame, selfAdd) {
            this.start = start
            this.end = end
        }

        constructor(owner: PlaybackCommand, earcon: Earcon, frame: Int, selfAdd: Boolean = true) : this(TTSMarkerType.StartEarcon, owner, frame, selfAdd) {
            this.earcon = earcon
        }

        fun clone(owner: PlaybackCommand, selfAdd: Boolean): TTSMarker {
            return when (type) {
                TTSMarkerType.Start, TTSMarkerType.Done -> TTSMarker(type, owner, frame, selfAdd)
                TTSMarkerType.RangeStart -> TTSMarker(owner, start, end, frame, selfAdd)
                TTSMarkerType.StartEarcon -> TTSMarker(owner, earcon, frame, selfAdd)
            }
        }

    }

    private val markers = CircularArray<TTSMarker>(128)
    private var nextMarker: TTSMarker? = null

    private var track: AudioTrack? = null
    private var frame: Int = 0 // Current buffer frame since last stop
    private var bytesPerFrame: Int = 1 // How many bytes 1 frame of sample data takes
    private var bufferSize: Int = 4096 // The size of the immediate AudioTrack buffer
    private val writer = BufferWriter()
    private val silence: ByteArray = ByteArray(2205*2) // 100ms worth of 16BIT PCM at 2205hz
    private var preventConsume: Boolean = false // Would prevent next command from being consumed during callbacks.
    //private val activeConfig: TTSQueue = TTSQueue(TTSQueueType.Setup)
    private val activeConfig: TTSConfig = TTSConfig()

    private lateinit var eventHandler: Handler

    fun speak(text: String, queueMode: Int, params: Bundle, utteranceId: String) {
        if (text.isBlank()) playSilentUtterance(100, queueMode, utteranceId)
        else {
            if (queueMode == TextToSpeech.QUEUE_FLUSH) interrupt()
            addCmd(SpeakCommand(text, params.getInt("offset", 0), params, utteranceId))
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun playSilentUtterance(durationMs: Long, queueMode: Int, utteranceId: String) {
        if (queueMode == TextToSpeech.QUEUE_FLUSH) interrupt()
        addCmd(SilenceCommand(durationMs, utteranceId))
    }

    fun playEarcon(earcon: String, queueMode: Int, params: Bundle, utteranceId: String) {
        if (queueMode == TextToSpeech.QUEUE_FLUSH) interrupt()
        if (legacyMode || earcons.contains(earcon)) addCmd(EarconCommand(earcon, params, utteranceId))
        else addCmd(SpeakCommand("Earcon: $earcon", 0, params, utteranceId))
    }

    fun addEarcon(earcon: String, resourceId: Int) {
        if (legacyMode) {
            tts.addEarcon(earcon, context.packageName, resourceId)
        } else {
            val player = MediaPlayer.create(context, resourceId,
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build(),
                earconId
            )

    //        Log.d(TTSPlayer.TAG, "Adding a new earcon: $earcon, duration: ${player.duration}")
            earcons[earcon] = Earcon(player)
        }
    }

    fun setPitch(pitch: Float) = addCmd(SetPitchCommand(pitch))
    fun setSpeechRate(rate: Float) = addCmd(SetSpeedCommand(rate))

    /** Sets the TTS language. If not called with immediate mode will always return LANG_AVAILABLE! */
    fun setLanguage(locale: Locale, immediate: Boolean = false): Int {
        return if (immediate) {
            activeConfig.language  = locale
            tts.setLanguage(locale)
        } else {
            addCmd(SetLanguageCommand(locale))
            TextToSpeech.LANG_AVAILABLE
        }
    }

    /** Sets the TTS language. If not called with immediate mode will always return SUCCESS! */
    fun setVoice(voice: Voice, immediate: Boolean = false): Int {
        return if (immediate) {
            activeConfig.voice = voice
            tts.setVoice(voice)
        } else {
            addCmd(SetVoiceCommand(voice))
            TextToSpeech.SUCCESS
        }
    }

    fun shutdown() {
        stop()
        tts.shutdown()
        disposed = true
        if (!legacyMode) {
            track?.release()
            track = null
            earcons.forEach { earcon -> earcon.value.player.release() }
            earcons.clear()
        }
    }

    fun stop() = interrupt()

    // Internal
    @Suppress("NOTHING_TO_INLINE") // Shut up Kotlin
    private inline fun devNull() = File("/dev/null")

    private fun initDefaultTrack(id: String) {
        if (track == null) onBeginSynthesis(id, 22050, AudioFormat.ENCODING_PCM_16BIT, 1)
    }

    private fun consumeNext() {
        if (preventConsume) return // Caused by addCmd during a callback
        synthesizing = null
        do {
            if (queue.isEmpty()) {
                synthesizing = null
                return
            }
            val cmd = queue.popFirst()
//            Log.d(TTSPlayer.TAG, "Consume $cmd @ $frame")
        } while (cmd.execute(this))

    }

    private fun restartTTS() {
        tts.shutdown()
        tts = TextToSpeech(context, this)
    }

    private fun interrupt() {
//        Log.d(TTSPlayer.TAG, "Interrupting playback!")
        tts.stop()
        if (!legacyMode) {

            writer.clear()
            earcons.forEach { earcon ->
                val player = earcon.value.player
                if (player.isPlaying) player.stop()
            }

            if (playing != null) {
                callback.onStop(playing!!.utteranceId, true)
                playing = null
            }
            if (synthesizing != null) {
                callback.onStop(synthesizing!!.utteranceId, false)
                synthesizing = null
            }
            while (!queue.isEmpty()) {
                val cmd = queue.popFirst()
                if (cmd is PlaybackCommand) callback.onStop(cmd.utteranceId, false)
                else cmd.execute(this) // Setup commands, to ensure our config is updated.
            }
            nextMarker = null
            markers.clear()
        } else {
            synthesizing = null
            queue.clear()
        }
    }

    private fun addCmd(cmd: TTSQueueCommand) {
//        Log.d(TTSPlayer.TAG, "Adding command ${cmd.type.name}")
        queue.addLast(cmd)
        if (synthesizing == null) consumeNext()
    }

    private fun addMarker(marker: TTSMarker) {
//        Log.d(TTSPlayer.TAG, "Adding marker: ${marker.type.name} @ ${marker.frame}")
        if (synthesizing != marker.owner) {
            Log.w(TTSPlayer.TAG, "Attempting to add marker at wrong time: $marker")
            return
        } // Avoid adding markers that are not part of the currently synthesizing sentence.
        if (nextMarker == null) {
            setMarker(marker)
        } else {
            markers.addLast(marker)
        }
    }

    private fun addMarkerUnsafe(marker: TTSMarker) {
        if (nextMarker == null) {
            setMarker(marker)
        } else {
            markers.addLast(marker)
        }
    }

    private fun setMarker(marker: TTSMarker, force: Boolean = false) {
        if (marker == nextMarker && !force) return
        nextMarker = marker
        val frame = marker.frame + marker.owner.begin
        if (track != null) { // In case marker position is in the past - fire it immediately
            if (frame <= track!!.playbackHeadPosition) onMarkerReached(track!!)
        }
//        if (marker.type != TTSMarkerType.RangeStart)
//            Log.d(TTSPlayer.TAG, "== Setting marker: $marker <- ${track?.playbackHeadPosition}")
//        track?.notificationMarkerPosition = if (frame == 0) -1 else frame
    }

    private inner class BufferWriter : Runnable {

        val data = CircularArray<ByteBuffer>(256)
        val mutex = Mutex()

        var written = 0 // Frames written
        var elapsed = 0 // Frames elapsed
        var lastPosition = 0 // Last position of the AudioTrack - UNRELIABLE
        var lastUnderrun = 0

        fun add(audio: ByteArray) = runBlocking {
            mutex.withLock {
                data.addLast(ByteBuffer.wrap(audio))
            }
        }

        fun addList(list: List<ByteArray>) = runBlocking {
            mutex.withLock {
                list.forEach { data.addLast(ByteBuffer.wrap(it)) }
            }
        }

        fun addSilence(durationMs: Int): Int = runBlocking {
            val samples = ceil(track!!.sampleRate * durationMs / 1000f).toInt()
            var bytesToPlay = samples * bytesPerFrame
            mutex.withLock {
                while (bytesToPlay > 0) {
                    data.addLast(ByteBuffer.wrap(silence, 0, min(bytesToPlay, silence.size)))
                    bytesToPlay -= silence.size
                }
            }
            samples
        }

        fun addSegment(audio: ByteArray, offset: Int, length: Int) = runBlocking {
            mutex.withLock {
                data.addLast(ByteBuffer.wrap(audio, offset, length))
            }
        }

        fun clear() = runBlocking {
            mutex.withLock {
                data.clear()
                track?.let { track ->
                    track.pause()
                    track.flush()
                    track.notificationMarkerPosition = 0
                    written = 0
                    elapsed = 0
                    lastPosition = 0
                    frame = 0
                }
            }
        }

        suspend fun buffer() {
            mutex.withLock {
                track?.let { track ->
                    while (!data.isEmpty()) {
                        val buf = data.first
                        val remaining = min(bufferSize, buf.remaining())
                        val total = track.write(buf, buf.remaining(), AudioTrack.WRITE_NON_BLOCKING)
                        this.written += total / bytesPerFrame
                        if (total < remaining) {
                            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()  // Primed
                        }
                        //if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
                        if (!buf.hasRemaining()) {
                            data.popFirst()
                        } else break
                    }
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        val pos = track.playbackHeadPosition
                        if (pos < lastPosition) {
                            // Underrun happened and it decided to reset position
                            // Thanks android for yet another undocumented behavior.
                            // Assume all data we have was consumed
                            Log.d(TTSPlayer.TAG, "=== RESET $pos <-> $lastPosition <-> $written")
                            elapsed = written
                        } else {
                            elapsed += pos - lastPosition
                        }
                        nextMarker?.let { marker ->
                            if (marker.frame+marker.owner.begin <= elapsed) {
                                val msg = eventHandler.obtainMessage()
                                msg.arg1 = elapsed
                                eventHandler.sendMessage(msg)
                            }
                        }
//                        if (lastPosition == track.playbackHeadPosition) {
//                            Log.d(TTSPlayer.TAG, "$lastPosition, ${lastPosition+bufferSize/bytesPerFrame}, $elapsed, $written, $nextMarker")
//                        }
                        lastPosition = pos
                    }
                }
            }
        }

        override fun run() = runBlocking {
            val frame = (1000L/60L)
            while (!disposed) {
                buffer()
                //Thread.currentThread().isDaemon
                Thread.sleep(frame) // Since I'm in blocking mode anyway
//                kotlinx.coroutines.delay(frame)
            }
        }

    }

    // Interface
    override fun onInit(status: Int) {
        tts.setOnUtteranceProgressListener(this)
        activeConfig.apply(tts) // Make sure to sync config on restarts.
        callback.onInit(status)
        if (!legacyMode) {
            eventHandler = Handler(Looper.myLooper()!!, this)
            val thread = Thread(writer, "tts_buffer_writer")
            thread.priority = Thread.MAX_PRIORITY
            thread.start()
        }
        if (synthesizing == null && !queue.isEmpty()) consumeNext()
    }

    override fun onBeginSynthesis(utteranceId: String, sampleRateInHz: Int, audioFormat: Int, channelCount: Int) {
//        Log.d(TTSPlayer.TAG, "onBeginSynthesis $utteranceId ${if (synthesizing?.internalId == utteranceId) "==" else "!="} ${synthesizing?.internalId}")
        if (legacyMode || synthesizing?.internalId != utteranceId) return

        if (track == null || track!!.sampleRate != sampleRateInHz || track!!.audioFormat != audioFormat || track!!.channelCount != channelCount) {
            // TODO: Don't do that right away, and instead delay until this utterance becomes active
            track?.release()
            val fmt =
                if (channelCount == 1) AudioFormat.CHANNEL_OUT_MONO
                else AudioFormat.CHANNEL_OUT_STEREO
            when (audioFormat) {
                AudioFormat.ENCODING_PCM_8BIT -> bytesPerFrame = channelCount
                AudioFormat.ENCODING_PCM_16BIT -> bytesPerFrame = channelCount * 2
                AudioFormat.ENCODING_PCM_FLOAT -> bytesPerFrame = channelCount * 4
            }
            bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, fmt, audioFormat)
            track = AudioTrack(
                AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build(),
                AudioFormat.Builder().setSampleRate(sampleRateInHz).setEncoding(audioFormat).setChannelMask(fmt).build(),
                bufferSize,
                AudioTrack.MODE_STREAM, sessionId
            )
            track!!.setPlaybackPositionUpdateListener(this)
            if (nextMarker != null) setMarker(nextMarker!!, true)

//            Log.d(TTSPlayer.TAG, "Creating AudioTrack with sample rate $sampleRateInHz, format $audioFormat, and $channelCount channels, buf size ${bufferSize}.")
        }
    }

    override fun onAudioAvailable(utteranceId: String, audio: ByteArray) {
        if (legacyMode || synthesizing?.internalId != utteranceId) return
        synthesizing!!.audio.add(audio)
        synthesizing!!.frames += audio.size / bytesPerFrame
        frame += audio.size / bytesPerFrame
        writer.add(audio)
    }

    override fun onRangeStart(utteranceId: String, frame: Int, start: Int, end: Int) {
        if (legacyMode) {
            // Pipe directly
            val offset = (synthesizing?.rangeOffset ?: 0)
            callback.onRangeStart(utteranceId, start + offset, end + offset)
        } else {
            if (synthesizing?.internalId != utteranceId) return
            // TTS engine bug? Expected (docs): start, end, frame, got: frame, start, end
            addMarker(TTSMarker(synthesizing!!, start, end, frame))
        }
    }

    override fun onStart(utteranceId: String) {
//        Log.d(TTSPlayer.TAG, "onStart $utteranceId ${if (synthesizing?.internalId == utteranceId) "==" else "!="} ${synthesizing?.internalId}")
        if (legacyMode) {
            callback.onStart(utteranceId)
        } else {
            if (synthesizing?.internalId != utteranceId) return
            synthesizing!!.begin = frame
            addMarker(TTSMarker(TTSMarkerType.Start, synthesizing!!, 0))
        }
    }

    override fun onDone(utteranceId: String) {
//        Log.d(TTSPlayer.TAG, "onDone $utteranceId ${if (synthesizing?.internalId == utteranceId) "==" else "!="} ${synthesizing?.internalId}")
        if (legacyMode) {
            preventConsume = true
            callback.onDone(utteranceId)
            preventConsume = false
            consumeNext()
        } else {
            if (synthesizing?.internalId != utteranceId) return
            synthesizing!!.let { cmd ->
                addMarker(TTSMarker(TTSMarkerType.Done, cmd, cmd.frames))
                // TODO: Cache
            }
            consumeNext()
        }
    }

    override fun onError(utteranceId: String, errorCode: Int) {
        preventConsume = true
        callback.onError(synthesizing?.utteranceId ?: utteranceId, errorCode)
        preventConsume = false
        consumeNext()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
    override fun onError(utteranceId: String) {
        onError(utteranceId, -1)
    }

    override fun onMarkerReached(track: AudioTrack) {
        nextMarker?.let { marker ->
            if (marker.frame > track.playbackHeadPosition) return

//            if (marker.type != TTSMarkerType.RangeStart)
//                Log.d(TTSPlayer.TAG, "-- Reached marker: $marker <- ${track.playbackHeadPosition}")
            when (marker.type) {
                TTSMarkerType.Start -> {
                    playing = marker.owner
                    callback.onStart(marker.owner.utteranceId)
                }
                TTSMarkerType.StartEarcon -> {
                    playing = marker.owner
                    val earcon = marker.earcon
                    earcon.player.stop()
                    earcon.player.prepare()
                    earcon.player.start()
                    callback.onStart(marker.owner.utteranceId)
                }
                TTSMarkerType.Done -> {
                    callback.onDone(marker.owner.utteranceId)
                    playing = null
                }
                TTSMarkerType.RangeStart -> {
                    if (marker.owner == playing) callback.onRangeStart(marker.owner.utteranceId, marker.start + marker.owner.rangeOffset, marker.end + marker.owner.rangeOffset)
                }
            }

            if (!markers.isEmpty()) setMarker(markers.popFirst())
            else {
                if (nextMarker == marker) nextMarker = null
                track.notificationMarkerPosition = 0
            }
        }
    }

    override fun onPeriodicNotification(track: AudioTrack) {}

    override fun handleMessage(msg: Message): Boolean {
        if (track == null) return true
        while (nextMarker != null && nextMarker!!.frame+nextMarker!!.owner.begin <= msg.arg1) {
            onMarkerReached(track!!)
        }
        return true
    }

}