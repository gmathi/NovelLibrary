package io.github.gmathi.novellibrary.activity.downloads

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.activity.startDownloadNovelService
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.*
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getGlideUrl
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_novel_downloads.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_download_queue_old.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

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
        fab.hide()

//        if (Utils.isServiceRunning(this, DownloadService.QUALIFIED_NAME)) {
//            fab.setImageResource(R.drawable.ic_pause_white_vector)
//            fab.tag = "playing"
//        } else {
//            fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
//            fab.tag = "paused"
//        }

//        fab.setOnClickListener { _ ->
//            if (!adapter.items.isEmpty()) {
//                if (fab.tag == "playing") {
//                    //pause all
//                    stopService(Intent(this, DownloadService::class.java))
//                    fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
//                    adapter.notifyDataSetChanged()
//                    fab.tag = "paused"
//                } else if (fab.tag == "paused") {
//                    //play all
//                    startService(Intent(this, DownloadService::class.java))
//                    fab.setImageResource(R.drawable.ic_pause_white_vector)
//                    adapter.notifyDataSetChanged()
//                    fab.tag = "playing"
//                }
//            }
//        }
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = dbHelper.getDownloadNovelNames() as ArrayList<String>, layoutResId = R.layout.listitem_download_queue_old, listener = this)
        recyclerView.setDefaultsNoAnimation(adapter)
//        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {
//
//            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
//                val position = parent?.getChildAdapterPosition(view)
//                if (position == parent?.adapter?.itemCount?.minus(1)) {
//                    outRect?.setEmpty()
//                } else {
//                    super.getItemOffsets(outRect, view, parent, state)
//                }
//            }
//        })
        swipeRefreshLayout.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: String, itemView: View, position: Int) {
        val novel = dbHelper.getNovel(item)
        if (novel?.imageUrl != null) {
            Glide.with(this)
                .load(novel.imageUrl?.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(itemView.novelImageView)
        }
        itemView.novelTitleTextView.text = item
        //val downloadedPages = dbHelper.getDownloadedChapterCount(novel!!.id)


        val isServiceRunning = Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)

        if (dbHelper.hasDownloadsInQueue(item) && isServiceRunning) {
            itemView.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
            itemView.playPauseImage.tag = Download.STATUS_IN_QUEUE
            val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
            itemView.novelProgressText.text = "${getString(R.string.download_in_queue)} - Remaining: $remainingDownloadsCount"
        } else {
            itemView.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
            itemView.playPauseImage.tag = Download.STATUS_PAUSED
            itemView.novelProgressText.text = getString(R.string.download_paused)
        }

        itemView.playPauseImage.setOnClickListener {
            when {
                itemView.playPauseImage.tag == Download.STATUS_PAUSED -> {
                    itemView.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                    itemView.playPauseImage.tag = Download.STATUS_IN_QUEUE
                    dbHelper.updateDownloadStatus(Download.STATUS_IN_QUEUE, item)
                    if (!Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        startDownloadNovelService(item)
                    else {
                        EventBus.getDefault().post(DownloadActionEvent(item, DownloadNovelService.ACTION_START))
                    }
                }

                itemView.playPauseImage.tag == Download.STATUS_IN_QUEUE -> {
                    itemView.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    itemView.playPauseImage.tag = Download.STATUS_PAUSED
                    dbHelper.updateDownloadStatus(Download.STATUS_PAUSED, item)
                    itemView.novelProgressText.text = getString(R.string.download_paused)
                    EventBus.getDefault().post(DownloadActionEvent(item, DownloadNovelService.ACTION_PAUSE))
                }
            }
            //adapter.notifyDataSetChanged()
        }

        itemView.deleteButton.setOnClickListener {
            confirmDeleteDialog(item)
        }
    }

    //Invoke it using --> notifyItemRangeChanged(positionStart, itemCount, payload);
    @SuppressLint("SetTextI18n")
    override fun bind(item: String, itemView: View, position: Int, payloads: MutableList<Any>?) {
        if (payloads == null || payloads.size == 0)
            bind(item, itemView, position)
        else {

            //Update the cells partially
            val downloadEvent = payloads[0]

            if (downloadEvent is DownloadWebPageEvent) {
                if (downloadEvent.download.novelName == item) {
                    val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                    itemView.novelProgressText.text = "Remaining: $remainingDownloadsCount, Current: ${downloadEvent.download.chapter}"
                }
            } else if (downloadEvent is DownloadNovelEvent) {
                if (downloadEvent.novelName == item) {
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (downloadEvent.type) {
                        EventType.PAUSED -> {
                            itemView.novelProgressText.text = getString(R.string.download_paused)
                        }
                        EventType.INSERT -> {
                            val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                            itemView.novelProgressText.text = "${getString(R.string.download_in_queue)} - Remaining: $remainingDownloadsCount"
                        }
                        EventType.RUNNING -> {
                            itemView.novelProgressText.text = "Collecting Novel Information…"
                        }
                    }

                }
            }
        }
    }

    override fun onItemClick(item: String) {

    }

    private fun confirmDeleteDialog(novelName: String) {
        MaterialDialog.Builder(this)
            .title(getString(R.string.confirm_remove))
            .content(getString(R.string.confirm_remove_download_description))
            .positiveText(R.string.remove)
            .negativeText(R.string.cancel)
            .onPositive { dialog, _ ->
                run {
                    dbHelper.deleteDownloads(novelName)
                    adapter.removeItem(novelName)
                    dialog.dismiss()
                }
            }
            .onNegative { dialog, _ ->
                run {
                    dialog.dismiss()
                }
            }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebPageDownloadEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        if (downloadWebPageEvent.type == EventType.COMPLETE) {
            val index = adapter.items.indexOf(downloadWebPageEvent.download.novelName)
            adapter.notifyItemRangeChanged(index, 1, downloadWebPageEvent)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelDownloadEvent(downloadNovelEvent: DownloadNovelEvent) {

        @Suppress("NON_EXHAUSTIVE_WHEN")
        when (downloadNovelEvent.type) {
            EventType.INSERT, EventType.RUNNING, EventType.PAUSED -> {
                val index = adapter.items.indexOf(downloadNovelEvent.novelName)
                adapter.notifyItemRangeChanged(index, 1, downloadNovelEvent)
            }

            EventType.DELETE -> {
                adapter.removeItem(downloadNovelEvent.novelName)
            }
        }
    }

//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onChapterServiceEvent(serviceEvent: ServiceEvent) {
//        if (serviceEvent.type == EventType.COMPLETE) {
//            fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
//            adapter.notifyDataSetChanged()
//            fab.tag = "paused"
//        } else if (serviceEvent.type == EventType.RUNNING) {
//            fab.setImageResource(R.drawable.ic_pause_white_vector)
//            adapter.notifyDataSetChanged()
//            fab.tag = "playing"
//        }
//    }


    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }


}
