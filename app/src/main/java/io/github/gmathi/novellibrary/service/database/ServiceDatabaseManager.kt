package io.github.gmathi.novellibrary.service.database

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.createWebPageSettings
import io.github.gmathi.novellibrary.database.dao.DownloadDao
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.database.dao.impl.DownloadDaoImpl
import io.github.gmathi.novellibrary.database.dao.impl.NovelDaoImpl
import io.github.gmathi.novellibrary.database.dao.impl.WebPageDaoImpl
import io.github.gmathi.novellibrary.database.repository.DownloadRepository
import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.database.repository.impl.DownloadRepositoryImpl
import io.github.gmathi.novellibrary.database.repository.impl.NovelRepositoryImpl
import io.github.gmathi.novellibrary.database.repository.impl.WebPageRepositoryImpl
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service-specific database manager that provides coroutine-based database operations
 * for background services and workers
 */
class ServiceDatabaseManager(private val dbHelper: DBHelper) {
    
    private val novelDao: NovelDao = NovelDaoImpl(dbHelper)
    private val webPageDao: WebPageDao = WebPageDaoImpl(dbHelper)
    private val downloadDao: DownloadDao = DownloadDaoImpl(dbHelper)
    
    private val novelRepository: NovelRepository = NovelRepositoryImpl(novelDao, dbHelper)
    private val webPageRepository: WebPageRepository = WebPageRepositoryImpl(webPageDao, dbHelper)
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl(downloadDao, dbHelper)
    
    /**
     * Get all novels for background sync operations
     */
    suspend fun getAllNovels(): List<Novel> = withContext(Dispatchers.IO) {
        novelRepository.getAllNovels()
    }
    
    /**
     * Get a novel by ID for service operations
     */
    suspend fun getNovel(novelId: Long): Novel? = withContext(Dispatchers.IO) {
        novelRepository.getNovel(novelId)
    }
    
    /**
     * Update novel metadata during sync operations
     */
    suspend fun updateNovelMetaData(novel: Novel) = withContext(Dispatchers.IO) {
        novelRepository.updateNovelMetaData(novel)
    }
    
    /**
     * Update chapters and releases count during sync
     */
    suspend fun updateChaptersAndReleasesCount(novelId: Long, totalChaptersCount: Long, newReleasesCount: Long) = withContext(Dispatchers.IO) {
        novelRepository.updateChaptersAndReleasesCount(novelId, totalChaptersCount, newReleasesCount)
    }
    
    /**
     * Get all web pages for a novel during sync operations
     */
    suspend fun getAllWebPages(novelId: Long): List<WebPage> = withContext(Dispatchers.IO) {
        webPageRepository.getAllWebPages(novelId)
    }
    
    /**
     * Create web page during sync operations
     */
    suspend fun createWebPage(webPage: WebPage): Boolean = withContext(Dispatchers.IO) {
        webPageRepository.createWebPage(webPage)
    }
    
    /**
     * Get next download item in queue for download service
     */
    suspend fun getDownloadItemInQueue(novelId: Long): Download? = withContext(Dispatchers.IO) {
        downloadRepository.getDownloadItemInQueue(novelId)
    }
    
    /**
     * Get remaining downloads count for a novel
     */
    suspend fun getRemainingDownloadsCountForNovel(novelId: Long): Int = withContext(Dispatchers.IO) {
        downloadRepository.getRemainingDownloadsCountForNovel(novelId)
    }
    
    /**
     * Update download status for all downloads
     */
    suspend fun updateDownloadStatus(status: Int): Long = withContext(Dispatchers.IO) {
        downloadDao.updateDownloadStatus(status)
    }
    
    /**
     * Perform a complex sync operation with proper transaction handling
     */
    suspend fun performSyncTransaction(
        novel: Novel,
        chapters: List<WebPage>,
        webPageSettings: List<WebPageSettings>
    ) = withContext(Dispatchers.IO) {
        novelRepository.withTransaction {
            // Update novel metadata
            novelRepository.updateNovelMetaData(novel)
            
            // Calculate new chapters count
            var newChaptersCount = chapters.size - novel.chaptersCount
            if (newChaptersCount <= 0) {
                newChaptersCount = 0
            }
            val newReleasesCount = novel.newReleasesCount + newChaptersCount
            
            // Update chapters and releases count
            novelRepository.updateChaptersAndReleasesCount(novel.id, chapters.size.toLong(), newReleasesCount)
            
            // Create web pages and settings
            chapters.forEach { chapter ->
                webPageRepository.createWebPage(chapter)
            }
            
            webPageSettings.forEach { settings ->
                // Note: WebPageSettings operations would need to be added to the repository
                // For now, we'll use the existing helper method
                dbHelper.createWebPageSettings(settings, null)
            }
        }
    }
    
    /**
     * Perform download operations with proper error handling
     */
    suspend fun performDownloadTransaction(
        downloads: List<Download>
    ) = withContext(Dispatchers.IO) {
        downloadRepository.withTransaction {
            downloads.forEach { download ->
                downloadRepository.createDownload(download)
            }
        }
    }
    
    /**
     * Get download by web page URL
     */
    suspend fun getDownload(webPageUrl: String): Download? = withContext(Dispatchers.IO) {
        downloadRepository.getDownload(webPageUrl)
    }
    
    /**
     * Update download status by web page URL
     */
    suspend fun updateDownloadStatusWebPageUrl(status: Int, webPageUrl: String) = withContext(Dispatchers.IO) {
        downloadRepository.updateDownloadStatusWebPageUrl(status, webPageUrl)
    }
    
    /**
     * Check if novel has downloads in queue
     */
    suspend fun hasDownloadsInQueue(novelId: Long): Boolean = withContext(Dispatchers.IO) {
        downloadRepository.hasDownloadsInQueue(novelId)
    }
    
    /**
     * Delete downloads for a novel
     */
    suspend fun deleteDownloads(novelId: Long) = withContext(Dispatchers.IO) {
        downloadRepository.deleteDownloads(novelId)
    }
}