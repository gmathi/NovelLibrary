package io.github.gmathi.novellibrary.database.repository.impl

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Implementation of WebPageRepository using coroutines and proper transaction handling
 */
class WebPageRepositoryImpl(
    private val webPageDao: WebPageDao,
    private val dbHelper: DBHelper
) : WebPageRepository {
    
    override suspend fun createWebPage(webPage: WebPage): Boolean {
        return webPageDao.createWebPage(webPage)
    }
    
    override suspend fun getWebPage(url: String): WebPage? {
        return webPageDao.getWebPage(url)
    }
    
    override fun getAllWebPagesFlow(novelId: Long): Flow<List<WebPage>> {
        return webPageDao.getAllWebPagesFlow(novelId)
    }
    
    override suspend fun getAllWebPages(novelId: Long): List<WebPage> {
        return webPageDao.getAllWebPages(novelId)
    }
    
    override fun getAllWebPagesFlow(novelId: Long, translatorSourceName: String?): Flow<List<WebPage>> {
        return webPageDao.getAllWebPagesFlow(novelId, translatorSourceName)
    }
    
    override suspend fun getAllWebPages(novelId: Long, translatorSourceName: String?): List<WebPage> {
        return webPageDao.getAllWebPages(novelId, translatorSourceName)
    }
    
    override suspend fun getWebPage(novelId: Long, offset: Int): WebPage? {
        return webPageDao.getWebPage(novelId, offset)
    }
    
    override suspend fun getWebPage(novelId: Long, translatorSourceName: String?, offset: Int): WebPage? {
        return webPageDao.getWebPage(novelId, translatorSourceName, offset)
    }
    
    override suspend fun deleteWebPages(novelId: Long) {
        webPageDao.deleteWebPages(novelId)
    }
    
    override suspend fun deleteWebPage(url: String) {
        webPageDao.deleteWebPage(url)
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