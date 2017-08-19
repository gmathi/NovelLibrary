//package io.github.gmathi.novellibrary.database
//
//import android.content.ContentValues
//import android.util.Log
//import com.google.gson.Gson
//import com.google.gson.reflect.TypeToken
//import io.github.gmathi.novellibrary.model.ChapterDownloads
//import io.github.gmathi.novellibrary.model.Novel
//import io.github.gmathi.novellibrary.model.WebPage
//import io.github.gmathi.novellibrary.util.Constants
//import java.util.ArrayList
//import kotlin.collections.HashMap
//
//private val LOG = "ChapterDownloadsHelper"
//fun DBHelper.createChapterDownloads(webPage: WebPage): Boolean {
//    val chapterDownloads = getChapterDownloads(webPage.id)
//    if (chapterDownloads != null) return false
//
//    val values = ContentValues()
//    values.put(DBKeys.KEY_NOVEL_ID, webPage.novelId)
//    values.put(DBKeys.KEY_WEB_PAGE_ID, webPage.id)
//    values.put(DBKeys.KEY_STATUS, 0)
//    values.put(DBKeys.KEY_METADATA, Gson().toJson(HashMap<String, String?>()))
//
//    return this.writableDatabase.insert(DBKeys.TABLE_CHAPTERS_DOWNLOADS, null, values) != -1L
//}
//
//fun DBHelper.getChapterDownloads(webPageId: Long): ChapterDownloads? {
//    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_CHAPTERS_DOWNLOADS + " WHERE " + DBKeys.KEY_WEB_PAGE_ID + " = " + webPageId
//    Log.d(LOG, selectQuery)
//    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
//    var dq: ChapterDownloads? = null
//    if (cursor != null) {
//        if (cursor.moveToFirst()) {
//            dq = ChapterDownloads()
//            dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
//            dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
//            dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
//        }
//        cursor.close()
//    }
//    return dq
//}
//
//fun DBHelper.getAllChapterDownloads(): List<ChapterDownloads> {
//    val list = ArrayList<ChapterDownloads>()
//    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_CHAPTERS_DOWNLOADS
//    Log.d(LOG, selectQuery)
//    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
//
//    // looping through all rows and adding to list
//    if (cursor != null) {
//        if (cursor.moveToFirst()) {
//            do {
//                val dq = ChapterDownloads()
//                dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
//                dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
//                dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
//
//                list.add(dq)
//            } while (cursor.moveToNext())
//        }
//        cursor.close()
//    }
//
//    return list
//}
//
//fun DBHelper.getAllUnfinishedChapterDownloads(): List<ChapterDownloads> {
//    val list = ArrayList<ChapterDownloads>()
//    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_CHAPTERS_DOWNLOADS + " WHERE ${DBKeys.KEY_STATUS} != ${Constants.STATUS_COMPLETE}"
//    Log.d(LOG, selectQuery)
//    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
//
//    // looping through all rows and adding to list
//    if (cursor != null) {
//        if (cursor.moveToFirst()) {
//            do {
//                val dq = ChapterDownloads()
//                dq.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
//                dq.status = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_STATUS))
//                dq.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<java.util.HashMap<String, String>>() {}.type)
//
//                list.add(dq)
//            } while (cursor.moveToNext())
//        }
//        cursor.close()
//    }
//
//    return list
//}
//
//fun DBHelper.updateChapterDownloads(ChapterDownloads: ChapterDownloads): Long {
//    val values = ContentValues()
//    values.put(DBKeys.KEY_NOVEL_ID, ChapterDownloads.novelId)
//    values.put(DBKeys.KEY_STATUS, ChapterDownloads.status)
//    values.put(DBKeys.KEY_METADATA, Gson().toJson(ChapterDownloads.metaData))
//
//    // updating row
//    return this.writableDatabase.update(DBKeys.TABLE_CHAPTERS_DOWNLOADS, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(ChapterDownloads.novelId.toString())).toLong()
//}
//
//fun DBHelper.updateChapterDownloadsStatus(status: Long, novelId: Long): Long {
//    val values = ContentValues()
//    values.put(DBKeys.KEY_STATUS, status)
//    return this.writableDatabase.update(DBKeys.TABLE_CHAPTERS_DOWNLOADS, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString())).toLong()
//}
//
//fun DBHelper.updateAllChapterDownloadsStatuses(status: Long) {
//    val values = ContentValues()
//    values.put(DBKeys.KEY_STATUS, status)
//    this.writableDatabase.update(DBKeys.TABLE_CHAPTERS_DOWNLOADS, values, "${DBKeys.KEY_STATUS} != ${Constants.STATUS_COMPLETE}", null)
//}
//
//fun DBHelper.deleteChapterDownloads(novelId: Long) {
//    this.writableDatabase.delete(DBKeys.TABLE_CHAPTERS_DOWNLOADS, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
//}
//
//fun DBHelper.getNovelsFromChapterDownloads(): ArrayList<Novel> {
//    val novels = ArrayList<Novel>()
//    return novels
//}
//
//fun DBHelper.getChaptersCountForNovelFromChapterDownloads(novelId: Long): Int {
//    val selectQuery = "SELECT count(" + DBKeys.KEY_WEB_PAGE_ID + ") FROM " + DBKeys.TABLE_CHAPTERS_DOWNLOADS + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND " + DBKeys.KEY_STATUS + " != 2"
//    Log.d(LOG, selectQuery)
//    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
//    var currentDownloadingCount = 0
//    if (cursor != null) {
//        if (cursor.moveToFirst()) {
//            currentDownloadingCount = cursor.getInt(0)
//        }
//        cursor.close()
//    }
//    return currentDownloadingCount
//}
