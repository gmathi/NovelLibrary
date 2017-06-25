package com.mgn.bingenovelreader.database

import android.content.ContentValues
import android.util.Log
import com.mgn.bingenovelreader.models.DownloadQueue
import java.util.*


private val LOG = "DownloadQueueHelper"

fun DBHelper.createDownloadQueue(novelId: Long): Boolean {
    val dq = getDownloadQueue(novelId)
    if (dq != null) return false

    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_ID, novelId)
    values.put(DBKeys.KEY_STATUS, 0)
    values.put(DBKeys.KEY_TOTAL_CHAPTERS, -1)
    values.put(DBKeys.KEY_CURRENT_CHAPTER, -1)
    values.put(DBKeys.KEY_CHAPTER_URLS_CACHED, 0)

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
            dq.totalChapters = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_TOTAL_CHAPTERS))
            dq.currentChapter = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CURRENT_CHAPTER))
            dq.chapterUrlsCached = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTER_URLS_CACHED))
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
                dq.totalChapters = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_TOTAL_CHAPTERS))
                dq.currentChapter = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CURRENT_CHAPTER))
                dq.chapterUrlsCached = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTER_URLS_CACHED))

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
    values.put(DBKeys.KEY_TOTAL_CHAPTERS, downloadQueue.totalChapters)
    values.put(DBKeys.KEY_CURRENT_CHAPTER, downloadQueue.currentChapter)
    values.put(DBKeys.KEY_CHAPTER_URLS_CACHED, downloadQueue.chapterUrlsCached)

    // updating row
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(downloadQueue.novelId.toString())).toLong()
}

fun DBHelper.updateDownloadQueueStatus(status: Long, novelId: Long): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateDownloadQueueChapterCount(totalChapters: Long, currentChapter: Long, novelId: Long): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_TOTAL_CHAPTERS, totalChapters)
    values.put(DBKeys.KEY_CURRENT_CHAPTER, currentChapter)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateChapterUrlsCached(chapterUrlsCached: Long, novelId: Long): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_CHAPTER_URLS_CACHED, chapterUrlsCached)
    return this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateDownloadQueueStatus(status: Int) {
    val values = ContentValues()
    values.put(DBKeys.KEY_STATUS, status)
    this.writableDatabase.update(DBKeys.TABLE_DOWNLOAD_QUEUE, values, null, null)
}

fun DBHelper.deleteDownloadQueue(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_DOWNLOAD_QUEUE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}

