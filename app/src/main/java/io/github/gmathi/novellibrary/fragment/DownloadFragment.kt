package io.github.gmathi.novellibrary.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.activity.startDownloadService
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.deleteDownload
import io.github.gmathi.novellibrary.database.getAllDownloads
import io.github.gmathi.novellibrary.database.updateDownloadStatus
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Download
import io.github.gmathi.novellibrary.model.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.EventType
import io.github.gmathi.novellibrary.service.download.DownloadService
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.setDefaultsNoAnimation
import kotlinx.android.synthetic.main.activity_download_queue.*
import kotlinx.android.synthetic.main.content_download_queue.*
import kotlinx.android.synthetic.main.listitem_download_queue.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class DownloadFragment : BaseFragment(), GenericAdapter.Listener<Download> {

    lateinit var adapter: GenericAdapter<Download>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.activity_download_queue, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        toolbar.title = getString(R.string.title_downloads)
        setRecyclerView()
        activity?.let {
            (it as NavDrawerActivity).setToolbar(toolbar)

            if (Utils.isServiceRunning(it, DownloadService.QUALIFIED_NAME)) {
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
                        it.stopService(Intent(it, DownloadService::class.java))
                        fab.setImageResource(R.drawable.ic_play_arrow_white_vector)
                        fab.tag = "paused"
                    } else if (fab.tag == "paused") {
                        //play all
                        it.startService(Intent(it, DownloadService::class.java))
                        fab.setImageResource(R.drawable.ic_pause_white_vector)
                        fab.tag = "playing"
                    }
                }
            }
        }
    }


    private fun setRecyclerView() {
        val items = dbHelper.getAllDownloads()
        adapter = GenericAdapter(ArrayList(items), R.layout.listitem_download_queue, this)
        recyclerView.setDefaultsNoAnimation(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(context, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
    }

    //region Adapter Listener Methods - onItemClick(), viewBinder()
    @SuppressLint("SetTextI18n")
    override fun bind(item: Download, itemView: View, position: Int) {

        itemView.title.text = item.novelName

        when {
            item.status == Download.STATUS_IN_QUEUE -> itemView.downloadPauseButton.setImageResource(R.drawable.ic_pause_white_vector)
            item.status == Download.STATUS_PAUSED -> itemView.downloadPauseButton.setImageResource(R.drawable.ic_play_arrow_white_vector)
            item.status == Download.STATUS_RUNNING -> itemView.downloadPauseButton.setImageResource(R.drawable.ic_cloud_download_white_vector)
        }


        itemView.subtitle.text = item.chapter

        itemView.downloadPauseButton.setOnClickListener {

            when {
                item.status == Download.STATUS_IN_QUEUE -> {
                    itemView.downloadPauseButton.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    item.status = Download.STATUS_PAUSED
                    dbHelper.updateDownloadStatus(Download.STATUS_PAUSED, item.webPageId)
                }
                item.status == Download.STATUS_PAUSED -> {
                    itemView.downloadPauseButton.setImageResource(R.drawable.ic_pause_white_vector)
                    item.status = Download.STATUS_IN_QUEUE
                    if (!Utils.isServiceRunning(activity!!, DownloadService.QUALIFIED_NAME))
                        activity?.startDownloadService()
                }
            //item.status == Download.STATUS_RUNNING ->
            //Do Nothing
            //itemView.downloadPauseButton.setImageResource(R.drawable.ic_cloud_download_white_vector)
            }
        }

        itemView.downloadDeleteButton.setOnClickListener {
            dbHelper.deleteDownload(item.webPageId)
            adapter.removeItem(item)
            //confirmDeleteAlert(item)
        }
    }

    override fun onItemClick(item: Download) {

    }
    //endregion

//    private fun confirmDeleteAlert(novel: Novel) {
//        activity?.let {
//            MaterialDialog.Builder(it)
//                .content(getString(R.string.remove_from_downloads))
//                .positiveText(getString(R.string.remove))
//                .negativeText(getString(R.string.cancel))
//                .icon(ContextCompat.getDrawable(it, R.drawable.ic_delete_white_vector)!!)
//                .typeface("source_sans_pro_regular.ttf", "source_sans_pro_regular.ttf")
//                .theme(Theme.DARK)
//                .onPositive { _, _ -> deleteNovel(novel) }
//                .show()
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDownloadEvent(webPageEvent: DownloadWebPageEvent) {
        if (webPageEvent.type == EventType.RUNNING) {
            adapter.items.firstOrNull { it.webPageId == webPageEvent.webPageId }?.status = Download.STATUS_RUNNING
            adapter.items.firstOrNull { it.webPageId == webPageEvent.webPageId }?.let {
                it.status = Download.STATUS_RUNNING
                adapter.updateItem(it)
            }
        }
        if (webPageEvent.type == EventType.COMPLETE) {
            adapter.removeItem(webPageEvent.download)
        }
    }
}
