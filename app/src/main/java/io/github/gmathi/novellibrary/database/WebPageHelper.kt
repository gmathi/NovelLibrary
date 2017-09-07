package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Constants
import java.util.*
import kotlin.collections.ArrayList



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
    values.put(DBKeys.KEY_IS_READ, webPage.isRead)
    values.put(DBKeys.KEY_ORDER_ID, webPage.orderId)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPage.metaData))

    return db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
}

fun DBHelper.createWebPage(webPage: WebPage, db: SQLiteDatabase): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPage.url)
    values.put(DBKeys.KEY_REDIRECT_URL, webPage.redirectedUrl)
    values.put(DBKeys.KEY_TITLE, webPage.title)
    values.put(DBKeys.KEY_CHAPTER, webPage.chapter)
    values.put(DBKeys.KEY_FILE_PATH, webPage.filePath)
    values.put(DBKeys.KEY_NOVEL_ID, webPage.novelId)
    values.put(DBKeys.KEY_IS_READ, webPage.isRead)
    values.put(DBKeys.KEY_ORDER_ID, webPage.orderId)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPage.metaData))
    return db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
}


fun DBHelper.addWebPages(webPages: ArrayList<WebPage>, novel: Novel, pageNum: Int) {

    for (i in 0..webPages.size - 1) {
        val orderId = (novel.chapterCount - (Constants.CHAPTER_PAGE_SIZE * pageNum) - 1 - i)
        val webPage = getWebPage(novel.id, orderId)
        if (webPage == null) {
            webPages[i].orderId = orderId
            webPages[i].novelId = novel.id
            createWebPage(webPages[i])
        }
    }
}

fun DBHelper.addWebPages(webPages: List<WebPage>, novel: Novel) {
    val db = this.writableDatabase
    db.beginTransaction()
    try {
        for (i in 0 until webPages.size) {
            val webPage = getWebPage(novel.id, i.toLong())
            if (webPage == null) {
                webPages[i].orderId = i.toLong()
                webPages[i].novelId = novel.id
                createWebPage(webPages[i], db)
            }
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

fun DBHelper.addWebPagesFromImportList(webPages: List<WebPage>, novel: Novel, bookmarkOrderId: Int) {
    val db = this.writableDatabase
    db.beginTransaction()
    try {
        for (i in 0 until webPages.size) {
            val webPage = getWebPage(novel.id, i.toLong())
            if (webPage == null) {
                webPages[i].orderId = i.toLong()
                webPages[i].novelId = novel.id
                webPages[i].isRead = if (bookmarkOrderId > i) 0 else 1
                val webPageId = createWebPage(webPages[i], db)
                if (bookmarkOrderId == i) {
                    updateBookmarkCurrentWebPageId(novel.id, webPageId)
                }
            }
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

fun DBHelper.getWebPageByWebPageId(webPageId: Long): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_ID + " = " + webPageId
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
            webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
            webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
            webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getWebPage(novelId: Long, orderId: Long): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND " + DBKeys.KEY_ORDER_ID + " = " + orderId
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
            webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
            webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
            webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return webPage
}


fun DBHelper.getLatestWebPageOrderId(novelId: Long): Long {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " ORDER BY " + DBKeys.KEY_ORDER_ID + " DESC LIMIT 1"
    Log.d(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            return cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
        }
        cursor.close()
    }
    return -1L
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
            webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
            webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
            webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getWebPageByRedirectedUrl(novelId: Long, redirectedUrl: String): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND " + DBKeys.KEY_REDIRECT_URL + " = " + "\"" + redirectedUrl + "\""
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
            webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
            webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
            webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return webPage
}


fun DBHelper.getWebPages(novel: Novel, pageNum: Int): ArrayList<WebPage> {
    val list = ArrayList<WebPage>()
    val orderIdStartIndex = novel.chapterCount - (Constants.CHAPTER_PAGE_SIZE * pageNum) - 1
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novel.id + " AND " + DBKeys.KEY_ORDER_ID + " <= " + orderIdStartIndex + " AND " + DBKeys.KEY_ORDER_ID + " > " + (orderIdStartIndex - Constants.CHAPTER_PAGE_SIZE) + " ORDER BY " + DBKeys.KEY_ORDER_ID + " DESC"
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
                webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
                webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
                webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)

                list.add(webPage)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list

}

fun DBHelper.getAllWebPages(novelId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " ORDER BY " + DBKeys.KEY_ORDER_ID + " ASC"
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
                webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
                webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
                webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)

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
                webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
                webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
                webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)

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
                webPage.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
                webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
                webPage.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)

                list.add(webPage)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getAllReadableWebPagesCount(novelId: Long): Int {
    var count = 0
    val selectQuery = "SELECT COUNT(" + DBKeys.KEY_ID + ") FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND file_path IS NOT NULL"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
    }
    return count
}


fun DBHelper.updateWebPage(webPage: WebPage): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_TITLE, webPage.title)
    values.put(DBKeys.KEY_REDIRECT_URL, webPage.redirectedUrl)
    values.put(DBKeys.KEY_FILE_PATH, webPage.filePath)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPage.metaData))

    return this.writableDatabase.update(DBKeys.TABLE_WEB_PAGE, values, DBKeys.KEY_ID + " = ? ", arrayOf(webPage.id.toString())).toLong()
}

fun DBHelper.updateWebPageReadStatus(webPage: WebPage): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_IS_READ, webPage.isRead)
    return this.writableDatabase.update(DBKeys.TABLE_WEB_PAGE, values, DBKeys.KEY_ID + " = ? ", arrayOf(webPage.id.toString())).toLong()
}


fun DBHelper.deleteWebPage(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}



