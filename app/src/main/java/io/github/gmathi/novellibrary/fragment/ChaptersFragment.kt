package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.databinding.FragmentSourceChaptersBinding
import io.github.gmathi.novellibrary.databinding.ListitemChapterBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.ChaptersUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.navigation.NavigationManager
import io.github.gmathi.novellibrary.util.system.startWebViewActivity
import io.github.gmathi.novellibrary.util.system.updateNovelBookmark
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@AndroidEntryPoint
class ChaptersFragment : BaseFragment(),
    GenericAdapterSelectTitleProvider.Listener<WebPage>, CompoundButton.OnCheckedChangeListener {

    companion object {
        private const val TAG = "ChaptersFragment"
        private const val NOVEL = "novel"
        private const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"

        fun newInstance(novel: Novel, translatorSourceName: String): ChaptersFragment {
            val bundle = Bundle()
            bundle.putParcelable(NOVEL, novel)
            bundle.putString(TRANSLATOR_SOURCE_NAME, translatorSourceName)
            val fragment = ChaptersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var navigationManager: NavigationManager

    lateinit var novel: Novel
    private lateinit var translatorSourceName: String
    lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>

    private var lastKnownRecyclerState: Parcelable? = null

    private lateinit var binding: FragmentSourceChaptersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = inflater.inflate(R.layout.fragment_source_chapters, container, false)
            ?: return null
        binding = FragmentSourceChaptersBinding.bind(result)
        return result
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = requireArguments().getParcelable<Novel>(NOVEL) as Novel
        translatorSourceName = requireArguments().getString(TRANSLATOR_SOURCE_NAME)
            ?: ALL_TRANSLATOR_SOURCES
        binding.progressLayout.showLoading()
        setRecyclerView()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterSelectTitleProvider(items = ArrayList(), layoutResId = R.layout.listitem_chapter, listener = this)
        binding.recyclerView.isVerticalScrollBarEnabled = true
        binding.recyclerView.setDefaultsNoAnimation(adapter)
        this.context?.let { binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(it, DividerItemDecoration.VERTICAL)) }
        binding.swipeRefreshLayout.isEnabled = false
    }

    private fun setData(shouldScrollToBookmark: Boolean = true, shouldScrollToFirstUnread: Boolean = true) {
        // Try to get data from ChaptersPagerActivity (legacy) or ChaptersMainFragment (new)
        val chaptersActivity = activity as? ChaptersPagerActivity
        val chaptersMainFragment = parentFragment as? ChaptersMainFragment
        
        val chapters = when {
            chaptersActivity != null -> {
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES)
                    chaptersActivity.vm.chapters
                else
                    chaptersActivity.vm.chapters?.filter { it.translatorSourceName == translatorSourceName }
            }
            chaptersMainFragment != null -> {
                val currentState = chaptersMainFragment.viewModel.uiState.value as? ChaptersUiState.Success
                if (translatorSourceName == ALL_TRANSLATOR_SOURCES)
                    currentState?.chapters
                else
                    currentState?.chapters?.filter { it.translatorSourceName == translatorSourceName }
            }
            else -> null
        } ?: ArrayList()

        if (chapters.isNotEmpty()) {
            adapter.updateData(if (novel.metadata["chapterOrder"] == "des") ArrayList(chapters.reversed()) else ArrayList(chapters))
            binding.progressLayout.showContent()
            if (shouldScrollToBookmark)
                scrollToBookmark()
            else if (shouldScrollToFirstUnread)
                scrollToFirstUnread(
                    chaptersActivity?.vm?.chapterSettings ?: ArrayList((chaptersMainFragment?.viewModel?.uiState?.value as? ChaptersUiState.Success)?.chapterSettings ?: emptyList())
                )
            val dataSet = chaptersActivity?.dataSet ?: chaptersMainFragment?.dataSet ?: HashSet()
            if (dataSet.isNotEmpty()) {
                lastKnownRecyclerState?.let { binding.recyclerView.layoutManager?.onRestoreInstanceState(it) }
            }
        } else
            binding.progressLayout.showEmpty(emptyText = "No Chapters Found!")
    }

    private fun scrollToBookmark() {
        if (novel.currentChapterUrl != null) {
            val index = adapter.items.indexOfFirst { it.url == novel.currentChapterUrl }
            if (index != -1)
                binding.recyclerView.scrollToPosition(index)
        }
    }

    private fun scrollToFirstUnread(chaptersSettings: ArrayList<WebPageSettings>) {
        if (novel.currentChapterUrl != null) {
            val index = if (novel.metadata["chapterOrder"] == "des")
                adapter.items.indexOfLast { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && !it.isRead } != null }
            else
                adapter.items.indexOfFirst { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && !it.isRead } != null }
            if (index != -1)
                binding.recyclerView.scrollToPosition(index)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun getSectionTitle(position: Int): String {
        return adapter.items[position].chapterName
    }

    override fun onItemClick(item: WebPage, position: Int) {
        if (novel.id != -1L) {
            updateNovelBookmark(novel, item)
            navigateToReader(item)
        } else {
            startWebViewActivity(item.url)
        }
    }

    /**
     * Navigate to reader using Navigation Component.
     */
    private fun navigateToReader(webPage: WebPage) {
        try {
            // For reader navigation, we need to use the orderId as chapter identifier
            // The reader expects a chapter ID to identify which chapter to display
            val chapterId = webPage.orderId
            
            // Since this is a child fragment, get NavController from parent fragment
            val navController = parentFragment?.findNavController()
            if (navController != null) {
                // Navigate using Navigation Component
                navigationManager.navigateToReaderFromChapters(
                    navController,
                    novel.id,
                    chapterId
                )
            } else {
                // Fallback to WebView if navigation controller not available
                startWebViewActivity(webPage.url)
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Error navigating to reader", e)
            // Fallback to WebView if navigation fails
            startWebViewActivity(webPage.url)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {
        val itemBinding = ListitemChapterBinding.bind(itemView)

        val chaptersPagerActivity = activity as? ChaptersPagerActivity
        val chaptersMainFragment = parentFragment as? ChaptersMainFragment
        val webPageSettings = chaptersPagerActivity?.vm?.chapterSettings?.firstOrNull { it.url == item.url }
            ?: (chaptersMainFragment?.viewModel?.uiState?.value as? ChaptersUiState.Success)?.chapterSettings?.firstOrNull { it.url == item.url }

        // Show download status - downloaded, downloading, or not downloaded
        if (webPageSettings?.filePath != null) {
            // Chapter is downloaded
            itemBinding.availableOfflineImageView.visibility = View.VISIBLE
            itemBinding.availableOfflineImageView.animation = null
        } else {
            // Check if chapter is currently being downloaded
            val isDownloading = isChapterCurrentlyDownloading(item.url)
            if (isDownloading) {
                // Show download progress animation
                itemBinding.availableOfflineImageView.visibility = View.VISIBLE
                // You could add a rotating animation here to show download in progress
                // itemBinding.availableOfflineImageView.startAnimation(rotateAnimation)
            } else {
                // Chapter is not downloaded and not downloading
                itemBinding.availableOfflineImageView.visibility = View.GONE
            }
        }

        itemBinding.isReadView.visibility = if (webPageSettings?.isRead == true) View.VISIBLE else View.GONE
        itemBinding.bookmarkView.visibility = if (item.url == novel.currentChapterUrl) View.VISIBLE else View.GONE

        itemBinding.chapterTitle.text = item.chapterName

        webPageSettings?.title?.let {
            if (it.contains(item.chapterName))
                itemBinding.chapterTitle.text = it
            else
                itemBinding.chapterTitle.text = "${item.chapterName}: $it"
        }
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(null)
        val dataSet = chaptersPagerActivity?.dataSet ?: chaptersMainFragment?.dataSet ?: HashSet()
        itemBinding.chapterCheckBox.isChecked = dataSet.any { it.orderId == item.orderId }
        itemBinding.chapterCheckBox.tag = item
        itemBinding.chapterCheckBox.setOnCheckedChangeListener(this@ChaptersFragment)


        itemView.setOnLongClickListener {
            itemBinding.chapterCheckBox.isChecked = true
            true
        }

        if (webPageSettings?.metadata?.get(Constants.MetaDataKeys.IS_FAVORITE)?.toBoolean() == true) {
            itemBinding.favoriteView.visibility = View.VISIBLE
        } else {
            itemBinding.favoriteView.visibility = View.GONE
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return
        
        // Try to get parent activity or fragment
        val chaptersPagerActivity = activity as? ChaptersPagerActivity
        val chaptersMainFragment = parentFragment as? ChaptersMainFragment
        
        when {
            chaptersPagerActivity != null -> {
                // If Novel is not in Library
                if (novel.id == -1L) {
                    if (isChecked)
                        chaptersPagerActivity.confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), { dialog ->
                            chaptersPagerActivity.vm.addNovelToLibrary()
                            chaptersPagerActivity.invalidateOptionsMenu()
                            chaptersPagerActivity.addToDataSet(webPage)
                            dialog.dismiss()
                        }, { dialog ->
                            chaptersPagerActivity.removeFromDataSet(webPage)
                            adapter.notifyItemChanged(adapter.items.indexOf(webPage))
                            dialog.dismiss()
                        })
                }
                //If Novel is already in library
                else {
                    if (isChecked)
                        chaptersPagerActivity.addToDataSet(webPage)
                    else
                        chaptersPagerActivity.removeFromDataSet(webPage)
                }
            }
            chaptersMainFragment != null -> {
                // If Novel is not in Library
                if (novel.id == -1L) {
                    if (isChecked)
                        chaptersMainFragment.confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), { dialog ->
                            chaptersMainFragment.viewModel.addNovelToLibrary()
                            requireActivity().invalidateOptionsMenu()
                            chaptersMainFragment.addToDataSet(webPage)
                            dialog.dismiss()
                        }, { dialog ->
                            chaptersMainFragment.removeFromDataSet(webPage)
                            adapter.notifyItemChanged(adapter.items.indexOf(webPage))
                            dialog.dismiss()
                        })
                }
                //If Novel is already in library
                else {
                    if (isChecked)
                        chaptersMainFragment.addToDataSet(webPage)
                    else
                        chaptersMainFragment.removeFromDataSet(webPage)
                }
            }
        }
    }

    //endregion

    /**
     * Check if a chapter is currently being downloaded
     */
    private fun isChapterCurrentlyDownloading(chapterUrl: String): Boolean {
        // Check if the download service is running and this chapter is in the download queue
        val chaptersMainFragment = parentFragment as? ChaptersMainFragment
        return chaptersMainFragment?.isChapterDownloading(chapterUrl) ?: false
    }

    //endregion

    //region Event Bus

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChapterActionModeEvent(chapterActionModeEvent: ChapterActionModeEvent) {
        if (chapterActionModeEvent.eventType == EventType.COMPLETE || (chapterActionModeEvent.eventType == EventType.UPDATE && (chapterActionModeEvent.translatorSourceName == translatorSourceName || translatorSourceName == ALL_TRANSLATOR_SOURCES))) {
            lastKnownRecyclerState = binding.recyclerView.layoutManager?.onSaveInstanceState()
            setData()
            return
        }
        if (chapterActionModeEvent.eventType == EventType.DOWNLOAD) {
            // Update specific chapter item to reflect download progress
            adapter.items.firstOrNull { it.url == chapterActionModeEvent.url }?.let { 
                val position = adapter.items.indexOf(it)
                if (position != -1) {
                    adapter.notifyItemChanged(position)
                }
            }
        }
    }


    //endregion
}