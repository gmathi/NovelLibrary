package io.github.gmathi.novellibrary.service.tts

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.activity.TextToSpeechControlsActivity
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
import io.github.gmathi.novellibrary.util.lang.albumArt
import io.github.gmathi.novellibrary.util.lang.displaySubtitle
import io.github.gmathi.novellibrary.util.lang.displayTitle
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
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


class TTSService : Service(), TextToSpeech.OnInitListener, DataAccessor {

    companion object {
        const val TAG = "TTSService"

        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"
        const val CHAPTER_INDEX = "chapterIndex"
        const val LINKED_PAGES = "linkedPages"

        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        const val ACTION_NEXT = "actionNext"
        const val ACTION_PREVIOUS = "actionPrevious"
        const val ACTION_OPEN_CONTROLS = "actionOpenControls"
        const val ACTION_NEXT_SENTENCE = "actionNextSentence"
        const val ACTION_PREV_SENTENCE = "actionPrevSentence"
        const val ACTION_UPDATE_TTS_SETTINGS = "actionUpdateTTSSettings"
    }

    override lateinit var firebaseAnalytics: FirebaseAnalytics
    override val sourceManager: SourceManager by injectLazy()
    override fun getContext(): Context = this
    override fun getLifecycle(): Lifecycle? = null

    override val dataCenter: DataCenter by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient

    private lateinit var tts: TextToSpeech
    private lateinit var mediaSession: MediaSessionCompat
    lateinit var mediaController: MediaControllerCompat
    private lateinit var ttsNotificationBuilder: TTSNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    override lateinit var dbHelper: DBHelper

    private var blockingJob: Job? = null
    var novel: Novel? = null
    var translatorSourceName: String? = null
    private var audioText: String? = null
    private var title: String? = null
    var chapterIndex: Int = 0

    var linkedPages: List<String> = emptyList()

    var lines: ArrayList<String> = ArrayList()
    var lineNumber: Int = 0
    private var isForegroundService: Boolean = false
    private val metadataCompat = MediaMetadataCompat.Builder()

    inner class TTSBinder : Binder() {

        fun getInstance(): TTSService = this@TTSService

    }
    private val binder = TTSBinder()
    var eventListener: TTSEventListener? = null

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onCreate() {
        super.onCreate()

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        //android.os.Debug.waitForDebugger()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, Intent(), 0)

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "NovelTTSService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }
        val pendingIntents = HashMap<String, PendingIntent>()
        pendingIntents[ACTION_PLAY] = createPendingIntent(ACTION_PLAY)
        pendingIntents[ACTION_PAUSE] = createPendingIntent(ACTION_PAUSE)
        pendingIntents[ACTION_STOP] = createPendingIntent(ACTION_STOP)
        pendingIntents[ACTION_NEXT] = createPendingIntent(ACTION_NEXT)
        pendingIntents[ACTION_PREVIOUS] = createPendingIntent(ACTION_PREVIOUS)
        pendingIntents[ACTION_OPEN_CONTROLS] = createPendingIntent(ACTION_OPEN_CONTROLS)

        ttsNotificationBuilder = TTSNotificationBuilder(this, pendingIntents)
        notificationManager = NotificationManagerCompat.from(this)

        dbHelper = DBHelper.getInstance(this)

        tts = TextToSpeech(this, this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                utteranceId?.let {
                    if (it == "KEEP_READING") {
                        lineNumber++
                        speakNexLine()
                    } else if (utteranceId == "STOP_READING") {
                        //Do Nothing
                    }
                }

            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
    }

    fun goToLine(line:Int) {
        val oldLine = lineNumber
        lineNumber = line.coerceIn(0, lines.count() - 1)
        speakNexLine(TextToSpeech.QUEUE_FLUSH)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "OnStartCommand")
        val action = intent?.action

        //If called from Notification Actions like play, pause, e.t.c.
        if (action != null) {
            chooseMediaControllerActions(action)
            return super.onStartCommand(intent, flags, startId)
        }

        //If called from activity
        val novelId = intent?.extras?.getLong(NOVEL_ID, -1L) ?: -1L
        novel = dbHelper.getNovel(novelId)
        novel?.let { setChapterIndex(it) }

        audioText = intent?.extras?.getString(AUDIO_TEXT_KEY, null) ?: ""
        linkedPages = intent?.extras?.getStringArrayList(LINKED_PAGES) ?: ArrayList()
        title = intent?.extras?.getString(TITLE, null) ?: ""
        translatorSourceName = intent?.extras?.getString(TRANSLATOR_SOURCE_NAME)
        chapterIndex = intent?.extras?.getInt(CHAPTER_INDEX, 0) ?: 0

        metadataCompat.displayTitle = novel?.name ?: "Novel Name Not Found"
        metadataCompat.displaySubtitle = title

        if (!novel?.imageUrl.isNullOrBlank()) {
            Glide.with(this).asBitmap().load(novel!!.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    metadataCompat.albumArt = resource
                    start()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    start()
                }

                fun start() {
                    mediaSession.setMetadata(metadataCompat.build())
                    startReading()
                }
            })
        } else {
            mediaSession.setMetadata(metadataCompat.build())
            startReading()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    fun chooseMediaControllerActions(action: String) {
//        Log.i(TAG, "TTS Action: $action")
        when (action) {
            ACTION_PLAY -> {
                speakNexLine()
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 1L, 1F).build())
            }
            ACTION_PAUSE -> {
                sendToTTS("", TextToSpeech.QUEUE_FLUSH, "STOP_READING")
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PAUSED, 1L, 1F).build())
            }
            ACTION_STOP -> {
                sendToTTS("", TextToSpeech.QUEUE_FLUSH, "STOP_COMPLETELY")
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 1L, 1F).build())
            }
            ACTION_NEXT -> {
                if (chapterIndex == (novel!!.chaptersCount - 1).toInt()) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "No More Chapters. You are up-to-date!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    chapterIndex++
                    if (!setAudioText()) {
                        chapterIndex-- // Could not load it due to offline or other reasons
                        eventListener?.onChapterLoadStop()
                    }

                }
            }
            ACTION_PREVIOUS -> {
                if (chapterIndex == 0) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, "First Chapter! Cannot go back!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    chapterIndex--
                    if (!setAudioText()) {
                        chapterIndex++ // Could not load it due to offline or other reasons
                        eventListener?.onChapterLoadStop()
                    }
                }
            }
            ACTION_PREV_SENTENCE -> {
                if (lineNumber > 0) lineNumber--
                speakNexLine(TextToSpeech.QUEUE_FLUSH)
            }
            ACTION_NEXT_SENTENCE -> {
                if (lineNumber < lines.count() - 1) lineNumber++
                speakNexLine(TextToSpeech.QUEUE_FLUSH)
            }
            ACTION_OPEN_CONTROLS -> {
                startActivity(Intent(this, TextToSpeechControlsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            ACTION_UPDATE_TTS_SETTINGS -> {
                tts.setPitch(dataCenter.ttsPitch)
                tts.setSpeechRate(dataCenter.ttsSpeechRate)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "English language is not available.", Toast.LENGTH_SHORT).show()
                }
            } else {
                startReading()
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startReading() {
        //Stop reading
        sendToTTS("", TextToSpeech.QUEUE_FLUSH)

        // Apply the pitch and speech rate options.
        tts.setPitch(dataCenter.ttsPitch)
        tts.setSpeechRate(dataCenter.ttsSpeechRate)
        if (novel != null && dataCenter.ttsMoveBookmark) dbHelper.getWebPage(novel!!.id, translatorSourceName, chapterIndex)?.let {
            updateNovelBookmark(novel!!, it, false)
        }

        //Reset old data
        lines.clear()
        lineNumber = 0
        val extractedLines: ArrayList<String> = ArrayList()
        audioText?.split("\n")?.filter { it.isNotBlank() }?.mapTo(extractedLines) { it.trim() }

        val characterLimit = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(500)
        extractedLines.forEach { extractedLine ->
            // If line length is less than `characterLimit`
            if (extractedLine.length < characterLimit) {
                lines.add(extractedLine)
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

//        Testing Only - Auto Next Chapter
//        val tempArray = audioText?.split("\n") ?: return
//        for (x in 0..10) {
//            lines.add(tempArray[x])
//        }

        //Set new data & start reading
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 1L, 1F).build())
        eventListener?.onReadingStart()
        speakNexLine()
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

    private fun speakNexLine(queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (lines.isNotEmpty() && lineNumber < lines.size) {
            sendToTTS(lines[lineNumber], queueMode, "KEEP_READING")
            eventListener?.onSentenceChange(lineNumber)
        } else {
            if (novel != null && dataCenter.ttsMarkChaptersRead) {
                dbHelper.getWebPage(novel!!.id, translatorSourceName, chapterIndex)?.let {
                    markChapterRead(it, true)
                }
            }
            if (dataCenter.readAloudNextChapter) {
                chooseMediaControllerActions(ACTION_NEXT)
            } else
                mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 1L, 1F).build())
        }
    }

    @Suppress("DEPRECATION")
    private fun sendToTTS(text: String, queueMode: Int, utteranceId: String = "DO_NOTHING") {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) // Define if you need it
        tts.speak(text, queueMode, params, utteranceId)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(action: String): PendingIntent {
        val actionIntent = Intent(this, TTSService::class.java)
        actionIntent.action = action
        return PendingIntent.getService(this, 0, actionIntent, 0)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //mediaController.playbackState?.let { updateNotification(it) }
            Log.e(TAG, "MetaData Changed: $metadata")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
            eventListener?.onPlaybackStateChange()
//            Log.e(TAG, "State Changed: $state")
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaController.metadata == null) {
                return
            }

            // Skip building a notification when state is "none".
            val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
                ttsNotificationBuilder.buildNotification(mediaSession.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {

                    if (!isForegroundService) {
                        //startService(Intent(applicationContext, this@TTSService.javaClass))
                        startForeground(TTS_NOTIFICATION_ID, notification)
                        isForegroundService = true
                    } else if (notification != null) {
                        notificationManager.notify(TTS_NOTIFICATION_ID, notification)
                    }
                }
                else -> {
                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false
                        tts.stop()
                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                            tts.shutdown()
                        }

                        if (notification != null) {
                            notificationManager.notify(TTS_NOTIFICATION_ID, notification)
                        } else {
                            stopForeground(true)
                        }
                    }
                }
            }
        }
    }

    //Helper functions
    private fun setChapterIndex(novel: Novel) {
        val webPage = if (novel.currentChapterUrl != null)
            dbHelper.getWebPage(novel.currentChapterUrl!!)
        else
            dbHelper.getWebPage(novel.id, translatorSourceName, 0)
        if (webPage?.url == null) return
        chapterIndex = dbHelper.getAllWebPages(novel.id, translatorSourceName).indexOfFirst { it.url == webPage.url }
    }

    private fun setAudioText(): Boolean {
        eventListener?.onChapterLoadStart()
        val webPage = dbHelper.getWebPage(novel!!.id, translatorSourceName, chapterIndex) ?: return false
        title = webPage.chapterName
        metadataCompat.displaySubtitle = title
        mediaSession.setMetadata(metadataCompat.build())

        val webPageSettings = dbHelper.getWebPageSettings(webPage.url) ?: return false
        if (webPageSettings.filePath != null) {
            val clean = loadFromFile(webPageSettings) ?: return false
            audioText = clean.text
            linkedPages = clean.bufferLinks
            eventListener?.onChapterLoadStop()
            startReading()
        } else if (!networkHelper.isConnectedToNetwork()) {
            // Can't load chapter if no network
            return false
        } else {
            loadFromWeb(webPageSettings)
        }
        return true
    }

    private fun loadFromFile(webPageSettings: WebPageSettings): TTSCleanDocument? {
        val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
        val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
        if (!input.exists()) {
            loadFromWeb(webPageSettings)
            return null
        }

        val url = webPageSettings.redirectedUrl ?: internalFilePath
        val doc = Jsoup.parse(input, "UTF-8", url) ?: return TTSCleanDocument("", emptyList())
        if (dataCenter.ttsMergeBufferChapters && webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
            var text: String = cleanDocumentText(doc).text

            val links: ArrayList<String> =
                Gson().fromJson(webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES_SETTINGS], object : TypeToken<java.util.ArrayList<String>>() {}.type)

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
        blockingJob?.cancel()
        blockingJob = GlobalScope.launch {
            try {
                var doc = withContext(Dispatchers.IO) { getWebPageDocument(webPageSettings.url) }
                if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
                    doc = withContext(Dispatchers.IO) { getWebPageDocument(doc.location().replace("rssbook", "book")) }
                }
                val clean = cleanDocumentText(doc)
                audioText = clean.text
                linkedPages = clean.bufferLinks
                if (dataCenter.ttsMergeBufferChapters) {
                    linkedPages.forEach { linkedUrl ->
                        val pageDoc = withContext(Dispatchers.IO) { getWebPageDocument(linkedUrl) }
                        val cleanPage = cleanDocumentText(pageDoc)
                        audioText += "\r\n\r\n" + cleanPage.text
                    }
                }
                title = doc.title()
                metadataCompat.displaySubtitle = title
                mediaSession.setMetadata(metadataCompat.build())
                eventListener?.onChapterLoadStop()
                startReading()
            } catch (e: Exception) {
                Logs.error("TTSService", "Unable to read chapter", e)
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@TTSService, "Unable to read chapter", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getWebPageDocument(url: String): Document {
        return client.newCall(GET(url)).safeExecute().asJsoup()
    }

    private fun cleanDocumentText(doc: Document): TTSCleanDocument {
        val htmlHelper = HtmlCleaner.getInstance(doc)
        htmlHelper.removeJS(doc)
        htmlHelper.additionalProcessing(doc)
        return TTSCleanDocument(doc.getFormattedText(), htmlHelper.getLinkedChapters(doc))
    }

}
