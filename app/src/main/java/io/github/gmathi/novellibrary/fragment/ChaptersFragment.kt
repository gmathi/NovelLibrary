chpackage io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.showEmpty
import io.github.gmathi.novellibrary.extensions.showLoading
import io.github.gmathi.novellibrary.extensions.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.extensions.startWebViewActivity
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.fragment_source_chapters.*
import kotlinx.android.synthetic.main.listitem_chapter.view.*
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_source_chapters, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = requireArguments().getSerializable(NOVEL) as Novel
        sourceId = requireArguments().getLong(SOURCE_ID)

        progressLayout.showLoading()
        setRecyclerView()
        setData()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapterSelectTitleProvider(items = ArrayList(), layoutResId = R.layout.listitem_chapter, listener = this)
        recyclerView.isVerticalScrollBarEnabled = true
        recyclerView.setDefaultsNoAnimation(adapter)
        this.context?.let { recyclerView.addItemDecoration(CustomDividerItemDecoration(it, DividerItemDecoration.VERTICAL)) }
        fastScrollView.setRecyclerView(recyclerView)
        swipeRefreshLayout.isEnabled = false
    }

    private fun setData(shouldScrollToBookmark: Boolean = true, shouldScrollToFirstUnread: Boolean = true) {
        val chaptersPagerActivity = activity as? ChaptersPagerActivity
        if (chaptersPagerActivity != null) {
            val chapters = (if (sourceId == -1L) chaptersPagerActivity.vm.chapters else chaptersPagerActivity.vm.chapters?.filter { it.sourceId == sourceId }) ?: ArrayList<WebPage>()
            if (chapters.isNotEmpty()) {
                adapter.updateData(if (novel.metaData["chapterOrder"] == "des") ArrayList(chapters.reversed()) else ArrayList(chapters))
                progressLayout.showContent()
                if (shouldScrollToBookmark)
                    scrollToBookmark()
                else if (shouldScrollToFirstUnread)
                    scrollToFirstUnread(chaptersPagerActivity.vm.chapterSettings ?: throw Error("Invalid Chapter Settings"))
                if (chaptersPagerActivity.dataSet.isNotEmpty()) {
                    lastKnownRecyclerState?.let { recyclerView.layoutManager?.onRestoreInstanceState(it) }
                }
            } else
                progressLayout.showEmpty(emptyText = "No Chapters Found!")
        }
    }

    private fun scrollToBookmark() {
        if (novel.currentWebPageUrl != null) {
            val index = adapter.items.indexOfFirst { it.url == novel.currentWebPageUrl }
            if (index != -1)
                recyclerView.scrollToPosition(index)
        }
    }

    private fun scrollToFirstUnread(chaptersSettings: ArrayList<WebPageSettings>) {
        if (novel.currentWebPageUrl != null) {
            val index = if (novel.metaData["chapterOrder"] == "des")
                adapter.items.indexOfLast { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
            else
                adapter.items.indexOfFirst { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
            if (index != -1)
                recyclerView.scrollToPosition(index)
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun getSectionTitle(position: Int): String {
        return adapter.items[position].chapter
    }

    override fun onItemClick(item: WebPage, position: Int) {
        if (novel.id != -1L) {
            novel.currentWebPageUrl = item.url
            dbHelper.updateNovel(novel)
            startReaderDBPagerActivity(novel, sourceId)
        } else
            startWebViewActivity(item.url)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {

        val webPageSettings = (activity as? ChaptersPagerActivity)?.vm?.chapterSettings?.firstOrNull { it.url == item.url }

        if (webPageSettings?.filePath != null) {
            itemView.availableOfflineImageView.visibility = View.VISIBLE
            itemView.availableOfflineImageView.animation = null
        } else {
//            if (Download.STATUS_IN_QUEUE.toString() == webPageSettings?.metaData[Constants.DOWNLOADING]) {
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
            itemView.availableOfflineImageView.visibility = View.GONE
        }

        itemView.isReadView.visibility = if (webPageSettings?.isRead == 1) View.VISIBLE else View.GONE
        itemView.bookmarkView.visibility = if (item.url == novel.currentWebPageUrl) View.VISIBLE else View.GONE

        itemView.chapterTitle.text = item.chapter

        webPageSettings?.title?.let {
            if (it.contains(item.chapter))
                itemView.chapterTitle.text = it
            else
                itemView.chapterTitle.text = "${item.chapter}: $it"
        }

        itemView.chapterCheckBox.isChecked = (activity as? ChaptersPagerActivity)?.dataSet?.contains(item) ?: false
        itemView.chapterCheckBox.tag = item
        itemView.chapterCheckBox.setOnCheckedChangeListener(this@ChaptersFragment)


        itemView.setOnLongClickListener {
            itemView.chapterCheckBox.isChecked = true
            true
        }

        if (webPageSettings?.metaData?.get(Constants.MetaDataKeys.IS_FAVORITE)?.toBoolean() == true) {
            itemView.favoriteView.visibility = View.VISIBLE
        } else {
            itemView.favoriteView.visibility = View.GONE
        }
    }

    override fun onCheckedChanged(buttonView: View?, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return
        val chaptersPagerActivity = (activity as? ChaptersPagerActivity) ?: return

        // If Novel is not in Library
        if (novel.id == -1L) {
            if (isChecked)
                chaptersPagerActivity.confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    chaptersPagerActivity.vm.addNovelToLibrary()
                    chaptersPagerActivity.invalidateOptionsMenu()
                    chaptersPagerActivity.addToDataSet(webPage)
                    dialog.dismiss()
                }, MaterialDialog.SingleButtonCallback { dialog, _ ->
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
            lastKnownRecyclerState = recyclerView.layoutManager?.onSaveInstanceState()
            setData()
        }
    }

    //endregion
}