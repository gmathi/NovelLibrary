package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MAX
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MIN
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.system.startReaderBackgroundSettingsActivity
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import java.io.File
import kotlin.math.abs

class ReaderSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val POSITION_READER_MODE = 0
        private const val POSITION_JAVASCRIPT_DISABLED = 1
        private const val POSITION_READER_MODE_THEME = 2
        private const val POSITION_JAP_SWIPE = 3
        private const val POSITION_SHOW_READER_SCROLL = 4
        private const val POSITION_SHOW_CHAPTER_COMMENTS = 5
        private const val POSITION_VOLUME_SCROLL = 6
        private const val POSITION_VOLUME_SCROLL_LENGTH = 7
        private const val POSITION_KEEP_SCREEN_ON = 8
        private const val POSITION_ENABLE_IMMERSIVE_MODE = 9
        private const val POSITION_SHOW_NAVBAR_AT_CHAPTER_END = 10
        private const val POSITION_ENABLE_CLUSTER_PAGES = 11
        private const val POSITION_DIRECTIONAL_LINKS = 12
        private const val POSITION_READER_MODE_BUTTON_VISIBILITY = 13
        private const val POSITION_KEEP_TEXT_COLOR = 14
        private const val POSITION_ALTERNATIVE_TEXT_COLORS = 15
        private const val POSITION_LIMIT_IMAGE_WIDTH = 16
        private const val POSITION_AUTO_READ_NEXT_CHAPTER = 17
        private const val POSITION_CUSTOM_QUERY_LOOKUPS = 18

    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescription: ArrayList<String>

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.reader_titles_list).asList())
        settingsItemsDescription = ArrayList(resources.getStringArray(R.array.reader_subtitles_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        //(itemView as ViewGroup).enabled(true)
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE
        itemBinding.currentValue.text = ""

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            POSITION_READER_MODE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.readerMode
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.readerMode = value
                    if (value) {
                        dataCenter.javascriptDisabled = value
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            POSITION_JAVASCRIPT_DISABLED -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.javascriptDisabled
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.javascriptDisabled = value
                    if (!value) {
                        dataCenter.readerMode = value
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            POSITION_READER_MODE_THEME -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
            }

            POSITION_JAP_SWIPE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.japSwipe
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.japSwipe = value }
            }
            POSITION_SHOW_READER_SCROLL -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.showReaderScroll
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showReaderScroll = value }
            }
            POSITION_SHOW_CHAPTER_COMMENTS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.showChapterComments
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showChapterComments = value }
            }
            POSITION_VOLUME_SCROLL -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.volumeScroll
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.volumeScroll = value
                    adapter.notifyDataSetChanged()
                }
            }
            POSITION_VOLUME_SCROLL_LENGTH -> {
                val enable = dataCenter.volumeScroll
                //itemView.enabled(enable)
                itemBinding.blackOverlay.visibility =
                    if (enable)
                        View.INVISIBLE
                    else
                        View.VISIBLE
                itemBinding.currentValue.visibility = View.VISIBLE
                val value = dataCenter.scrollLength
                itemBinding.currentValue.text = "${if (value < 0) resources.getString(R.string.reverse) else ""} ${abs(value)}"
                if (enable)
                    itemView.setOnClickListener {
                        changeScrollDistance(itemBinding.currentValue)
                    }
            }
            POSITION_KEEP_SCREEN_ON -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.keepScreenOn
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.keepScreenOn = value }
            }
            POSITION_ENABLE_IMMERSIVE_MODE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.enableImmersiveMode
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableImmersiveMode = value }
            }
            POSITION_SHOW_NAVBAR_AT_CHAPTER_END -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.showNavbarAtChapterEnd
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showNavbarAtChapterEnd = value }
            }
            POSITION_ENABLE_CLUSTER_PAGES -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.enableClusterPages
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableClusterPages = value }
            }
            POSITION_DIRECTIONAL_LINKS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.enableDirectionalLinks
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableDirectionalLinks = value }
            }
            POSITION_READER_MODE_BUTTON_VISIBILITY -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.isReaderModeButtonVisible
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.isReaderModeButtonVisible = value }
            }
            POSITION_KEEP_TEXT_COLOR -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.keepTextColor
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.keepTextColor = value }
            }
            POSITION_ALTERNATIVE_TEXT_COLORS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.alternativeTextColors
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.alternativeTextColors = value }
            }
            POSITION_LIMIT_IMAGE_WIDTH -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.limitImageWidth
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.limitImageWidth = value }
            }
            POSITION_AUTO_READ_NEXT_CHAPTER -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.readAloudNextChapter
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.readAloudNextChapter = value }
            }
            POSITION_CUSTOM_QUERY_LOOKUPS -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
            }
        }

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: String, position: Int) {
//        if (item == getString(R.string.sync_interval)) {
//            showSyncIntervalDialog()
//        }
        if (position == POSITION_READER_MODE_THEME) {
            startReaderBackgroundSettingsActivity()
        } else if (position == POSITION_CUSTOM_QUERY_LOOKUPS) {
            MaterialDialog(this).show {
                title(R.string.custom_query_lookups_edit)
                input(
                    hintRes = R.string.custom_query_lookups_hint,
                    prefill = dataCenter.userSpecifiedSelectorQueries,
                    inputType = InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_CLASS_TEXT
                )
                positiveButton(R.string.fui_button_text_save) { widget ->
                    dataCenter.userSpecifiedSelectorQueries = widget.getInputField()?.text.toString()
                    firebaseAnalytics.logEvent(FAC.Event.SELECTOR_QUERY) {
                        param(FirebaseAnalytics.Param.VALUE, widget.getInputField()?.text.toString())
                    }
                }
                negativeButton(R.string.cancel)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region Delete Files
    private fun deleteFilesDialog() {
        MaterialDialog(this).show {
            title(R.string.clear_data)
            message(R.string.clear_data_description)
            positiveButton(R.string.clear) { dialog ->
                val snackBar = Snackbar.make(
                    findViewById(android.R.id.content),
                    getString(R.string.clearing_data) + " - " + getString(R.string.please_wait),
                    Snackbar.LENGTH_INDEFINITE
                )
                deleteFiles()
                snackBar.dismiss()
                dialog.dismiss()
            }
            negativeButton(R.string.cancel) { dialog ->
                dialog.dismiss()
            }
        }
    }

    private fun deleteFiles() {
        try {
            deleteDir(cacheDir)
            deleteDir(filesDir)
            dbHelper.removeAll()
            dataCenter.saveNovelSearchHistory(ArrayList())
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                deleteDir(File(dir, children[i]))
            }
            dir.delete()
        } else if (dir != null && dir.isFile) {
            dir.delete()
        } else {
            false
        }
    }
    //endregion

    private fun changeScrollDistance(textView: TextView) {
        var value = dataCenter.scrollLength

        val dialog = MaterialDialog(this).show {
            title(R.string.volume_scroll_length)
            customView(R.layout.dialog_slider, scrollable = true)
            onDismiss {
                dataCenter.scrollLength = value
            }
        }

        val seekBar = dialog.getCustomView()?.findViewById<TwoWaySeekBar>(R.id.seekBar) ?: return
        seekBar.notifyWhileDragging = true
        seekBar.setOnSeekBarChangedListener { _, progress ->
            value = progress.toInt()
            textView.text = "${if (value < 0) resources.getString(R.string.reverse) else ""} ${abs(value)}"
        }
        seekBar.setAbsoluteMinMaxValue(VOLUME_SCROLL_LENGTH_MIN.toDouble(), VOLUME_SCROLL_LENGTH_MAX.toDouble())
        seekBar.setProgress(dataCenter.scrollLength.toDouble())
    }

}