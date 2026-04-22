package io.github.gmathi.novellibrary.service.ai_tts

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.github.gmathi.novellibrary.activity.AiTtsControlsActivity
import io.github.gmathi.novellibrary.cleaner.HtmlCleaner
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.notification.Notifications
import org.jsoup.Jsoup
import java.io.File
import io.github.gmathi.novellibrary.worker.AiTtsModelDownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

private const val LOG_TAG = "AiTtsService"

class AiTtsService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener, AiTtsEventListener {

    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()

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
        const val ACTION_OPEN_CONTROLS = "io.github.gmathi.novellibrary.AI_TTS_OPEN_CONTROLS"
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
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private val metadataBuilder = MediaMetadataCompat.Builder()
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null  // API 26+

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val noisyReceiver = NoisyReceiver()
    private var noisyReceiverRegistered = false

    private var chapterIndex: Int = 0
    private var novelId: Long = -1L
    private var translatorSourceName: String? = null

    // BroadcastReceiver for MODEL_READY
    private val modelReadyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AiTtsModelDownloadWorker.ACTION_MODEL_DOWNLOAD_PROGRESS -> {
                    val voiceId = intent.getStringExtra(AiTtsModelDownloadWorker.KEY_VOICE_ID) ?: return
                    val progress = intent.getIntExtra(AiTtsModelDownloadWorker.KEY_PROGRESS, -1)
                    val prefs = dataCenter.aiTtsPreferences
                    if (voiceId == prefs.voiceId && pendingStartExtras != null) {
                        Logs.debug(LOG_TAG, "modelReadyReceiver: download progress=$progress for voiceId=$voiceId")
                        player.setDownloadingModel(progress)
                    }
                }
                AiTtsModelDownloadWorker.ACTION_MODEL_READY -> {
                    val voiceId = intent.getStringExtra(AiTtsModelDownloadWorker.KEY_VOICE_ID) ?: return
                    Logs.debug(LOG_TAG, "modelReadyReceiver: model ready for voiceId=$voiceId")
                    // Re-trigger startup if this is the voice we were waiting for
                    pendingStartExtras?.let { extras ->
                        val prefs = dataCenter.aiTtsPreferences
                        if (voiceId == prefs.voiceId) {
                            Logs.info(LOG_TAG, "modelReadyReceiver: re-triggering startup for voiceId=$voiceId")
                            actionStartup(extras)
                            pendingStartExtras = null
                        }
                    }
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
                    stopForeground(STOP_FOREGROUND_DETACH)
                    notificationBuilder.notify(notification)
                }
                is AiTtsPlaybackState.Stopped, is AiTtsPlaybackState.Idle -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
                else -> {
                    // DownloadingModel, LoadingModel, Error — still need to call startForeground()
                    // because startForegroundService() was used and Android requires it within 5 s.
                    startForeground(Notifications.ID_AI_TTS_PLAYBACK, notification)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val pendingIntentFlags: Int =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        mediaButtonIntent.setClass(this, MediaButtonReceiver::class.java)
        val mbrIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, pendingIntentFlags)
        val mbrComponent = ComponentName(this, MediaButtonReceiver::class.java)

        modelManager = AiTtsModelManager(this)
        val prefs = dataCenter.aiTtsPreferences
        player = AiTtsPlayer(modelManager, prefs, this)
        Logs.info(LOG_TAG, "onCreate: service created, voiceId='${prefs.voiceId}' abi=${modelManager.primaryAbi}")

        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_PLAY_PAUSE
            )

        mediaSession = MediaSessionCompat(this, "AiTtsService", mbrComponent, mbrIntent).apply {
            setCallback(AiTtsSessionCallback())
            setPlaybackState(stateBuilder.build())
            setSessionActivity(
                PendingIntent.getActivity(
                    this@AiTtsService, 0,
                    Intent(this@AiTtsService, AiTtsControlsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    pendingIntentFlags
                )
            )
            setMediaButtonReceiver(
                PendingIntent.getService(
                    this@AiTtsService, 0,
                    Intent(this@AiTtsService, AiTtsService::class.java).apply { action = ACTION_PLAY_PAUSE },
                    pendingIntentFlags
                )
            )
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        notificationBuilder = AiTtsNotificationBuilder(this, buildPendingIntents())

        // Must call startForeground() synchronously in onCreate() to satisfy Android's
        // requirement that a foreground service calls it within 5 seconds of
        // startForegroundService(). The NotificationController coroutine may not emit
        // fast enough to meet that deadline, causing an ANR.
        startForeground(
            Notifications.ID_AI_TTS_PLAYBACK,
            notificationBuilder.buildNotification(mediaSession.sessionToken)
        )

        NotificationController().start()

        ContextCompat.registerReceiver(
            this,
            modelReadyReceiver,
            android.content.IntentFilter(AiTtsModelDownloadWorker.ACTION_MODEL_READY).also {
                it.addAction(AiTtsModelDownloadWorker.ACTION_MODEL_DOWNLOAD_PROGRESS)
            },
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
            ACTION_OPEN_CONTROLS -> {
                startActivity(
                    Intent(this, AiTtsControlsActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            Intent.ACTION_MEDIA_BUTTON -> MediaButtonReceiver.handleIntent(mediaSession, intent)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Logs.info(LOG_TAG, "onDestroy: tearing down service")
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
    }

    private fun unhookSystem() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Logs.debug(LOG_TAG, "onAudioFocusChange: focusChange=$focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> player.pause()
            AudioManager.AUDIOFOCUS_GAIN -> player.start()
        }
    }

    private fun actionStartup(extras: Bundle) {
        val text = extras.getString(AUDIO_TEXT_KEY)
        val title = extras.getString(TITLE) ?: ""
        val novelId = extras.getLong(NOVEL_ID, -1L)
        val linkedPages = extras.getStringArrayList(LINKED_PAGES) ?: arrayListOf()
        val chapterIdx = extras.getInt(CHAPTER_INDEX, 0)
        val translatorSource = extras.getString(TRANSLATOR_SOURCE_NAME)?.ifEmpty { null }

        this.novelId = novelId
        this.translatorSourceName = translatorSource

        Logs.debug(LOG_TAG,"actionStartup: title='$title' chapterIdx=$chapterIdx textLength=${text?.length ?: -1} linkedPages=${linkedPages.size}")
        Logs.debug(LOG_TAG, "actionStartup: instance=$instance, player=${instance?.player}")

        if (text == null) {
            Logs.warning(LOG_TAG, "actionStartup: AUDIO_TEXT_KEY is null, aborting startup")
            return
        }

        val prefs = dataCenter.aiTtsPreferences

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
            player.setDownloadingModel()
            AiTtsModelDownloadWorker.enqueue(this, prefs.voiceId)
            return
        }

        Logs.debug(LOG_TAG,"actionStartup: model ready, starting playback")
        hookSystem()
        player.setData(text, title, linkedPages, chapterIdx)
        updateMediaSessionMetadata(title, "Chapter ${chapterIdx + 1}", novelId)
        player.start()
    }

    // AiTtsEventListener callbacks
    override fun onSentenceChanged(sentenceIndex: Int, sentence: String) {
        Logs.debug(LOG_TAG, "onSentenceChanged: index=$sentenceIndex sentence='${sentence.take(50)}'")
    }

    override fun onPlaybackStateChanged(state: AiTtsPlaybackState) {
        Logs.debug(LOG_TAG, "onPlaybackStateChanged: state=$state")
        val pbState = when (state) {
            is AiTtsPlaybackState.Playing -> PlaybackStateCompat.STATE_PLAYING
            is AiTtsPlaybackState.Paused -> PlaybackStateCompat.STATE_PAUSED
            is AiTtsPlaybackState.Stopped -> PlaybackStateCompat.STATE_STOPPED
            is AiTtsPlaybackState.LoadingModel -> PlaybackStateCompat.STATE_BUFFERING
            is AiTtsPlaybackState.DownloadingModel -> PlaybackStateCompat.STATE_BUFFERING
            is AiTtsPlaybackState.Error -> PlaybackStateCompat.STATE_ERROR
            is AiTtsPlaybackState.Idle -> PlaybackStateCompat.STATE_NONE
        }
        mediaSession.setPlaybackState(
            stateBuilder.setState(pbState, player.currentSentenceIndex.value * 1000L, 0f).build()
        )
    }

    override fun onChapterChanged(chapterIndex: Int) {
        Logs.debug(LOG_TAG, "onChapterChanged: chapterIndex=$chapterIndex")
        this.chapterIndex = chapterIndex
        scope.launch(Dispatchers.IO) {
            loadChapter(chapterIndex)
        }
    }

    override fun onError(message: String) {
        Logs.error(LOG_TAG, "onError: $message")
        stopSelf()
    }

    /**
     * Loads chapter content from the database, parses the HTML, extracts text,
     * and feeds it to the player for playback.
     */
    private fun loadChapter(chapterIndex: Int) {
        if (novelId == -1L) {
            Logs.warning(LOG_TAG, "loadChapter: novelId not set, cannot load chapter")
            return
        }

        val webPage = dbHelper.getWebPage(novelId, translatorSourceName, chapterIndex)
        if (webPage == null) {
            Logs.warning(LOG_TAG, "loadChapter: no web page for novelId=$novelId chapterIndex=$chapterIndex")
            scope.launch(Dispatchers.Main) { player.stop() }
            return
        }

        val webPageSettings = dbHelper.getWebPageSettings(webPage.url)
        if (webPageSettings == null) {
            Logs.warning(LOG_TAG, "loadChapter: no settings for url=${webPage.url}")
            scope.launch(Dispatchers.Main) { player.stop() }
            return
        }

        try {
            val doc = loadDocument(webPageSettings)
            if (doc == null) {
                Logs.warning(LOG_TAG, "loadChapter: could not load document for chapter $chapterIndex")
                scope.launch(Dispatchers.Main) { player.stop() }
                return
            }

            val htmlHelper = HtmlCleaner.getInstance(doc)
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            val text = doc.getFormattedText()
            val title = doc.title()
            val linkedPageUrls = ArrayList(htmlHelper.getLinkedChapters(doc).map { it.href })

            Logs.debug(LOG_TAG, "loadChapter: loaded chapter $chapterIndex title='$title' textLength=${text.length}")

            scope.launch(Dispatchers.Main) {
                player.setData(text, title, linkedPageUrls, chapterIndex)
                updateMediaSessionMetadata(title, "Chapter ${chapterIndex + 1}", novelId)
                player.start()
            }
        } catch (e: Exception) {
            Logs.error(LOG_TAG, "loadChapter: failed to load chapter: ${e.message}", e)
            scope.launch(Dispatchers.Main) { player.stop() }
        }
    }

    /**
     * Loads the HTML document for a chapter — from offline file first, falling back to web.
     */
    private fun loadDocument(webPageSettings: WebPageSettings): org.jsoup.nodes.Document? {
        val filePath = webPageSettings.filePath
        if (filePath != null) {
            val input = File(filePath)
            if (input.exists()) {
                val url = webPageSettings.redirectedUrl ?: "$FILE_PROTOCOL$filePath"
                return Jsoup.parse(input, "UTF-8", url)
            }
        }
        // Fall back to loading from web
        val url = webPageSettings.redirectedUrl ?: webPageSettings.url
        Logs.debug(LOG_TAG, "loadDocument: loading from web: $url")
        return WebPageDocumentFetcher.document(url)
    }

    /** Update media session metadata so the notification shows title and chapter info. */
    private fun updateMediaSessionMetadata(title: String, chapterTitle: String, novelId: Long) {
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, chapterTitle)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, chapterTitle)
        mediaSession.setMetadata(metadataBuilder.build())

        // Load novel cover art asynchronously, like the regular TTS player does
        val novel = dbHelper.getNovel(novelId)
        if (novel != null && !novel.imageUrl.isNullOrBlank()) {
            Glide.with(this).asBitmap().load(novel.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Logs.debug(LOG_TAG, "updateMediaSessionMetadata: novel art loaded")
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                    metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, resource)
                    mediaSession.setMetadata(metadataBuilder.build())
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Logs.debug(LOG_TAG, "updateMediaSessionMetadata: failed to load novel art")
                }
            })
        }
    }

    private fun buildPendingIntents(): HashMap<String, PendingIntent> {
        val pendingIntentFlags: Int =
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        fun createServiceIntent(action: String): PendingIntent {
            val actionIntent = Intent(this, AiTtsService::class.java)
            actionIntent.action = action
            return PendingIntent.getService(this, 0, actionIntent, pendingIntentFlags)
        }

        // Use MediaButtonReceiver intents for transport controls so hardware/bluetooth
        // media buttons route through the media session callback properly.
        val mbrComponent = MediaButtonReceiver.getMediaButtonReceiverComponent(this)
        fun mediaButtonReceiverIntent(@PlaybackStateCompat.MediaKeyAction action: Long): PendingIntent {
            val keyCode = PlaybackStateCompat.toKeyCode(action)
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
            intent.component = mbrComponent
            intent.putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            return PendingIntent.getBroadcast(
                this, keyCode, intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        return hashMapOf(
            AiTtsNotificationBuilder.ACTION_PLAY to mediaButtonReceiverIntent(PlaybackStateCompat.ACTION_PLAY),
            AiTtsNotificationBuilder.ACTION_PAUSE to mediaButtonReceiverIntent(PlaybackStateCompat.ACTION_PAUSE),
            AiTtsNotificationBuilder.ACTION_STOP to mediaButtonReceiverIntent(PlaybackStateCompat.ACTION_STOP),
            AiTtsNotificationBuilder.ACTION_NEXT to mediaButtonReceiverIntent(PlaybackStateCompat.ACTION_SKIP_TO_NEXT),
            AiTtsNotificationBuilder.ACTION_PREVIOUS to mediaButtonReceiverIntent(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS),
            AiTtsNotificationBuilder.ACTION_OPEN_CONTROLS to createServiceIntent(ACTION_OPEN_CONTROLS)
        )
    }
}
