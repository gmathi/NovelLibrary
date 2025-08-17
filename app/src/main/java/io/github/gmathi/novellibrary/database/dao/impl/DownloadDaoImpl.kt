package io.github.gmathi.novellibrary.database.dao.impl

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.database.dao.DownloadDao
import io.github.gmathi.novellibrary.model.database.Download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashMap

class DownloadDaoImpl(private val dbHelper: DBHelper) : DownloadDao {
    
    override suspend fun createDownload(download: Download) = withContext(Dispatchers.IO) {
        //Check 1st
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_WEB_PAGE_URL} = ?"
        val cursor = dbHelper.writableDatabase.rawQuery(selectQuery, arrayOf(download.webPageUrl))
        val recordExists = cursor != null && cursor.count > 0
        cursor?.close()
        if (recordExists)
            return@withContext

        val values = ContentValues().apply {
            put(DBKeys.KEY_NAME, download.novelName)
            put(DBKeys.KEY_NOVEL_ID, download.novelId)
            put(DBKeys.KEY_WEB_PAGE_URL, download.webPageUrl)
            put(DBKeys.KEY_CHAPTER, download.chapter)
            put(DBKeys.KEY_STATUS, Download.STATUS_IN_QUEUE)
            put(DBKeys.KEY_ORDER_ID, download.orderId)
            put(DBKeys.KEY_METADATA, Gson().toJson(HashMap<String, String?>()))
        }
        dbHelper.writableDatabase.insert(DBKeys.TABLE_DOWNLOAD, null, values)
    }
    
    override suspend fun getDownload(webPageUrl: String): Download? = withContext(Dispatchers.IO) {
        var download: Download? = null
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_WEB_PAGE_URL} = ?"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(webPageUrl))
        cursor?.use {
            if (it.moveToFirst())
                download = getDownloadFromCursor(it)
        }
        download
    }
    
    override fun getAllDownloadsFlow(): Flow<List<Download>> = flow {
        emit(getAllDownloads())
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllDownloads(): List<Download> = withContext(Dispatchers.IO) {
        val list = ArrayList<Download>()
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD}"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(getDownloadFromCursor(it))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override fun getAllDownloadsForNovelFlow(novelId: Long): Flow<List<Download>> = flow {
        emit(getAllDownloadsForNovel(novelId))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllDownloadsForNovel(novelId: Long): List<Download> = withContext(Dispatchers.IO) {
        val list = ArrayList<Download>()
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NOVEL_ID} = ?"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()))
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(getDownloadFromCursor(it))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun getAllDownloadsForNovel(novelId: Long, downloadUrls: List<String>): List<Download> = withContext(Dispatchers.IO) {
        val list = ArrayList<Download>()
        val selectQuery =
            "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_WEB_PAGE_URL} IN (${TextUtils.join(",", Collections.nCopies(downloadUrls.count(), "?"))})"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()) + downloadUrls.toTypedArray())
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(getDownloadFromCursor(it))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun getDownloadItemInQueue(): Download? = withContext(Dispatchers.IO) {
        var download: Download? = null
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} LIMIT 1"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, null)
        cursor?.use {
            if (it.moveToFirst())
                download = getDownloadFromCursor(it)
        }
        download
    }
    
    override suspend fun getDownloadItemInQueue(novelId: Long): Download? = withContext(Dispatchers.IO) {
        var download: Download? = null
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} AND ${DBKeys.KEY_NOVEL_ID} = ? LIMIT 1"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()))
        cursor?.use {
            if (it.moveToFirst())
                download = getDownloadFromCursor(it)
        }
        download
    }
    
    override suspend fun getDownloadNovelIds(): List<Long> = withContext(Dispatchers.IO) {
        val list = ArrayList<Long>()
        val selectQuery = "SELECT DISTINCT(${DBKeys.KEY_NOVEL_ID}) FROM ${DBKeys.TABLE_DOWNLOAD}"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    list.add(it.getLong(it.getColumnIndex(DBKeys.KEY_NOVEL_ID)))
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun hasDownloadsInQueue(novelId: Long): Boolean = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT COUNT(*) FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} LIMIT 1"
        (DatabaseUtils.longForQuery(dbHelper.readableDatabase, selectQuery, arrayOf(novelId.toString())).toInt() > 0)
    }
    
    override suspend fun getRemainingDownloadsCountForNovel(novelId: Long): Int = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT COUNT(*) FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NOVEL_ID} = ?"
        DatabaseUtils.longForQuery(dbHelper.readableDatabase, selectQuery, arrayOf(novelId.toString())).toInt()
    }
    
    override suspend fun updateDownloadStatusWebPageUrl(status: Int, webPageUrl: String) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_STATUS, status)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_WEB_PAGE_URL + " = ?", arrayOf(webPageUrl))
        Unit
    }
    
    override suspend fun updateDownloadStatusNovelId(status: Int, novelId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_STATUS, status)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateDownloadStatus(status: Int): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_STATUS, status)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, null, null).toLong()
    }
    
    override suspend fun deleteDownload(webPageUrl: String) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_WEB_PAGE_URL + " = ?", arrayOf(webPageUrl))
        Unit
    }
    
    override suspend fun deleteDownloads(novelId: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    private fun getDownloadFromCursor(cursor: Cursor): Download {
        val download = Download(
            webPageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_WEB_PAGE_URL)),
            novelName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)),
            novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID)),
            chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
        )
        download.status = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_STATUS))
        download.orderId = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
        download.metadata = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
        return download
    }
}