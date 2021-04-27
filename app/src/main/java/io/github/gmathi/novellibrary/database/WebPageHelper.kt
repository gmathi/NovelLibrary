package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import io.github.gmathi.novellibrary.model.database.WebPage


//private const val LOG = "WebPageHelper"

fun DBHelper.createWebPage(webPage: WebPage, db: SQLiteDatabase? = null): Boolean {
    val writableDatabase = db ?: this.writableDatabase
    //Check 1st
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_URL} = ? LIMIT 1"
    val cursor = writableDatabase.rawQuery(selectQuery, arrayOf(webPage.url))
    val recordExists = cursor != null && cursor.count > 0
    cursor.close()
    if (recordExists)
        return false

    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPage.url)
    values.put(DBKeys.KEY_CHAPTER, webPage.chapterName)
    values.put(DBKeys.KEY_NOVEL_ID, webPage.novelId)
    values.put(DBKeys.KEY_ORDER_ID, webPage.orderId)
    values.put(DBKeys.KEY_TRANSLATOR_SOURCE_NAME, webPage.translatorSourceName)
    return writableDatabase.insert(DBKeys.TABLE_WEB_PAGE, null, values) != -1L
}

fun DBHelper.getWebPage(url: String): WebPage? {
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_URL} = ?"
    var webPage: WebPage? = null
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(url))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getAllWebPages(novelId: Long): ArrayList<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(getWebPageFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getAllWebPages(novelId: Long, translatorSourceName: String?): List<WebPage> {
    if (translatorSourceName == null) {
        return getAllWebPages(novelId)
    }

    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), translatorSourceName))

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(getWebPageFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.deleteWebPages(novelId: Long, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}

fun DBHelper.deleteWebPage(url: String, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, "${DBKeys.KEY_URL} = ?", arrayOf(url))
}

private fun getWebPageFromCursor(cursor: Cursor): WebPage {
    val webPage = WebPage(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER)))
    webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
    webPage.translatorSourceName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TRANSLATOR_SOURCE_NAME))
    webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
    return webPage
}

fun DBHelper.getWebPage(novelId: Long, offset: Int): WebPage? {
    var webPage: WebPage? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT ?, 1"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), offset.toString()))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getWebPage(novelId: Long, translatorSourceName: String?, offset: Int): WebPage? {
    if (translatorSourceName == null) {
        return getWebPage(novelId, offset)
    }

    var webPage: WebPage? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT ?, 1"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString(), translatorSourceName, offset.toString()))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}



