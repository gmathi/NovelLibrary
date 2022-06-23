package io.github.gmathi.novellibrary.service.tts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.cleaner.HtmlCleaner
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.model.other.TTSCleanDocument
import io.github.gmathi.novellibrary.model.other.TTSFilterTarget
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.FILE_PROTOCOL
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.getLinkedPagesCompat
import io.github.gmathi.novellibrary.util.lang.*
import io.github.gmathi.novellibrary.util.network.asJsoup
import io.github.gmathi.novellibrary.util.network.safeExecute
import io.github.gmathi.novellibrary.util.showToastWithMain
import io.github.gmathi.novellibrary.util.system.DataAccessor
import io.github.gmathi.novellibrary.util.system.markChapterRead
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class TTSPlayer(private val context: Context,
                private val mediaSession: MediaSessionCompat,
                private val stateBuilder: PlaybackStateCompat.Builder) : TextToSpeech.OnInitListener, DataAccessor {

    companion object {

        const val TAG = "NLTTS_Player"

        const val STATE_PLAY = 1
        const val STATE_STOP = 0
        const val STATE_LOADING = 2
        const val STATE_DISPOSE = 3

        const val TYPE_EARCON = "earcon"
        const val TYPE_SENTENCE = "sentence"
        const val TYPE_DIALOGUE = "dialogue"
        const val TYPE_DIALOGUE_SPEAKER = "dialogue_speaker"
        const val TYPE_CHAPTER_CHANGE_EARCON = "chapter_change_earcon"
        const val TYPE_SPECIAL = "special"

        const val SCENE_CHANGE_EARCON = "◇ ◇ ◇"
        const val CHAPTER_CHANGE_EARCON = "##next_chapter##"
    }

    override val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    override val dataCenter: DataCenter by injectLazy()
    override val dbHelper: DBHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()

    override fun getContext(): Context = this.context

    init {
        Log.d(TAG, "Initialized DataAccessor")
    }

    // TTS
    private val tts: TextToSpeech = TextToSpeech(context, this)
    private var ttsReady = false
    // This is required to make hardware media keys to be delivered to the app. Otherwise it just doesn't work.
    // God bless the dangled mess of android media session.
    private val silence: MediaPlayer = MediaPlayer.create(context, R.raw.silence).apply {
        isLooping = true
    }

    // Player metadata
    lateinit var novel: Novel
    var translatorSourceName: String? = null
    var chapterIndex: Int = 0
    var chapterCount: Int = 0
    var albumArt: Bitmap? = null
    var linkedPages: ArrayList<LinkedPage> = ArrayList()
    private val metadata = MediaMetadataCompat.Builder()

    var isDisposed = false

    val isPlaying: Boolean
        get() = desiredState == STATE_PLAY

    // Player state
    private val chapterCache: MutableList<TTSCleanDocument> = mutableListOf()

    private lateinit var title: String
    private lateinit var rawText: String
    var lineNumber: Int = 0
    var lines: MutableList<TTSLine> = mutableListOf()

    data class TTSLine(val line: String, val mode: TTSReadMode = TTSReadMode.ModeRegular, val speaker: String? = null, val sequential: Boolean = false) {
        fun getDisplayString(): String {
            return if (mode == TTSReadMode.ModeDialogue && speaker != null) {
                "$line $speaker"
            } else line
        }
    }
    enum class TTSReadMode {
        ModeRegular,
        ModeDialogue,
        ModeSceneChange,
    }

    private var desiredState: Int = STATE_STOP
    private var currentState: Int = STATE_STOP

    // Web loader
    private val client: OkHttpClient
        get() = networkHelper.cloudflareClient
    private var webLoadingJobIndex = -1
    private var webLoadingJob: Job? = null

    override fun getLifecycle(): Lifecycle? {
        TODO("Not yet implemented")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.addEarcon(SCENE_CHANGE_EARCON, BuildConfig.APPLICATION_ID, R.raw.scene_change)
            tts.addEarcon(CHAPTER_CHANGE_EARCON, BuildConfig.APPLICATION_ID, R.raw.chapter_change)
            selectLanguage()
            ttsReady = true
            Log.d(TAG, "TTS initialized")
            if (desiredState == STATE_PLAY && currentState != STATE_PLAY) {
                Log.d(TAG, "Play was called before TTS initialization: Starting now")
                start()
            }
        } else {
            context.showToastWithMain("Could not initialize TextToSpeech.", Toast.LENGTH_SHORT)
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                if (currentState == STATE_PLAY) {
                    when (utteranceId) {
                        TYPE_SENTENCE, TYPE_DIALOGUE_SPEAKER, TYPE_EARCON -> {
                            lineNumber++
                            speakLine()
                        }
                        TYPE_DIALOGUE -> {
                            if (dataCenter.ttsPreferences.downpitchDialogue) tts.setPitch(dataCenter.ttsPreferences.pitch)
                            if (lines[lineNumber].speaker == null) {
                                lineNumber++
                                speakLine()
                            } else {
                                tts.speak(lines[lineNumber].speaker, TextToSpeech.QUEUE_ADD, ttsBundle(TYPE_DIALOGUE_SPEAKER), TYPE_DIALOGUE_SPEAKER)
                            }
                        }
                        TYPE_CHAPTER_CHANGE_EARCON -> speakLine()
                    }
                }
             }

            // TODO: Improve utteranceId management to prevent misinterpreting

            // TODO: Try onAudioAvailable + synthesizeToFile(/dev/null) approach?

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Error playing TTS $utteranceId")
            }

            override fun onStart(utteranceId: String?) {
            }
        })
    }

    //#region Startup

    fun setFrom(other: TTSPlayer) {
        Log.d(TAG, "Copying metadata from another player instance")
        setMetadata(other.novel, other.translatorSourceName, other.chapterIndex, other.chapterCount)
        setData(other.rawText, other.title, other.linkedPages)
    }

    fun setBundle(extras: Bundle): TTSCleanDocument? {
        Log.d(TAG, "Loading metadata from startup bundle")
        val novelId = extras.getLong(TTSService.NOVEL_ID, -1L)
        val novel = dbHelper.getNovel(novelId)
        if (novel == null) {
            Log.e(TAG, "Invalid startup data! No novel provided!")
            return null
        }
        val translatorSourceName = extras.getString(TTSService.TRANSLATOR_SOURCE_NAME, null)
        val allPages:List<WebPage> = dbHelper.getAllWebPages(novel.id, translatorSourceName)
        val chapterIndex =
                if (extras.containsKey(TTSService.CHAPTER_INDEX)) extras.getInt(TTSService.CHAPTER_INDEX, 0)
                else {
                    val webPage =
                            if (novel.currentChapterUrl != null) dbHelper.getWebPage(novel.currentChapterUrl!!)
                            else dbHelper.getWebPage(novelId, translatorSourceName, 0)
                    if (webPage?.url == null) 0
                    else allPages.indexOfFirst { it.url == webPage.url }
                }

        val title = extras.getString(TTSService.TITLE) ?: ""
        setMetadata(novel, translatorSourceName, chapterIndex, allPages.count())

        fun buildQueue() {
            // Chapter Queue
            Log.d(TAG, "Building chapter queue")
//            mediaSession.setQueueTitle(novel.name)
//            val queue = allPages.mapIndexed { index, chapter ->
//                MediaSessionCompat.QueueItem(MediaDescriptionCompat.Builder().run {
//                    setMediaId(index.toString())
//                    setTitle(chapter.chapterName)
//                    if (albumArt != null) setIconBitmap(albumArt)
//                    build()
//                }, index.toLong())
//            }
//            mediaSession.setQueue(queue)
        }
        if (!novel.imageUrl.isNullOrBlank()) {
            Glide.with(context).asBitmap().load(novel.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    Log.d(TAG, "Novel art loaded successfully")
                    albumArt = resource
                    metadata.albumArt = resource
                    mediaSession.setMetadata(metadata.build())
                    buildQueue()
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Log.d(TAG, "Failed to load novel art")
                    buildQueue()
                }
            })
        } else {
            buildQueue()
        }

        return extras.getString(TTSService.AUDIO_TEXT_KEY)?.let {
            val pages:ArrayList<LinkedPage> = extras.getParcelableArrayList(TTSService.LINKED_PAGES)?:ArrayList()
            TTSCleanDocument(it, pages, title, chapterIndex)
        }
    }

    private fun setMetadata(novel: Novel, source: String?, chapterIndex: Int, trackCount: Int) {
        this.novel = novel
        this.translatorSourceName = source
        this.chapterIndex = chapterIndex
        this.chapterCount = trackCount
        Log.d(TAG, "Loaded novel data: ${novel.name}, $source, $chapterIndex, $trackCount")

        dataCenter.internalPut {
            putLong(TTSService.STATE_NOVEL_ID, novel.id)
            putString(TTSService.STATE_TRANSLATOR_SOURCE_NAME, translatorSourceName)
            putInt(TTSService.STATE_CHAPTER_INDEX, chapterIndex)
        }

        metadata.displayTitle = novel.name
        metadata.trackCount = trackCount.toLong()
        metadata.trackNumber = (chapterIndex+1).toLong()
        metadata.id = chapterIndex.toString()
        metadata.putLong(TTSService.NOVEL_ID, novel.id)
        metadata.putString(TTSService.TRANSLATOR_SOURCE_NAME, source ?: "")
        metadata.putLong(TTSService.CHAPTER_INDEX, chapterIndex.toLong())
        chapterCache.clear() // Ensure our cache is properly discarded.
        mediaSession.setMetadata(metadata.build())  // TODO: Omit?
    }

    fun setData(text: String, title: String, linkedPages: ArrayList<LinkedPage>) {
        Log.d(TAG, "Setting raw text data for chapter '$title'")
        this.rawText = text
        this.title = title
        this.linkedPages = linkedPages
        processRawText()
        if (ttsReady) selectLanguage()
        metadata.displaySubtitle = title
        metadata.duration = lines.count().toLong() * 1000L
        metadata.trackNumber = (chapterIndex+1).toLong()
    }

    //#endregion

    //#region Controls

    fun start() {
        if (isDisposed) return
        desiredState = STATE_PLAY
        // Start silence playback even if TTS is not ready
        // Avoid cut audio because android did boot up the bluetooth sound output.
        if (!silence.isPlaying) silence.start()
        // Have to wait for TTS to initialize
        if (!ttsReady) {
            Log.d(TAG, "Attempting to start prior TTS initialization: Waiting for init")
            return
        }
        currentState = STATE_PLAY
        Log.d(TAG, "TTSPlayer.start()")
        updateVoiceConfig()
        mediaSession.setMetadata(metadata.build())
        speakLine(TextToSpeech.QUEUE_ADD)
    }

    fun goto(line:Int) {
        if (isDisposed) return
        lineNumber = line.coerceIn(0, lines.count() - 1)
        Log.d(TAG, "Going to line $line")
        if (currentState != STATE_PLAY) {
            start()
        } else
            speakLine(TextToSpeech.QUEUE_FLUSH)
    }

    fun stop(withState: Int = PlaybackStateCompat.STATE_PAUSED) {
        if (isDisposed) return
        desiredState = STATE_STOP
        if (currentState == STATE_STOP) return
        if (currentState != STATE_LOADING) currentState = STATE_STOP
        Log.d(TAG, "TTSPlayer.stop()")
        if (silence.isPlaying) silence.stop()
        tts.stop()
        setPlaybackState(withState)
    }

    fun destroy() {
        if (isDisposed) return
        Log.d(TAG, "Disposing TTSPlayer instance")
        if (silence.isPlaying) silence.stop()
        silence.release()

        tts.stop()
        tts.shutdown()

        webLoadingJobIndex = -1
        webLoadingJob?.cancel()

        desiredState = STATE_DISPOSE
        currentState = STATE_DISPOSE

        setPlaybackState(PlaybackStateCompat.STATE_NONE)

        isDisposed = true
    }

    fun previousChapter() {
        if (isDisposed) return
        Log.d(TAG, "Playing previous chapter")
        currentState = STATE_LOADING
        desiredState = STATE_PLAY
        tts.stop()
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        loadChapter(chapterIndex - 1)
    }

    fun loadCurrentChapter() {
        if (isDisposed) return
        Log.d(TAG, "Loading current chapter")
        currentState = STATE_LOADING
        desiredState = STATE_PLAY
        tts.stop()
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        loadChapter(chapterIndex)
    }

    fun loadLinkedPage(url: String) {
        Log.d(TAG, "Requested to load a linked page!")
        currentState = STATE_LOADING
        desiredState = STATE_PLAY
        tts.stop()
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        loadChapter(chapterIndex, false, url)
        // TODO: Preserve list of linked pages
    }

    fun gotoChapter(chapter: Int) {
        if (isDisposed) return
        Log.d(TAG, "Playing chapter with index $chapter")
        currentState = STATE_LOADING
        desiredState = STATE_PLAY
        tts.stop()
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        loadChapter(chapter)
    }

    fun nextChapter(withEarcon: Boolean = false) {
        if (isDisposed) return
        Log.d(TAG, "Playing next chapter (change earcon: $withEarcon)")
        currentState = STATE_LOADING
        desiredState = STATE_PLAY
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        if (withEarcon && dataCenter.ttsPreferences.chapterChangeSFX) sendChapterChangeEarconToTTS(TextToSpeech.QUEUE_FLUSH)
        else tts.stop()
        loadChapter(chapterIndex + 1)
    }

    fun clearChapterCache() {
        chapterCache.clear()
    }

    //#endregion

    //#region Playback

    fun speakLine(queueMode:Int = TextToSpeech.QUEUE_ADD) {
        if (lines.isNotEmpty() && lineNumber < lines.size) {
            sendLineToTTS(lines[lineNumber], queueMode)
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING) // Update state
        } else {
            if (dataCenter.ttsPreferences.markChaptersRead) {
                dbHelper.getWebPage(novel.id, translatorSourceName, chapterIndex)?.let {
                    markChapterRead(it, true)
                }
            }
            if (dataCenter.readAloudNextChapter) {
                nextChapter(true)
            } else {
                stop(PlaybackStateCompat.STATE_NONE)
            }
        }
    }

    private fun ttsBundle(type: String): Bundle {
        val params = Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, type)
        return params
    }

    private fun sendChapterChangeEarconToTTS(queueMode: Int) {
        tts.playEarcon(CHAPTER_CHANGE_EARCON, queueMode, ttsBundle(TYPE_CHAPTER_CHANGE_EARCON), TYPE_CHAPTER_CHANGE_EARCON)
    }

    private fun sendLineToTTS(line: TTSLine, queueMode: Int) {
        when (line.mode) {
            TTSReadMode.ModeRegular -> tts.speak(line.line, queueMode, ttsBundle(TYPE_SENTENCE), TYPE_SENTENCE)
            TTSReadMode.ModeDialogue -> {
                if (dataCenter.ttsPreferences.downpitchDialogue) tts.setPitch(dataCenter.ttsPreferences.pitch * .95f)
                tts.speak(line.line, queueMode, ttsBundle(TYPE_DIALOGUE), TYPE_DIALOGUE)
            }
            TTSReadMode.ModeSceneChange -> tts.playEarcon(SCENE_CHANGE_EARCON, queueMode, ttsBundle(TYPE_EARCON), TYPE_EARCON)
        }
    }

    private fun sendToTTS(text: String, queueMode: Int, playMode: TTSReadMode = TTSReadMode.ModeRegular) {
        val params = Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)

        if (text == SCENE_CHANGE_EARCON || text == CHAPTER_CHANGE_EARCON) {
            val earconType = if (text == CHAPTER_CHANGE_EARCON) TYPE_CHAPTER_CHANGE_EARCON else TYPE_EARCON
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, earconType)
            tts.playEarcon(SCENE_CHANGE_EARCON, queueMode, params, earconType)
        } else {
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, TYPE_EARCON)
            tts.speak(text, queueMode, params, TYPE_SENTENCE)
        }
    }

    private fun setPlaybackState(state: Int) {
        // Playback speed is set to 0 because our duration is faked one, and we update position manually
        // instead of relying on MediaSession doing it for us.
        mediaSession.setPlaybackState(stateBuilder.setState(state, lineNumber * 1000L, 0.0f).build())
    }

    //#endregion

    //#region Chapter loading

    private fun loadChapter(index: Int, forCaching: Boolean = false, url: String? = null): Boolean {
        chapterCache.find { it.chapterIndex == index }?.let {
            if (!forCaching) {
                Log.d(TAG, "Already cached chapter $index")
                updateChapterIndex(index)
                setData(it.text, it.title, it.bufferLinks)
                start()
            }
            return@loadChapter true
        }
        val webPage = dbHelper.getWebPage(novel.id, translatorSourceName, index) ?: return false
        val webPageSettings = dbHelper.getWebPageSettings(webPage.url) ?: return false

        if (webPageSettings.filePath != null) {
            val clean = loadFromFile(webPageSettings, index, url)
            if (clean == null) {
                if (networkHelper.isConnectedToNetwork()) loadFromWeb(webPageSettings, index, url)
                else return false
            } else {
                cacheChapter(clean)
                if (!forCaching) {
                    updateChapterIndex(index)
                    setData(clean.text, clean.title, clean.bufferLinks)
                    start()
                }
            }
        } else if (!networkHelper.isConnectedToNetwork()) {
            // Can't load chapter if no network
            return false
        } else {
            if (!forCaching) {
                updateChapterIndex(index)
                mediaSession.setMetadata(metadata.build())
            }
            loadFromWeb(webPageSettings, index, url)
        }
        return true
    }

    private fun updateChapterIndex(index: Int) {
        metadata.trackNumber = (index+1).toLong()
        metadata.putLong(TTSService.CHAPTER_INDEX, index.toLong())
        dataCenter.internalPut {
            putInt(TTSService.STATE_CHAPTER_INDEX, index)
        }
        chapterIndex = index
        if (dataCenter.ttsPreferences.moveBookmark) dbHelper.getWebPage(novel.id, translatorSourceName, index)?.let {
            updateNovelBookmark(novel, it, false)
        }
    }

    private fun loadFromFile(webPageSettings: WebPageSettings, index: Int, linkedUrl: String?) : TTSCleanDocument? {
        Log.d(TAG, "Loading from file ${webPageSettings.title} ${webPageSettings.filePath}")
        val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
        val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
        if (!input.exists()) return null

        if (linkedUrl != null) {
            val linkedPage = dbHelper.getWebPageSettings(linkedUrl)
            if (linkedPage != null) {
                return loadFromFile(linkedPage, index, null)
            } else {
                context.showToastWithMain("Could not find the linked page in offline storage!", Toast.LENGTH_LONG)
            }
        }

        val url = webPageSettings.redirectedUrl ?: internalFilePath
        // Old behavior: Failure to parse would cause it to read empty text chapter.
        val doc = Jsoup.parse(input, "UTF-8", url) ?: return null
        if (dataCenter.ttsPreferences.mergeBufferChapters && webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
            var text: String = cleanDocumentText(doc, index).text

            val links: ArrayList<LinkedPage> = webPageSettings.getLinkedPagesCompat()

            if (dataCenter.ttsPreferences.discardInitialBufferPage && links.size > 0) {
                text = ""
            }

            val pageTexts = links.mapNotNull { link ->
                val settings = dbHelper.getWebPageSettings(link.href) ?: return@mapNotNull null
                if (settings.filePath != null) {
                    loadFromFile(settings, index, null)?.text
                } else null
//                else {
//                    // TODO: Load linked page from web when source page is offline.
//                    // Requires rewriting offline loader to be async
//                }
            }
            if (dataCenter.ttsPreferences.useLongestPage) {
                var longest: String? = null
                var longestSize = -1
                pageTexts.forEach { s ->
                    if (s.length > longestSize) {
                        longest = s
                        longestSize = s.length
                    }
                }
                if (longest != null) text += "\r\n\r\n" + longest
            } else {
                pageTexts.forEach { s -> text += "\r\n\r\n" + s }
            }

            return TTSCleanDocument(text, ArrayList(), doc.title(), index)
        } else {
            return cleanDocumentText(doc, index)
        }
    }

    private fun loadFromWeb(webPageSettings: WebPageSettings, index: Int, linkedUrl:String?) {
        if (webLoadingJobIndex == index) return
        setPlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        val url = linkedUrl ?: webPageSettings.url
        Log.d(TAG, "Loading from web $index ${webPageSettings.title} ${webPageSettings.url}")
        webLoadingJobIndex = index
        webLoadingJob?.cancel()
        webLoadingJob = launchIO {
            try {
                val doc = withIOContext { getWebPageDocument(url) }
                val clean = cleanDocumentText(doc, index)
                var text: String = clean.text
                if (dataCenter.ttsPreferences.mergeBufferChapters && linkedUrl == null) {
                    if (dataCenter.ttsPreferences.discardInitialBufferPage && clean.bufferLinks.size > 0) {
                        text = ""
                    }
                    val pageTexts = clean.bufferLinks.map { linkedUrl ->
                        val pageDoc = withIOContext { getWebPageDocument(linkedUrl.href) }
                        val cleanPage = cleanDocumentText(pageDoc, index)
                        cleanPage.text
                    }
                    if (dataCenter.ttsPreferences.useLongestPage) {
                        var longest: String? = null
                        var longestSize = -1
                        pageTexts.forEach { s ->
                            if (s.length > longestSize) {
                                longest = s
                                longestSize = s.length
                            }
                        }
                        if (longest != null) text += "\r\n\r\n" + longest
                    } else {
                        pageTexts.forEach { s -> text += "\r\n\r\n" + s }
                    }
                }
                cacheChapter(TTSCleanDocument(text, clean.bufferLinks, clean.title, clean.chapterIndex))

                if (chapterIndex == index && desiredState == STATE_PLAY) {
                    // TODO: linkedPages handling
                    // In theory it should have an interface for user to click and load, but right now
                    // we don't have any handling of linkedPages outside of merge option
                    setData(text, doc.title(), clean.bufferLinks)
                    start()
                } // else -> loaded for caching
            } catch (e: Exception) {
                Log.e(TAG, "Unable to read chapter ${webPageSettings.url}", e)
                context.showToastWithMain("Unable to read chapter", Toast.LENGTH_LONG)
                tts.speak("Unable to read next chapter!", TextToSpeech.QUEUE_FLUSH, ttsBundle(TYPE_SPECIAL), TYPE_SPECIAL)
            }
        }
    }

    private fun cacheChapter(clean: TTSCleanDocument) {
        while (chapterCache.size > 5) {
            chapterCache.removeAt(0)
        }
        chapterCache.indexOfFirst { it.chapterIndex == clean.chapterIndex }.let { if (it != -1) chapterCache.removeAt(it) }
        chapterCache.add(clean)
    }

    private fun getWebPageDocument(url: String): Document {
        return WebPageDocumentFetcher.document(url)
    }

    private fun cleanDocumentText(doc: Document, index: Int): TTSCleanDocument {
        val htmlHelper = HtmlCleaner.getInstance(doc)
        htmlHelper.removeJS(doc)
        htmlHelper.additionalProcessing(doc)
        return TTSCleanDocument(doc.getFormattedText(), htmlHelper.getLinkedChapters(doc), doc.title(), index)
    }

    //#endregion

    //#region Processing of raw text data

    private fun processRawText() {
        lines.clear()
        lineNumber = 0
        val extractedLines: ArrayList<String> = ArrayList()
        rawText.split("\n").filter { it.isNotBlank() }.mapTo(extractedLines) { it.trim() }

        // TODO: Avoid doing JSON parsing every time
        // Also it's done twice, because Utils.getDocumentText also parses it
        val filters = dataCenter.ttsPreferences.filterList.filter { it.target == TTSFilterTarget.Line }.map { it.compile(null) }


        fun findSplitIndex(text:String, maxLength:Int, regex: Regex):Int {
            var index = -1
            while (index < maxLength) {
                val next = regex.find(text, index+1)?.range?.start
                if (next != null && next < maxLength) index = next
                else break
            }
            return index
        }

        fun findSplitIndex(text:String, maxLength:Int, char: Char):Int {
            var index = -1
            while (index < maxLength) {
                val next = text.indexOf(char, index+1)
                if (next != -1 && next < maxLength) index = next
                else break
            }
            return index
        }

        // TODO: Detect dialogue prior line-splitting
        // TODO: Handle <div><i></i></div> as "inner dialogue"
        // TODO: Improve dialogue detection
        // Detects:
        // "dialogue"
        // "dialogue" [speaker]
        val dialogueRegex = """^\s*("+.+"+|\p{Pi}+.+\p{Pf}+|\p{Ps}+.+\p{Pe}+)(?: *(\p{Ps}+.{1,30}\p{Pe}+))?\s*$""".toRegex()
        fun makeLine(text: String, sequential: Boolean) {
            val dialogue = dialogueRegex.matchEntire(text)
            if (dialogue != null) {
                lines.add(TTSLine(dialogue.groupValues[1], TTSReadMode.ModeDialogue, dialogue.groupValues[2], sequential))
            } else {
                lines.add(TTSLine(text,
                    mode = if(text == SCENE_CHANGE_EARCON) TTSReadMode.ModeSceneChange else TTSReadMode.ModeRegular,
                    sequential = sequential
                ))
            }
        }

        // Limit the character count to manageable and reasonable values.
        // Even if TTS engine says it can process more at once, it doesn't mean we want it to,
        // since it will produce audible pauses. Processing in smaller chunks is better.
        val characterLimit = TextToSpeech.getMaxSpeechInputLength().coerceAtMost(500)
        extractedLines.forEach { extractedLine ->
            var lineToBreak = extractedLine.trim()
            filters.forEach { lineToBreak = it.apply(lineToBreak) }
            lineToBreak = lineToBreak.trim()

            if (lineToBreak.length < characterLimit) {
                makeLine(lineToBreak, false)
                return@forEach
            }

            // Permit line splits only when sentence ending character is followed by a space or a line end.
            // Also include common sentence ending characters for dialogue or TN notes.
            val sentenceEndRegex = """[.!?;"'」』”»“‘)\]](?>[\s\r]|$)""".toRegex()

            //Break a single big line in to multiple lines
            var sequentialMarker = false
            while (lineToBreak.isNotEmpty()) {
                var index = findSplitIndex(lineToBreak, characterLimit, sentenceEndRegex)
                // If we can't split the sentence within boundaries by using common sentence finishers
                // first try to split on a comma, and fall back to space if none found.
                if (index == -1) index = findSplitIndex(lineToBreak, characterLimit, ',')
                if (index == -1) index = findSplitIndex(lineToBreak, characterLimit, ' ')
                if (index == -1) {
                    // Last ditch: Couldn't find any way to split, just cut it.
                    while (lineToBreak.length > characterLimit) {
                        makeLine(lineToBreak.substring(0, characterLimit), sequentialMarker)
                        sequentialMarker = true
                        lineToBreak = lineToBreak.substring(characterLimit)
                    }
                    return@forEach
                } else {
                    makeLine(lineToBreak.substring(0, index + 1), sequentialMarker)
                    sequentialMarker = true
                    lineToBreak = lineToBreak.substring(index + 1).trim()
                    if (lineToBreak.length < characterLimit) {
                        makeLine(lineToBreak, sequentialMarker)
                        return@forEach
                    }
                }
            }
        }
        sendSentences()
    }

    //#endregion

    //#region Configuration

    fun sendSentences() {
        mediaSession.sendSessionEvent(TTSService.EVENT_SENTENCE_LIST, Bundle().apply {
            putStringArrayList(TTSService.KEY_SENTENCES, lines.mapTo(ArrayList()) { it.getDisplayString() })
        })
    }

    fun sendLinkedPages() {
        mediaSession.sendSessionEvent(TTSService.EVENT_LINKED_PAGES, Bundle().apply {
            putParcelableArrayList(TTSService.LINKED_PAGES, linkedPages)
        })
    }

    fun updateVoiceConfig() {
        tts.setPitch(dataCenter.ttsPreferences.pitch)
        tts.setSpeechRate(dataCenter.ttsPreferences.speechRate)
    }

    fun selectLanguage(): Boolean {
        val locale = dataCenter.ttsPreferences.language ?: Locale.getDefault()
        val result = tts.setLanguage(locale)
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            context.showToastWithMain("Language ${locale.displayName} is not available.", Toast.LENGTH_SHORT)
            false
        } else true
    }

    //#endregion


}