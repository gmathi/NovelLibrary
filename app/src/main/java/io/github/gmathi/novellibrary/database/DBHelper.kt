package io.github.gmathi.novellibrary.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.gmathi.novellibrary.model.Novel


class DBHelper
private constructor(context: Context) : SQLiteOpenHelper(context, DBKeys.DATABASE_NAME, null, DBKeys.DATABASE_VERSION) {

    companion object {
        private const val TAG = "DBHelper"

        private var sInstance: DBHelper? = null

        @Synchronized
        fun getInstance(context: Context): DBHelper {
            if (sInstance == null) {
                sInstance = DBHelper(context.applicationContext)
            }
            return sInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // creating required tables
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_TABLE_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
        db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
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
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_NEW_RELEASES_COUNT + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_NEW_RELEASES_COUNT + "=0")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_CURRENT_WEB_PAGE_ID + "=-1")
            db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_WEB_PAGE)
            db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
            version = DBKeys.VER_WEB_PAGE_ORDER_ID
        }

        if (version == DBKeys.VER_WEB_PAGE_ORDER_ID) {
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_CHAPTERS_COUNT + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_CHAPTERS_COUNT + "=" + DBKeys.KEY_NEW_RELEASES_COUNT)
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
            db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_DOWNLOAD_QUEUE)
            db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
            version = DBKeys.VER_DOWNLOADS
        }

        if (version == DBKeys.VER_DOWNLOADS) {
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_CHAPTERS_COUNT + "=" + DBKeys.KEY_NEW_RELEASES_COUNT)
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_NEW_RELEASES_COUNT + "= 0")
            db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
            db.execSQL("ALTER TABLE " + DBKeys.TABLE_NOVEL + " ADD COLUMN " + DBKeys.KEY_NOVEL_SECTION_ID + " INTEGER")
            db.execSQL("UPDATE " + DBKeys.TABLE_NOVEL + " SET " + DBKeys.KEY_NOVEL_SECTION_ID + "= -1")
            version = DBKeys.VER_NEW_RELEASES
        }

        if (version == DBKeys.VER_NEW_RELEASES) {

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
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_DOWNLOAD)
        db.execSQL("DROP TABLE IF EXISTS " + DBKeys.TABLE_NOVEL_SECTION)

        // create new tables
        onCreate(db)
    }

    // Custom Methods

    fun cleanupNovelData(novel: Novel) {
        deleteNovel(novel.id)
        deleteWebPage(novel.id)
        deleteNovelGenre(novel.id)
        deleteDownloads(novel.name)
    }

}

