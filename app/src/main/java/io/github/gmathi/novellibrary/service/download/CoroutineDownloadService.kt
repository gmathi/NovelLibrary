package io.github.gmathi.novellibrary.service.download

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.database.ServiceDatabaseManager
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Semaphore

/**
 * Coroutine-based download service that replaces the thread-based approach
 */
class CoroutineDownloadService(
    private val context: Context,
    private val novelId: Long,
    private val dbHelper: DBHelper,
    private val downloadListener: DownloadListener
) : DownloadListener {

    private val serviceDatabaseManager = ServiceDatabaseManager(dbHelper)
    private val downloadJobs = mutableMapOf<String, Job>()
    private val eventChannel = Channel<DownloadWebPageEvent>(Channel.UNLIMITED)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "CoroutineDownloadService"
        private const val MAX_CONCURRENT_DOWNLOADS = 10
    }

    /**
     * Start the download process for the novel
     */
    fun startDownload() {
        serviceScope.launch {
            try {
                processDownloadQueue()
            } catch (e: Exception) {
                Logs.error(TAG, "Download process failed", e)
                downloadListener.handleEvent(DownloadNovelEvent(EventType.PAUSED, novelId))
            }
        }
    }

    /**
     * Process the download queue using structured concurrency
     */
    private suspend fun processDownloadQueue() {
        var download = serviceDatabaseManager.getDownloadItemInQueue(novelId)
        val semaphore = Semaphore(MAX_CONCURRENT_DOWNLOADS)
        
        while (download != null && serviceScope.isActive) {
            
            if (!NetworkHelper(context).isConnectedToNetwork()) {
                throw CancellationException(Constants.NO_NETWORK)
            }
            
            // Acquire semaphore to limit concurrent downloads
            semaphore.acquire()
            
            val downloadJob = serviceScope.launch {
                try {
                    processWebPageDownload(download!!)
                } catch (e: Exception) {
                    Logs.error(TAG, "Web page download failed: ${download?.webPageUrl}", e)
                    handleEvent(DownloadWebPageEvent(EventType.PAUSED, download!!.webPageUrl, download!!))
                } finally {
                    semaphore.release()
                }
            }
            
            downloadJobs[download.webPageUrl] = downloadJob
            
            // Wait for this download to complete before getting the next one
            downloadJob.join()
            downloadJobs.remove(download.webPageUrl)
            
            // Check if service is still active
            if (!serviceScope.isActive) break
            
            download = serviceDatabaseManager.getDownloadItemInQueue(novelId)
        }
        
        // Wait for all remaining downloads to complete
        downloadJobs.values.forEach { it.join() }
        
        // Check final status
        val remainingCount = serviceDatabaseManager.getRemainingDownloadsCountForNovel(novelId)
        if (remainingCount == 0) {
            downloadListener.handleEvent(DownloadNovelEvent(EventType.DELETE, novelId))
        } else {
            downloadListener.handleEvent(DownloadNovelEvent(EventType.PAUSED, novelId))
        }
    }

    /**
     * Process individual web page download
     */
    private suspend fun processWebPageDownload(download: Download) {
        handleEvent(DownloadWebPageEvent(EventType.RUNNING, download.webPageUrl, download))
        
        // Create and run the web page download thread in a coroutine
        withContext(Dispatchers.IO) {
            val downloadThread = DownloadWebPageThread(context, download, dbHelper, this@CoroutineDownloadService)
            
            // Convert thread execution to coroutine
            val deferred = async(Dispatchers.IO) {
                downloadThread.run()
            }
            
            try {
                deferred.await()
                handleEvent(DownloadWebPageEvent(EventType.COMPLETE, download.webPageUrl, download))
            } catch (e: Exception) {
                deferred.cancel()
                throw e
            }
        }
    }

    /**
     * Cancel a specific download by URL
     */
    suspend fun cancelDownload(webPageUrl: String) {
        downloadJobs[webPageUrl]?.cancel()
        downloadJobs.remove(webPageUrl)
    }

    /**
     * Cancel all downloads for this novel
     */
    fun cancelAllDownloads() {
        serviceScope.cancel()
        downloadJobs.clear()
    }

    /**
     * Check if downloads are currently running
     */
    fun isDownloading(): Boolean {
        return downloadJobs.isNotEmpty() && serviceScope.isActive
    }

    /**
     * Get the current download status
     */
    suspend fun getDownloadStatus(): DownloadStatus {
        val remainingCount = serviceDatabaseManager.getRemainingDownloadsCountForNovel(novelId)
        val activeDownloads = downloadJobs.size
        
        return DownloadStatus(
            novelId = novelId,
            remainingDownloads = remainingCount,
            activeDownloads = activeDownloads,
            isRunning = isDownloading()
        )
    }

    override fun handleEvent(downloadNovelEvent: DownloadNovelEvent) {
        downloadListener.handleEvent(downloadNovelEvent)
    }

    override fun handleEvent(downloadWebPageEvent: DownloadWebPageEvent) {
        // Send event through channel for reactive handling
        serviceScope.launch {
            eventChannel.trySend(downloadWebPageEvent)
        }
        downloadListener.handleEvent(downloadWebPageEvent)
    }

    /**
     * Get a flow of download events for reactive UI updates
     */
    fun getDownloadEventsFlow() = eventChannel.receiveAsFlow()

    /**
     * Clean up resources
     */
    fun cleanup() {
        serviceScope.cancel()
        eventChannel.close()
        downloadJobs.clear()
    }
}

/**
 * Data class representing the current download status
 */
data class DownloadStatus(
    val novelId: Long,
    val remainingDownloads: Int,
    val activeDownloads: Int,
    val isRunning: Boolean
)