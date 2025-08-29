package io.github.gmathi.novellibrary.activity

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
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
import io.github.gmathi.novellibrary.databinding.FragmentTextToSpeechQuickSettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemSentenceBinding
import io.github.gmathi.novellibrary.extensions.isPlaying
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.util.fromHumanPercentage
import io.github.gmathi.novellibrary.util.lang.duration
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.lang.trackNumber
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startTTSService
import io.github.gmathi.novellibrary.util.system.startTTSSettingsActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.toHumanPercentage
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import kotlinx.coroutines.*
import java.lang.String.format
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.round

class TextToSpeechControlsActivity : BaseActivity(), GenericAdapter.Listener<String> {
    companion object {
        const val TAG = "NLTTS_Controls"
    }

    lateinit var binding: ActivityTextToSpeechControlsBinding
    lateinit var contentBinding: ContentTextToSpeechControlsBinding
    lateinit var quickSettings: FragmentTextToSpeechQuickSettingsBinding
    var isServiceConnected = false

    lateinit var adapter: GenericAdapter<String>
    var lastSentence: Int = -1
    var hasSentences = false
    private var sentenceRangeStart: Int = -1
    private var sentenceRangeEnd: Int = -1

    var novel: Novel? = null
    var translatorSource: String? = null
    var chapterIndex: Int = 0
    private var linkedPages = ArrayList<LinkedPage>()
    private var linkedPageButtons: MutableMap<Int, String> = mutableMapOf()

    private var stopTimer: Long = 0L
    private var lastMinutes: Long = 0L
    private var timerUpdateJob: Job? = null
    private val updateTimerCallback = object : ResultReceiver(Handler(Looper.myLooper()!!)) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            quickSettings.autoStopTimerSwitch.isChecked = resultCode == 1
            timerUpdateJob?.cancel()
            if (resultCode == 1) {
                resultData?.getLong("time")?.let {
                    stopTimer = it
                    startTimerUpdates()
                }
                resultData?.getBoolean("active")?.let {
                    if (it) {
                        quickSettings.autoStopTimerSwitch.isChecked = true
                    } else {
                        quickSettings.autoStopTimerSwitch.isChecked = false
                        quickSettings.autoStopTimerStatus.setText(R.string.off)
                    }
                }
            } else {
                quickSettings.autoStopTimerStatus.setText(R.string.off)
            }
        }
    }

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
        quickSettings = FragmentTextToSpeechQuickSettingsBinding.bind(binding.quickSettings.getHeaderView(0))

        browser = MediaBrowserCompat(this, ComponentName(this, TTSService::class.java), connCallback, null)

        // Enable marquee
        contentBinding.ttsNovelChapter.isSelected = true
        contentBinding.ttsNovelName.isSelected = true

        contentBinding.prevChapterButton.setOnClickListener {
            if (dataCenter.ttsPreferences.swapRewindSkip) controller?.transportControls?.rewind()
            else controller?.transportControls?.skipToPrevious()
        }
        contentBinding.nextChapterButton.setOnClickListener {
            if (dataCenter.ttsPreferences.swapRewindSkip) controller?.transportControls?.fastForward()
            else controller?.transportControls?.skipToNext()
        }
        contentBinding.prevSentenceButton.setOnClickListener {
            if (dataCenter.ttsPreferences.swapRewindSkip) controller?.transportControls?.skipToPrevious()
            else controller?.transportControls?.rewind()
        }
        contentBinding.nextSentenceButton.setOnClickListener {
            if (dataCenter.ttsPreferences.swapRewindSkip) controller?.transportControls?.skipToNext()
            else controller?.transportControls?.fastForward()
        }
        contentBinding.playButton.setOnClickListener {
            if (controller?.playbackState?.state == PlaybackStateCompat.STATE_NONE) {
                if (novel != null) {
                    startTTSService(novel!!.id, translatorSource, chapterIndex)
                }
            } else if (controller?.playbackState?.isPlaying == true) controller?.transportControls?.pause()
            else controller?.transportControls?.play()
        }
        contentBinding.scrollIntoViewButton.setOnClickListener {
            scrollToPosition(lastSentence, true)
        }

        //#region quick settings

        quickSettings.pitch.run {
            // TwoWaySeekBar can't handle 50...100 values
            setAbsoluteMinMaxValue(0.0, (TTSService.PITCH_MAX - TTSService.PITCH_MIN).toHumanPercentage().toDouble())
            setProgress((dataCenter.ttsPreferences.pitch - TTSService.PITCH_MIN).toHumanPercentage().toDouble())

            setOnSeekBarChangedListener { bar, value ->
                val rounded = round(value / 10.0) * 10.0
                dataCenter.ttsPreferences.pitch = rounded.fromHumanPercentage().toFloat() + TTSService.PITCH_MIN
                bar.setProgress(rounded)
                controller?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            }
        }
        quickSettings.speechRate.run {
            setAbsoluteMinMaxValue(0.0, (TTSService.SPEECH_RATE_MAX - TTSService.SPEECH_RATE_MIN).toHumanPercentage().toDouble())
            setProgress((dataCenter.ttsPreferences.speechRate - TTSService.SPEECH_RATE_MIN).toHumanPercentage().toDouble())

            setOnSeekBarChangedListener { bar, value ->
                val rounded = round(value / 10.0) * 10.0
                dataCenter.ttsPreferences.speechRate = rounded.fromHumanPercentage().toFloat() + TTSService.SPEECH_RATE_MIN
                bar.setProgress(rounded)
                controller?.sendCommand(TTSService.ACTION_UPDATE_SETTINGS, null, null)
            }
        }

        quickSettings.autoReadNextChapter.run {
            isChecked = dataCenter.readAloudNextChapter
            setOnCheckedChangeListener { _, b -> dataCenter.readAloudNextChapter = b }
        }

        quickSettings.moveBookmark.run {
            isChecked = dataCenter.ttsPreferences.moveBookmark
            setOnCheckedChangeListener { _, b -> dataCenter.ttsPreferences.moveBookmark = b }
        }

        quickSettings.markRead.run {
            isChecked = dataCenter.ttsPreferences.markChaptersRead
            setOnCheckedChangeListener { _, b -> dataCenter.ttsPreferences.markChaptersRead = b }
        }

        quickSettings.mergeChapters.run {
            isChecked = dataCenter.ttsPreferences.mergeBufferChapters
            setOnCheckedChangeListener { _, b -> dataCenter.ttsPreferences.mergeBufferChapters = b }
        }

        quickSettings.discardBufferPage.run {
            isChecked = dataCenter.ttsPreferences.discardInitialBufferPage
            setOnCheckedChangeListener { _, b -> dataCenter.ttsPreferences.discardInitialBufferPage = b }
        }

        quickSettings.useLongestPage.run {
            isChecked = dataCenter.ttsPreferences.useLongestPage
            setOnCheckedChangeListener { _, b -> dataCenter.ttsPreferences.useLongestPage = b }
        }

        quickSettings.keepScreenOn.run {
            isChecked = dataCenter.ttsPreferences.keepScreenOn
            setOnCheckedChangeListener { _, b ->
                dataCenter.ttsPreferences.keepScreenOn = b
                if (b) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        quickSettings.reloadChapter.setOnClickListener {
            controller?.sendCommand(TTSService.COMMAND_RELOAD_CHAPTER, null, null)
            binding.root.closeDrawers()
        }

        quickSettings.openReader.setOnClickListener {
            openReader()
        }

        quickSettings.openSettings.setOnClickListener {
            startTTSSettingsActivity()
            binding.root.closeDrawers()
        }

        quickSettings.autoStopTimerPick.setOnClickListener {
            val listener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                dataCenter.ttsPreferences.stopTimer = (hour * 60 + minute).toLong()
                controller?.sendCommand(TTSService.COMMAND_UPDATE_TIMER, Bundle().apply {
                    putBoolean("reset", true)
                }, updateTimerCallback)
                quickSettings.autoStopTimerPick.text =
                    getString(R.string.set_auto_stop_timer, dataCenter.ttsPreferences.stopTimer / 60, dataCenter.ttsPreferences.stopTimer % 60)
            }
            TimePickerDialog(
                this, listener, (dataCenter.ttsPreferences.stopTimer / 60).toInt(), (dataCenter.ttsPreferences.stopTimer % 60).toInt(), true
            ).show()
        }
        quickSettings.autoStopTimerPick.text =
            getString(R.string.set_auto_stop_timer, dataCenter.ttsPreferences.stopTimer / 60, dataCenter.ttsPreferences.stopTimer % 60)

        quickSettings.autoStopTimerSwitch.setOnCheckedChangeListener { _, b ->
            controller?.sendCommand(TTSService.COMMAND_UPDATE_TIMER, Bundle().apply {
                putBoolean("active", b)
            }, updateTimerCallback)
        }

        // Timer updates are now handled by startTimerUpdates() coroutine
        //#endregion

        if (dataCenter.ttsPreferences.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = novel.name
    }

    private fun populateSettings() {
        val prefs = dataCenter.ttsPreferences
        quickSettings.pitch.setProgress((prefs.pitch - TTSService.PITCH_MIN).toHumanPercentage().toDouble())
        quickSettings.speechRate.setProgress((prefs.speechRate - TTSService.SPEECH_RATE_MIN).toHumanPercentage().toDouble())
        quickSettings.autoReadNextChapter.isChecked = dataCenter.readAloudNextChapter
        quickSettings.moveBookmark.isChecked = prefs.moveBookmark
        quickSettings.markRead.isChecked = prefs.markChaptersRead
        quickSettings.mergeChapters.isChecked = prefs.mergeBufferChapters
        quickSettings.keepScreenOn.isChecked = prefs.keepScreenOn
        refreshTimerState()
        if (prefs.keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun refreshTimerState() {
        controller?.sendCommand(TTSService.COMMAND_UPDATE_TIMER, null, updateTimerCallback)
    }

    private fun startTimerUpdates() {
        timerUpdateJob?.cancel()
        timerUpdateJob = lifecycleScope.launch {
            while (isActive) {
                val minutes = ceil((stopTimer - System.currentTimeMillis()).toDouble() / 60000).toLong().coerceAtLeast(0)
                if (lastMinutes != minutes) {
                    lastMinutes = minutes
                    quickSettings.autoStopTimerStatus.text = format(Locale.US, "%d:%02d", minutes / 60, minutes % 60)
                }
                delay(1000) // Update every second instead of every frame
            }
        }
    }

    fun initController() {
        controller?.let {
            it.registerCallback(ctrlCallback)
            ctrlCallback.onMetadataChanged(it.metadata)
            ctrlCallback.onPlaybackStateChanged(it.playbackState)
            it.sendCommand(TTSService.COMMAND_REQUEST_SENTENCES, null, null)
            it.sendCommand(TTSService.COMMAND_REQUEST_LINKED_PAGES, null, null)
            refreshTimerState()
        }
    }

    private inner class TTSController : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            if (metadata == null) return
            val novelId = metadata.getLong(TTSService.NOVEL_ID)
            if (novel?.id != novelId) {
                lifecycleScope.launch {
                    novel = withContext(Dispatchers.IO) {
                        dbHelper.getNovel(novelId)
                    }
                    translatorSource = metadata.getString(TTSService.TRANSLATOR_SOURCE_NAME)
                    novel?.imageUrl?.let { image ->
                        Glide.with(this@TextToSpeechControlsActivity).load(image.getGlideUrl()).apply(RequestOptions.circleCropTransform())
                            .into(contentBinding.ttsNovelCover)
                    }
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
            if (extras == null) return
            when (event) {
                TTSService.EVENT_SENTENCE_LIST -> setRecycleView(extras)
                TTSService.COMMAND_UPDATE_TIMER -> updateTimerCallback.send(if (extras.getBoolean("active")) 1 else 0, extras)
                TTSService.EVENT_LINKED_PAGES -> updateLinkedPages(extras.getParcelableArrayList(TTSService.LINKED_PAGES) ?: ArrayList())
                TTSService.EVENT_TEXT_RANGE -> setTextRanges(extras)
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

    private fun updateLinkedPages(pages: ArrayList<LinkedPage>) {
        linkedPages = pages
        linkedPageButtons = linkedPageButtons.filterTo(mutableMapOf()) { entry ->
            if (pages.find { it.href == entry.value } == null) {
                quickSettings.root.removeView(quickSettings.root.findViewById(entry.key))
                false
            } else true
        }
        val themeWrapper = ContextThemeWrapper(this@TextToSpeechControlsActivity, androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless_Colored)
        pages.forEach { page ->
            if (!linkedPageButtons.containsValue(page.href)) {
                val row = TableRow(this@TextToSpeechControlsActivity)
                val button = Button(themeWrapper)
                row.addView(button)
                row.id = View.generateViewId()
                quickSettings.root.addView(row)
                linkedPageButtons[row.id] = page.href
                button.text = if (page.isMainContent) {
                    page.label + " [!]"
                } else {
                    page.label
                }
                button.setOnClickListener {
                    controller?.sendCommand(TTSService.COMMAND_LOAD_BUFFER_LINK, Bundle().apply { putString("href", page.href) }, null)
                }
            }
        }
        quickSettings.noLinkedPages.visibility = if (pages.size == 0) View.VISIBLE else View.GONE
    }

    private fun setTextRanges(extras: Bundle) {
        sentenceRangeStart = extras.getInt(TTSService.TEXT_RANGE_START)
        sentenceRangeEnd = extras.getInt(TTSService.TEXT_RANGE_END)
        adapter.notifyItemChanged(lastSentence, "range")
    }

    private fun updatePosition(oldPosition: Int) {
        val layout = (contentBinding.sentencesView.layoutManager as LinearLayoutManager)
        val visibleStart = layout.findFirstCompletelyVisibleItemPosition()
        val visibleEnd = layout.findLastCompletelyVisibleItemPosition()
        val shouldAutoScroll = !layout.isSmoothScrolling && (oldPosition in visibleStart..visibleEnd || oldPosition == -1)

        sentenceRangeStart = -1

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
        binding.sentenceLayout.setBackgroundColor(
            ContextCompat.getColor(
                this, if (lastSentence == position) R.color.colorLightBlue
                else android.R.color.transparent
            )
        )
        if (lastSentence == position && sentenceRangeStart != -1) {
            val span = SpannableString(item)
            span.setSpan(UnderlineSpan(), sentenceRangeStart.coerceIn(0, item.length), sentenceRangeEnd.coerceIn(0, item.length), 0)
            binding.sentenceContents.applyFont(assets).text = span
        } else {
            binding.sentenceContents.applyFont(assets).text = item
        }
    }

    override fun bind(item: String, itemView: View, position: Int, payloads: MutableList<Any>?) {
        if (payloads?.contains("range") == true) {
            val span = SpannableString(item)
            span.setSpan(UnderlineSpan(), sentenceRangeStart.coerceIn(0, item.length), sentenceRangeEnd.coerceIn(0, item.length), 0)
            itemView.findViewById<TextView>(R.id.sentenceContents)?.text = span
        } else if (payloads?.contains("active") == true) {
            itemView.findViewById<TextView>(R.id.sentenceContents)?.setBackgroundColor(
                ContextCompat.getColor(
                    this, if (lastSentence == position) R.color.colorLightBlue
                    else android.R.color.transparent
                )
            )
        } else super.bind(item, itemView, position, payloads)
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
        populateSettings()
        //
    }

    override fun onStop() {
        super.onStop()
        timerUpdateJob?.cancel()
        controller?.unregisterCallback(ctrlCallback)
        browser.disconnect()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_text_to_speech, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_open_reader -> {
                openReader()
            }

            R.id.action_open_settings -> {
                binding.root.openDrawer(binding.quickSettings)
                //startTTSSettingsActivity()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openReader() {
        if (controller?.metadata != null) {
            val meta = controller!!.metadata
            val novelId = meta.getLong(TTSService.NOVEL_ID)
            
            lifecycleScope.launch {
                val novel = withContext(Dispatchers.IO) {
                    dbHelper.getNovel(novelId)
                } ?: return@launch

                var translator: String? = meta.getString(TTSService.TRANSLATOR_SOURCE_NAME)
                if (translator == "") translator = null
                
                val chapter = withContext(Dispatchers.IO) {
                    dbHelper.getWebPage(novelId, translator, meta.getLong(TTSService.CHAPTER_INDEX).toInt())
                }
                
                chapter?.let {
                    updateNovelBookmark(novel, it)
                    finishAfterTransition()
                    startReaderDBPagerActivity(novel, translator)
                }
            }
        }
    }

}