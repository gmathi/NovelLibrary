package com.mgn.bingenovelreader.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.adapters.GenericAdapter
import com.mgn.bingenovelreader.database.*
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.models.DownloadQueue
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.services.DownloadService
import com.mgn.bingenovelreader.utils.Constants
import com.mgn.bingenovelreader.utils.Util
import com.mgn.bingenovelreader.utils.setDefaults
import kotlinx.android.synthetic.main.activity_download_queue.*
import kotlinx.android.synthetic.main.content_download_queue.*
import kotlinx.android.synthetic.main.listitem_download_queue.view.*
import org.jetbrains.anko.Bold
import org.jetbrains.anko.alert
import org.jetbrains.anko.append
import org.jetbrains.anko.buildSpanned
import java.io.File

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

        if (DownloadService.IS_DOWNLOADING) {
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
                    dbHelper.updateAllDownloadQueueStatuses(Constants.STATUS_STOPPED)
                    fab.setImageResource(R.drawable.ic_play_arrow_black_vector)
                    fab.tag = "paused"
                } else if (fab.tag == "paused") {
                    //play all
                    dbHelper.updateAllDownloadQueueStatuses(Constants.STATUS_DOWNLOAD)
                    fab.setImageResource(R.drawable.ic_pause_black_vector)
                    fab.tag = "playing"
                    startDownloadService(adapter.items[0].novelId)
                }
                adapter.updateData(ArrayList(dbHelper.getAllDownloadQueue()))
            }
        }
    }

    private fun setRecyclerView() {
        val items = dbHelper.getAllDownloadQueue()
        adapter = GenericAdapter(items = ArrayList(items), layoutResId = R.layout.listitem_download_queue, listener = this)
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

    //region Adapter Listener Methods - onItemClick(), viewBinder()
    @SuppressLint("SetTextI18n")
    override fun bind(item: DownloadQueue, itemView: View) {
        val novel = dbHelper.getNovel(item.novelId)
        itemView.novelName.text = novel?.name
        if (novel?.imageFilePath != null)
            Glide.with(this).load(File(novel.imageFilePath)).into(itemView.novelIcon)



        if (item.status.toInt() == Constants.STATUS_DOWNLOAD) {
            itemView.downloadPauseButton.setImageResource(R.drawable.ic_pause_black_vector)
        } else {
            itemView.downloadPauseButton.setImageResource(R.drawable.ic_play_arrow_black_vector)
        }
        val totalChapterCount = dbHelper.getAllWebPages(novelId = novel?.id!!).count()
        if (totalChapterCount != 0) {
            val displayText = "${dbHelper.getAllReadableWebPages(novel.id).count()}/$totalChapterCount"
            itemView.downloadProgress.text = displayText
        } else {
            itemView.downloadProgress.text = "Waiting to download…"
        }

        val preText = if (DownloadService.NOVEL_ID != novel.id) "In Queue - " else "Downloading: "
        val displayText = preText + itemView.downloadProgress.text.toString()
        itemView.downloadProgress.text = displayText

        itemView.downloadPauseButton.setOnClickListener {
            if (item.status.toInt() == Constants.STATUS_DOWNLOAD) {
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_STOPPED.toLong(), item.novelId)
                itemView.downloadPauseButton.setImageResource(R.drawable.ic_play_arrow_black_vector)
            } else if (item.status.toInt() == Constants.STATUS_STOPPED) {
                dbHelper.updateDownloadQueueStatus(Constants.STATUS_DOWNLOAD.toLong(), item.novelId)
                itemView.downloadPauseButton.setImageResource(R.drawable.ic_pause_black_vector)
                startDownloadService(item.novelId)
            }
        }

        itemView.downloadDeleteButton.setOnClickListener {
            confirmDeleteAlert(novel)
        }
    }

    override fun onItemClick(item: DownloadQueue) {
        // Do Nothing
        //        val serviceIntent = Intent(this, DownloadService::class.java)
        //        startService(serviceIntent)
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
//            dq.currentChapter = intent.extras.getInt(Constants.CURRENT_CHAPTER_COUNT).toLong()
//            dq.totalChapters = intent.extras.getInt(Constants.TOTAL_CHAPTERS_COUNT).toLong()
            val dq = dbHelper.getDownloadQueue(intent.extras.getLong(Constants.NOVEL_ID))
            if (dq != null)
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


    private fun confirmDeleteAlert(novel: Novel) {
        //TODO: Need to make the text spannable
        alert(buildSpanned {
            append("Delete ")
            append("${novel.name}", Bold)
            append("?")
        }.toString(), "Confirm Delete") {
            positiveButton("Yesh~") { deleteNovel(novel) }
            negativeButton("Never Mind!") { }
        }.show()
    }

    private fun deleteNovel(novel: Novel) {
        Util.deleteNovel(this, novel)
        setResult(Constants.DOWNLOAD_QUEUE_ACT_RES_CODE, Intent())
        val index = adapter.items.indexOfFirst { it.novelId == novel.id }
        if (index != -1) adapter.removeItemAt(index)
    }

}
