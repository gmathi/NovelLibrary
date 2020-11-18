package io.github.gmathi.novellibrary.service.tts

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import co.metalab.asyncawait.async
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.github.gmathi.novellibrary.cleaner.HtmlHelper
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.extensions.albumArt
import io.github.gmathi.novellibrary.extensions.displaySubtitle
import io.github.gmathi.novellibrary.extensions.displayTitle
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPageSettings
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.service.tts.TTSNotificationBuilder.Companion.TTS_NOTIFICATION_ID
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.getGlideUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "TTSService"

        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val SOURCE_ID = "sourceId"

        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        const val ACTION_NEXT = "actionNext"
        const val ACTION_PREVIOUS = "actionPrevious"
    }

    private lateinit var tts: TextToSpeech
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var ttsNotificationBuilder: TTSNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var dbHelper: DBHelper

    private var novel: Novel? = null
    private var sourceId: Long = 0L
    private var audioText: String? = null
    private var title: String? = null
    private var chapterIndex: Int = 0

    private var lines: ArrayList<String> = ArrayList()
    private var lineNumber: Int = 0
    private var isForegroundService: Boolean = false
    private val metadataCompat = MediaMetadataCompat.Builder()


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

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

                    }
                }

            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
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
        title = intent?.extras?.getString(TITLE, null) ?: ""
        sourceId = intent?.extras?.getLong(SOURCE_ID, 0L) ?: 0L

        metadataCompat.displayTitle = novel?.name ?: "Novel Name Not Found"
        metadataCompat.displaySubtitle = title

        if (!novel?.imageUrl.isNullOrBlank() && Utils.isConnectedToNetwork(this)) {
            Glide.with(this).asBitmap().load(novel!!.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    metadataCompat.albumArt = resource
                    mediaSession.setMetadata(metadataCompat.build())
                    startReading()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
        } else {
            mediaSession.setMetadata(metadataCompat.build())
            startReading()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun chooseMediaControllerActions(action: String) {
        Log.i(TAG, "TTS Action: $action")
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
                    Toast.makeText(this, "No More Chapters. You are up-to-date!", Toast.LENGTH_LONG).show()
                } else {
                    chapterIndex++
                    setAudioText()
                }
            }
            ACTION_PREVIOUS -> {
                if (chapterIndex == 0) {
                    Toast.makeText(this, "First Chapter! Cannot go back!", Toast.LENGTH_LONG).show()
                } else {
                    chapterIndex--
                    setAudioText()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "English language is not available.", Toast.LENGTH_SHORT).show()
            } else {
                startReading()
            }
        } else {
            Toast.makeText(this, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startReading() {
        //Stop reading
        sendToTTS("", TextToSpeech.QUEUE_FLUSH)

        //Reset old data
        lines.clear()
        lineNumber = 0
        var extractedLines: ArrayList<String> = ArrayList()
        audioText?.split("\n")?.filter { it.isNotEmpty() }?.mapTo(extractedLines) { it }

        val characterLimit = 50
        extractedLines.forEach { extractedLine ->
            //If line length is less than `characterLimit`
            if (extractedLine.length < characterLimit) {
                lines.add(extractedLine)
                return@forEach
            }

            //Break a single big line in to multiple lines
            var lineToBreak = extractedLine.trim()
            while (lineToBreak.isNotEmpty()) {
                var index = lineToBreak.indexOf(".")
                while (index != -1 && index < characterLimit) {
                    index = lineToBreak.indexOf(".", startIndex = index+1)
                }

                if (index == -1) {
                    lines.add(lineToBreak)
                    return@forEach
                } else {
                    lines.add(lineToBreak.substring(0, index+1))
                    lineToBreak = lineToBreak.substring(index+1, lineToBreak.length).trim()
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
        speakNexLine()
    }

    private fun speakNexLine() {
        if (lines.isNotEmpty() && lineNumber < lines.size) {
            sendToTTS(lines[lineNumber], TextToSpeech.QUEUE_ADD, "KEEP_READING")
        } else {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, queueMode, params, utteranceId)
        } else {
            tts.speak(text, queueMode, null)
        }
    }

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
            Log.e(TAG, "State Changed: $state")
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
        val webPage = if (novel.currentWebPageUrl != null)
            dbHelper.getWebPage(novel.currentWebPageUrl!!)
        else
            dbHelper.getWebPage(novel.id, sourceId, 0)
        if (webPage?.url == null) return
        chapterIndex = dbHelper.getAllWebPages(novel.id, sourceId).indexOfFirst { it.url == webPage.url }
    }

    private fun setAudioText() {
        val webPage = dbHelper.getWebPage(novel!!.id, sourceId, chapterIndex) ?: return
        title = webPage.chapter
        metadataCompat.displaySubtitle = title
        mediaSession.setMetadata(metadataCompat.build())

        val webPageSettings = dbHelper.getWebPageSettings(webPage.url) ?: return
        if (webPageSettings.filePath != null) {
            audioText = loadFromFile(webPageSettings) ?: return
            startReading()
        } else {
            loadFromWeb(webPageSettings)
        }
    }

    private fun loadFromFile(webPageSettings: WebPageSettings): String? {
        val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
        val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
        if (!input.exists()) {
            loadFromWeb(webPageSettings)
            return null
        }

        val url = webPageSettings.redirectedUrl ?: internalFilePath
        val doc = Jsoup.parse(input, "UTF-8", url) ?: return ""
        return cleanDocumentText(doc)
    }

    private fun loadFromWeb(webPageSettings: WebPageSettings) {
        async.cancelAll()
        async {
            try {
                var doc = await { NovelApi.getDocument(webPageSettings.url) }
                if (doc.location().contains("rssbook") && doc.location().contains(HostNames.QIDIAN)) {
                    doc = await { NovelApi.getDocument(doc.location().replace("rssbook", "book")) }
                }
                audioText = cleanDocumentText(doc)
                title = doc.title()
                metadataCompat.displaySubtitle = title
                mediaSession.setMetadata(metadataCompat.build())
                startReading()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@TTSService, "Unable to read chapter", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun cleanDocumentText(doc: Document): String {
        val htmlHelper = HtmlHelper.getInstance(doc)
        htmlHelper.removeJS(doc)
        htmlHelper.additionalProcessing(doc)
        return doc.getFormattedText()
    }

}
