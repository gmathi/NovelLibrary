package io.github.gmathi.novellibrary.database.repository.impl

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.DownloadDao
import io.github.gmathi.novellibrary.database.repository.DownloadRepository
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.model.database.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of DownloadRepository using coroutines and proper transaction handling
 */
class DownloadRepositoryImpl(
    private val downloadDao: DownloadDao,
    private val dbHelper: DBHelper
) : DownloadRepository {
    
    override suspend fun createDownload(download: Download) {
        downloadDao.createDownload(download)
    }
    
    override suspend fun getDownload(webPageUrl: String): Download? {
        return downloadDao.getDownload(webPageUrl)
    }
    
    override fun getAllDownloadsFlow(): Flow<List<Download>> {
        return downloadDao.getAllDownloadsFlow()
    }
    
    override suspend fun getAllDownloads(): List<Download> {
        return downloadDao.getAllDownloads()
    }
    
    override fun getAllDownloadsForNovelFlow(novelId: Long): Flow<List<Download>> {
        return downloadDao.getAllDownloadsForNovelFlow(novelId)
    }
    
    override suspend fun getAllDownloadsForNovel(novelId: Long): List<Download> {
        return downloadDao.getAllDownloadsForNovel(novelId)
    }
    
    override suspend fun getDownloadItemInQueue(): Download? {
        return downloadDao.getDownloadItemInQueue()
    }
    
    override suspend fun getDownloadItemInQueue(novelId: Long): Download? {
        return downloadDao.getDownloadItemInQueue(novelId)
    }
    
    override suspend fun hasDownloadsInQueue(novelId: Long): Boolean {
        return downloadDao.hasDownloadsInQueue(novelId)
    }
    
    override suspend fun getRemainingDownloadsCountForNovel(novelId: Long): Int {
        return downloadDao.getRemainingDownloadsCountForNovel(novelId)
    }
    
    override suspend fun updateDownloadStatusWebPageUrl(status: Int, webPageUrl: String) {
        downloadDao.updateDownloadStatusWebPageUrl(status, webPageUrl)
    }
    
    override suspend fun updateDownloadStatusNovelId(status: Int, novelId: Long) {
        downloadDao.updateDownloadStatusNovelId(status, novelId)
    }
    
    override suspend fun deleteDownload(webPageUrl: String) {
        downloadDao.deleteDownload(webPageUrl)
    }
    
    override suspend fun deleteDownloads(novelId: Long) {
        downloadDao.deleteDownloads(novelId)
    }
    
    override suspend fun <T> withTransaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        var result: T? = null
        var exception: Exception? = null
        
        dbHelper.writableDatabase.runTransaction { _ ->
            try {
                result = kotlinx.coroutines.runBlocking { block() }
            } catch (e: Exception) {
                exception = e
                throw e
            }
        }
        
        exception?.let { throw it }
        return@withContext result!!
    }
}