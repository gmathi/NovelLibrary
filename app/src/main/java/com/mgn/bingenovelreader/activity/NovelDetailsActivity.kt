package com.mgn.bingenovelreader.activities

import android.content.BroadcastReceiver
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.activity.ReaderPagerActivity
import com.mgn.bingenovelreader.adapter.GenericAdapter
import com.mgn.bingenovelreader.database.getAllReadableWebPages
import com.mgn.bingenovelreader.database.getDownloadQueue
import com.mgn.bingenovelreader.database.getNovel
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.event.NovelEvent
import com.mgn.bingenovelreader.model.Novel
import com.mgn.bingenovelreader.model.WebPage
import com.mgn.bingenovelreader.util.*
import kotlinx.android.synthetic.main.activity_novel_details.*
import kotlinx.android.synthetic.main.content_novel_details.*
import kotlinx.android.synthetic.main.listitem_novel_details.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.Bold
import org.jetbrains.anko.alert
import org.jetbrains.anko.append
import org.jetbrains.anko.buildSpanned


class NovelDetailsActivity : AppCompatActivity(), GenericAdapter.Listener<WebPage> {

    var novel: Novel? = null
    lateinit var adapter: GenericAdapter<WebPage>
    lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novel_details)

        //Get Data From Intent & Database
        var chapters: ArrayList<WebPage> = ArrayList()
        val novelId = intent.getLongExtra(Constants.NOVEL_ID, -1L)
        if (novelId != -1L) novel = dbHelper.getNovel(novelId)
        if (novel != null) {
            chapters = ArrayList(dbHelper.getAllReadableWebPages(novelId))
        } else
            finish()

        toolbar.title = novel?.name
        setRecyclerView(chapters)
        fab.setOnClickListener { confirmDeleteAlert() }
        val dq = dbHelper.getDownloadQueue(novel!!.id)
        setLoadingView(R.drawable.no_chapters, if (dq != null) "Downloading… [{-_-}] ZZZzz zz z..." else "There are no chapters!! (╥﹏╥)")
        enableLoadingView(chapters.isEmpty(), recyclerView)
    }

    fun setRecyclerView(chapters: ArrayList<WebPage>) {
        adapter = GenericAdapter(items = chapters, layoutResId = R.layout.listitem_novel_details, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view);
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state);
                }
            }
        })

    }

    override fun bind(item: WebPage, itemView: View) {
        if (novel!!.currentWebPageId < item.id!!) {
            itemView.novelDetailsOverlay.visibility = View.INVISIBLE
            itemView.novelDetailsIcon.visibility = View.GONE
        } else {
            itemView.novelDetailsOverlay.visibility = View.VISIBLE
            itemView.novelDetailsIcon.visibility = View.VISIBLE
            itemView.novelDetailsIcon.setImageResource(R.drawable.ic_remove_red_eye_black_vector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.novelDetailsIcon.drawable.mutate().setTint(ContextCompat.getColor(this, R.color.DimGray))
            }
        }
        if (novel!!.currentWebPageId == item.id!!) {
            itemView.novelDetailsOverlay.visibility = View.INVISIBLE
            itemView.novelDetailsIcon.visibility = View.VISIBLE
            itemView.novelDetailsIcon.setImageResource(R.drawable.ic_bookmark_black_vector)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                itemView.novelDetailsIcon.drawable.mutate().setTint(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            }
        }

        itemView.novelDetailsTitle.text = item.chapter + ": " + item.title
        itemView.novelDetailsTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.toFloat())
    }

    override fun onItemClick(item: WebPage) {
        toast("${item.chapter} Clicked")
        startReaderActivity(item)
    }

    private fun startReaderActivity(webPage: WebPage) {
        val intent = Intent(this, ReaderPagerActivity::class.java)
        intent.putExtra(Constants.NOVEL_ID, webPage.novelId)
        intent.putExtra(Constants.WEB_PAGE_ID, webPage.id)
        startActivityForResult(intent, Constants.READER_ACT_REQ_CODE)
    }

    private fun confirmDeleteAlert() {
        //TODO: Need to make the text spannable
        alert(buildSpanned {
            append("Delete ")
            append("${novel?.name}", Bold)
            append("?")
        }.toString(), "Confirm Delete") {
            positiveButton("Yesh~") { deleteNovel() }
            negativeButton("Never Mind!") { }
        }.show()
    }

    private fun deleteNovel() {
        Util.deleteNovel(this, novel)
        val intent = Intent()
        val bundle = Bundle()
        bundle.putLong(Constants.NOVEL_ID, novel!!.id)
        intent.putExtras(bundle)
        setResult(Constants.NOVEL_DETAILS_RES_CODE, intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        scrollToBookmark()
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        val novelId = intent.extras.getLong(Constants.NOVEL_ID)
        if (novelId == -1L || novelId != novel?.id) return
        val newData = ArrayList(dbHelper.getAllReadableWebPages(novelId))
        adapter.updateData(newData)
        enableLoadingView(adapter.items.isEmpty(), recyclerView)
    }

    /**
     *  Return from the reader activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.READER_ACT_REQ_CODE) {
            novel = dbHelper.getNovel(novel!!.id)
            val chapters = ArrayList(dbHelper.getAllReadableWebPages(novel!!.id))
            adapter.updateData(chapters)
            scrollToBookmark()
        }
    }

    /**
     * Scrolls the recyclerView to the current bookmark
     */
    private fun scrollToBookmark() {
        val index = adapter.items.indexOfFirst { it.id == novel?.currentWebPageId }
        if (index != -1)
            recyclerView.scrollToPosition(index)
    }


}
