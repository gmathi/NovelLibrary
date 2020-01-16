package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import co.metalab.asyncawait.async
import com.afollestad.materialdialogs.MaterialDialog
import com.hanks.library.AnimateCheckBox
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersPagerActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapterSelectTitleProvider
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.startReaderDBPagerActivity
import io.github.gmathi.novellibrary.extensions.startWebViewActivity
import io.github.gmathi.novellibrary.model.*
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
            bundle.putParcelable(NOVEL, novel)
            bundle.putLong(SOURCE_ID, sourceId)
            val fragment = ChaptersFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    lateinit var novel: Novel
    lateinit var adapter: GenericAdapterSelectTitleProvider<WebPage>

    private var sourceId: Long = -1L
    private var isSortedAsc: Boolean = true

    private var counter = 0
    private var lastKnownRecyclerState: Parcelable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_source_chapters, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        novel = arguments!!.getParcelable(NOVEL)!!
        sourceId = arguments!!.getLong(SOURCE_ID)

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
            val chapters = if (sourceId == -1L) chaptersPagerActivity.chapters else chaptersPagerActivity.chapters.filter { it.sourceId == sourceId }
            if (!chapters.isEmpty()) {
                adapter.updateData(if (isSortedAsc) ArrayList(chapters) else ArrayList(chapters.reversed()))
                progressLayout.showContent()
                if (shouldScrollToBookmark)
                    scrollToBookmark()
                else if (shouldScrollToFirstUnread)
                    scrollToFirstUnread(chaptersPagerActivity.chaptersSettings)
                if (chaptersPagerActivity.dataSet.isNotEmpty()) {
                    lastKnownRecyclerState?.let { recyclerView.layoutManager?.onRestoreInstanceState(it) }
                }
            } else
                progressLayout.showEmpty(ContextCompat.getDrawable(chaptersPagerActivity, R.drawable.ic_warning_white_vector), "No Chapters Found!")
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
            val index =
            if (isSortedAsc)
                adapter.items.indexOfFirst { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
            else
                adapter.items.indexOfLast { chapter -> chaptersSettings.firstOrNull { it.url == chapter.url && it.isRead == 0 } != null }
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

        val webPageSettings = (activity as? ChaptersPagerActivity)?.chaptersSettings?.firstOrNull { it.url == item.url }

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
            itemView.availableOfflineImageView.visibility = View.INVISIBLE
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
    }

    override fun onCheckedChanged(buttonView: View?, isChecked: Boolean) {
        val webPage = (buttonView?.tag as WebPage?) ?: return
        val chaptersPagerActivity = (activity as? ChaptersPagerActivity) ?: return

        // If Novel is not in Library
        if (novel.id == -1L) {
            if (isChecked)
                chaptersPagerActivity.confirmDialog(getString(R.string.add_to_library_dialog_content, novel.name), MaterialDialog.SingleButtonCallback { dialog, _ ->
                    chaptersPagerActivity.addNovelToLibrary()
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

    //region Options Menu

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_fragment_chapters, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sort -> {
                isSortedAsc = !isSortedAsc
                setData(shouldScrollToBookmark = false)
                //checkData()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    private fun checkData() {
//        counter++
//        if (counter >= 20 && dataCenter.lockRoyalRoad) {
//            dataCenter.lockRoyalRoad = false
//            Toast.makeText(activity, "You have unlocked a new source in search!", Toast.LENGTH_SHORT).show()
//        }
//    }

    //endregion

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }


}