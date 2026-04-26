package io.github.gmathi.novellibrary.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.gmathi.novellibrary.compose.downloads.DownloadsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.service.download.ServiceActionHandler
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.system.startDownloadNovelService
import io.github.gmathi.novellibrary.viewmodel.DownloadsViewModel

class DownloadsActivity : BaseActivity(), DownloadListener {

    companion object {
        private const val TAG = "DownloadsActivity"
    }

    override val skipWindowInsets: Boolean = true

    private val viewModel: DownloadsViewModel by viewModels()

    private var downloadNovelService: DownloadNovelService? = null
    private var isServiceConnected: Boolean = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as DownloadNovelService.DownloadNovelBinder
            downloadNovelService = binder.getService()
            downloadNovelService?.downloadListener = this@DownloadsActivity
            isServiceConnected = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isServiceConnected = false
            downloadNovelService?.downloadListener = null
            downloadNovelService = null
            viewModel.onServiceDisconnected()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.serviceActionHandler = object : ServiceActionHandler {
            override fun handleAction(novelId: Long, action: String) {
                downloadNovelService?.handleNovelDownload(novelId, action)
            }

            override fun startService(novelId: Long) {
                startDownloadNovelService(novelId)
                bindToService()
            }

            override fun isServiceRunning(): Boolean {
                return Utils.isServiceRunning(
                    this@DownloadsActivity,
                    DownloadNovelService.QUALIFIED_NAME
                )
            }
        }

        setContent {
            NovelLibraryTheme {
                DownloadsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (Utils.isServiceRunning(this, DownloadNovelService.QUALIFIED_NAME)) {
            bindToService()
        }
        viewModel.loadDownloads()
    }

    override fun onStop() {
        super.onStop()
        if (isServiceConnected) {
            downloadNovelService?.downloadListener = null
            unbindService(serviceConnection)
            isServiceConnected = false
            downloadNovelService = null
        }
    }

    // ── DownloadListener ────────────────────────────────────────────────

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        viewModel.onDownloadWebPageEvent(downloadWebPageEvent)
    }

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        viewModel.onDownloadNovelEvent(downloadNovelEvent)
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private fun bindToService() {
        Intent(this, DownloadNovelService::class.java).also { intent ->
            val bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) {
                Logs.warning(TAG, "bindService() returned false — service could not be bound")
            }
        }
    }
}
