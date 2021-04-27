package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Constants
import java.util.*
import kotlin.collections.ArrayList

private const val LOG = "WebPageSettingsHelper"

fun DBHelper.createWebPageSettings(webPageSettings: WebPageSettings, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    //Check 1st
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_URL} = ? LIMIT 1"
    val cursor = writableDatabase.rawQuery(selectQuery, arrayOf(webPageSettings.url))
    val recordExists = cursor != null && cursor.count > 0
    cursor.close()
    if (recordExists)
        return

    //Insert otherwise
    val values = ContentValues()
    values.put(DBKeys.KEY_URL, webPageSettings.url)
    values.put(DBKeys.KEY_NOVEL_ID, webPageSettings.novelId)
    values.put(DBKeys.KEY_REDIRECT_URL, webPageSettings.redirectedUrl)
    values.put(DBKeys.KEY_TITLE, webPageSettings.title)
    values.put(DBKeys.KEY_FILE_PATH, webPageSettings.filePath)
    values.put(DBKeys.KEY_IS_READ, if (webPageSettings.isRead) 1 else 0)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPageSettings.metadata))

    writableDatabase.insert(DBKeys.TABLE_WEB_PAGE_SETTINGS, null, values)
}

fun DBHelper.getWebPageSettings(url: String, db: SQLiteDatabase? = null): WebPageSettings? {
    val readableDatabase = db ?: this.readableDatabase
    var webPageSettings: WebPageSettings? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_URL} = ?"
    val cursor = readableDatabase.rawQuery(selectQuery, arrayOf(url))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPageSettings = getWebPageSettingsFromCursor(cursor)
        }
        cursor.close()
    }
    return webPageSettings
}

fun DBHelper.getWebPageSettingsByRedirectedUrl(redirectedUrl: String, db: SQLiteDatabase? = null): WebPageSettings? {
    val readableDatabase = db ?: this.readableDatabase
    var webPageSettings: WebPageSettings? = null
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_REDIRECT_URL} = ?"
    val cursor = readableDatabase.rawQuery(selectQuery, arrayOf(redirectedUrl))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            webPageSettings = getWebPageSettingsFromCursor(cursor)
        }
        cursor.close()
    }
    return webPageSettings
}

fun DBHelper.getAllWebPageSettings(novelId: Long, db: SQLiteDatabase? = null): ArrayList<WebPageSettings> {
    val readableDatabase = db ?: this.readableDatabase
    val list = ArrayList<WebPageSettings>()
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_NOVEL_ID} = ?"
    val cursor = readableDatabase.rawQuery(selectQuery, arrayOf(novelId.toString()))
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


private fun getWebPageSettingsFromCursor(cursor: Cursor): WebPageSettings {
    val webPageSettings = WebPageSettings(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID)))
    webPageSettings.redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
    webPageSettings.title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
    webPageSettings.filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
    webPageSettings.isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ)) == 1
    webPageSettings.metadata = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
    return webPageSettings
}


fun DBHelper.updateWebPageSettingsReadStatus(webPageSettings: WebPageSettings, markRead: Boolean, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    if (!markRead) {
        webPageSettings.metadata.remove(Constants.MetaDataKeys.SCROLL_POSITION)
    }
    val values = ContentValues()
    values.put(DBKeys.KEY_IS_READ, if (markRead) 1 else 0)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPageSettings.metadata))

    writableDatabase.update(DBKeys.TABLE_WEB_PAGE_SETTINGS, values, "${DBKeys.KEY_URL} = ?", arrayOf(webPageSettings.url))
}

fun DBHelper.updateWebPageSettings(webPageSettings: WebPageSettings, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_TITLE, webPageSettings.title)
    values.put(DBKeys.KEY_REDIRECT_URL, webPageSettings.redirectedUrl)
    values.put(DBKeys.KEY_FILE_PATH, webPageSettings.filePath)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(webPageSettings.metadata))
    writableDatabase.update(DBKeys.TABLE_WEB_PAGE_SETTINGS, values, "${DBKeys.KEY_URL} = ?", arrayOf(webPageSettings.url))
}


fun DBHelper.deleteWebPageSettings(novelId: Long, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    writableDatabase.delete(DBKeys.TABLE_WEB_PAGE_SETTINGS, "${DBKeys.KEY_NOVEL_ID} = ?", arrayOf(novelId.toString()))
}

fun DBHelper.deleteWebPageSettings(url: String, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    writableDatabase.delete(DBKeys.TABLE_WEB_PAGE_SETTINGS, "${DBKeys.KEY_URL} = ?", arrayOf(url))
}


