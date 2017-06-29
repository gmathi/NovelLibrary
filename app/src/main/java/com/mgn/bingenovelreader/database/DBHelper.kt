package com.mgn.bingenovelreader.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.mgn.bingenovelreader.models.DownloadQueue
import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.models.NovelGenre
import com.mgn.bingenovelreader.utils.Constants


class DBHelper(context: Context) : SQLiteOpenHelper(context, DBKeys.DATABASE_NAME, null, DBKeys.DATABASE_VERSION) {

    private val LOG = "DBHelper"

    override fun onCreate(db: SQLiteDatabase) {
        // creating required tables
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_TABLE_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD_QUEUE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_WEB_PAGE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_GENRE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL_GENRE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_DOWNLOAD_QUEUE)

        // create new tables
        onCreate(db)
    }

    // Custom Methods
    fun insertNovel(novel: Novel): Long {
        val novelId = createNovel(novel)
        novel.genres?.forEach {
            val genreId = createGenre(it)
            createNovelGenre(NovelGenre(novelId, genreId))
        }
        return novelId
    }


    fun getFirstDownloadableQueueItem(): DownloadQueue? {
        val selectQuery = "SELECT * FROM " + DBKeys.TABLE_DOWNLOAD_QUEUE + " WHERE " + DBKeys.KEY_STATUS + " = " + Constants.STATUS_DOWNLOAD + " ORDER BY " + DBKeys.KEY_NOVEL_ID + " ASC LIMIT 1"
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

    fun getDownloadedChapterCount(novelId: Long): Int {
        val selectQuery = "SELECT count(novel_id) FROM " + DBKeys.TABLE_WEB_PAGE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId + " AND file_path IS NOT NULL"
        Log.d(LOG, selectQuery)
        val cursor = this.readableDatabase.rawQuery(selectQuery, null)
        var currentChapterCount = 0
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                currentChapterCount = cursor.getInt(0)
            }
            cursor.close()
        }
        return currentChapterCount
    }

    fun cleanupNovelData(novelId: Long) {
        deleteNovel(novelId)
        deleteWebPage(novelId)
        deleteNovelGenre(novelId)
        deleteDownloadQueue(novelId)
    }


}

