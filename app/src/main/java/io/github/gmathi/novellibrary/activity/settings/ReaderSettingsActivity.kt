package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.util.system.startReaderBackgroundSettingsActivity
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MAX
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_MIN
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.reader_titles_list).asList())
        settingsItemsDescription = ArrayList(resources.getStringArray(R.array.reader_subtitles_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        //(itemView as ViewGroup).enabled(true)
        itemView.blackOverlay.visibility = View.INVISIBLE
        itemView.widgetChevron.visibility = View.INVISIBLE
        itemView.widgetSwitch.visibility = View.INVISIBLE
        itemView.currentValue.visibility = View.INVISIBLE
        itemView.currentValue.text = ""

        itemView.title.applyFont(assets).text = item
        itemView.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemView.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            POSITION_READER_MODE -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.readerMode
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.readerMode = value
                    if (value) {
                        dataCenter.javascriptDisabled = value
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            POSITION_JAVASCRIPT_DISABLED -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.javascriptDisabled
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.javascriptDisabled = value
                    if (!value) {
                        dataCenter.readerMode = value
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            POSITION_READER_MODE_THEME -> {
                itemView.widgetChevron.visibility = View.VISIBLE
            }

            POSITION_JAP_SWIPE -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.japSwipe
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.japSwipe = value }
            }
            POSITION_SHOW_READER_SCROLL -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.showReaderScroll
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showReaderScroll = value }
            }
            POSITION_SHOW_CHAPTER_COMMENTS -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.showChapterComments
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showChapterComments = value }
            }
            POSITION_VOLUME_SCROLL -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.volumeScroll
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.volumeScroll = value
                    adapter.notifyDataSetChanged()
                }
            }
            POSITION_VOLUME_SCROLL_LENGTH -> {
                val enable = dataCenter.volumeScroll
                //itemView.enabled(enable)
                itemView.blackOverlay.visibility =
                        if (enable)
                            View.INVISIBLE
                        else
                            View.VISIBLE
                itemView.currentValue.visibility = View.VISIBLE
                val value = dataCenter.scrollLength
                itemView.currentValue.text = "${if (value < 0) resources.getString(R.string.reverse) else ""} ${abs(value)}"
                if (enable)
                    itemView.setOnClickListener {
                        changeScrollDistance(itemView.currentValue)
                    }
            }
            POSITION_KEEP_SCREEN_ON -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.keepScreenOn
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.keepScreenOn = value }
            }
            POSITION_ENABLE_IMMERSIVE_MODE -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.enableImmersiveMode
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableImmersiveMode = value }
            }
            POSITION_SHOW_NAVBAR_AT_CHAPTER_END -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.showNavbarAtChapterEnd
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.showNavbarAtChapterEnd = value }
            }
            POSITION_ENABLE_CLUSTER_PAGES -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.enableClusterPages
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableClusterPages = value }
            }
            POSITION_DIRECTIONAL_LINKS -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.enableDirectionalLinks
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableDirectionalLinks = value }
            }
            POSITION_READER_MODE_BUTTON_VISIBILITY -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.isReaderModeButtonVisible
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.isReaderModeButtonVisible = value }
            }
            POSITION_KEEP_TEXT_COLOR -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.keepTextColor
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.keepTextColor = value }
            }
            POSITION_ALTERNATIVE_TEXT_COLORS -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.alternativeTextColors
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.alternativeTextColors = value }
            }
            POSITION_LIMIT_IMAGE_WIDTH -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.limitImageWidth
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.limitImageWidth = value }
            }
            POSITION_AUTO_READ_NEXT_CHAPTER -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.readAloudNextChapter
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.readAloudNextChapter = value }
            }
            POSITION_CUSTOM_QUERY_LOOKUPS -> {
                itemView.widgetChevron.visibility = View.VISIBLE
            }
        }

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String, position: Int) {
//        if (item == getString(R.string.sync_interval)) {
//            showSyncIntervalDialog()
//        }
        if (position == POSITION_READER_MODE_THEME) {
            startReaderBackgroundSettingsActivity()
        } else if (position == POSITION_CUSTOM_QUERY_LOOKUPS) {
            MaterialDialog.Builder(this)
                .title(getString(R.string.custom_query_lookups_edit))
                .inputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE + InputType.TYPE_CLASS_TEXT)
                .input(getString(R.string.custom_query_lookups_hint), dataCenter.customQueryLookups) { _, _ -> }
                .positiveText(getString(R.string.fui_button_text_save))
                .negativeText(getString(R.string.cancel))
                .onPositive { widget, _ ->
                    dataCenter.customQueryLookups = widget.inputEditText?.text.toString()
                }
                .show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region Delete Files
    private fun deleteFilesDialog() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.clear_data))
            .content(getString(R.string.clear_data_description))
            .positiveText(R.string.clear)
            .negativeText(R.string.cancel)
            .onPositive { dialog, _ ->
                val progressDialog = MaterialDialog.Builder(this)
                    .title(getString(R.string.clearing_data))
                    .content(getString(R.string.please_wait))
                    .progress(true, 0)
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show()
                deleteFiles(progressDialog)
                dialog.dismiss()
            }
            .onNegative { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deleteFiles(dialog: MaterialDialog) {
        try {
            deleteDir(cacheDir)
            deleteDir(filesDir)
            dbHelper.removeAll()
            dataCenter.saveNovelSearchHistory(ArrayList())
            dialog.dismiss()
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

        val dialog = MaterialDialog.Builder(this)
                .title(R.string.volume_scroll_length)
                .customView(R.layout.dialog_slider, true)
                .dismissListener { dataCenter.scrollLength = value }
                .build()
        dialog.show()

        val seekBar = dialog.customView?.findViewById<TwoWaySeekBar>(R.id.seekBar)
        seekBar?.notifyWhileDragging = true
        seekBar?.setOnSeekBarChangedListener { _, progress ->
            value = progress.toInt()
            textView.text = "${if (value < 0) resources.getString(R.string.reverse) else ""} ${abs(value)}"
        }
        seekBar?.setAbsoluteMinMaxValue(VOLUME_SCROLL_LENGTH_MIN.toDouble(), VOLUME_SCROLL_LENGTH_MAX.toDouble())
        seekBar?.setProgress(dataCenter.scrollLength.toDouble())
    }

}