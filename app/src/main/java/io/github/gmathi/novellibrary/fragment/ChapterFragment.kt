package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import co.metalab.asyncawait.async
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.ChaptersNewActivity
import io.github.gmathi.novellibrary.activity.ReaderPagerActivity
import io.github.gmathi.novellibrary.activity.shareUrl
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.addWebPages
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.getWebPages
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.ChapterEvent
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelEvent
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getChapterUrlsForPage
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_chapter_new.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class ChapterFragment : BaseFragment(), GenericAdapter.Listener<WebPage> {

    lateinit var novel: Novel
    var pageNum: Int = 0

    lateinit var adapter: GenericAdapter<WebPage>


    companion object {
        fun newInstance(novel: Novel, pageNum: Int): ChapterFragment {
            val bundle = Bundle()
            bundle.putSerializable("novel", novel)
            bundle.putInt("pageNum", pageNum)
            val fragment = ChapterFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.content_recycler_view, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //(activity as AppCompatActivity).setSupportActionBar(null)
        setRecyclerView()

        if (savedInstanceState != null) {
            novel = savedInstanceState.getSerializable("novel") as Novel
            pageNum = savedInstanceState.getInt("pageNum")
            @Suppress("UNCHECKED_CAST")
            adapter.updateData(savedInstanceState.getSerializable("chapters") as java.util.ArrayList<WebPage>)

        } else {
            novel = arguments.getSerializable("novel") as Novel
            pageNum = arguments.getInt("pageNum")

            if (novel.id != -1L) {
                getChaptersForPageDB()
            }

            if (adapter.items.isEmpty()) {
                progressLayout.showLoading()
            }
            getChaptersForPage()
        }
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = ArrayList<WebPage>(), layoutResId = R.layout.listitem_chapter_new, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(activity, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.setOnRefreshListener { getChaptersForPage() }
    }

    private fun getChaptersForPageDB() {
        val results = dbHelper.getWebPages(novel, pageNum)
        adapter.updateData(results)
        scrollToBookmark()
    }

    private fun getChaptersForPage() {
        if (!Utils.checkNetwork(activity)) {
            if (adapter.items.isEmpty())
                progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.no_internet), getString(R.string.try_again), {
                    progressLayout.showLoading()
                    getChaptersForPage()
                })
            return
        }

        async getChapter@ {
            try {
                val results = await { NovelApi().getChapterUrlsForPage(novel.url!!, pageNum) }
                if (results != null) {

                    if (novel.id != -1L) {
                        await { dbHelper.addWebPages(results, novel, pageNum) }
                        getChaptersForPageDB()
                    } else {
                        adapter.updateData(results)
                    }

                    if (isFragmentActive()) {
                        progressLayout.showContent()
                        swipeRefreshLayout.isRefreshing = false
                    }

                } else {  //If results are null

                    if (isFragmentActive())
                        if (adapter.items.isEmpty())
                            progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                                getChaptersForPage()
                            })
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isFragmentActive())
                    if (adapter.items.isEmpty())
                        progressLayout.showError(ContextCompat.getDrawable(context, R.drawable.ic_warning_white_vector), getString(R.string.failed_to_load_url), getString(R.string.try_again), {
                            getChaptersForPage()
                        })
            }
        }
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()

    override fun onItemClick(item: WebPage) {
        startReaderPagerActivity(novel, item, adapter.items)
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: WebPage, itemView: View, position: Int) {

        if (item.filePath != null) {
            itemView.greenView.visibility = View.VISIBLE
            itemView.greenView.setBackgroundColor(ContextCompat.getColor(activity, R.color.DarkGreen))
        } else {
            if (Constants.STATUS_DOWNLOAD.toString() == item.metaData[Constants.DOWNLOADING]) {
                itemView.greenView.visibility = View.VISIBLE
                itemView.greenView.setBackgroundColor(ContextCompat.getColor(activity, R.color.white))
                itemView.greenView.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.alpha_animation))
            } else
                itemView.greenView.visibility = View.GONE
        }

//        itemView.greenView.visibility = if (item.filePath != null) View.VISIBLE else View.GONE
        itemView.isReadView.visibility = if (item.isRead == 1) View.VISIBLE else View.GONE
        itemView.bookmarkView.visibility = if (item.id != -1L && item.id == novel.currentWebPageId) View.VISIBLE else View.INVISIBLE

        if (item.chapter != null)
            itemView.chapterTitle.text = item.chapter

        if (item.title != null) {
            if ((item.chapter != null) && item.title!!.contains(item.chapter!!))
                itemView.chapterTitle.text = item.title
            else
                itemView.chapterTitle.text = "${item.chapter}: ${item.title}"
        }

        itemView.chapterCheckBox.visibility = if (novel.id != -1L) View.VISIBLE else View.GONE
        itemView.chapterCheckBox.isChecked = (activity as ChaptersNewActivity).updateSet.contains(item)
        itemView.chapterCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                (activity as ChaptersNewActivity).addToUpdateSet(item)
            else
                (activity as ChaptersNewActivity).removeFromUpdateSet(item)
        }

        itemView.setOnLongClickListener {
            if (item.redirectedUrl != null)
                activity.shareUrl(item.redirectedUrl!!)
            else
                activity.shareUrl(item.url!!)
            true
        }
    }

    //endregion

    override fun onDestroy() {
        super.onDestroy()
        async.cancelAll()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        val webPage = event.webPage
        if (webPage != null) {
            adapter.updateItem(webPage)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onChapterEvent(event: ChapterEvent) {
        if (event.novel.id != -1L && novel.id == -1L) {
            novel = event.novel
            getChaptersForPage()
        } else
            adapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.READER_ACT_REQ_CODE) {
            if (novel.id != -1L) {
                novel = dbHelper.getNovel(novel.id)!!
                getChaptersForPageDB()
            }
        }
    }

    private fun scrollToBookmark() {
        if (novel.currentWebPageId != -1L) {
            val index = adapter.items.indexOfFirst { it.id == novel.currentWebPageId }
            if (index != -1)
                recyclerView.scrollToPosition(index)
        }
    }

    fun selectAll() {
        adapter.items.filter { it.id != -1L }.forEach {
            (activity as ChaptersNewActivity).addToUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    fun clearSelection() {
        adapter.items.filter { it.id != -1L }.forEach {
            (activity as ChaptersNewActivity).removeFromUpdateSet(it)
        }
        adapter.notifyDataSetChanged()
    }

    fun startReaderPagerActivity(novel: Novel, webPage: WebPage, chapters: ArrayList<WebPage>?) {
        val intent = Intent(activity, ReaderPagerActivity::class.java)
        val bundle = Bundle()
        bundle.putSerializable("novel", novel)
        bundle.putSerializable("webPage", webPage)
        bundle.putSerializable("chapters", chapters)
        intent.putExtras(bundle)
        this.startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable("chapters", adapter.items)
        outState?.putSerializable("novel", novel)
        outState?.putInt("pageNum", pageNum)
    }
}
