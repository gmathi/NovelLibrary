package io.github.gmathi.novellibrary.data.repository

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.download.DownloadListener
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.event.ModernEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.util.*

class DownloadManagementRepository {
    
    companion object {
        const val TAG = "DownloadManagementRepository"
    }

    private val dbHelper: DBHelper by injectLazy()

    /**
     * Get all downloads
     */
    suspend fun getAllDownloads(): List<Download> = withContext(Dispatchers.IO) {
        dbHelper.getAllDownloads()
    }

    /**
     * Get downloads for a specific novel
     */
    suspend fun getDownloadsForNovel(novelId: Long): List<Download> = withContext(Dispatchers.IO) {
        dbHelper.getAllDownloadsForNovel(novelId)
    }

    /**
     * Get downloads for a novel with specific URLs
     */
    suspend fun getDownloadsForNovel(novelId: Long, downloadUrls: List<String>): List<Download> = withContext(Dispatchers.IO) {
        dbHelper.getAllDownloadsForNovel(novelId, downloadUrls)
    }

    /**
     * Get download by web page URL
     */
    suspend fun getDownload(webPageUrl: String): Download? = withContext(Dispatchers.IO) {
        dbHelper.getDownload(webPageUrl)
    }

    /**
     * Get next download item in queue
     */
    suspend fun getNextDownloadInQueue(): Download? = withContext(Dispatchers.IO) {
        dbHelper.getDownloadItemInQueue()
    }

    /**
     * Get next download item in queue for a specific novel
     */
    suspend fun getNextDownloadInQueue(novelId: Long): Download? = withContext(Dispatchers.IO) {
        dbHelper.getDownloadItemInQueue(novelId)
    }

    /**
     * Get all novel IDs that have downloads
     */
    suspend fun getDownloadNovelIds(): List<Long> = withContext(Dispatchers.IO) {
        dbHelper.getDownloadNovelIds()
    }

    /**
     * Check if novel has downloads in queue
     */
    suspend fun hasDownloadsInQueue(novelId: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.hasDownloadsInQueue(novelId)
    }

    /**
     * Get remaining downloads count for a novel
     */
    suspend fun getRemainingDownloadsCountForNovel(novelId: Long): Int = withContext(Dispatchers.IO) {
        dbHelper.getRemainingDownloadsCountForNovel(novelId)
    }

    /**
     * Create download entry
     */
    suspend fun createDownload(download: Download, db: android.database.sqlite.SQLiteDatabase? = null) = withContext(Dispatchers.IO) {
        try {
            dbHelper.createDownload(download, db)
            Logs.info(TAG, "Created download for: ${download.chapter}")
        } catch (e: Exception) {
            Logs.error(TAG, "Error creating download", e)
            throw e
        }
    }

    /**
     * Update download status by web page URL
     */
    suspend fun updateDownloadStatusByUrl(status: Int, webPageUrl: String) = withContext(Dispatchers.IO) {
        try {
            dbHelper.updateDownloadStatusWebPageUrl(status, webPageUrl)
            Logs.info(TAG, "Updated download status to $status for URL: $webPageUrl")
        } catch (e: Exception) {
            Logs.error(TAG, "Error updating download status", e)
            throw e
        }
    }

    /**
     * Update download status by novel ID
     */
    suspend fun updateDownloadStatusByNovelId(status: Int, novelId: Long) = withContext(Dispatchers.IO) {
        try {
            dbHelper.updateDownloadStatusByNovelId(status, novelId)
            
            // Post event for status update
            val event = DownloadNovelEvent(EventType.UPDATE, novelId)
            ModernEventBus.postAsync(event)
            
            Logs.info(TAG, "Successfully updated download status to $status for novel ID: $novelId")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error updating download status", e)
            throw e
        }
    }

    /**
     * Update all download statuses
     */
    suspend fun updateAllDownloadStatuses(status: Int): Long = withContext(Dispatchers.IO) {
        try {
            val updatedCount = dbHelper.updateDownloadStatus(status)
            Logs.info(TAG, "Updated $updatedCount download statuses to $status")
            updatedCount
        } catch (e: Exception) {
            Logs.error(TAG, "Error updating all download statuses", e)
            throw e
        }
    }

    /**
     * Delete download by web page URL
     */
    suspend fun deleteDownload(webPageUrl: String) = withContext(Dispatchers.IO) {
        try {
            val download = dbHelper.getDownload(webPageUrl)
            dbHelper.deleteDownload(webPageUrl)
            
            download?.let {
                // Post event for download deletion
                val event = DownloadNovelEvent(EventType.DELETE, it.novelId)
                ModernEventBus.postAsync(event)
            }
            
            Logs.info(TAG, "Successfully deleted download for URL: $webPageUrl")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error deleting download", e)
            throw e
        }
    }

    /**
     * Delete all downloads for a novel
     */
    suspend fun deleteDownloadsForNovel(novelId: Long) = withContext(Dispatchers.IO) {
        try {
            dbHelper.deleteDownloads(novelId)
            Logs.info(TAG, "Deleted all downloads for novel ID: $novelId")
        } catch (e: Exception) {
            Logs.error(TAG, "Error deleting downloads for novel", e)
            throw e
        }
    }

    /**
     * Add chapters to download queue
     */
    suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>, novel: Novel, progressCallback: ((Int) -> Unit)? = null) = withContext(Dispatchers.IO) {
        try {
            var progress = 0
            dbHelper.writableDatabase.runTransaction { writableDatabase ->
                webPages.forEach { webPage ->
                    val download = Download(webPage.url, novel.name, novel.id, webPage.chapterName)
                    download.orderId = webPage.orderId.toInt()
                    dbHelper.createDownload(download, writableDatabase)
                    
                    progress++
                    progressCallback?.invoke(progress)
                }
            }
            
            // Post event for download insertion
            val event = DownloadNovelEvent(EventType.INSERT, novel.id)
            ModernEventBus.postAsync(event)
            
            Logs.info(TAG, "Successfully added ${webPages.size} chapters to download queue for novel: ${novel.name}")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error adding chapters to download queue", e)
            throw e
        }
    }

    /**
     * Check if download service is running
     */
    suspend fun isDownloadServiceRunning(context: Context): Boolean = withContext(Dispatchers.IO) {
        Utils.isServiceRunning(context, DownloadNovelService.QUALIFIED_NAME)
    }

    /**
     * Check network connectivity
     */
    suspend fun isNetworkConnected(context: Context): Boolean = withContext(Dispatchers.IO) {
        NetworkHelper(context).isConnectedToNetwork()
    }

    /**
     * Get download statistics
     */
    suspend fun getDownloadStatistics(): io.github.gmathi.novellibrary.viewmodel.DownloadManagementViewModel.DownloadStatistics = withContext(Dispatchers.IO) {
        val allDownloads = dbHelper.getAllDownloads()
        
        val totalDownloads = allDownloads.size
        val completedDownloads = allDownloads.count { it.status == Download.STATUS_COMPLETED }
        val runningDownloads = allDownloads.count { it.status == Download.STATUS_RUNNING }
        val queuedDownloads = allDownloads.count { it.status == Download.STATUS_IN_QUEUE }
        
        io.github.gmathi.novellibrary.viewmodel.DownloadManagementViewModel.DownloadStatistics(
            totalDownloads = totalDownloads,
            completedDownloads = completedDownloads,
            runningDownloads = runningDownloads,
            queuedDownloads = queuedDownloads
        )
    }

    /**
     * Get download progress for a novel
     */
    suspend fun getDownloadProgressForNovel(novelId: Long): Map<String, Any> = withContext(Dispatchers.IO) {
        val downloads = dbHelper.getAllDownloadsForNovel(novelId)
        
        val totalDownloads = downloads.size
        val queuedDownloads = downloads.count { it.status == Download.STATUS_IN_QUEUE }
        val runningDownloads = downloads.count { it.status == Download.STATUS_RUNNING }
        val pausedDownloads = downloads.count { it.status == Download.STATUS_PAUSED }
        val hasDownloadsInQueue = queuedDownloads > 0
        
        mapOf(
            "totalDownloads" to totalDownloads,
            "queuedDownloads" to queuedDownloads,
            "runningDownloads" to runningDownloads,
            "pausedDownloads" to pausedDownloads,
            "hasDownloadsInQueue" to hasDownloadsInQueue
        )
    }

    /**
     * Pause all downloads
     */
    suspend fun pauseAllDownloads() = withContext(Dispatchers.IO) {
        try {
            val allDownloads = dbHelper.getAllDownloads()
            dbHelper.pauseAllDownloads()
            
            // Post events for all novel updates
            allDownloads.groupBy { it.novelId }.forEach { (novelId, _) ->
                val event = DownloadNovelEvent(EventType.PAUSED, novelId)
                ModernEventBus.postAsync(event)
            }
            
            Logs.info(TAG, "Successfully paused all downloads")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error pausing all downloads", e)
            throw e
        }
    }

    /**
     * Resume all downloads
     */
    suspend fun resumeAllDownloads() = withContext(Dispatchers.IO) {
        try {
            val allDownloads = dbHelper.getAllDownloads()
            dbHelper.resumeAllDownloads()
            
            // Post events for all novel updates
            allDownloads.groupBy { it.novelId }.forEach { (novelId, _) ->
                val event = DownloadNovelEvent(EventType.RUNNING, novelId)
                ModernEventBus.postAsync(event)
            }
            
            Logs.info(TAG, "Successfully resumed all downloads")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error resuming all downloads", e)
            throw e
        }
    }

    /**
     * Clear completed downloads
     */
    suspend fun clearCompletedDownloads() = withContext(Dispatchers.IO) {
        try {
            val completedDownloads = dbHelper.getAllDownloads().filter { it.status == Download.STATUS_COMPLETED }
            dbHelper.clearCompletedDownloads()
            
            // Post events for novel updates
            completedDownloads.groupBy { it.novelId }.forEach { (novelId, _) ->
                val event = DownloadNovelEvent(EventType.DELETE, novelId)
                ModernEventBus.postAsync(event)
            }
            
            Logs.info(TAG, "Successfully cleared completed downloads")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error clearing completed downloads", e)
            throw e
        }
    }

    /**
     * Get download history
     */
    suspend fun getDownloadHistory(limit: Int = 100): List<Download> = withContext(Dispatchers.IO) {
        val allDownloads = getAllDownloads()
        allDownloads.sortedByDescending { it.orderId }.take(limit)
    }

    /**
     * Post download web page event
     */
    fun postDownloadWebPageEvent(event: DownloadWebPageEvent) {
        ModernEventBus.postAsync(event)
    }

    /**
     * Post download novel event
     */
    fun postDownloadNovelEvent(event: DownloadNovelEvent) {
        ModernEventBus.postAsync(event)
    }

    /**
     * Register download listener
     */
    suspend fun registerDownloadListener(listener: DownloadListener) = withContext(Dispatchers.IO) {
        // This would typically be handled by the service
        Logs.info(TAG, "Download listener registered")
    }

    /**
     * Unregister download listener
     */
    suspend fun unregisterDownloadListener(listener: DownloadListener) = withContext(Dispatchers.IO) {
        // This would typically be handled by the service
        Logs.info(TAG, "Download listener unregistered")
    }
} 