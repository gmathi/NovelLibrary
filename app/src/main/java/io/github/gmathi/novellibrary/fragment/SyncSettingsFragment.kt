package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getAllNovelSections
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.databinding.FragmentSyncSettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemTitleSubtitleWidgetBinding
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.lang.launchIO
import io.github.gmathi.novellibrary.util.lang.launchUI
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.*

@AndroidEntryPoint
class SyncSettingsFragment : BaseViewBindingFragment<FragmentSyncSettingsBinding>(), GenericAdapter.Listener<String> {

    companion object {
        const val POSITION_ENABLE = 0
        const val POSITION_STATUS = 1
        const val POSITION_LOG_IN = 2
        const val POSITION_ADD_NOVELS = 3
        const val POSITION_DELETE_NOVELS = 4
        const val POSITION_BOOKMARKS = 5
        const val POSITION_MAKE_SYNC = 6
        const val POSITION_FORGET = 7
    }

    private val args: SyncSettingsFragmentArgs by navArgs()
    
    private lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>
    private lateinit var settingsItemsDescriptions: ArrayList<String>

    private lateinit var novelSync: NovelSync

    override fun getLayoutId() = R.layout.fragment_sync_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setBinding(FragmentSyncSettingsBinding.bind(view))
        
        val sync = NovelSync.getInstance(args.url, dbHelper, dataCenter, networkHelper, sourceManager, true)
        if (sync == null) {
            findNavController().popBackStack()
            return
        }
        novelSync = sync
        
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
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemTitleSubtitleWidgetBinding.bind(itemView)
        itemBinding.blackOverlay.visibility = View.INVISIBLE
        itemBinding.widgetChevron.visibility = View.INVISIBLE
        itemBinding.widgetSwitch.visibility = View.INVISIBLE
        itemBinding.currentValue.visibility = View.INVISIBLE

        itemBinding.title.applyFont(requireContext().assets).text = item
        itemBinding.subtitle.applyFont(requireContext().assets).text = settingsItemsDescriptions[position]

        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(requireContext(), R.color.black_transparent)
            else ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )

        itemBinding.widgetSwitch.setOnCheckedChangeListener(null)
        when (position) {
            POSITION_ENABLE -> {
                itemBinding.widgetSwitch.visibility = View.VISIBLE
                itemBinding.widgetSwitch.isChecked = dataCenter.getSyncEnabled(novelSync.host)
                itemBinding.widgetSwitch.setOnCheckedChangeListener { _, isChecked ->
                    dataCenter.setSyncEnabled(novelSync.host, isChecked)
                }
            }
            POSITION_STATUS -> {
                itemBinding.widgetChevron.visibility = View.VISIBLE
                if (novelSync.loggedIn()) {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_check_circle_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorStateGreen)
                    itemBinding.subtitle.text = getString(R.string.logged_in)
                } else {
                    itemBinding.widgetChevron.setImageResource(R.drawable.ic_warning_white_vector)
                    itemBinding.widgetChevron.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.colorStateOrange)
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
            POSITION_LOG_IN -> {
                findNavController().navigate(
                    SyncSettingsFragmentDirections.actionSyncSettingsToSyncLogin(
                        url = novelSync.getLoginURL(),
                        lookup = novelSync.getCookieLookupRegex()
                    )
                )
            }
            POSITION_MAKE_SYNC -> {
                if (novelSync.loggedIn()) {
                    MaterialDialog(requireContext()).show {
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
                    MaterialDialog(requireContext()).show {
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
        val view = requireActivity().findViewById<ViewGroup>(android.R.id.content)
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
            val sections = dbHelper.getAllNovelSections()
            val novels = dbHelper.getAllNovels().filter { it.url.contains(novelSync.host) }

            val total = novels.count()
            withContext(Dispatchers.Main) { snackProgressBarManager.updateTo(snackProgressBar.setProgressMax(total)) }

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
}