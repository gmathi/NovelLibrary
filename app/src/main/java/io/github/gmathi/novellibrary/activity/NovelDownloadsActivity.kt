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
import androidx.activity.viewModels
import androidx.annotation.UiThread
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.databinding.ActivityNovelDownloadsBinding
import io.github.gmathi.novellibrary.databinding.ListitemDownloadQueueOldBinding
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.event.ModernEventBus
import io.github.gmathi.novellibrary.util.lang.getGlideUrl
import io.github.gmathi.novellibrary.util.view.setDefaultsNoAnimation
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService
import io.github.gmathi.novellibrary.viewmodel.DownloadManagementViewModel


class NovelDownloadsActivity : BaseActivity(), GenericAdapter.Listener<Long>, DownloadListener {

    companion object {
        private const val TAG = "NovelDownloadsActivity"
    }

    // ViewModel
    private val viewModel: DownloadManagementViewModel by viewModels()

    // Service binding (kept for backward compatibility)
    private var downloadNovelService: DownloadNovelService? = null
    private var isServiceConnected: Boolean = false

    private lateinit var binding: ActivityNovelDownloadsBinding
    lateinit var adapter: GenericAdapter<Long>

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNovelDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.fab.hide()
        
        // Initialize ViewModel
        viewModel.init(this)
        
        // Setup UI
        setupRecyclerView()
        setupObservers()
        setupEventSubscriptions()
    }

    private fun setupRecyclerView() {
        adapter = GenericAdapter(
            items = ArrayList(), 
            layoutResId = R.layout.listitem_download_queue_old, 
            listener = this
        )
        binding.contentRecyclerView.recyclerView.setDefaultsNoAnimation(adapter)
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    private fun setupObservers() {
        // Observe UI state
        viewModel.uiState.observe(this, Observer { uiState ->
            when (uiState) {
                is DownloadManagementViewModel.DownloadManagementUiState.Loading -> {
                    // Show loading state if needed
                }
                is DownloadManagementViewModel.DownloadManagementUiState.Success -> {
                    // UI is ready
                }
                is DownloadManagementViewModel.DownloadManagementUiState.Error -> {
                    // Show error state
                }
                else -> {
                    // Idle state
                }
            }
        })

        // Observe novel downloads
        viewModel.novelDownloads.observe(this, Observer { novelDownloads ->
            val novelIds = novelDownloads.keys.toList()
            adapter.updateItems(ArrayList(novelIds))
        })

        // Observe download operations state
        viewModel.downloadOperationsState.observe(this, Observer { operationsState ->
            when (operationsState) {
                is DownloadManagementViewModel.DownloadOperationsState.Loading -> {
                    // Show loading indicator
                }
                is DownloadManagementViewModel.DownloadOperationsState.Success -> {
                    // Hide loading indicator
                }
                is DownloadManagementViewModel.DownloadOperationsState.Error -> {
                    // Show error message
                }
                else -> {
                    // Idle state
                }
            }
        })

        // Observe service status
        viewModel.serviceStatus.observe(this, Observer { serviceStatus ->
            // Update UI based on service status
        })

        // Observe network status
        viewModel.networkStatus.observe(this, Observer { isConnected ->
            // Update UI based on network status
        })
    }

    private fun setupEventSubscriptions() {
        // Subscribe to download novel events
        subscribeToDownloadNovelEvents { event ->
            Logs.info(TAG, "Received DownloadNovelEvent: ${event.type} for novel ID: ${event.novelId}")
            
            binding.contentRecyclerView.recyclerView.post {
                when (event.type) {
                    EventType.INSERT, EventType.RUNNING, EventType.PAUSED -> {
                        val index = adapter.items.indexOf(event.novelId)
                        if (index != -1) {
                            adapter.notifyItemRangeChanged(index, 1, event)
                        }
                    }
                    EventType.DELETE -> {
                        adapter.removeItem(event.novelId)
                        if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
                            downloadNovelService?.handleNovelDownload(event.novelId, DownloadNovelService.ACTION_REMOVE)
                        }
                    }
                    else -> {
                        //Do Nothing
                    }
                }
            }
        }

        // Subscribe to download web page events
        subscribeToDownloadWebPageEvents { event ->
            Logs.info(TAG, "Received DownloadWebPageEvent: ${event.type} for novel ID: ${event.download.novelId}")
            
            binding.contentRecyclerView.recyclerView.post {
                val index = adapter.items.indexOf(event.download.novelId)
                if (index != -1) {
                    adapter.notifyItemRangeChanged(index, 1, event)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bind(item: Long, itemView: View, position: Int) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        val novel = dbHelper.getNovel(novelId = item)
        
        // Load novel image
        if (!novel?.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(novel!!.imageUrl!!.getGlideUrl())
                .apply(RequestOptions.circleCropTransform())
                .into(binding.novelImageView)
        }
        
        binding.novelTitleTextView.text = novel?.name

        // Get download progress for this novel
        val novelProgress = viewModel.novelDownloadProgress.value?.get(item)
        val hasDownloadsInQueue = novelProgress?.hasDownloadsInQueue ?: false
        val isServiceRunning = viewModel.serviceStatus.value?.isRunning ?: false

        if (hasDownloadsInQueue && isServiceRunning) {
            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
            binding.novelProgressText.text = "Downloading: retrieving status…"
        } else {
            binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
            binding.playPauseImage.tag = Download.STATUS_PAUSED
            binding.novelProgressText.text = getString(R.string.download_paused)
        }

        // Setup play/pause button
        binding.playPauseImage.setOnClickListener {
            when (binding.playPauseImage.tag) {
                Download.STATUS_PAUSED -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                    viewModel.resumeDownloadsForNovel(item, this)
                }
                Download.STATUS_IN_QUEUE -> {
                    binding.playPauseImage.setImageResource(R.drawable.ic_play_arrow_white_vector)
                    binding.playPauseImage.tag = Download.STATUS_PAUSED
                    viewModel.pauseDownloadsForNovel(item)
                }
            }
        }

        // Setup delete button
        binding.deleteButton.setOnClickListener {
            confirmDeleteDialog(item)
        }
    }

    //Invoke it using --> notifyItemRangeChanged(positionStart, itemCount, payload);
    @SuppressLint("SetTextI18n")
    override fun bind(item: Long, itemView: View, position: Int, payloads: MutableList<Any>?) {
        val binding = ListitemDownloadQueueOldBinding.bind(itemView)
        if (payloads == null || payloads.size == 0) {
            bind(item, itemView, position)
        } else {
            //Update the cells partially
            val downloadEvent = payloads[0]

            if (downloadEvent is DownloadWebPageEvent) {
                if (downloadEvent.download.novelId == item && binding.playPauseImage.tag != Download.STATUS_PAUSED) {
                    val novelProgress = viewModel.novelDownloadProgress.value?.get(item)
                    val remainingDownloadsCount = novelProgress?.totalDownloads ?: 0
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
                            val novelProgress = viewModel.novelDownloadProgress.value?.get(item)
                            val remainingDownloadsCount = novelProgress?.totalDownloads ?: 0
                            binding.novelProgressText.text = "${getString(R.string.download_in_queue)} - Remaining: $remainingDownloadsCount"
                        }
                        EventType.RUNNING -> {
                            binding.playPauseImage.setImageResource(R.drawable.ic_pause_white_vector)
                            binding.playPauseImage.tag = Download.STATUS_IN_QUEUE
                            binding.novelProgressText.text = "Collecting Novel Information…"
                        }
                        else -> {
                            // Handle other cases
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
                viewModel.deleteDownloadsForNovel(novelId)
                if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
                    downloadNovelService?.handleNovelDownload(novelId, DownloadNovelService.ACTION_REMOVE)
                }
                dialog.dismiss()
            }
            negativeButton(R.string.cancel) { dialog ->
                dialog.dismiss()
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
            val index = adapter.items.indexOf(downloadWebPageEvent.download.novelId)
            if (index != -1) {
                adapter.notifyItemRangeChanged(index, 1, downloadWebPageEvent)
            }
        }
    }

    @UiThread
    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        binding.contentRecyclerView.recyclerView.post {
            when (downloadNovelEvent.type) {
                EventType.INSERT, EventType.RUNNING, EventType.PAUSED -> {
                    val index = adapter.items.indexOf(downloadNovelEvent.novelId)
                    if (index != -1) {
                        adapter.notifyItemRangeChanged(index, 1, downloadNovelEvent)
                    }
                }
                EventType.DELETE -> {
                    adapter.removeItem(downloadNovelEvent.novelId)
                    if (isServiceConnected && Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)) {
                        downloadNovelService?.handleNovelDownload(downloadNovelEvent.novelId, DownloadNovelService.ACTION_REMOVE)
                    }
                }
                else -> {
                    //Do Nothing
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        
        // Bind to service if running
        val isServiceRunning = Utils.isServiceRunning(this@NovelDownloadsActivity, DownloadNovelService.QUALIFIED_NAME)
        if (isServiceRunning) {
            bindService()
        }
    }

    override fun onStop() {
        super.onStop()
        // Cleanup if needed
    }

    private fun bindService() {
        Intent(this, DownloadNovelService::class.java).also { intent ->
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        }
    }
}
