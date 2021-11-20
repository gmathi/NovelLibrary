package io.github.gmathi.novellibrary.activity

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.databinding.ActivityTextToSpeechControlsBinding
import io.github.gmathi.novellibrary.databinding.ContentTextToSpeechControlsBinding
import io.github.gmathi.novellibrary.databinding.ListitemSentenceBinding
import io.github.gmathi.novellibrary.extensions.isPlaying
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.lang.duration
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.lang.trackNumber
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startTTSService
import io.github.gmathi.novellibrary.util.system.startTTSSettingsActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation

class TextToSpeechControlsActivity : BaseActivity(), GenericAdapter.Listener<String> {
    companion object {
        const val TAG = "TTSControlsActivity"
    }

    lateinit var binding: ActivityTextToSpeechControlsBinding
    lateinit var contentBinding: ContentTextToSpeechControlsBinding
    var isServiceConnected = false

    lateinit var adapter: GenericAdapter<String>
    var lastSentence:Int = -1
    var hasSentences = false

    var novel: Novel? = null
    var translatorSource: String? = null
    var chapterIndex: Int = 0

    private lateinit var browser: MediaBrowserCompat
    private val connCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            isServiceConnected = true
            browser.sessionToken.also { token ->
                controller = MediaControllerCompat(this@TextToSpeechControlsActivity, token)
                MediaControllerCompat.setMediaController(this@TextToSpeechControlsActivity, controller)
            }

            contentBinding.ttsInactiveOverlay.visibility = View.GONE
            initController()
        }

        override fun onConnectionSuspended() {
            isServiceConnected = false
            controller?.unregisterCallback(ctrlCallback)
            controller = null
            contentBinding.ttsInactiveOverlay.visibility = View.VISIBLE
        }
    }
    var controller: MediaControllerCompat? = null
    private val ctrlCallback = TTSController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTextToSpeechControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentBinding = binding.contentTextToSpeechControls

        browser = MediaBrowserCompat(this, ComponentName(this, TTSService::class.java), connCallback, null)

        // Enable marquee
        contentBinding.ttsNovelChapter.isSelected = true
        contentBinding.ttsNovelName.isSelected = true

        contentBinding.prevChapterButton.setOnClickListener {
            controller?.transportControls?.skipToPrevious()
        }
        contentBinding.nextChapterButton.setOnClickListener {
            controller?.transportControls?.skipToNext()
        }
        contentBinding.prevSentenceButton.setOnClickListener {
            controller?.transportControls?.rewind()
        }
        contentBinding.nextSentenceButton.setOnClickListener {
            controller?.transportControls?.fastForward()
        }
        contentBinding.playButton.setOnClickListener {
            if (controller?.playbackState?.state == PlaybackStateCompat.STATE_NONE) {
                if (novel != null) {
                    startTTSService(novel!!.id, translatorSource, chapterIndex)
                }
            } else if (controller?.playbackState?.isPlaying == true)
                controller?.transportControls?.pause()
            else
                controller?.transportControls?.play()
        }
        contentBinding.scrollIntoViewButton.setOnClickListener {
            scrollToPosition(lastSentence, true)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = novel.name
    }

//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        when (keyCode) {
//            KeyEvent.KEYCODE_MEDIA_PLAY,
//                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
//                    if (controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
//                        controller?.transportControls?.pause()
//                    else
//                        controller?.transportControls?.play()
//                }
//        }
//        return super.onKeyDown(keyCode, event)
//    }

    fun initController() {
        controller?.registerCallback(ctrlCallback)
        ctrlCallback.onMetadataChanged(controller?.metadata)
        ctrlCallback.onPlaybackStateChanged(controller?.playbackState)
        controller?.sendCommand(TTSService.COMMAND_REQUEST_SENTENCES, null, null)
    }

    private inner class TTSController : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            if (metadata == null) return
            val novelId = metadata.getLong(TTSService.NOVEL_ID)
            if (novel?.id != novelId) {
                novel = dbHelper.getNovel(novelId)
                translatorSource = metadata.getString(TTSService.TRANSLATOR_SOURCE_NAME)
                novel?.let { novel ->
                    Glide.with(this@TextToSpeechControlsActivity)
                        .load(novel.imageUrl?.getGlideUrl())
                        .apply(RequestOptions.circleCropTransform())
                        .into(contentBinding.ttsNovelCover)
                }
            }
            chapterIndex = metadata.trackNumber.toInt() - 1

            contentBinding.chapterProgress.max = (metadata.duration / 1000L).toInt()

            contentBinding.ttsNovelName.applyFont(assets).text = metadata.description.title
            contentBinding.ttsNovelChapter.applyFont(assets).text = metadata.description.subtitle
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            if (state == null) return
            val oldPosition = lastSentence
            lastSentence = (state.position / 1000L).toInt()
            contentBinding.chapterProgress.progress = lastSentence
            if (oldPosition != lastSentence && hasSentences) {
                updatePosition(oldPosition)
            }

            var drawable = R.drawable.ic_play_arrow_white_vector
            var label = getString(R.string.play)

            when (state.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    drawable = R.drawable.ic_pause_white_vector
                    label = getString(R.string.pause)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                }
                PlaybackStateCompat.STATE_NONE -> {
                    drawable = R.drawable.ic_stop_white_vector
                }
                PlaybackStateCompat.STATE_BUFFERING -> {
                    drawable = R.drawable.ic_file_download_white_vector
                    label = getString(R.string.loading)
                }
                else -> {}
            }

            contentBinding.playButton.let {
                it.setImageDrawable(ContextCompat.getDrawable(this@TextToSpeechControlsActivity, drawable))
                it.contentDescription = label
            }

            // Disable controls while buffering
            if (contentBinding.playButton.isEnabled != (state.state != PlaybackStateCompat.STATE_BUFFERING)) {
                contentBinding.playbackControls.children.forEach {
                    it.isEnabled = !it.isEnabled
                }
            }

        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
            if (event == TTSService.EVENT_SENTENCE_LIST && extras != null) {
                setRecycleView(extras)
            }
        }

    }

    private fun setRecycleView(extras: Bundle) {
        val lines = extras.getStringArrayList(TTSService.KEY_SENTENCES) ?: ArrayList()
        adapter = GenericAdapter(lines, layoutResId = R.layout.listitem_sentence, listener = this)
        contentBinding.sentencesView.setDefaultsNoAnimation(adapter)
        scrollToPosition(lastSentence, false)
        hasSentences = true
    }

    private fun updatePosition(oldPosition: Int) {
        val layout = (contentBinding.sentencesView.layoutManager as LinearLayoutManager)
        val visibleStart = layout.findFirstCompletelyVisibleItemPosition()
        val visibleEnd = layout.findLastCompletelyVisibleItemPosition()
        val shouldAutoScroll = !layout.isSmoothScrolling && (oldPosition in visibleStart..visibleEnd || oldPosition == -1)

        if (oldPosition != -1) adapter.notifyItemChanged(oldPosition)
        adapter.notifyItemChanged(lastSentence)
        // If previous sentence was within boundaries and we left visible boundaries - scroll to new sentence.
        // If either of them are not satisfied - that means user scrolled away from the current sentence
        // and we don't want to be annoying by forcefully scrolling him back
        if (shouldAutoScroll && lastSentence !in visibleStart..visibleEnd) contentBinding.sentencesView.smoothScrollToPosition(lastSentence)

        contentBinding.sentencesView.invalidate()
    }

    private fun scrollToPosition(position: Int, smooth: Boolean) {
        val view = contentBinding.sentencesView
        val layout = view.layoutManager as LinearLayoutManager
        if (smooth) {
            val scroller = CenterSmoothScroller(view.context)
            scroller.targetPosition = position
            layout.startSmoothScroll(scroller)
        } else {
            layout.scrollToPosition(position)
        }
    }

    class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {

        override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
            return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
        }

    }

    override fun bind(item: String, itemView: View, position: Int) {
        val binding = ListitemSentenceBinding.bind(itemView)
        binding.sentenceContents.applyFont(assets).text = item
        binding.sentenceLayout.setBackgroundColor(ContextCompat.getColor(this,
            if (lastSentence == position) R.color.colorLightBlue
            else android.R.color.transparent
        ))
    }

    override fun onItemClick(item: String, position: Int) {
        if (isServiceConnected) {
            controller?.transportControls?.seekTo(position * 1000L)
        }
    }

    override fun onStart() {
        super.onStart()
        browser.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
        //
    }

    override fun onStop() {
        super.onStop()
        controller?.unregisterCallback(ctrlCallback)
        browser.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_text_to_speech, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.action_open_reader -> {
                if (controller?.metadata != null) {
                    val meta = controller!!.metadata
                    val novelId = meta.getLong(TTSService.NOVEL_ID)
                    val novel = dbHelper.getNovel(novelId) ?: return super.onOptionsItemSelected(item)

                    var translator:String? = meta.getString(TTSService.TRANSLATOR_SOURCE_NAME)
                    if (translator == "") translator = null
                    dbHelper.getWebPage(novelId, translator, meta.getLong(TTSService.CHAPTER_INDEX).toInt())?.let { chapter ->
                        updateNovelBookmark(novel, chapter)
                        finishAfterTransition()
                        startReaderDBPagerActivity(novel, translator)
                    }
                }
            }
            R.id.action_open_settings -> {
                startTTSSettingsActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}