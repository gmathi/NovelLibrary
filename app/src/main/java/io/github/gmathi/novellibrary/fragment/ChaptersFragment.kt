package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.databinding.FragmentSourceChaptersBinding
import io.github.gmathi.novellibrary.databinding.ListitemChapterBinding
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.ALL_TRANSLATOR_SOURCES
import io.github.gmathi.novellibrary.extensions.setDefaultsNoAnimation
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startWebViewActivity
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersFragment : BaseFragment(),
    GenericAdapterSelectTitleProvider.Listener<WebPage>,
    AnimateCheckBox.OnCheckedChangeListener {

    companion object {

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

    lateinit var novel: Novel
    private lateinit var translatorSourceName: String
    lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>

    private var lastKnownRecyclerState: Parcelable? = null

    private lateinit var binding: FragmentSourceChaptersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = inflater.inflate(R.layout.fragment_source_chapters, container, false) ?: return null
        binding = FragmentSourceChaptersBinding.bind(result)
        return result
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = requireArguments().getParcelable<Novel>(NOVEL) as Novel
        translatorSourceName = requireArguments().getString(TRANSLATOR_SOURCE_NAME) ?: ALL_TRANSLATOR_SOURCES
        binding.progressLayout.showLoading()
        setRecyclerView()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterSelectTitleProvider(items = ArrayList(), layoutResId = R.layout.listitem_chapter, listener = this)
        binding.recyclerView.isVerticalScrollBarEnabled = true
        binding.recyclerView.setDefaultsNoAnimation(adapter)
        this.context?.let { binding.recyclerView.addItemDecoration(CustomDividerItemDecoration(it, DividerItemDecoration.VERTICAL)) }
        binding.fastScrollView.setRecyclerView(binding.recyclerView)
        binding.swipeRefreshLayout.isEnabled = false
    }

    private fun setData(shouldScrollToBookmark: Boolean = true, shouldScrollToFirstUnread: Boolean = true) {
        (activity as? ChaptersPagerActivity)?.let { activity ->

            val chapters = (
                    if (translatorSourceName == ALL_TRANSLATOR_SOURCES)
                        activity.vm.chapters
                    else
                        activity.vm.chapters?.filter { it.translatorSourceName == translatorSourceName }
                    ) ?: ArrayList()

            if (chapters.isNotEmpty()) {
                adapter.updateData(if (novel.metadata["chapterOrder"] == "des") ArrayList(chapters.reversed()) else ArrayList(chapters))
                binding.progressLayout.showContent()
                if (shouldScrollToBookmark)
                    scrollToBookmark()
                else if (shouldScrollToFirstUnread)
                    scrollToFirstUnread(activity.vm.chapterSettings ?: throw Error("Invalid Chapter Settings"))
                if (activity.dataSet.isNotEmpty()) {
                    lastKnownRecyclerState?.let { binding.recyclerView.layoutManager?.onRestoreInstanceState(it) }
                }
            } else
                binding.progressLayout.showEmpty(emptyText = "No Chapters Found!")
        }
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
            novel.currentChapterUrl = item.url
            dbHelper.updateNovel(novel)
            NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncBookmarks(it.host)) it.setBookmark(novel, item) }
            startReaderDBPagerActivity(novel, translatorSourceName)
        } else
            startWebViewActivity(item.url)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {
        val itemBinding = ListitemChapterBinding.bind(itemView)

        val webPageSettings = (activity as? ChaptersPagerActivity)?.vm?.chapterSettings?.firstOrNull { it.url == item.url }

        if (webPageSettings?.filePath != null) {
            itemBinding.availableOfflineImageView.visibility = View.VISIBLE
            itemBinding.availableOfflineImageView.animation = null
        } else {
//            if (Download.STATUS_IN_QUEUE.toString() == webPageSettings?.metadata[Constants.DOWNLOADING]) {
//                if (item.id != -1L && DownloadService.chapters.contains(item)) {
//                    itemView.greenView.visibility = View.VISIBLE
//                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@OldChaptersActivity, R.color.white))
//                    itemView.greenView.startAnimation(AnimationUtils.loadAnimation(this@OldChaptersActivity, R.anim.alpha_animation))
//                } else {
//                    itemView.greenView.visibility = View.VISIBLE
//                    itemView.greenView.setBackgroundColor(ContextCompat.getColor(this@OldChaptersActivity, R.color.Red))
//                    itemView.greenView.animation = null
//                }
//            } else
            itemBinding.availableOfflineImageView.visibility = View.GONE
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

        itemBinding.chapterCheckBox.isChecked = (activity as? ChaptersPagerActivity)?.dataSet?.contains(item) ?: false
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

    override fun onCheckedChanged(buttonView: View?, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return
        val chaptersPagerActivity = (activity as? ChaptersPagerActivity) ?: return

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
    fun onDownloadEvent(webPageEvent: DownloadWebPageEvent) {
        if (webPageEvent.download.novelName == novel.name) {
            adapter.items.firstOrNull { it.url == webPageEvent.webPageUrl }?.let { adapter.updateItem(it) }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChapterActionModeEvent(chapterActionModeEvent: ChapterActionModeEvent) {
        if (chapterActionModeEvent.eventType == EventType.COMPLETE || (chapterActionModeEvent.eventType == EventType.UPDATE && (chapterActionModeEvent.translatorSourceName == translatorSourceName || translatorSourceName == ALL_TRANSLATOR_SOURCES))) {
            lastKnownRecyclerState = binding.recyclerView.layoutManager?.onSaveInstanceState()
            setData()
        }
    }

    //endregion
}