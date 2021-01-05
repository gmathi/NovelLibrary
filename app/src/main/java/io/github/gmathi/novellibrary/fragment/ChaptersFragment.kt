package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.DialogCallback
import com.afollestad.materialdialogs.MaterialDialog
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.databinding.FragmentSourceChaptersBinding
import io.github.gmathi.novellibrary.databinding.ListitemChapterBinding
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.extensions.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.ChapterActionModeEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import io.github.gmathi.novellibrary.util.system.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.util.system.startWebViewActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChaptersFragment : BaseFragment(),
    GenericAdapterSelectTitleProvider.Listener<WebPage>,
    AnimateCheckBox.OnCheckedChangeListener {

    companion object {

        private const val NOVEL = "novel"
        private const val SOURCE_ID = "sourceId"

        fun newInstance(novel: Novel, sourceId: Long): ChaptersFragment {
            val bundle = Bundle()
            bundle.putSerializable(NOVEL, novel)
            bundle.putLong(SOURCE_ID, sourceId)
            val fragment = ChaptersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var novel: Novel
    lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>

    private var sourceId: Long = -1L
    private var lastKnownRecyclerState: Parcelable? = null
    
    private lateinit var binding: FragmentSourceChaptersBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result = inflater.inflate(R.layout.fragment_source_chapters, container, false) ?: return null
        binding = FragmentSourceChaptersBinding.bind(result)
        return result
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = requireArguments().getSerializable(NOVEL) as Novel
        sourceId = requireArguments().getLong(SOURCE_ID)

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
        val chaptersPagerActivity = activity as? ChaptersPagerActivity
        if (chaptersPagerActivity != null) {
            val chapters = (if (sourceId == -1L) chaptersPagerActivity.vm.chapters else chaptersPagerActivity.vm.chapters?.filter { it.translatorSourceId == sourceId }) ?: ArrayList<WebPage>()
            if (chapters.isNotEmpty()) {
                adapter.updateData(if (novel.metadata["chapterOrder"] == "des") ArrayList(chapters.reversed()) else ArrayList(chapters))
                binding.progressLayout.showContent()
                if (shouldScrollToBookmark)
                    scrollToBookmark()
                else if (shouldScrollToFirstUnread)
                    scrollToFirstUnread(chaptersPagerActivity.vm.chapterSettings ?: throw Error("Invalid Chapter Settings"))
                if (chaptersPagerActivity.dataSet.isNotEmpty()) {
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
                adapter.items.indexOfLast { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
            else
                adapter.items.indexOfFirst { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
            if (index != -1)
                binding.recyclerView.scrollToPosition(index)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun getSectionTitle(position: Int): String {
        return adapter.items[position].chapter
    }

    override fun onItemClick(item: WebPage, position: Int) {
        if (novel.id != -1L) {
            novel.currentChapterUrl = item.url
            db.novelDao().update(novel)
            NovelSync.getInstance(novel)?.applyAsync(lifecycleScope) { if (dataCenter.getSyncBookmarks(it.host)) it.setBookmark(novel, item) }
            startReaderDBPagerActivity(novel, sourceId)
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

        itemBinding.isReadView.visibility = if (webPageSettings?.isRead == 1) View.VISIBLE else View.GONE
        itemBinding.bookmarkView.visibility = if (item.url == novel.currentChapterUrl) View.VISIBLE else View.GONE

        itemBinding.chapterTitle.text = item.chapter

        webPageSettings?.title?.let {
            if (it.contains(item.chapter))
                itemBinding.chapterTitle.text = it
            else
                itemBinding.chapterTitle.text = "${item.chapter}: $it"
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
                    adapter.notifyDataSetChanged()
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
        if (chapterActionModeEvent.eventType == EventType.COMPLETE || (chapterActionModeEvent.eventType == EventType.UPDATE && (chapterActionModeEvent.sourceId == sourceId || sourceId == -1L))) {
            lastKnownRecyclerState = binding.recyclerView.layoutManager?.onSaveInstanceState()
            setData()
        }
    }

    //endregion
}