package io.github.gmathi.novellibrary.service.tts

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.salomonbrys.kotson.fromJson
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ReaderDBPagerActivity
import io.github.gmathi.novellibrary.activity.TextToSpeechControlsActivity
import io.github.gmathi.novellibrary.activity.settings.TTSSettingsActivity
import io.github.gmathi.novellibrary.cleaner.HtmlCleaner
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.TTSCleanDocument
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.tts.TTSNotificationBuilder.Companion.TTS_NOTIFICATION_ID
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.lang.*
import io.github.gmathi.novellibrary.util.network.asJsoup
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.system.markChapterRead
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TTSService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener, DataAccessor {

    companion object {
        const val TAG = "TTSServiceMain"

        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"
        const val CHAPTER_INDEX = "chapterIndex"
        const val LINKED_PAGES = "linkedPages"

        const val ACTION_OPEN_CONTROLS = "open_controls"
        const val ACTION_OPEN_SETTINGS = "open_settings"
        const val ACTION_OPEN_READER = "open_reader"
        const val ACTION_UPDATE_SETTINGS = "update_settings"
        const val ACTION_STOP_ALT = "stop_alt"

        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY_PAUSE = "actionPlayPause"
        const val ACTION_PLAY = "actionPlay"
        const val ACTION_NEXT = "actionNext"
        const val ACTION_PREVIOUS = "actionPrevious"

        const val COMMAND_CONTINUE = "KEEP_READING"

        const val ACTION_STARTUP = "startup"

        const val COMMAND_REQUEST_SENTENCES = "request_sentences"
        const val COMMAND_UPDATE_LANGUAGE = "update_language"
        const val EVENT_SENTENCE_LIST = "sentence_list"
        const val KEY_SENTENCES = "sentences"
    }

    override lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun getContext(): Context = this
    override fun getLifecycle(): Lifecycle? = null

    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: TTSSessionCallback
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var metadataBuilder = MediaMetadataCompat.Builder()
//    private val noisyReceiver = NoisyReceiver() // TODO
    private lateinit var notificationBuilder: TTSNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notification: NotificationController
    private var albumArt: Bitmap? = null

    private val focusLock = Any()
    private var resumeOnFocus = true

    lateinit var player: TTSPlayer

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "Service start!")
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        player = TTSPlayer(this)

//        val am = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        am.registerMediaButtonEventReceiver(ComponentName(this, TTSMediaKeyHandler::class.java))

        fun createPendingIntent(action: String):PendingIntent {
            val actionIntent = Intent(this, TTSService::class.java)
            actionIntent.action = action
            return PendingIntent.getService(this, 0, actionIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT }
                else { PendingIntent.FLAG_UPDATE_CURRENT }
            )
        }

        mediaSession = MediaSessionCompat(baseContext, TAG).apply {
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
                .addCustomAction(ACTION_STOP_ALT, "Stop (notification discard)", R.drawable.ic_stop_white_vector)
                .addCustomAction(ACTION_OPEN_SETTINGS, "Open Settings", R.drawable.ic_settings_white_vector)
                .addCustomAction(ACTION_OPEN_READER, "Open Reader", R.drawable.ic_chrome_reader_mode_white_vector)
                .addCustomAction(ACTION_OPEN_CONTROLS, "Open Controls", R.drawable.ic_queue_music_white_vector)
//                .addCustomAction(ACTION_UPDATE_SETTINGS, "Update Settings", R.drawable.ic_refresh_white_vector)
//                .addCustomAction(COMMAND_REQUEST_SENTENCES, "Request sentences", R.drawable.ic_menu_white_vector)
            setPlaybackState(stateBuilder.build())

            mediaCallback = TTSSessionCallback()
            setCallback(mediaCallback)
            setSessionActivity(PendingIntent.getActivity(this@TTSService, 0, Intent(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT }
                else { PendingIntent.FLAG_UPDATE_CURRENT }
            ))// createPendingIntent(ACTION_OPEN_CONTROLS))
            setMediaButtonReceiver(PendingIntent.getService(this@TTSService, 0,
                Intent(this@TTSService, TTSService::class.java).apply { action = ACTION_PLAY_PAUSE },
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT }
                else { PendingIntent.FLAG_UPDATE_CURRENT }
            ))
            isActive = true
            setSessionToken(sessionToken)
        }

        val pendingIntents = HashMap<String, PendingIntent>()
        pendingIntents[ACTION_PLAY] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        pendingIntents[ACTION_PAUSE] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
        pendingIntents[ACTION_STOP] = createPendingIntent(ACTION_STOP_ALT) //MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        pendingIntents[ACTION_NEXT] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
        pendingIntents[ACTION_PREVIOUS] = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
        pendingIntents[ACTION_OPEN_CONTROLS] = createPendingIntent(ACTION_OPEN_CONTROLS)

        notificationBuilder = TTSNotificationBuilder(this, pendingIntents)
        notificationManager = NotificationManagerCompat.from(this)
        notification = NotificationController()
    }

    override fun onDestroy() {
        Log.d(TAG,"Service destroyed!")
        if (mediaSession.isActive) {
            mediaCallback.onStop()
        }
//        player.destroy()
//        Log.d(TAG, "Player destroyed!")
        mediaSession.release()
//        Log.d(TAG, "Media session released!")
        super.onDestroy()
//        stopForeground(true)
//        Log.d(TAG, "super.onDestroy()")
    }


    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot {
        return BrowserRoot("NONE", null)
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(mutableListOf())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        Log.d(TAG, "Start command! ${intent?.action} $flags $startId")

        MediaButtonReceiver.handleIntent(mediaSession, intent)

        when (intent?.action) {
            ACTION_STARTUP -> actionStartup(intent)
            ACTION_OPEN_CONTROLS -> {
                startActivity(Intent(this, TextToSpeechControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_OPEN_READER -> {
                if (player.novel != null) {
                    val novel = player.novel!!
                    dbHelper.getWebPage(novel.id, player.translatorSourceName, player.chapterIndex)?.let { chapter ->
                        updateNovelBookmark(novel, chapter, false)
                    }

                    val bundle = Bundle()
                    bundle.putParcelable("novel", novel)
                    if (player.translatorSourceName != null)
                        bundle.putString("translatorSourceName", player.translatorSourceName)
                    startActivity(Intent(this, ReaderDBPagerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), bundle)
                }
            }
            ACTION_OPEN_SETTINGS -> {
                startActivity(Intent(this, TTSSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_UPDATE_SETTINGS -> {
                player.updateSettings()
            }
            ACTION_STOP_ALT -> {
                // To avoid TTS service from crashing entire app
                // Instead of just letting notification to call onStop, we have to jump hoops
                // Notification discard -> onStartCommand (here) -> player.stop with state set to NONE ->
                // Notification update callback sees NONE -> calls actual onStop
                // Also that notification update have to control foreground state, because ???
                // If I don't do it like that - app crashes, and that issue was around since 2018 based on
                // android issue tracker, and was declared as "intended behavior".
                player.stop(PlaybackStateCompat.STATE_NONE)
            }
            null, "android.intent.action.MEDIA_BUTTON" -> {} // No action required, handled by MediaButtonReceiver
            else -> Log.w(TAG, "Unknown TTS action! ${intent.action}")
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun actionStartup(intent: Intent) {
        Log.d(TAG,"Booting up!")
        val extras = intent.extras!!

        val novelId = extras.getLong(NOVEL_ID, -1L)
        val novel = dbHelper.getNovel(novelId)
        val translatorSourceName = extras.getString(TRANSLATOR_SOURCE_NAME, null)
        val chapterIndex =
            if (extras.containsKey(CHAPTER_INDEX)) extras.getInt(CHAPTER_INDEX, 0)
            else novel?.let { n ->
                val webPage = if (n.currentChapterUrl != null)
                    dbHelper.getWebPage(n.currentChapterUrl!!)
                else
                    dbHelper.getWebPage(n.id, translatorSourceName, 0)

                if (webPage?.url == null) 0
                else dbHelper.getAllWebPages(n.id, translatorSourceName).indexOfFirst { it.url == webPage.url }
            } ?: 0
        val rawText = extras.getString(AUDIO_TEXT_KEY, "")!!
        val linkedPages = extras.getStringArrayList(LINKED_PAGES) ?: ArrayList()
        val title = extras.getString(TITLE, null) ?: ""

        metadataBuilder.displayTitle = novel?.name ?: "Novel Name Not Found"
        metadataBuilder.displaySubtitle = title
        metadataBuilder.trackCount =
            if (novel != null) dbHelper.getAllWebPages(novel.id, translatorSourceName).count().toLong()
            else 0L

        metadataBuilder.putLong(NOVEL_ID, novel?.id ?: 0L)
        metadataBuilder.putString(TRANSLATOR_SOURCE_NAME, translatorSourceName ?: "")
        metadataBuilder.putLong(CHAPTER_INDEX, chapterIndex.toLong())

        // Load up the poster image if possible
        if (!novel?.imageUrl.isNullOrBlank()) {
            Glide.with(this).asBitmap().load(novel!!.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    albumArt = resource
                    metadataBuilder.albumArt = resource
                    start()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    start()
                }

                fun start() {
                    player.setData(novel, translatorSourceName, chapterIndex, rawText, linkedPages)
                    mediaCallback.onPlay()
                }
            })
        } else {
            player.setData(novel, translatorSourceName, chapterIndex, rawText, linkedPages)
            mediaCallback.onPlay()
        }

        // Queue
        if (novel != null) {
            mediaSession.setQueueTitle(novel.name)
            val queue = dbHelper.getAllWebPages(novel.id, translatorSourceName).map { chapter ->
                MediaSessionCompat.QueueItem(MediaDescriptionCompat.Builder().run {
                    setMediaId(chapter.orderId.toString())
                    setTitle(chapter.chapterName)
                    build()
                }, chapter.orderId)
            }
            // String mediaId, CharSequence title, CharSequence subtitle,
            //            CharSequence description, Bitmap icon, Uri iconUri, Bundle extras, Uri mediaUri
            mediaSession.setQueue(queue)
        }
    }

    private inner class TTSSessionCallback : MediaSessionCompat.Callback() {

        private lateinit var focusRequest: AudioFocusRequest

        override fun onPlay() {
            Log.d(TAG, "onPlay")

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
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                return

            startService(Intent(applicationContext, TTSService::class.java))

            mediaSession.isActive = true

            player.start()

//            registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

//            startForeground(TTS_NOTIFICATION_ID, notificationBuilder.buildNotification(mediaSession.sessionToken))
        }

        override fun onStop() {

            Log.d(TAG, "onStop")

            val am = baseContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(this@TTSService)
            }
            stopSelf()

            mediaSession.isActive = false

            resumeOnFocus = false
//            player.stop(PlaybackStateCompat.STATE_NONE)
            player.destroy()

//            Log.d(TAG,"Service destroyed!")
//            player.destroy()
//            Log.d(TAG, "Player destroyed!")
//            mediaSession.release()
//            Log.d(TAG, "Media session released!")
            stopForeground(true)
//            stopForeground(false)
        }

        override fun onPause() {
            Log.d(TAG, "onPause")

            resumeOnFocus = false
//            stopForeground(false)

//            unregisterReceiver(noisyReceiver)
            player.stop()
        }

        override fun onSeekTo(pos: Long) {
            player.goto((pos / 1000L).toInt())
        }

        override fun onRewind() {
            player.goto(player.lineNumber - 1)
        }

        override fun onFastForward() {
            player.goto(player.lineNumber + 1)
        }

//        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
//            super.onPlayFromMediaId(mediaId, extras)
//        }

        override fun onSkipToNext() {
            player.nextChapter()
        }

        override fun onSkipToPrevious() {
            player.previousChapter()
        }

        override fun onCommand(command: String?, extras: Bundle?, cb: ResultReceiver?) {
            Log.d(TAG, "Command: $command")
            when (command) {
                COMMAND_REQUEST_SENTENCES -> player.sendSentences()
                ACTION_UPDATE_SETTINGS -> player.updateSettings()
                COMMAND_UPDATE_LANGUAGE -> player.updateLanguage()
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
                synchronized(focusLock) { resumeOnFocus = true }
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

    // Main class that handles playback and processing
    inner class TTSPlayer(val context: TTSService) : TextToSpeech.OnInitListener {

        private val tts:TextToSpeech = TextToSpeech(context.getContext(), this)
        private var ttsReady = false
        var lineNumber: Int = 0
        var lines:MutableList<String> = mutableListOf()

        var novel: Novel? = null
        var translatorSourceName: String? = null
        var chapterIndex = 0
        private var rawText = ""
        private lateinit var linkedPages: List<String>

        private var playAfterLoad = false

        private var webLoadingJob: Job? = null

        //#region Init android TTS
        init {
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == COMMAND_CONTINUE) {
                        lineNumber++
                        speakNextLine()
                    }
                }

                override fun onError(utteranceId: String?) {
                }

                override fun onStart(utteranceId: String?) {
                }
            })
        }

        override fun onInit(status: Int) {
            if (status == TextToSpeech.SUCCESS) {
                if (updateLanguage()) {
                    ttsReady = true
                    if (playAfterLoad) this.start()
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context.getContext(), "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        //#endregion

        // Initializer on startup
        fun setData(novel:Novel?, source:String?, chapterIndex:Int, text:String, pages:List<String>) {
            this.novel = novel
            this.translatorSourceName = source
            this.chapterIndex = chapterIndex
            this.rawText = text
            this.linkedPages = pages
            processRawText()
            if (ttsReady)
                updateLanguage()
//            start()
            metadataBuilder.trackNumber = chapterIndex.toLong()
            metadataBuilder.duration = lines.count().toLong() * 1000L // Convert duration to 1s = 1 line
            mediaSession.setMetadata(metadataBuilder.build())
        }

        // Chapter update
        private fun setData(text: String, pages: List<String>) {
            rawText = text
            linkedPages = pages
            processRawText()
            start()
            metadataBuilder.duration = lines.count().toLong() * 1000L
            metadataBuilder.trackNumber = chapterIndex.toLong()
            mediaSession.setMetadata(metadataBuilder.build())
        }

        fun start() {
            if (!ttsReady) {
                playAfterLoad = true
                return
            }
            playAfterLoad = false
            updateSettings()
            speakNextLine()
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
            mediaSession.setMetadata(metadataBuilder.build())
        }

        fun goto(line:Int) {
            lineNumber = line.coerceIn(0, lines.count() - 1)
            speakNextLine(TextToSpeech.QUEUE_FLUSH)
        }

        fun stop(withState: Int = PlaybackStateCompat.STATE_PAUSED) {
            tts.stop()
            webLoadingJob?.cancel()
            setPlaybackState(withState)
        }

        fun destroy() {
            tts.stop()
            webLoadingJob?.cancel()
            tts.shutdown()
            setPlaybackState(PlaybackStateCompat.STATE_NONE)
        }

        fun previousChapter() {
            if (novel == null) return
            player.stop()
            loadChapter(chapterIndex - 1)
        }

        fun nextChapter() {
            if (novel == null) return
            player.stop()
            loadChapter(chapterIndex + 1)
        }

        fun updateSettings() {
            tts.setSpeechRate(dataCenter.ttsSpeechRate)
            tts.setPitch(dataCenter.ttsPitch)
        }

        fun updateLanguage(): Boolean {
            val locale = dataCenter.ttsLanguage ?: Locale.getDefault()
            val result = tts.setLanguage(locale)
            return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context.getContext(), "Language ${locale.displayName} is not available.", Toast.LENGTH_SHORT).show()
                }
                false
            } else true
        }

        private fun processRawText() {
            lines.clear()
            lineNumber = 0
            val extractedLines: ArrayList<String> = ArrayList()
            rawText.split("\n").filter { it.isNotBlank() }.mapTo(extractedLines) { it.trim() }

            // Limit the character count to manageable and reasonable values.
            // Even if TTS engine says it can process more at once, it doesn't mean we want it to,
            // since it will produce audible pauses. Processing in smaller chunks is better.
            val characterLimit = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(500)
            extractedLines.forEach { extractedLine ->
                if (extractedLine.length < characterLimit) {
                    lines.add(extractedLine.trim())
                    return@forEach
                }

                // Permit line splits only when sentence ending character is followed by a space or a line end.
                // Also include common sentence ending characters for dialogue or TN notes.
                val sentenceEndRegex = """[.!?;"'」』”»“‘)\]](?>[\s\r]|$)""".toRegex()

                //Break a single big line in to multiple lines
                var lineToBreak = extractedLine.trim()
                while (lineToBreak.isNotEmpty()) {
                    var index = findSplitIndex(lineToBreak, characterLimit, sentenceEndRegex)
                    // If we can't split the sentence within boundaries by using common sentence finishers
                    // first try to split on a comma, and fall back to space if none found.
                    if (index == -1) index = findSplitIndex(lineToBreak, characterLimit, ',')
                    if (index == -1) index = findSplitIndex(lineToBreak, characterLimit, ' ')
                    if (index == -1) {
                        // Last ditch: Couldn't find any way to split, just cut it.
                        while (lineToBreak.length > characterLimit) {
                            lines.add(lineToBreak.substring(0, characterLimit))
                            lineToBreak = lineToBreak.substring(characterLimit)
                        }
                        lines.add(lineToBreak)
                        return@forEach
                    } else {
                        lines.add(lineToBreak.substring(0, index + 1))
                        lineToBreak = lineToBreak.substring(index + 1).trim()
                        if (lineToBreak.length < characterLimit) {
                            lines.add(lineToBreak)
                            return@forEach
                        }
                    }
                }
            }
            sendSentences()
        }

        private fun getSentences(bundle: Bundle) {
            bundle.putStringArrayList(KEY_SENTENCES, lines.toCollection(ArrayList<String>()))
        }

        fun sendSentences() {
            mediaSession.sendSessionEvent(EVENT_SENTENCE_LIST, Bundle().run {
                getSentences(this)
                this
            })
        }

        private fun findSplitIndex(text:String, maxLength:Int, regex: Regex):Int {
            var index = -1
            while (index < maxLength) {
                val next = regex.find(text, index+1)?.range?.start
                if (next != null && next < maxLength) index = next
                else break
            }
            return index
        }
        private fun findSplitIndex(text:String, maxLength:Int, char: Char):Int {
            var index = -1
            while (index < maxLength) {
                val next = text.indexOf(char, index+1)
                if (next != -1 && next < maxLength) index = next
                else break
            }
            return index
        }

        private fun currPosition():Long {
            return lineNumber.toLong() * 1000L
        }

        private fun setPlaybackState(state: Int) {
            // Playback speed is set to 0 because our duration is faked one, and we update position manually
            // instead of relying on MediaSession doing it for us.
            mediaSession.setPlaybackState(stateBuilder.setState(state, currPosition(), 0.0f).build())
        }

        fun speakNextLine(queueMode:Int = TextToSpeech.QUEUE_ADD) {
            if (lines.isNotEmpty() && lineNumber < lines.size) {
                sendToTTS(lines[lineNumber], queueMode)
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING)
    //            eventListener?.onSentenceChange(lineNumber)
            } else {
                if (novel != null && context.dataCenter.ttsMarkChaptersRead) {
                    context.dbHelper.getWebPage(novel!!.id, translatorSourceName, chapterIndex)?.let {
                        context.markChapterRead(it, true)
                    }
                }
                if (dataCenter.readAloudNextChapter) {
                    nextChapter()
                } else {
                    stop(PlaybackStateCompat.STATE_NONE)
                }
            }
        }

        private fun loadChapter(index: Int): Boolean {
            val webPage = dbHelper.getWebPage(novel!!.id, translatorSourceName, index) ?: return false
            val webPageSettings = dbHelper.getWebPageSettings(webPage.url) ?: return false

            // TODO: Allow pre-loading
            if (webPageSettings.filePath != null) {
                val clean = loadFromFile(webPageSettings)
                if (clean == null) {
                    if (networkHelper.isConnectedToNetwork()) loadFromWeb(webPageSettings)
                    else return false
                } else {
                    setData(clean.text, clean.bufferLinks)
                    metadataBuilder.displaySubtitle = webPage.chapterName
                }
            } else if (!networkHelper.isConnectedToNetwork()) {
                // Can't load chapter if no network
                return false
            } else {
                loadFromWeb(webPageSettings)
            }
            if (novel != null && dataCenter.ttsMoveBookmark) dbHelper.getWebPage(novel!!.id, translatorSourceName, chapterIndex)?.let {
                updateNovelBookmark(novel!!, it, false)
            }
            metadataBuilder.trackNumber = index.toLong()
            metadataBuilder.putLong(CHAPTER_INDEX, index.toLong())
            chapterIndex = index
            mediaSession.setMetadata(metadataBuilder.build())
            return true
        }

        private fun loadFromFile(webPageSettings: WebPageSettings) : TTSCleanDocument? {
            Log.d(TAG, "Loading from file ${webPageSettings.title} ${webPageSettings.filePath}")
            val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
            val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
            if (!input.exists()) return null

            val url = webPageSettings.redirectedUrl ?: internalFilePath
            // Old behavior: Failure to parse would cause it to read empty text chapter.
            val doc = Jsoup.parse(input, "UTF-8", url) ?: return null
            if (dataCenter.ttsMergeBufferChapters && webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                var text: String = cleanDocumentText(doc).text

                val links: ArrayList<String> =
                    Gson().fromJson(webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES_SETTINGS]?:"[]")

                links.forEach { linkedUrl ->
                    val settings = dbHelper.getWebPageSettings(linkedUrl) ?: return@forEach
                    if (settings.filePath != null) {
                        text += "\r\n\r\n" + loadFromFile(settings)?.text
                    }
//                else {
//                    // TODO: Load linked page from web when source page is offline
//                }
                }

                return TTSCleanDocument(text, emptyList())
            } else {
                return cleanDocumentText(doc)
            }
        }

        private fun loadFromWeb(webPageSettings: WebPageSettings) {
            setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
            Log.d(TAG, "Loading from web ${webPageSettings.title} ${webPageSettings.url}")
            webLoadingJob?.cancel()
            webLoadingJob = launchIO {
                try {
                    val doc = withIOContext { getWebPageDocument(webPageSettings.url) }
                    val clean = cleanDocumentText(doc)
                    var text: String = clean.text
                    linkedPages = clean.bufferLinks
                    if (dataCenter.ttsMergeBufferChapters) {
                        linkedPages.forEach { linkedUrl ->
                            val pageDoc = withIOContext { getWebPageDocument(linkedUrl) }
                            val cleanPage = cleanDocumentText(pageDoc)
                            text += "\r\n\r\n" + cleanPage.text
                        }
                    }
                    setData(text, linkedPages)
                    metadataBuilder.displaySubtitle = doc.title()
                    mediaSession.setMetadata(metadataBuilder.build())
                } catch (e: Exception) {
                    Logs.error(TAG, "Unable to read chapter", e)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this@TTSService, "Unable to read chapter", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        private suspend fun getWebPageDocument(url: String): Document {
            val doc = client.newCall(GET(url)).safeExecute().asJsoup()
            // Clearly a crutch that should not be there?
            if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
                return withIOContext { getWebPageDocument(doc.location().replace("rssbook", "book")) }
            }
            return doc
        }

        private fun cleanDocumentText(doc: Document): TTSCleanDocument {
            val htmlHelper = HtmlCleaner.getInstance(doc)
            htmlHelper.removeJS(doc)
            htmlHelper.additionalProcessing(doc)
            return TTSCleanDocument(doc.getFormattedText(), htmlHelper.getLinkedChapters(doc))
        }

        private fun sendToTTS(text: String, queueMode: Int) {
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, COMMAND_CONTINUE) // Define if you need it
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)

            tts.speak(text, queueMode, params, COMMAND_CONTINUE)
        }

    }

    private inner class NotificationController : MediaControllerCompat.Callback() {

        private val controller = MediaControllerCompat(this@TTSService, mediaSession.sessionToken)

        init {
            controller.registerCallback(this)
        }

        private var isForegroundService=false

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //mediaController.playbackState?.let { updateNotification(it) }
//            Log.e(TAG, "MetaData Changed: $metadata")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
//            eventListener?.onPlaybackStateChange()
//            Log.e(TAG, "State Changed: $state")
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaSession.controller.metadata == null) {
                return
            }

            @SuppressLint("SwitchIntDef")
            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    val notification = notificationBuilder.buildNotification(mediaSession.sessionToken)
                    if (!isForegroundService) {
                        startService(Intent(applicationContext, this@TTSService.javaClass))
                        startForeground(TTS_NOTIFICATION_ID, notification)
                        isForegroundService = true
                    } else
                        notificationManager.notify(TTS_NOTIFICATION_ID, notification)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false
                    }
                    notificationManager.notify(TTS_NOTIFICATION_ID, notificationBuilder.buildNotification(mediaSession.sessionToken))
                }
                PlaybackStateCompat.STATE_NONE -> {
                    // It has to be here. Don't ask why. It's some Android shitty design causing app to crash
                    // on service destruction for no reason with error that has nothing to do with what we actually do.
                    isForegroundService = false
                    mediaCallback.onStop()
                    controller.unregisterCallback(this)
                }
            }
        }
    }

}