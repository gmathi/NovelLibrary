package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.DatabaseUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.WebPageSettings
import io.github.gmathi.novellibrary.util.Logs
import java.util.*
import kotlin.collections.ArrayList


private const val LOG = "WebPageSettingsHelper"

fun DBHelper.createWebPageSettings(webPageSettings: WebPageSettings): Long {
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPageSettings.url)
    values.put(DBKeys.KEY_NOVEL_ID, webPageSettings.novelId)
    values.put(DBKeys.KEY_REDIRECT_URL, webPageSettings.redirectedUrl)
    values.put(DBKeys.KEY_TITLE, webPageSettings.title)
    values.put(DBKeys.KEY_FILE_PATH, webPageSettings.filePath)
    values.put(DBKeys.KEY_IS_READ, webPageSettings.isRead)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPageSettings.metaData))

    return db.insert(DBKeys.TABLE_WEB_PAGE_SETTINGS, null, values)
}

fun DBHelper.getWebPageSettings(url: String): WebPageSettings? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_URL} = \"$url\""
    Logs.debug(LOG, selectQuery)
    var webPageSettings: WebPageSettings? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPageSettings = getWebPageSettingsFromCursor(cursor)
        }
        cursor.close()
    }
    return webPageSettings
}

fun DBHelper.getWebPageSettingsByRedirectedUrl(redirectedUrl: String): WebPageSettings? {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_REDIRECT_URL} = \"$redirectedUrl\""
    Logs.debug(LOG, selectQuery)
    var webPageSettings: WebPageSettings? = null
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPageSettings = getWebPageSettingsFromCursor(cursor)
        }
        cursor.close()
    }
    return webPageSettings
}

fun DBHelper.getAllWebPageSettings(novelId: Long): List<WebPageSettings> {
    val list = ArrayList<WebPageSettings>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_WEB_PAGE_SETTINGS + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(getWebPageSettingsFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getAllDownloadedWebPageSettingss(novelId: Long): List<WebPageSettings> {
    val list = ArrayList<WebPageSettings>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_FILE_PATH} IS NOT NULL"
    Logs.debug(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(getWebPageSettingsFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getDownloadedWebPageSettingssCount(novelId: Long): Int {
    val selectQuery = "SELECT COUNT(*) FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_NOVEL_ID} = $novelId AND ${DBKeys.KEY_FILE_PATH} IS NOT NULL"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    return DatabaseUtils.longForQuery(db, selectQuery, null).toInt()
}


fun DBHelper.updateWebPageSettings(webPageSettings: WebPageSettings) {
    val values = ContentValues()
    values.put(DBKeys.KEY_TITLE, webPageSettings.title)
    values.put(DBKeys.KEY_REDIRECT_URL, webPageSettings.redirectedUrl)
    values.put(DBKeys.KEY_FILE_PATH, webPageSettings.filePath)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPageSettings.metaData))

    this.writableDatabase.update(DBKeys.TABLE_WEB_PAGE_SETTINGS, values, DBKeys.KEY_URL + " = ? ", arrayOf(webPageSettings.url))
}

fun DBHelper.updateWebPageSettingsReadStatus(webPageSettings: WebPageSettings) {
    val values = ContentValues()
    values.put(DBKeys.KEY_IS_READ, webPageSettings.isRead)
    this.writableDatabase.update(DBKeys.TABLE_WEB_PAGE_SETTINGS, values, DBKeys.KEY_URL + " = ? ", arrayOf(webPageSettings.url))
}


fun DBHelper.deleteWebPageSettings(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE_SETTINGS, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}

fun DBHelper.deleteWebPageSettings(url: String) {
    this.writableDatabase.delete(DBKeys.TABLE_WEB_PAGE_SETTINGS, DBKeys.KEY_URL + " = ?", arrayOf(url))
}

private fun getWebPageSettingsFromCursor(cursor: Cursor): WebPageSettings {
    val webPageSettings = WebPageSettings(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID)))
    webPageSettings.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
    webPageSettings.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
    webPageSettings.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
    webPageSettings.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ))
    webPageSettings.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)

    return webPageSettings
}