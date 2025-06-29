package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityNovelDownloadsBinding
import io.github.gmathi.novellibrary.databinding.ListitemDownloadQueueOldBinding
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.ImageLoaderHelper
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService


class NovelDownloadsActivity : BaseActivity(), GenericAdapter.Listener<Long>, DownloadListener {

    companion object {
        private const val TAG = "NovelDownloadsActivity"
    }

    private var downloadNovelService: DownloadNovelService? = null
    private var isServiceConnected: Boolean = false

    private lateinit var binding: ActivityNovelDownloadsBinding

    /** Defines callbacks for service binding, passed to bindService()  */
    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as DownloadNovelService.DownloadNovelBinder
            downloadNovelService = binder.getService()
            downloadNovelService?.downloadListener = this@NovelDownloadsActivity
            isServiceConnected = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
        }
    }

    lateinit var adapter: GenericAdapter<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNovelDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.fab.hide()
        setRecyclerView()
    }

    private fun setRecyclerView() {
        adapter = GenericAdapter(items = dbHelper.getDownloadNovelIds() as ArrayList<Long>, layoutResId = R.layout.listitem_download_queue_old, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaultsNoAnimation(adapter)
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: Long, itemView: View, position: Int) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        val novel = dbHelper.getNovel(novelId = item)
        ImageLoaderHelper.loadRoundedImage(this, binding.novelImageView, novel?.imageUrl, 8f)
        binding.novelTitleTextView.text = novel?.name
        //val downloadedPages = dbHelper.getDownloadedChapterCount(novel!!.id)

        if (dbHelper.hasDownloadsInQueue(item) && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
            //val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
            binding.novelProgressText.text = "Downloading: retrieving status…"
        } else {
            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
            binding.playPauseImage.tag = Download.STATUS_PAUSED
            binding.novelProgressText.text = getString(R.string.download_paused)
        }

        binding.playPauseImage.setOnClickListener {
            when (binding.playPauseImage.tag) {
                Download.STATUS_PAUSED -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                    dbHelper.updateDownloadStatusNovelId(Download.STATUS_IN_QUEUE, item)
                    if (Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
                        downloadNovelService?.handleNovelDownload(item, DownloadNovelService.ACTION_START)
                    } else {
                        startDownloadNovelService(item)
                        bindService()
                    }
                }
                Download.STATUS_IN_QUEUE -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_PAUSED
                    dbHelper.updateDownloadStatusNovelId(Download.STATUS_PAUSED, item)
                    binding.novelProgressText.text = getString(R.string.download_paused)
                    if (Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(item, DownloadNovelService.ACTION_PAUSE)
                }
            }
            //adapter.notifyDataSetChanged()
        }

        binding.deleteButton.setOnClickListener {
            confirmDeleteDialog(item)
        }
    }

    //Invoke it using --> notifyItemRangeChanged(positionStart, itemCount, payload);
    @SuppressLint("SetTextI18n")
    override fun bind(item: Long, itemView: View, position: Int, payloads: MutableList<Any>?) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        if (payloads == null || payloads.size == 0)
            bind(item, itemView, position)
        else {

            //Update the cells partially
            val downloadEvent = payloads[0]

            if (downloadEvent is DownloadWebPageEvent) {
                if (downloadEvent.download.novelId == item && binding.playPauseImage.tag != Download.STATUS_PAUSED) {
                    val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                    binding.novelProgressText.text = "Remaining: $remainingDownloadsCount, Current: ${downloadEvent.download.chapter}"
                }
            } else if (downloadEvent is DownloadNovelEvent) {
                if (downloadEvent.novelId == item) {
                    @Suppress("NON_EXHAUSTIVE_WHEN")
                    when (downloadEvent.type) {
                        EventType.PAUSED -> {
                            binding.novelProgressText.text = getString(R.string.download_paused)
                            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_PAUSED
                        }
                        EventType.INSERT -> {
                            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                            val remainingDownloadsCount = dbHelper.getRemainingDownloadsCountForNovel(item)
                            binding.novelProgressText.text = "${getString(R.string.download_in_queue)} - Remaining: $remainingDownloadsCount"
                        }
                        EventType.RUNNING -> {
                            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                            binding.novelProgressText.text = "Collecting Novel Information…"
                        }
                        else -> {

                        }
                    }

                }
            }
        }
    }

    override fun onItemClick(item: Long, position: Int) {
        //TODO: See the chapters screen
    }

    private fun confirmDeleteDialog(novelId: Long) {
        MaterialDialog(this).show {
            title(R.string.confirm_remove)
            message(R.string.confirm_remove_download_description)
            positiveButton(R.string.remove) { dialog ->
                this@NovelDownloadsActivity.run {
                    dbHelper.deleteDownloads(novelId)
                    adapter.removeItem(novelId)
                    if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(novelId, DownloadNovelService.ACTION_REMOVE)
                    dialog.dismiss()
                }
            }
            negativeButton(R.string.cancel) { dialog ->
                this@NovelDownloadsActivity.run {
                    dialog.dismiss()
                }
            }

            lifecycleOwner(this@NovelDownloadsActivity)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }


    @UiThread
    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        binding.contentRecyclerView.recyclerView.post {
            //if (downloadWebPageEvent.type == EventType.COMPLETE) {
            val index = adapter.items.indexOf(downloadWebPageEvent.download.novelId)
            adapter.notifyItemRangeChanged(index, 1, downloadWebPageEvent)
            //}
        }
    }

    @UiThread
    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        binding.contentRecyclerView.recyclerView.post {
            when (downloadNovelEvent.type) {
                EventType.INSERT, EventType.RUNNING, EventType.PAUSED -> {
                    val index = adapter.items.indexOf(downloadNovelEvent.novelId)
                    adapter.notifyItemRangeChanged(index, 1, downloadNovelEvent)
                }
                EventType.DELETE -> {
                    adapter.removeItem(downloadNovelEvent.novelId)
                    if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME))
                        downloadNovelService?.handleNovelDownload(downloadNovelEvent.novelId, DownloadNovelService.ACTION_REMOVE)
                }
                else -> {
                    //Do Nothing
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        val isServiceRunning = Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)
        if (isServiceRunning) {
            bindService()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isServiceConnected) {
            unbindService(mConnection)
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
        }
    }

    private fun bindService() {
        Intent(this, DownloadNovelService::class.java).also { intent ->
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }

}
