package io.github.gmathi.novellibrary.database.dao.impl

import android.content.ContentValues
import android.database.Cursor
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class WebPageDaoImpl(private val dbHelper: DBHelper) : WebPageDao {
    
    override suspend fun createWebPage(webPage: WebPage): Boolean = withContext(Dispatchers.IO) {
        //Check 1st
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_URL} = ? LIMIT 1"
        val cursor = dbHelper.writableDatabase.rawQuery(selectQuery, arrayOf(webPage.url))
        val recordExists = cursor != null && cursor.count > 0
        cursor?.close()
        if (recordExists)
            return@withContext false

        val values = ContentValues().apply {
            put(DBKeys.KEY_URL, webPage.url)
            put(DBKeys.KEY_CHAPTER, webPage.chapterName)
            put(DBKeys.KEY_NOVEL_ID, webPage.novelId)
            put(DBKeys.KEY_ORDER_ID, webPage.orderId)
            put(DBKeys.KEY_TRANSLATOR_SOURCE_NAME, webPage.translatorSourceName)
        }
        dbHelper.writableDatabase.insert(DBKeys.TABLE_WEB_PAGE, null, values) != -1L
    }
    
    override suspend fun getWebPage(url: String): WebPage? = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_URL} = ?"
        var webPage: WebPage? = null
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(url))
        cursor?.use {
            if (it.moveToFirst()) {
                webPage = getWebPageFromCursor(it)
            }
        }
        webPage
    }
    
    override fun getAllWebPagesFlow(novelId: Long): Flow<List<WebPage>> = flow {
        emit(getAllWebPages(novelId))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllWebPages(novelId: Long): List<WebPage> = withContext(Dispatchers.IO) {
        val list = ArrayList<WebPage>()
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()))
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(getWebPageFromCursor(it))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override fun getAllWebPagesFlow(novelId: Long, translatorSourceName: String?): Flow<List<WebPage>> = flow {
        emit(getAllWebPages(novelId, translatorSourceName))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllWebPages(novelId: Long, translatorSourceName: String?): List<WebPage> = withContext(Dispatchers.IO) {
        if (translatorSourceName == null) {
            return@withContext getAllWebPages(novelId)
        }

        val list = ArrayList<WebPage>()
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), translatorSourceName))

        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(getWebPageFromCursor(it))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun getWebPage(novelId: Long, offset: Int): WebPage? = withContext(Dispatchers.IO) {
        var webPage: WebPage? = null
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT ?, 1"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), offset.toString()))
        cursor?.use {
            if (it.moveToFirst()) {
                webPage = getWebPageFromCursor(it)
            }
        }
        webPage
    }
    
    override suspend fun getWebPage(novelId: Long, translatorSourceName: String?, offset: Int): WebPage? = withContext(Dispatchers.IO) {
        if (translatorSourceName == null) {
            return@withContext getWebPage(novelId, offset)
        }

        var webPage: WebPage? = null
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT ?, 1"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), translatorSourceName, offset.toString()))
        cursor?.use {
            if (it.moveToFirst()) {
                webPage = getWebPageFromCursor(it)
            }
        }
        webPage
    }
    
    override suspend fun deleteWebPages(novelId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun deleteWebPage(url: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, "${DBKeys.KEY_URL} = ?", arrayOf(url))
        Unit
    }
    
    private fun getWebPageFromCursor(cursor: Cursor): WebPage {
        val webPage = WebPage(
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), 
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
        )
        webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
        webPage.translatorSourceName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TRANSLATOR_SOURCE_NAME))
        webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
        return webPage
    }
}