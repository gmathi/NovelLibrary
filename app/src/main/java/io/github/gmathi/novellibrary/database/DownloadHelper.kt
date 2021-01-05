package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.database.Download
import java.util.*
import kotlin.collections.HashMap


//private const val LOG = "DownloadHelper"

fun DBHelper.createDownload(download: Download, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    //Check 1st
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_WEB_PAGE_URL} = ?"
    val cursor = writableDatabase.rawQuery(selectQuery, arrayOf(download.webPageUrl))
    val recordExists = cursor != null && cursor.count > 0
    cursor.close()
    if (recordExists)
        return

    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, download.novelName)
    values.put(DBKeys.KEY_WEB_PAGE_URL, download.webPageUrl)
    values.put(DBKeys.KEY_CHAPTER, download.chapter)
    values.put(DBKeys.KEY_STATUS, Download.STATUS_PAUSED)
    values.put(DBKeys.KEY_ORDER_ID, download.orderId)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(HashMap<String, String?>()))
    writableDatabase.insert(DBKeys.TABLE_DOWNLOAD, null, values)
}

fun DBHelper.getDownload(webPageUrl: String): Download? {
    var download: Download? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_WEB_PAGE_URL} = ?"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(webPageUrl))
    if (cursor != null) {
        if (cursor.moveToFirst())
            download = getDownload(cursor)
        cursor.close()
    }
    return download
}

private fun getDownload(cursor: Cursor): Download {
    val download = Download(
        webPageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_WEB_PAGE_URL)),
        novelName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)),
        chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
    )
    download.status = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_STATUS))
    download.orderId = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
    download.metadata = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
    return download
}

fun DBHelper.getAllDownloads(): List<Download> {
    val list = ArrayList<Download>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD}"
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
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

fun DBHelper.getAllDownloadsForNovel(novelName: String): List<Download> {
    val list = ArrayList<Download>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NAME} = ?"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelName))
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


fun DBHelper.updateDownloadStatusWebPageUrl(status: Int, webPageUrl: String) {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_WEB_PAGE_URL + " = ?", arrayOf(webPageUrl)).toLong()
}

fun DBHelper.updateDownloadStatusNovelName(status: Int, novelName: String) {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, DBKeys.KEY_NAME + " = ?", arrayOf(novelName)).toLong()
}

fun DBHelper.updateDownloadStatus(status: Int): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD, values, null, null).toLong()
}

fun DBHelper.deleteDownload(webPageUrl: String) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_WEB_PAGE_URL + " = ?", arrayOf(webPageUrl))
}

fun DBHelper.deleteDownloads(novelName: String) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD, DBKeys.KEY_NAME + " = ?", arrayOf(novelName))
}

fun DBHelper.getDownloadItemInQueue(): Download? {
    var download: Download? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} LIMIT 1"
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst())
            download = getDownload(cursor)
        cursor.close()
    }
    return download
}

fun DBHelper.getDownloadItemInQueue(novelName: String): Download? {
    var download: Download? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} AND ${DBKeys.KEY_NAME} = ? LIMIT 1"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelName))
    if (cursor != null) {
        if (cursor.moveToFirst())
            download = getDownload(cursor)
        cursor.close()
    }
    return download
}

fun DBHelper.getDownloadNovelNames(): List<String> {
    val list = ArrayList<String>()
    val selectQuery = "SELECT DISTINCT(${DBKeys.KEY_NAME}) FROM ${DBKeys.TABLE_DOWNLOAD}"
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
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
    val selectQuery = "SELECT COUNT(*) FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NAME} = ? AND ${DBKeys.KEY_STATUS} = ${Download.STATUS_IN_QUEUE} LIMIT 1"
    return (DatabaseUtils.longForQuery(this.readableDatabase, selectQuery, arrayOf(novelName)).toInt() > 0)
}

fun DBHelper.getRemainingDownloadsCountForNovel(novelName: String): Int {
    val selectQuery = "SELECT COUNT(*) FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NAME} = ?"
    return DatabaseUtils.longForQuery(this.readableDatabase, selectQuery, arrayOf(novelName)).toInt()
}

fun DBHelper.getAllDownloadsForNovel(novelName: String, downloadUrls: List<String>): List<Download> {
    val list = ArrayList<Download>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_DOWNLOAD} WHERE ${DBKeys.KEY_NAME} = ? AND ${DBKeys.KEY_WEB_PAGE_URL} IN (${TextUtils.join(",", Collections.nCopies(downloadUrls.count(), "?"))})"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelName) + downloadUrls.toTypedArray())
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



