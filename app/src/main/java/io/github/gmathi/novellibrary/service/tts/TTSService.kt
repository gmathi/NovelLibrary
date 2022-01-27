package io.github.gmathi.novellibrary.service.tts

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.activity.TextToSpeechControlsActivity
import io.github.gmathi.novellibrary.activity.settings.TTSSettingsActivity
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.extensions.isPlaying
import io.github.gmathi.novellibrary.service.tts.TTSNotificationBuilder.Companion.TTS_NOTIFICATION_ID
import io.github.gmathi.novellibrary.util.lang.*
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.HashMap

class TTSService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val TAG = "NLTTS_Main"

        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"
        const val CHAPTER_INDEX = "chapterIndex"
        const val LINKED_PAGES = "linkedPages"
        const val KEY_SENTENCES = "sentences"

        const val ACTION_OPEN_CONTROLS = "open_controls"
        const val ACTION_OPEN_SETTINGS = "open_settings"
        const val ACTION_OPEN_READER = "open_reader"
        const val ACTION_UPDATE_SETTINGS = "update_settings"

        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY_PAUSE = "actionPlayPause"
        const val ACTION_PLAY = "actionPlay"
        const val ACTION_NEXT = "actionNext"
        const val ACTION_PREVIOUS = "actionPrevious"

        const val ACTION_STARTUP = "startup"

        const val COMMAND_REQUEST_LINKED_PAGES = "cmd_$LINKED_PAGES"
        const val COMMAND_REQUEST_SENTENCES = "cmd_$KEY_SENTENCES"
        const val COMMAND_UPDATE_LANGUAGE = "update_language"
        const val COMMAND_UPDATE_TIMER = "update_timer"
        const val COMMAND_LOAD_BUFFER_LINK = "cmd_load_buffer_link"
        const val COMMAND_RELOAD_CHAPTER = "cmd_reload_chapter"
        const val EVENT_SENTENCE_LIST = "event_$KEY_SENTENCES"
        const val EVENT_LINKED_PAGES = "event_$LINKED_PAGES"

        private const val STATE_PREFIX = "TTSService."
        const val STATE_NOVEL_ID = STATE_PREFIX + NOVEL_ID
        const val STATE_TRANSLATOR_SOURCE_NAME = STATE_PREFIX + TRANSLATOR_SOURCE_NAME
        const val STATE_CHAPTER_INDEX = STATE_PREFIX + CHAPTER_INDEX

        const val PITCH_MIN = 0.5f
        const val PITCH_MAX = 2.0f
        const val SPEECH_RATE_MIN = 0.5f
        const val SPEECH_RATE_MAX = 3.0f
    }

    lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: TTSSessionCallback
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    private val noisyReceiver = NoisyReceiver()
    private var noisyReceiverHooked = false

    private lateinit var notificationBuilder: TTSNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notification: NotificationController
    private lateinit var stopTimer: StopTimerController

    private lateinit var focusRequest: AudioFocusRequest

    private var isHooked = false
    private var isForeground = false

    private val focusLock = Any()
    private var resumeOnFocus = true

    lateinit var player: TTSPlayer
    var initialized: Boolean = false

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Service start!")

        val pendingIntentFlags:Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT }
            else { PendingIntent.FLAG_UPDATE_CURRENT }

        fun createPendingIntent(action: String):PendingIntent {
            val actionIntent = Intent(this, TTSService::class.java)
            actionIntent.action = action
            return PendingIntent.getService(this, 0, actionIntent, pendingIntentFlags)
        }

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, pendingIntentFlags)
        val mbrComponent = ComponentName(this, MediaButtonReceiver::class.java)

        mediaSession = MediaSessionCompat(baseContext, TAG, mbrComponent, mbrIntent).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS +
                    MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS +
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            @SuppressLint("WrongConstant")
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    // PlaybackStateCompat actions
                    // Stop, Play, Pause, Play/Pause, Skip to next/prev, forward/rewind, seek, play from media ID
                    0x77F
                )
                .addCustomAction(ACTION_OPEN_SETTINGS, "Open Settings", R.drawable.ic_settings_white_vector)
                .addCustomAction(ACTION_OPEN_READER, "Open Reader", R.drawable.ic_chrome_reader_mode_white_vector)
                .addCustomAction(ACTION_OPEN_CONTROLS, "Open Controls", R.drawable.ic_queue_music_white_vector)
//                .addCustomAction(ACTION_UPDATE_SETTINGS, "Update Settings", R.drawable.ic_refresh_white_vector)
//                .addCustomAction(COMMAND_REQUEST_SENTENCES, "Request sentences", R.drawable.ic_menu_white_vector)
            setPlaybackState(stateBuilder.build())

            mediaCallback = TTSSessionCallback()
            setCallback(mediaCallback)
            setSessionActivity(PendingIntent.getActivity(this@TTSService, 0,
                    Intent(this@TTSService, TextToSpeechControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    pendingIntentFlags
            ))
            // createPendingIntent(ACTION_OPEN_CONTROLS))

            // TODO: Store session data for restarting
            setMediaButtonReceiver(PendingIntent.getService(this@TTSService, 0,
                Intent(this@TTSService, TTSService::class.java).apply { action = ACTION_PLAY_PAUSE }, pendingIntentFlags))
            setSessionToken(sessionToken)

            try {
                isActive = true
            }
            catch (e: NullPointerException) {
                // VLC source code info:
                // Some versions of KitKat do not support AudioManager.registerMediaButtonIntent
                // with a PendingIntent. They will throw a NullPointerException, in which case
                // they should be able to activate a MediaSessionCompat with only transport
                // controls.
                isActive = false
                setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS + MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS)
                isActive = true
            }

        }

        player = TTSPlayer(this, mediaSession, stateBuilder)

        val pendingIntents = HashMap<String, PendingIntent>()
        pendingIntents[ACTION_PLAY] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        pendingIntents[ACTION_PAUSE] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
        pendingIntents[ACTION_STOP] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        pendingIntents[ACTION_NEXT] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        pendingIntents[ACTION_PREVIOUS] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        pendingIntents[ACTION_OPEN_CONTROLS] = createPendingIntent(ACTION_OPEN_CONTROLS)

        notificationBuilder = TTSNotificationBuilder(this, pendingIntents)
        notificationManager = NotificationManagerCompat.from(this)
        notification = NotificationController()

        stopTimer = StopTimerController()
    }

    override fun onDestroy() {
        Log.d(TAG,"Service destroyed!")
        unhookSystem()
        player.destroy()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("NONE", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d(TAG, "Start command! ${intent?.action} $flags $startId")

        when (intent?.action) {
            ACTION_STARTUP -> actionStartup(intent.extras!!)
            ACTION_OPEN_CONTROLS -> {
                startActivity(Intent(this, TextToSpeechControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_OPEN_READER -> {
                val novel = player.novel
                player.dbHelper.getWebPage(novel.id, player.translatorSourceName, player.chapterIndex)?.let { chapter ->
                    player.updateNovelBookmark(novel, chapter, false)
                }

                val bundle = Bundle()
                bundle.putParcelable("novel", novel)
                if (player.translatorSourceName != null)
                    bundle.putString("translatorSourceName", player.translatorSourceName)
                startActivity(Intent(this, ReaderDBPagerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), bundle)
            }
            ACTION_OPEN_SETTINGS -> {
                startActivity(Intent(this, TTSSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_UPDATE_SETTINGS -> {
                player.updateVoiceConfig()
            }
            ACTION_PLAY_PAUSE -> {
                if (player.isDisposed || !initialized) {
                    val data = Bundle()
                    // TODO: Fix it pausing due to MediaButtonReceiver calling onPause
                    player.dataCenter.internalGet {
                        data.putLong(NOVEL_ID, getLong(STATE_NOVEL_ID, 0))
                        val translator = getString(STATE_TRANSLATOR_SOURCE_NAME, null)
                        if (translator != null) data.putString(TRANSLATOR_SOURCE_NAME, translator)
                        data.putInt(CHAPTER_INDEX, getInt(STATE_CHAPTER_INDEX, 0))
                    }
                    actionStartup(data)
                } else {
                    if (player.isPlaying) mediaCallback.onPause()
                    else mediaCallback.onPlay()
                }
            }
            Intent.ACTION_MEDIA_BUTTON -> {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
            null -> {} // No action required
            else -> Log.w(TAG, "Unknown TTS action! ${intent.action}")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun actionStartup(extras: Bundle) {
        Log.d(TAG,"Booting up!")
        if (player.isDisposed) {
            player = TTSPlayer(this, mediaSession, stateBuilder)
        }
        val startupText = player.setBundle(extras)
        initialized = true
        if (hookSystem()) {
            if (startupText == null) {
                Log.d(TAG, "Startup had no text: Load from chapter metadata")
                player.loadCurrentChapter()
            } else {
                player.setData(startupText.text, startupText.title, startupText.bufferLinks)
                player.start()
            }
        }
    }

    // Initialize all the hooks that are required for MediaSession to run properly.
    // Except player.start
    fun hookSystem(): Boolean {
        if (isHooked) {
            if (!isForeground) {
                startService(Intent(applicationContext, TTSService::class.java))
                startForeground(TTS_NOTIFICATION_ID, notificationBuilder.buildNotification(mediaSession.sessionToken))
                isForeground = true
            }
            if (!noisyReceiverHooked) {
                registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                noisyReceiverHooked = true
            }
            return true
        }

        val am = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setOnAudioFocusChangeListener(this@TTSService)
                setAudioAttributes(AudioAttributes.Builder().run {
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    build()
                })
                build()
            }
            am.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(this@TTSService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return false

        startService(Intent(applicationContext, TTSService::class.java))
        mediaSession.isActive = true
        startForeground(TTS_NOTIFICATION_ID, notificationBuilder.buildNotification(mediaSession.sessionToken))
        notification.start()
        stopTimer.start()

        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        noisyReceiverHooked = true

        isForeground = true

        isHooked = true
        return true
    }

    fun unhookSystem(): Boolean {
        if (!isHooked) return false
        val am = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(this@TTSService)
        }
        notification.stop()
        stopTimer.stop()

        stopSelf()
        mediaSession.isActive = false
        if (isForeground) {
            stopForeground(true)
            isForeground = false
        }
        if (noisyReceiverHooked) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverHooked = false
        }
        resumeOnFocus = false

        isHooked = false
        return true
    }

    private inner class TTSSessionCallback : MediaSessionCompat.Callback() {

        override fun onPlay() {
            Log.d(TAG, "onPlay")
            if (hookSystem()) {
                if (player.isDisposed) {
                    val old = player
                    player = TTSPlayer(this@TTSService, mediaSession, stateBuilder)
                    player.setFrom(old)
                }
                player.start()
                stopTimer.reset()
            }
        }

        override fun onStop() {
            Log.d(TAG, "onStop")
            if (unhookSystem()) {
                player.destroy()
            }
        }

        override fun onPause() {
            Log.d(TAG, "onPause")
            player.stop()
            stopTimer.reset()
            resumeOnFocus = false
            if (isForeground) {
                stopForeground(false)
                isForeground = false
            }
            if (noisyReceiverHooked) {
                unregisterReceiver(noisyReceiver)
                noisyReceiverHooked = false
            }
        }

        override fun onSeekTo(pos: Long) {
            player.goto((pos / 1000L).toInt())
            stopTimer.reset()
        }

        override fun onRewind() {
            player.goto(player.lineNumber - 1)
            stopTimer.reset()
        }

        override fun onFastForward() {
            player.goto(player.lineNumber + 1)
            stopTimer.reset()
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            mediaId?.toIntOrNull()?.let {
                Log.d(TAG, "Play from media ID: $mediaId")
                player.gotoChapter(it)
                stopTimer.reset()
            }
        }
//        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
//            super.onPlayFromMediaId(mediaId, extras)
        // TODO: Support chapter navigation
//        }

        override fun onSkipToNext() {
            player.nextChapter()
            stopTimer.reset()
        }

        override fun onSkipToPrevious() {
            player.previousChapter()
            stopTimer.reset()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
//            Log.d(TAG, "Command: $command")
            when (command) {
                COMMAND_REQUEST_SENTENCES -> player.sendSentences()
                COMMAND_REQUEST_LINKED_PAGES -> player.sendLinkedPages()
                ACTION_UPDATE_SETTINGS -> player.updateVoiceConfig()
                COMMAND_UPDATE_LANGUAGE -> player.selectLanguage()
                COMMAND_UPDATE_TIMER -> {
                    if (extras?.containsKey("active") == true) extras.getBoolean("active").let {
                        if (it)
                            Log.d(TAG, "Starting auto-stop timer")
                        else
                            Log.d(TAG, "Stopping auto-stop timer (user-request)")
                        if (it && !stopTimer.isActive) stopTimer.reset(false)
                        stopTimer.isActive = it
                    }
                    if (extras?.getBoolean("reset") == true)
                        stopTimer.reset(false)
                    cb?.send(if (stopTimer.isActive) 1 else 0, Bundle().apply {
                        putLong("time", stopTimer.stopTime)
                        putBoolean("active", stopTimer.isActive)
                    })
                }
                COMMAND_LOAD_BUFFER_LINK -> extras?.getString("href")?.let { player.loadLinkedPage(it) }
                COMMAND_RELOAD_CHAPTER -> {
                    player.clearChapterCache()
                    player.loadCurrentChapter()
                }
            }
            super.onCommand(command, extras, cb)
        }

    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d(TAG, "Focus change $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocus) {
                synchronized(focusLock) { resumeOnFocus = false }
                player.start()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                synchronized(focusLock) { resumeOnFocus = false }
                player.stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                synchronized(focusLock) { resumeOnFocus = player.isPlaying }
                player.stop()
            }
        }
    }

    private inner class NoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                resumeOnFocus = false
                player.stop()
            }
        }
    }

    private inner class NotificationController : MediaControllerCompat.Callback() {

        private val controller = MediaControllerCompat(this@TTSService, mediaSession.sessionToken)

        fun start() {
            controller.registerCallback(this)
        }

        fun stop() {
            controller.unregisterCallback(this)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (state != null && mediaSession.controller.metadata != null) {
                notificationManager.notify(TTS_NOTIFICATION_ID, notificationBuilder.buildNotification(mediaSession.sessionToken))
            }
        }
    }

    private inner class StopTimerController : MediaControllerCompat.Callback() {
        private val controller = MediaControllerCompat(this@TTSService, mediaSession.sessionToken)

        var isActive: Boolean = false
        var stopTime: Long = 0

        fun reset(withEvent: Boolean = true) {
            // In case we pause or do something else that interrupts playback - reset the timer.
            stopTime = System.currentTimeMillis() + java.util.concurrent.TimeUnit.MINUTES.toMillis(player.dataCenter.ttsPreferences.stopTimer)
            if (isActive && withEvent) {
                Log.d(TAG, "Resetting the auto-stop timer")
                mediaSession.sendSessionEvent(COMMAND_UPDATE_TIMER, Bundle().apply {
                    putBoolean("active", isActive)
                    putLong("time", stopTime)
                })
            }
        }

        fun start() {
            controller.registerCallback(this)
        }

        fun stop() {
            controller.unregisterCallback(this)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            if (isActive) {
                if (state?.isPlaying == true && System.currentTimeMillis() >= stopTime) {
                    Log.d(TAG, "Pausing TTS due to auto-stop timer")
                    isActive = false
                    controller.transportControls.pause()
                    mediaSession.sendSessionEvent(COMMAND_UPDATE_TIMER, Bundle().apply {
                        putBoolean("active", isActive)
                        putLong("time", 0)
                    })
                    // TODO: Save state on exit so user can restore it
                }
            }
        }

    }

}