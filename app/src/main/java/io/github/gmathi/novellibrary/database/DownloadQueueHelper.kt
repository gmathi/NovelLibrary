package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.DownloadQueue
import io.github.gmathi.novellibrary.util.Constants
import java.util.*
import kotlin.collections.HashMap


private val LOG = "DownloadQueueHelper"

fun DBHelper.createDownloadQueue(novelId: Long): Boolean {
    val dq = getDownloadQueue(novelId)
    if (dq != null) return false

    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_ID, novelId)
    values.put(DBKeys.KEY_STATUS, 0)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(HashMap<String, String?>()))

    this.writableDatabase.insert(DBKeys.TABLE_DOWNLOAD_QUEUE, null, values)
    return true
}

fun DBHelper.getDownloadQueue(novelId: Long): DownloadQueue? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD_QUEUE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    var dq: DownloadQueue? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            dq = DownloadQueue()
            dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
            dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
            dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return dq
}

fun DBHelper.getAllDownloadQueue(): List<DownloadQueue> {
    val list = ArrayList<DownloadQueue>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD_QUEUE
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    // looping through all rows and adding to list
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val dq = DownloadQueue()
                dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
                dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
                dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)

                list.add(dq)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    return list
}

fun DBHelper.getAllUnfinishedDownloadQueues(): List<DownloadQueue> {
    val list = ArrayList<DownloadQueue>()
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD_QUEUE + " WHERE ${DBKeys.KEY_STATUS} != ${Constants.STATUS_COMPLETE}"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)

    // looping through all rows and adding to list
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val dq = DownloadQueue()
                dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
                dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
                dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)

                list.add(dq)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    return list
}

fun DBHelper.updateDownloadQueue(downloadQueue: DownloadQueue): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_ID, downloadQueue.novelId)
    values.put(DBKeys.KEY_STATUS, downloadQueue.status)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(downloadQueue.metaData))

    // updating row
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(downloadQueue.novelId.toString())).toLong()
}

fun DBHelper.updateDownloadQueueStatus(status: Long, novelId: Long): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateAllDownloadQueueStatuses(status: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, "${DBKeys.KEY_STATUS} != ${Constants.STATUS_COMPLETE}", null)
}

fun DBHelper.deleteDownloadQueue(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD_QUEUE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}


fun DBHelper.getFirstDownloadableQueueItem(): DownloadQueue? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD_QUEUE + " WHERE " + DBKeys.KEY_STATUS + " = " + Constants.STATUS_DOWNLOAD + " ORDER BY " + DBKeys.KEY_NOVEL_ID + " ASC LIMIT 1"
    Log.d(LOG, selectQuery)
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    var dq: DownloadQueue? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            dq = DownloadQueue()
            dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
            dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
            dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
        }
        cursor.close()
    }
    return dq
}


