package io.github.gmathi.novellibrary.service.ai_tts

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import io.github.gmathi.novellibrary.worker.AiTtsModelDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val LOG_TAG = "AiTtsService"

class AiTtsService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener, AiTtsEventListener {

    companion object {
        /** Non-null while the service is running. UI can observe [instance] to access player state. */
        @Volatile
        var instance: AiTtsService? = null
            private set

        const val ACTION_STARTUP = "io.github.gmathi.novellibrary.AI_TTS_STARTUP"
        const val ACTION_PLAY_PAUSE = "io.github.gmathi.novellibrary.AI_TTS_PLAY_PAUSE"
        const val ACTION_STOP = "io.github.gmathi.novellibrary.AI_TTS_STOP"
        const val ACTION_NEXT = "io.github.gmathi.novellibrary.AI_TTS_NEXT"
        const val ACTION_PREVIOUS = "io.github.gmathi.novellibrary.AI_TTS_PREVIOUS"
        const val AUDIO_TEXT_KEY = "audioText"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val LINKED_PAGES = "linkedPages"
        const val CHAPTER_INDEX = "chapterIndex"
        const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"
    }

    lateinit var player: AiTtsPlayer
    private lateinit var modelManager: AiTtsModelManager
    private lateinit var notificationBuilder: AiTtsNotificationBuilder
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null  // API 26+

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val noisyReceiver = NoisyReceiver()
    private var noisyReceiverRegistered = false

    private var chapterIndex: Int = 0

    // BroadcastReceiver for MODEL_READY
    private val modelReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val voiceId = intent.getStringExtra(AiTtsModelDownloadWorker.KEY_VOICE_ID) ?: return
            // Re-trigger startup if this is the voice we were waiting for
            pendingStartExtras?.let { extras ->
                val prefs = DataCenter(context).aiTtsPreferences
                if (voiceId == prefs.voiceId) {
                    actionStartup(extras)
                    pendingStartExtras = null
                }
            }
        }
    }
    private var pendingStartExtras: Bundle? = null

    inner class AiTtsSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() { player.start() }
        override fun onPause() { player.pause() }
        override fun onStop() {
            player.stop()
            stopSelf()
        }
        override fun onSkipToNext() { player.nextSentence() }
        override fun onSkipToPrevious() { player.prevSentence() }
        override fun onCustomAction(action: String?, extras: Bundle?) {
            when (action) {
                ACTION_NEXT -> player.nextChapter()
                ACTION_PREVIOUS -> player.prevChapter()
            }
        }
    }

    inner class NoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    inner class NotificationController {
        fun start() {
            scope.launch {
                player.playbackState.collect { state ->
                    val notification = notificationBuilder.buildNotification(mediaSession.sessionToken)
                    updateNotification(notification, state)
                }
            }
        }

        private fun updateNotification(notification: android.app.Notification, state: AiTtsPlaybackState) {
            when (state) {
                is AiTtsPlaybackState.Playing -> startForeground(
                    Notifications.ID_AI_TTS_PLAYBACK,
                    notification
                )
                is AiTtsPlaybackState.Paused -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(false)
                    }
                    notificationBuilder.notify(notification)
                }
                is AiTtsPlaybackState.Stopped, is AiTtsPlaybackState.Idle -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                }
                else -> { /* LoadingModel, Error — keep current notification */ }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        modelManager = AiTtsModelManager(this)
        val prefs = DataCenter(this).aiTtsPreferences
        player = AiTtsPlayer(modelManager, prefs, this)

        mediaSession = MediaSessionCompat(this, "AiTtsService").apply {
            setCallback(AiTtsSessionCallback())
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        notificationBuilder = AiTtsNotificationBuilder(this, buildPendingIntents())
        NotificationController().start()

        ContextCompat.registerReceiver(
            this,
            modelReadyReceiver,
            android.content.IntentFilter(AiTtsModelDownloadWorker.ACTION_MODEL_READY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logs.debug(LOG_TAG,"onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_STARTUP -> actionStartup(intent.extras ?: Bundle())
            ACTION_PLAY_PAUSE -> if (player.playbackState.value is AiTtsPlaybackState.Playing) player.pause() else player.start()
            ACTION_STOP -> { player.stop(); stopSelf() }
            ACTION_NEXT -> player.nextSentence()
            ACTION_PREVIOUS -> player.prevSentence()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        instance = null
        unhookSystem()
        unregisterReceiver(modelReadyReceiver)
        if (noisyReceiverRegistered) {
            unregisterReceiver(noisyReceiver)
            noisyReceiverRegistered = false
        }
        player.destroy()
        mediaSession.release()
        scope.cancel()
        super.onDestroy()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("ai_tts_root", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    private fun hookSystem() {
        // Register noisy receiver
        if (!noisyReceiverRegistered) {
            ContextCompat.registerReceiver(this, noisyReceiver, android.content.IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
            noisyReceiverRegistered = true
        }

        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
    }

    private fun unhookSystem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
            AudioManager.AUDIOFOCUS_GAIN -> player.start()
        }
    }

    private fun actionStartup(extras: Bundle) {
        val text = extras.getString(AUDIO_TEXT_KEY)
        val title = extras.getString(TITLE) ?: ""
        val linkedPages = extras.getStringArrayList(LINKED_PAGES) ?: arrayListOf()
        val chapterIdx = extras.getInt(CHAPTER_INDEX, 0)

        Logs.debug(LOG_TAG,"actionStartup: title='$title' chapterIdx=$chapterIdx textLength=${text?.length ?: -1} linkedPages=${linkedPages.size}")
        Logs.debug(LOG_TAG, "actionStartup: instance=$instance, player=${instance?.player}")

        if (text == null) {
            Logs.warning(LOG_TAG, "actionStartup: AUDIO_TEXT_KEY is null, aborting startup")
            return
        }

        val prefs = DataCenter(this).aiTtsPreferences

        Logs.debug(LOG_TAG, "actionStartup: abi=${modelManager.primaryAbi} nativelySupported=${modelManager.isNativelySupported} voiceId='${prefs.voiceId}'")
        if (!modelManager.isNativelySupported && modelManager.availableVoices().isEmpty()) {
            Logs.warning(LOG_TAG, "actionStartup: AI TTS not supported on abi=${modelManager.primaryAbi}, aborting")
            stopSelf()
            return
        }

        // If the stored voice is not compatible with the current ABI, fall back to the
        // ABI-appropriate default so the right model gets downloaded.
        val availableIds = modelManager.availableVoices().map { it.id }.toSet()
        if (prefs.voiceId !in availableIds) {
            val fallback = modelManager.defaultVoiceId()
            Logs.debug(LOG_TAG, "actionStartup: voiceId='${prefs.voiceId}' not available on ${modelManager.primaryAbi}, switching to '$fallback'")
            prefs.voiceId = fallback
        }

        if (!modelManager.isModelDownloaded(prefs.voiceId)) {
            Logs.debug(LOG_TAG,"actionStartup: model not downloaded, enqueueing download and saving pending extras")
            pendingStartExtras = extras
            AiTtsModelDownloadWorker.enqueue(this, prefs.voiceId)
            return
        }

        Logs.debug(LOG_TAG,"actionStartup: model ready, starting playback")
        hookSystem()
        player.setData(text, title, linkedPages, chapterIdx)
        player.start()
    }

    // AiTtsEventListener callbacks
    override fun onSentenceChanged(sentenceIndex: Int, sentence: String) {}

    override fun onPlaybackStateChanged(state: AiTtsPlaybackState) {}

    override fun onChapterChanged(chapterIndex: Int) {
        this.chapterIndex = chapterIndex
    }

    override fun onError(message: String) {
        stopSelf()
    }

    private fun buildPendingIntents(): HashMap<String, PendingIntent> {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        fun intent(action: String) = PendingIntent.getService(
            this, 0, Intent(this, AiTtsService::class.java).setAction(action), flags
        )
        // Keys must match AiTtsNotificationBuilder companion constants so the builder
        // can look them up by its own key names.
        return hashMapOf(
            AiTtsNotificationBuilder.ACTION_PLAY to intent(ACTION_PLAY_PAUSE),
            AiTtsNotificationBuilder.ACTION_PAUSE to intent(ACTION_PLAY_PAUSE),
            AiTtsNotificationBuilder.ACTION_STOP to intent(ACTION_STOP),
            AiTtsNotificationBuilder.ACTION_NEXT to intent(ACTION_NEXT),
            AiTtsNotificationBuilder.ACTION_PREVIOUS to intent(ACTION_PREVIOUS),
            // No controls activity defined yet — map to stop as a safe fallback
            AiTtsNotificationBuilder.ACTION_OPEN_CONTROLS to intent(ACTION_STOP)
        )
    }
}
