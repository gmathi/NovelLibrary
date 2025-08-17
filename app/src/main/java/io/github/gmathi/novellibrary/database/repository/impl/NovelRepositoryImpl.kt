package io.github.gmathi.novellibrary.database.repository.impl

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.model.database.Novel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of NovelRepository using coroutines and proper transaction handling
 */
class NovelRepositoryImpl(
    private val novelDao: NovelDao,
    private val dbHelper: DBHelper
) : NovelRepository {
    
    override suspend fun insertNovel(novel: Novel): Long {
        return novelDao.insertNovel(novel)
    }
    
    override suspend fun getNovelByUrl(novelUrl: String): Novel? {
        return novelDao.getNovelByUrl(novelUrl)
    }
    
    override suspend fun getNovel(novelId: Long): Novel? {
        return novelDao.getNovel(novelId)
    }
    
    override fun getAllNovelsFlow(): Flow<List<Novel>> {
        return novelDao.getAllNovelsFlow()
    }
    
    override suspend fun getAllNovels(): List<Novel> {
        return novelDao.getAllNovels()
    }
    
    override fun getAllNovelsFlow(novelSectionId: Long): Flow<List<Novel>> {
        return novelDao.getAllNovelsFlow(novelSectionId)
    }
    
    override suspend fun getAllNovels(novelSectionId: Long): List<Novel> {
        return novelDao.getAllNovels(novelSectionId)
    }
    
    override suspend fun updateNovel(novel: Novel): Long {
        return novelDao.updateNovel(novel)
    }
    
    override suspend fun updateNovelMetaData(novel: Novel) {
        novelDao.updateNovelMetaData(novel)
    }
    
    override suspend fun updateChaptersAndReleasesCount(novelId: Long, totalChaptersCount: Long, newReleasesCount: Long) {
        novelDao.updateChaptersAndReleasesCount(novelId, totalChaptersCount, newReleasesCount)
    }
    
    override suspend fun updateNewReleasesCount(novelId: Long, newReleasesCount: Long) {
        novelDao.updateNewReleasesCount(novelId, newReleasesCount)
    }
    
    override suspend fun updateBookmarkCurrentWebPageUrl(novelId: Long, currentChapterUrl: String?) {
        novelDao.updateBookmarkCurrentWebPageUrl(novelId, currentChapterUrl)
    }
    
    override suspend fun updateTotalChapterCount(novelId: Long, totalChaptersCount: Long) {
        novelDao.updateTotalChapterCount(novelId, totalChaptersCount)
    }
    
    override suspend fun updateNovelOrderId(novelId: Long, orderId: Long) {
        novelDao.updateNovelOrderId(novelId, orderId)
    }
    
    override suspend fun updateNovelSectionId(novelId: Long, novelSectionId: Long) {
        novelDao.updateNovelSectionId(novelId, novelSectionId)
    }
    
    override suspend fun deleteNovel(id: Long) {
        novelDao.deleteNovel(id)
    }
    
    override suspend fun resetNovel(novel: Novel) {
        novelDao.resetNovel(novel)
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