package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.util.system.startBackupSettingsActivity
import io.github.gmathi.novellibrary.util.system.startLanguagesActivity
import io.github.gmathi.novellibrary.service.sync.BackgroundNovelSyncTask
import io.github.gmathi.novellibrary.util.Constants.SYSTEM_DEFAULT
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import java.util.*

class GeneralSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "GeneralSettingsActivity"

        private const val POSITION_LOAD_LIBRARY_SCREEN = 0
        private const val POSITION_BACKUP_AND_RESTORE = 1
        private const val POSITION_ENABLE_NOTIFICATIONS = 2
        private const val POSITION_LANGUAGES = 3
        private const val POSITION_ENABLE_SCROLLING_TEXT = 4

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
        settingsItems = ArrayList(resources.getStringArray(R.array.general_titles_list).asList())

        val items = ArrayList(resources.getStringArray(R.array.general_subtitles_list).asList())
        val systemDefault = resources.getString(R.string.locale_system_default)
        if (items.contains(systemDefault)) {
            val language = try {
                dataCenter.language
            } catch (e: KotlinNullPointerException) {
                SYSTEM_DEFAULT
            }
            items[items.indexOfFirst { it == systemDefault }] = LanguageActivity.getLanguageName(this, language)
        }
        settingsItemsDescription = items

        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE
        itemBinding.blackOverlay.visibility = View.INVISIBLE

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            POSITION_LOAD_LIBRARY_SCREEN -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.loadLibraryScreen
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.loadLibraryScreen = value }
            }

            POSITION_BACKUP_AND_RESTORE, POSITION_LANGUAGES -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
            }

            POSITION_ENABLE_NOTIFICATIONS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.enableNotifications
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.enableNotifications = value
                    if (value)
                        BackgroundNovelSyncTask.scheduleRepeat(applicationContext)
                    else
                        BackgroundNovelSyncTask.cancelAll(applicationContext)
                }
            }

            POSITION_ENABLE_SCROLLING_TEXT -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.enableScrollingText
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableScrollingText = value }
            }
        }

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String, position: Int) {
        when (item) {
            getString(R.string.backup_and_restore) -> startBackupSettingsActivity()
            getString(R.string.change_language) -> startLanguagesActivity(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}