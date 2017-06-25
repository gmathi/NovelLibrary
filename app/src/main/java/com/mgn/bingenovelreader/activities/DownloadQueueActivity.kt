package com.mgn.bingenovelreader.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.database.getAllDownloadQueue
import com.mgn.bingenovelreader.database.getNovel
import com.mgn.bingenovelreader.database.updateDownloadQueueStatus
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.models.DownloadQueue
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.services.DownloadService
import com.mgn.bingenovelreader.utils.Constants
import com.mgn.bingenovelreader.utils.setDefaults
import kotlinx.android.synthetic.main.activity_download_queue.*
import kotlinx.android.synthetic.main.content_download_queue.*
import kotlinx.android.synthetic.main.listitem_string.view.*

class DownloadQueueActivity : AppCompatActivity(), GenericAdapter.Listener<DownloadQueue> {

    lateinit var adapter: GenericAdapter<DownloadQueue>
    lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_queue)
        setSupportActionBar(toolbar)
        setRecyclerView()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    manageBroadcasts(intent)
                }
            }
        }

        if (dbHelper.getFirstDownloadableQueueItem() != null) {
            fab.setImageResource(R.drawable.ic_pause_black_vector)
            fab.tag = "playing"
        } else {
            fab.setImageResource(R.drawable.ic_play_arrow_black_vector)
            fab.tag = "paused"
        }

        fab.setOnClickListener {
            if (!adapter.items.isEmpty()) {
                if (fab.tag == "playing") {
                    //pause all
                    dbHelper.updateDownloadQueueStatus(Constants.STATUS_STOPPED)
                    fab.setImageResource(R.drawable.ic_play_arrow_black_vector)
                    fab.tag = "paused"
                } else if (fab.tag == "paused") {
                    //play all
                    dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD)
                    fab.setImageResource(R.drawable.ic_pause_black_vector)
                    fab.tag = "playing"
                    startDownloadService(adapter.items[0].novelId)
                }
            }
        }
    }

    private fun setRecyclerView() {
        val items = dbHelper.getAllDownloadQueue()
        adapter = GenericAdapter(items = ArrayList(items), layoutResId = R.layout.listitem_string, listener = this)
        downloadQueueRecyclerView.setDefaults(adapter)
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()
    @SuppressLint("SetTextI18n")
    override fun bind(item: DownloadQueue, itemView: View) {
        val novel = dbHelper.getNovel(item.novelId) as Novel?
        var chapterCountText = "(Waiting to download)"
        if (item.totalChapters.toInt() != -1)
            chapterCountText = "(${item.currentChapter}/${item.totalChapters})"
        itemView.listItemTitle.text = "${novel?.name} $chapterCountText"
    }

    override fun onItemClick(item: DownloadQueue) {
        //Do Nothing
        val serviceIntent = Intent(this, DownloadService::class.java)
        startService(serviceIntent)
    }
    //endregion

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    fun registerReceiver() {
        val filter = IntentFilter()
        filter.addAction(Constants.DOWNLOAD_QUEUE_NOVEL_UPDATE)
        filter.addAction(Constants.DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE)
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiver, filter)
    }

    private fun manageBroadcasts(intent: Intent) {
        if (intent.action == Constants.DOWNLOAD_QUEUE_NOVEL_UPDATE) {
            val dq = DownloadQueue()
            dq.novelId = intent.extras.getLong(Constants.NOVEL_ID)
            dq.currentChapter = intent.extras.getInt(Constants.CURRENT_CHAPTER_COUNT).toLong()
            dq.totalChapters = intent.extras.getInt(Constants.TOTAL_CHAPTERS_COUNT).toLong()
            adapter.updateItem(dq)

        } else if (intent.action == Constants.DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE) {
            val dq = DownloadQueue()
            dq.novelId = intent.extras.getLong(Constants.NOVEL_ID)
            adapter.removeItem(dq)
        }
    }

    private fun startDownloadService(novelId: Long) {
        val serviceIntent = Intent(this, DownloadService::class.java)
        serviceIntent.putExtra(Constants.NOVEL_ID, novelId)
        startService(serviceIntent)
    }

}
