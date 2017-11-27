package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.getDownloadNovelNames
import io.github.gmathi.novellibrary.database.getDownloadedChapterCount
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.DownloadEvent
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.service.download.DownloadService
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_novel_downloads.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_download_queue_old.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class NovelDownloadsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        private val TAG = "NovelDownloadsActivity"
    }

    lateinit var adapter: GenericAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_novel_downloads)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setFab()
        setRecyclerView()
    }

    private fun setFab() {
        if (Utils.isServiceRunning(this, DownloadService.QUALIFIED_NAME)) {
            fab.setImageResource(R.drawable.ic_pause_white_vector)
            fab.tag = "playing"
        } else {
            fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
            fab.tag = "paused"
        }

        fab.setOnClickListener { _ ->
            if (!adapter.items.isEmpty()) {
                if (fab.tag == "playing") {
                    //pause all
                    stopService(Intent(this, DownloadService::class.java))
                    fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    fab.tag = "paused"
                } else if (fab.tag == "paused") {
                    //play all
                    startService(Intent(this, DownloadService::class.java))
                    fab.setImageResource(R.drawable.ic_pause_white_vector)
                    fab.tag = "playing"
                }
            }
        }
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = dbHelper.getDownloadNovelNames() as ArrayList<String>, layoutResId = R.layout.listitem_download_queue_old, listener = this)
        recyclerView.setDefaultsNoAnimation(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: String, itemView: View, position: Int) {
        val novel = dbHelper.getNovel(item)
        if (novel?.imageUrl != null) {
            Glide.with(this)
                .load(novel.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.novelImageView)
        }
        itemView.novelTitleTextView.text = item
        val downloadedPages = dbHelper.getDownloadedChapterCount(novel!!.id)
        itemView.novelProgressText.text = "$downloadedPages / ${novel.newChapterCount}"


    }

    override fun onItemClick(item: String) {

    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadEvent(downloadEvent: DownloadEvent) {
        if (downloadEvent.type == EventType.COMPLETE) {
            adapter.updateItem(downloadEvent.download.novelName)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }


}
