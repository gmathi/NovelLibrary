package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Download
import java.util.*
import kotlin.collections.HashMap


private val LOG = "DownloadHelper"

fun DBHelper.createDownload(download: Download): Boolean {
//    val existingDownload = getDownload(download.webPageId)
//    if (existingDownload != null) return false

    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, download.novelName)
    values.put(DBKeys.KEY_WEB_PAGE_ID, download.webPageId)
    values.put(DBKeys.KEY_CHAPTER, download.chapter)
    values.put(DBKeys.KEY_STATUS, Download.STATUS_IN_QUEUE)
    values.put(DBKeys.KEY_ORDER_ID, download.orderId)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(HashMap<String, String?>()))

    this.writableDatabase.insert(DBKeys.TABLE_DOWNLOAD, null, values)
    return true
}

fun DBHelper.getDownload(webPageId: Long): Download? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD + " WHERE " + DBKeys.KEY_WEB_PAGE_ID + " = " + webPageId
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    var download: Download? = null
    if (cursor != null) {
        if (cursor.moveToFirst())
            download = getDownload(cursor)
        cursor.close()
    }
    return download
}

fun DBHelper.getAllDownloads(): List<Download> {
    val list = ArrayList<Download>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    // looping through all rows and adding to list
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(getDownload(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    return list
}

fun DBHelper.updateDownloadStatus(status: Int, webPageId: Long): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_WEB_PAGE_ID + " = ?", arrayOf(webPageId.toString())).toLong()
}

fun DBHelper.updateDownloadStatus(status: Int, novelName: String): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_NAME + " = ?", arrayOf(novelName)).toLong()
}

fun DBHelper.deleteDownload(webPageId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_WEB_PAGE_ID + " = ?", arrayOf(webPageId.toString()))
}

fun DBHelper.deleteDownloads(novelName: String) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_NAME + " = ?", arrayOf(novelName))
}

fun DBHelper.getDownloadItemInQueue(): Download? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD + " WHERE " + DBKeys.KEY_STATUS + " = " + Download.STATUS_IN_QUEUE + " LIMIT 1"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    var download: Download? = null
    if (cursor != null) {
        if (cursor.moveToFirst())
            download = getDownload(cursor)
        cursor.close()
    }
    return download
}

fun DBHelper.getDownloadNovelNames(): List<String> {
    val list = ArrayList<String>()
    val selectQuery = "SELECT DISTINCT(${DBKeys.KEY_NAME}) AS ${DBKeys.KEY_NAME} FROM " + DBKeys.TABLE_DOWNLOAD
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    // looping through all rows and adding to list
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    return list
}


fun DBHelper.hasDownloadsInQueue(novelName: String): Boolean {
    var hasDownloadsInQueue = false
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD + " WHERE " +
            DBKeys.KEY_NAME + " = \"" + novelName + "\" AND " +
            DBKeys.KEY_STATUS + " = " + Download.STATUS_IN_QUEUE + " LIMIT 1"

    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    // looping through all rows and adding to list
    if (cursor != null) {
        hasDownloadsInQueue = cursor.moveToFirst()
        cursor.close()
    }

    return hasDownloadsInQueue
}

private fun getDownload(cursor: Cursor): Download {
    val download = Download(cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_WEB_PAGE_ID)), "", "")
    download.novelName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
    download.chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
    download.status = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_STATUS))
    download.orderId = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
    download.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
    return download
}



