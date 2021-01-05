package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.util.system.startSyncLoginActivity
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.lang.launchIO
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.ArrayList

class SyncSettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        const val POSITION_ENABLE = 0
        const val POSITION_STATUS = 1
        const val POSITION_LOG_IN = 2
        const val POSITION_ADD_NOVELS = 3
        const val POSITION_DELETE_NOVELS = 4
        const val POSITION_BOOKMARKS = 5
        const val POSITION_MAKE_SYNC = 6
//        const val POSITION_FETCH_NOVELS = 7
//        const val POSITION_FETCH_BOOKMARKS = 8
//        const val POSITION_FETCH_SECTIONS = 9
//        const val POSITION_MAKE_FETCH = 10
        const val POSITION_FORGET = 7 // 11
    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescriptions: ArrayList<String>

    private lateinit var novelSync: NovelSync
    
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sync = NovelSync.getInstance(intent.getStringExtra("url")!!, true)
        if (sync == null) {
            finish()
            return
        }
        novelSync = sync
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.sync_options).asList())
        settingsItemsDescriptions = ArrayList(resources.getStringArray(R.array.sync_options_descriptions).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_title_subtitle_widget, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE

        itemBinding.title.applyFont(assets).text = item
        itemBinding.subtitle.applyFont(assets).text = settingsItemsDescriptions[position]

        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))

        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        when(position) {
            POSITION_ENABLE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncEnabled(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked ->
                    dataCenter.setSyncEnabled(novelSync.host, isChecked)
//                    if (isChecked) {
//                        // TODO: Ask to perform full sync if logged in
//                    }
                }
            }
            POSITION_STATUS -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                if (novelSync.loggedIn()) {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_check_circle_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(this, R.color.colorStateGreen)
                    itemBinding.subtitle.text = getString(R.string.logged_in)
                } else {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_warning_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(this, R.color.colorStateOrange)
                    itemBinding.subtitle.text = getString(R.string.not_logged_in)
                }
            }
            POSITION_LOG_IN -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
            }
            POSITION_ADD_NOVELS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncAddNovels(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncAddNovels(novelSync.host, isChecked) }
            }
            POSITION_DELETE_NOVELS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncDeleteNovels(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncDeleteNovels(novelSync.host, isChecked) }
            }
            POSITION_BOOKMARKS -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncBookmarks(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked -> dataCenter.setSyncBookmarks(novelSync.host, isChecked) }
            }
            POSITION_MAKE_SYNC -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                itemBinding.widgetChevron.setImageResource(R.drawable.ic_sync_white_vector)
            }
            POSITION_FORGET -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                itemBinding.widgetChevron.setImageResource(R.drawable.ic_delete_white_vector)
            }
        }
    }

    override fun onItemClick(item: String, position: Int) {
        when (position) {
            POSITION_LOG_IN -> startSyncLoginActivity(novelSync.getLoginURL(), novelSync.getCookieLookupRegex())
            POSITION_MAKE_SYNC -> {
                if (novelSync.loggedIn()) {
                    MaterialDialog(this).show {
                        title(R.string.confirm_full_sync)
                        message(R.string.confirm_full_sync_description)
                        positiveButton(R.string.okay) {
                            makeFullSync()
                        }
                        negativeButton(R.string.cancel)
                    }
                }
            }
            POSITION_FORGET -> {
                if (novelSync.loggedIn()) {
                    MaterialDialog(this).show {
                        title(R.string.confirm_forget_cookies)
                        message(R.string.confirm_forget_cookies_description)
                        positiveButton(R.string.okay) {
                            novelSync.forget()
                            adapter.notifyDataSetChanged()
                        }
                        negativeButton(R.string.cancel)
                    }
                }
            }
        }
    }
    
    private var makeFullSyncInProgress = false

    private fun makeFullSync() {
        makeFullSyncInProgress = true
        val view = findViewById<ViewGroup>(android.R.id.content)
        view.isEnabled = false
        val snackProgressBarManager = Utils.createSnackProgressBarManager(view, null)
        val snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_HORIZONTAL, getString(R.string.sync_in_progress) + " - " + getString(R.string.please_wait))

        launchUI {
            snackProgressBarManager.show(
                snackProgressBar,
                SnackProgressBarManager.LENGTH_INDEFINITE
            )
        }

        launchIO {
            val sections = db.novelSectionDao().getAll()
            val novels = db.novelDao().getAll().filter { it.url.contains(novelSync.host) }

            val total = novels.count()

            async(Dispatchers.Main) {
                snackProgressBarManager.updateTo(snackProgressBar.setProgressMax(total))
            }

            var counter = 0
            novelSync.batchAdd(novels, sections) { novelName ->
                if (++counter == total) {
                    async(Dispatchers.Main) {
                        snackProgressBarManager.disable()
                        view.isEnabled = true
                        makeFullSyncInProgress = false
                    }
                } else {
                    async(Dispatchers.Main) {
                        try {
                            snackProgressBarManager.updateTo(
                                snackProgressBar.setMessage(
                                    getString(
                                        R.string.sync_batch_progress_title,
                                        counter,
                                        total
                                    ) + " - " + getString(R.string.sync_batch_progress, novelName)
                                )
                            )
                            snackProgressBarManager.setProgress(counter)
                        } catch (ex: Exception) {
                            // Could probably fail because snackProgressBarManager became invalid
                        }
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        if (!makeFullSyncInProgress) {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}