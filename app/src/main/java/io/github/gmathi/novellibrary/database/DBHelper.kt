package io.github.gmathi.novellibrary.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelGenre


class DBHelper(context: Context) : SQLiteOpenHelper(context, DBKeys.DATABASE_NAME, null, DBKeys.DATABASE_VERSION) {

    private val LOG = "DBHelper"

    override fun onCreate(db: SQLiteDatabase) {
        // creating required tables
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_TABLE_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD_QUEUE)
        db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
        db.execSQL("INSERT INTO " + DBKeys.TABLE_SOURCE + " (" + DBKeys.KEY_ID + ", " + DBKeys.KEY_NAME + ") VALUES (-1, 'All')")

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        var version = oldVersion

        if (version == DBKeys.INITIAL_VERSION) {
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_ORDER_ID + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_ORDER_ID + "=" + DBKeys.KEY_ID)
            version = DBKeys.VER_NOVEL_ORDER_ID
        }

        if (version == DBKeys.VER_NOVEL_ORDER_ID) {
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_CHAPTER_COUNT + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_CHAPTER_COUNT + "=0")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_CURRENT_WEB_PAGE_ID + "=-1")
            db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_WEB_PAGE)
            db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
            version = DBKeys.VER_WEB_PAGE_ORDER_ID
        }

        if (version == DBKeys.VER_WEB_PAGE_ORDER_ID) {
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_NEW_CHAPTER_COUNT + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_NEW_CHAPTER_COUNT + "=" + DBKeys.KEY_CHAPTER_COUNT)
            version = DBKeys.VER_NOVEL_SYNC
        }

        if (version == DBKeys.VER_NOVEL_SYNC) {
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_WEB_PAGE + " ADD COLUMN " + DBKeys.KEY_SOURCE_ID + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_WEB_PAGE + " SET " + DBKeys.KEY_SOURCE_ID + "= -1")
            db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
            db.execSQL("INSERT INTO " + DBKeys.TABLE_SOURCE + " (" + DBKeys.KEY_ID + ", " + DBKeys.KEY_NAME + ") VALUES (-1, 'All')")
            version = DBKeys.VER_CHAPTER_SOURCE
        }

        if (version == DBKeys.VER_CHAPTER_SOURCE) {
            //In Case the version is updated again!

        }

        // on upgrade drop older tables
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL)
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_WEB_PAGE)
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_GENRE)
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL_GENRE)
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_DOWNLOAD_QUEUE)

        // create new tables
        //onCreate(db)
    }

    fun removeAll() {
        // db.delete(String tableName, String whereClause, String[] whereArgs);
        // If whereClause is null, it will delete all rows.
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_WEB_PAGE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_GENRE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL_GENRE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_DOWNLOAD_QUEUE)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_SOURCE)
//        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_CHAPTERS_DOWNLOADS)
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

