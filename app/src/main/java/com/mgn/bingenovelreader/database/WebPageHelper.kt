package com.mgn.bingenovelreader.database

import android.content.ContentValues
import android.util.Log
import com.mgn.bingenovelreader.model.WebPage
import java.util.*

private val LOG = "WebPageHelper"

fun DBHelper.createWebPage(webPage: WebPage): Long {
    val db = this.writableDatabase

    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPage.url)
    values.put(DBKeys.KEY_REDIRECT_URL, webPage.redirectedUrl)
    values.put(DBKeys.KEY_TITLE, webPage.title)
    values.put(DBKeys.KEY_CHAPTER, webPage.chapter)
    values.put(DBKeys.KEY_FILE_PATH, webPage.filePath)
    values.put(DBKeys.KEY_NOVEL_ID, webPage.novelId)

    return db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
}

fun DBHelper.getWebPage(novelId: Long, url: String): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND " + DBKeys.KEY_URL + " = " + "\"" + url + "\""
    Log.d(LOG, selectQuery)
    var webPage: WebPage? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = WebPage()
            webPage.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
            webPage.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
            webPage.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
            webPage.chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
            webPage.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
            webPage.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
            webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getAllWebPages(novelId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " ORDER BY " + DBKeys.KEY_ID + " DESC"
    Log.d(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val webPage = WebPage()
                webPage.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                webPage.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
                webPage.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
                webPage.chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
                webPage.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
                webPage.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
                webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))

                list.add(webPage)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getAllWebPagesToDownload(novelId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND file_path IS NULL ORDER BY " + DBKeys.KEY_ID + " ASC"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val webPage = WebPage()
                webPage.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                webPage.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
                webPage.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
                webPage.chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
                webPage.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
                webPage.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
                webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))

                list.add(webPage)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}


fun DBHelper.getAllReadableWebPages(novelId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND file_path IS NOT NULL ORDER BY " + DBKeys.KEY_ID + " ASC"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val webPage = WebPage()
                webPage.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                webPage.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
                webPage.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
                webPage.chapter = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
                webPage.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
                webPage.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
                webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))

                list.add(webPage)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}


fun DBHelper.updateWebPage(webPage: WebPage): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_TITLE, webPage.title)
    values.put(DBKeys.KEY_REDIRECT_URL, webPage.redirectedUrl)
    values.put(DBKeys.KEY_FILE_PATH, webPage.filePath)

    return this.writableDatabase.update(DBKeys.TABLE_WEB_PAGE, values, DBKeys.KEY_ID + " = ? ", arrayOf(webPage.id.toString())).toLong()
}

fun DBHelper.deleteWebPage(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}

