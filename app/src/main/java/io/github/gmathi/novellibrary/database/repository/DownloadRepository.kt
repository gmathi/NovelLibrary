package io.github.gmathi.novellibrary.database.repository

import io.github.gmathi.novellibrary.model.database.Download
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Download operations using coroutines and Flow
 */
interface DownloadRepository {
    
    /**
     * Create a download entry in the database
     * @param download The download to create
     */
    suspend fun createDownload(download: Download)
    
    /**
     * Get a download by web page URL
     * @param webPageUrl The web page URL
     * @return The download if found, null otherwise
     */
    suspend fun getDownload(webPageUrl: String): Download?
    
    /**
     * Get all downloads as a Flow for reactive updates
     * @return Flow of list of all downloads
     */
    fun getAllDownloadsFlow(): Flow<List<Download>>
    
    /**
     * Get all downloads (one-time query)
     * @return List of all downloads
     */
    suspend fun getAllDownloads(): List<Download>
    
    /**
     * Get all downloads for a specific novel as a Flow
     * @param novelId The novel ID
     * @return Flow of list of downloads for the novel
     */
    fun getAllDownloadsForNovelFlow(novelId: Long): Flow<List<Download>>
    
    /**
     * Get all downloads for a specific novel (one-time query)
     * @param novelId The novel ID
     * @return List of downloads for the novel
     */
    suspend fun getAllDownloadsForNovel(novelId: Long): List<Download>
    
    /**
     * Get the next download item in queue
     * @return The next download in queue, null if none
     */
    suspend fun getDownloadItemInQueue(): Download?
    
    /**
     * Get the next download item in queue for a specific novel
     * @param novelId The novel ID
     * @return The next download in queue for the novel, null if none
     */
    suspend fun getDownloadItemInQueue(novelId: Long): Download?
    
    /**
     * Check if a novel has downloads in queue
     * @param novelId The novel ID
     * @return True if novel has downloads in queue, false otherwise
     */
    suspend fun hasDownloadsInQueue(novelId: Long): Boolean
    
    /**
     * Get remaining downloads count for a novel
     * @param novelId The novel ID
     * @return Number of remaining downloads for the novel
     */
    suspend fun getRemainingDownloadsCountForNovel(novelId: Long): Int
    
    /**
     * Update download status by web page URL
     * @param status The new status
     * @param webPageUrl The web page URL
     */
    suspend fun updateDownloadStatusWebPageUrl(status: Int, webPageUrl: String)
    
    /**
     * Update download status for all downloads of a novel
     * @param status The new status
     * @param novelId The novel ID
     */
    suspend fun updateDownloadStatusNovelId(status: Int, novelId: Long)
    
    /**
     * Delete a download by web page URL
     * @param webPageUrl The web page URL
     */
    suspend fun deleteDownload(webPageUrl: String)
    
    /**
     * Delete all downloads for a novel
     * @param novelId The novel ID
     */
    suspend fun deleteDownloads(novelId: Long)
    
    /**
     * Perform database transaction with proper error handling
     * @param block The transaction block to execute
     */
    suspend fun <T> withTransaction(block: suspend () -> T): T
}