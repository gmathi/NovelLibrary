package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.util.Logs


private const val LOG = "WebPageHelper"

fun DBHelper.createWebPage(webPage: WebPage): Long {
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPage.url)
    values.put(DBKeys.KEY_CHAPTER, webPage.chapter)
    values.put(DBKeys.KEY_NOVEL_ID, webPage.novelId)
    values.put(DBKeys.KEY_ORDER_ID, webPage.orderId)
    values.put(DBKeys.KEY_SOURCE_ID, webPage.sourceId)

    return db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
}

fun DBHelper.getWebPage(url: String): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_URL} = \"$url\""
    Logs.debug(LOG, selectQuery)
    var webPage: WebPage? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}

//fun DBHelper.getWebPage(novelId: Long, orderId: Long): WebPage? {
//    val db = this.readableDatabase
//    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND " + DBKeys.KEY_ORDER_ID + " = " + orderId
//    Logs.debug(LOG, selectQuery)
//    var webPage: WebPage? = null
//    val cursor = db.rawQuery(selectQuery, null)
//    if (cursor != null) {
//        if (cursor.moveToFirst()) {
//            webPage = getWebPageFromCursor(cursor)
//        }
//        cursor.close()
//    }
//    return webPage
//}

fun DBHelper.getWebPage(novelId: Long, url: String): WebPage? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_URL} = \"$url\""
    Logs.debug(LOG, selectQuery)
    var webPage: WebPage? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getAllWebPages(novelId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " ORDER BY " + DBKeys.KEY_ORDER_ID + " ASC"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

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

fun DBHelper.getAllWebPages(novelId: Long, sourceId: Long): List<WebPage> {
    val list = ArrayList<WebPage>()
    val selectQuery =
            if (sourceId == -1L)
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
            else
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_SOURCE_ID} = $sourceId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

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

fun DBHelper.deleteWebPages(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}

fun DBHelper.deleteWebPage(novelId: Long, orderId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE, "${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_ORDER_ID} = ?", arrayOf(novelId.toString(), orderId.toString()))
}


private fun getWebPageFromCursor(cursor: Cursor): WebPage {
    val webPage = WebPage(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER)))
    webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
    webPage.sourceId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_SOURCE_ID))
    webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))

    return webPage
}

fun DBHelper.getWebPage(novelId: Long, sourceId: Long, offset: Int): WebPage? {
    val selectQuery =
            if (sourceId == -1L)
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT $offset, 1"
            else
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_SOURCE_ID} = $sourceId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC LIMIT $offset, 1"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase

    var webPage: WebPage? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPage = getWebPageFromCursor(cursor)
        }
        cursor.close()
    }
    return webPage
}

fun DBHelper.getWebPagesCount(novelId: Long, sourceId: Long): Int {
    val selectQuery =
            if (sourceId == -1L)
                "SELECT COUNT(*) FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId"
            else
                "SELECT COUNT(*) FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_SOURCE_ID} = $sourceId"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    return DatabaseUtils.longForQuery(db, selectQuery, null).toInt()
}



