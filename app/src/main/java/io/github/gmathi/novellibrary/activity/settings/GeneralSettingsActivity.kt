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
import io.github.gmathi.novellibrary.extensions.startBackupSettingsActivity
import io.github.gmathi.novellibrary.extensions.startLanguagesActivity
import io.github.gmathi.novellibrary.service.sync.BackgroundNovelSyncTask
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_title_subtitle_widget.view.*
import java.util.*

class GeneralSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private const val TAG = "GeneralSettingsActivity"

        private const val POSITION_LOAD_LIBRARY_SCREEN = 0
        private const val POSITION_BACKUP_AND_RESTORE = 1
        //        private const val POSITION_ENABLE_CLOUD_FLARE = 2
        private const val POSITION_ENABLE_NOTIFICATIONS = 2
        //private const val POSITION_FASTER_DOWNLOADS = 4

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
        settingsItems = ArrayList(resources.getStringArray(R.array.general_titles_list).asList())

        val items = ArrayList(resources.getStringArray(R.array.general_subtitles_list).asList())
        val systemDefault = resources.getString(R.string.system_default)
        if (items.contains(systemDefault)) {
            val language = try {
                dataCenter.language.split('_')[0]
            } catch (e: KotlinNullPointerException) {
                "systemDefault"
            }
            items[items.indexOfFirst { it == systemDefault }] =
                    if (language != "systemDefault")
                        Locale(language).displayLanguage
                    else systemDefault
        }
        settingsItemsDescription = items

        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.widgetChevron.visibility = View.INVISIBLE
        itemView.widgetSwitch.visibility = View.INVISIBLE
        itemView.widgetButton.visibility = View.INVISIBLE

        itemView.title.applyFont(assets).text = item
        itemView.subtitle.applyFont(assets).text = settingsItemsDescription[position]
        itemView.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            POSITION_LOAD_LIBRARY_SCREEN -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.loadLibraryScreen
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.loadLibraryScreen = value }
            }

            POSITION_BACKUP_AND_RESTORE -> {
                itemView.widgetChevron.visibility = View.VISIBLE
            }

//            POSITION_ENABLE_CLOUD_FLARE -> {
//                itemView.widgetSwitch.visibility = View.VISIBLE
//                itemView.widgetSwitch.isChecked = dataCenter.enableCloudFlare
//                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.enableCloudFlare = value }
//            }

            POSITION_ENABLE_NOTIFICATIONS -> {
                itemView.widgetSwitch.visibility = View.VISIBLE
                itemView.widgetSwitch.isChecked = dataCenter.enableNotifications
                itemView.widgetSwitch.setOnCheckedChangeListener { _, value ->
                    dataCenter.enableNotifications = value
                    if (value)
                        BackgroundNovelSyncTask.scheduleRepeat(applicationContext)
                    else
                        BackgroundNovelSyncTask.cancelAll(applicationContext)
                }
            }

//            POSITION_FASTER_DOWNLOADS -> {
//                itemView.widgetSwitch.visibility = View.VISIBLE
//                itemView.widgetSwitch.isChecked = dataCenter.experimentalDownload
//                itemView.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.experimentalDownload = value }
//            }
        }

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.backup_and_restore) -> startBackupSettingsActivity()
            getString(R.string.change_language) -> startLanguagesActivity(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
    //endregion

}
