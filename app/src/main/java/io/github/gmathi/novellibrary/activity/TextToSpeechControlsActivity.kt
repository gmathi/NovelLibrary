package io.github.gmathi.novellibrary.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.databinding.ActivityTextToSpeechControlsBinding
import io.github.gmathi.novellibrary.databinding.ContentTextToSpeechControlsBinding
import io.github.gmathi.novellibrary.databinding.ListitemSentenceBinding
import io.github.gmathi.novellibrary.extensions.isPlaying
import io.github.gmathi.novellibrary.service.tts.TTSEventListener
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startTTSSettingsActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation

class TextToSpeechControlsActivity : BaseActivity(), TTSEventListener, GenericAdapter.Listener<String> {
    companion object {
        const val TAG = "TextToSpeechControlsActivity"
    }

    lateinit var binding: ActivityTextToSpeechControlsBinding
    lateinit var contentBinding: ContentTextToSpeechControlsBinding
    var tts:TTSService? = null
    var isServiceConnected = false

    lateinit var adapter: GenericAdapter<String>
    var lastSentence:Int = -1

    private val ttsConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as TTSService.TTSBinder
            tts = binder.getInstance()
            tts?.eventListener = this@TextToSpeechControlsActivity
            isServiceConnected = true
            contentBinding.ttsInactiveOverlay.visibility = View.GONE
            setRecycleView()
            setPlaybackState()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isServiceConnected = false
            tts?.eventListener = null
            contentBinding.ttsInactiveOverlay.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextToSpeechControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        contentBinding = binding.contentTextToSpeechControls

        // Enable marquee
        contentBinding.ttsNovelChapter.isSelected = true
        contentBinding.ttsNovelName.isSelected = true

        contentBinding.prevChapterButton.setOnClickListener {
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_PREVIOUS)
        }
        contentBinding.nextChapterButton.setOnClickListener {
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_NEXT)
        }
        contentBinding.prevSentenceButton.setOnClickListener {
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_PREV_SENTENCE)
        }
        contentBinding.nextSentenceButton.setOnClickListener {
            if (isServiceConnected) tts?.chooseMediaControllerActions(TTSService.ACTION_NEXT_SENTENCE)
        }
        contentBinding.playButton.setOnClickListener {
            if (isServiceConnected) tts?.chooseMediaControllerActions(
                if (tts?.mediaController?.playbackState?.isPlaying == true) TTSService.ACTION_PAUSE
                else TTSService.ACTION_PLAY
            )
        }
        contentBinding.scrollIntoViewButton.setOnClickListener {
            scrollToPosition(lastSentence, true)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = novel.name
    }

    private fun setRecycleView() {
        if (tts == null || !isServiceConnected) return
        val tts = this.tts!!
        adapter = GenericAdapter(tts.lines, layoutResId = R.layout.listitem_sentence, listener = this)
        lastSentence = tts.lineNumber
        contentBinding.sentencesView.setDefaultsNoAnimation(adapter)

        tts.mediaController.metadata?.let { metadata ->
            contentBinding.ttsNovelName.applyFont(assets).text = metadata.description.title
            contentBinding.ttsNovelChapter.applyFont(assets).text = metadata.description.subtitle
            if (tts.novel != null) {
                Glide.with(this)
                    .load(tts.novel?.imageUrl?.getGlideUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(contentBinding.ttsNovelCover)
            }
        }
        setProgress()
        scrollToPosition(lastSentence, false)
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

    private fun setProgress() {
        if (tts == null || !isServiceConnected) return
        val tts = this.tts!!
        contentBinding.chapterProgress.progress = tts.lineNumber
        contentBinding.chapterProgress.max = tts.lines.count()
    }

    private fun setPlaybackState() {
        if (tts?.mediaController?.playbackState?.isPlaying == true) {
            contentBinding.playButton.let {
                it.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_pause_white_vector))
                it.contentDescription = getString(R.string.pause)
            }
        } else {
            contentBinding.playButton.let {
                it.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_play_arrow_white_vector))
                it.contentDescription = getString(R.string.play)
            }
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
            tts?.goToLine(position)
            setPlaybackState()
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, TTSService::class.java).also { intent ->
            bindService(intent, ttsConnection, 0)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(ttsConnection)
        tts?.eventListener = null
        isServiceConnected = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_text_to_speech, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> finish()
            R.id.action_open_reader -> {
                finishAfterTransition()
                if (isServiceConnected) tts?.let { tts ->
                    tts.novel?.let { novel ->
                        dbHelper.getWebPage(novel.id, tts.translatorSourceName, tts.chapterIndex)?.let { chapter ->
                            updateNovelBookmark(novel, chapter)
                            startReaderDBPagerActivity(novel, tts.translatorSourceName)
                        }
                    }
                }
            }
            R.id.action_open_settings -> {
                startTTSSettingsActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onReadingStart() {
        runOnUiThread {
            setRecycleView()
        }
    }

    override fun onReadingStop() {
    }

    override fun onSentenceChange(sentenceIndex: Int) {
        runOnUiThread {
            val layout = (contentBinding.sentencesView.layoutManager as LinearLayoutManager)
            val visibleStart = layout.findFirstCompletelyVisibleItemPosition()
            val visibleEnd = layout.findLastCompletelyVisibleItemPosition()
            val shouldAutoScroll = !layout.isSmoothScrolling && lastSentence in visibleStart..visibleEnd

            adapter.notifyItemChanged(lastSentence)
            lastSentence = sentenceIndex
            adapter.notifyItemChanged(sentenceIndex)
            // If previous sentence was within boundaries and we left visible boundaries - scroll to new sentence.
            // If either of them are not satisfied - that means user scrolled away from the current sentence
            // and we don't want to be annoying by forcefully scrolling him back
            if (shouldAutoScroll && sentenceIndex !in visibleStart..visibleEnd) contentBinding.sentencesView.smoothScrollToPosition(sentenceIndex)

            contentBinding.sentencesView.invalidate()
            setProgress()
        }
    }

    override fun onPlaybackStateChange() {
        runOnUiThread {
            setPlaybackState()
        }
    }

    override fun onChapterLoadStart() {
        runOnUiThread {
            contentBinding.playbackControls.children.forEach {
                it.isEnabled = false
            }
        }
    }

    override fun onChapterLoadStop() {
        runOnUiThread {
            contentBinding.playbackControls.children.forEach {
                it.isEnabled = true
            }
        }
    }

}