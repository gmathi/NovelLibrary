package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants

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

        @Synchronized
        fun refreshInstance(context: Context): DBHelper {
            sInstance = DBHelper(context.applicationContext)
            return sInstance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // creating required tables
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE_SETTINGS)
        db.execSQL(DBKeys.CREATE_TABLE_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
        db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
        db.execSQL(DBKeys.CREATE_TABLE_LARGE_PREFERENCE)

        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE_SETTINGS)

        insertDefaultValues(db)
    }

    private fun insertDefaultValues(db: SQLiteDatabase) {
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    private fun copyDataToDownloads(db: SQLiteDatabase) {
        val selectQuery =
            "SELECT d.${DBKeys.KEY_NAME}, d.${DBKeys.KEY_CHAPTER}, d.${DBKeys.KEY_STATUS}, d.${DBKeys.KEY_ORDER_ID}, d.${DBKeys.KEY_METADATA}, w.${DBKeys.KEY_URL} FROM ${DBKeys.TABLE_DOWNLOAD}_old d, ${DBKeys.TABLE_WEB_PAGE} w WHERE d.${DBKeys.KEY_WEB_PAGE_ID} = w.${DBKeys.KEY_ID}"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {

                    val values = ContentValues()
                    values.put(DBKeys.KEY_NAME, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)))
                    values.put(DBKeys.KEY_WEB_PAGE_URL, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)))
                    values.put(DBKeys.KEY_CHAPTER, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER)))
                    values.put(DBKeys.KEY_STATUS, cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_STATUS)))
                    values.put(DBKeys.KEY_ORDER_ID, cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID)))
                    values.put(DBKeys.KEY_METADATA, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)))

                    db.insert(DBKeys.TABLE_DOWNLOAD, null, values)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    private fun copyDataToWebPageSettings(db: SQLiteDatabase) {
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE}_old"
        val cursor = db.rawQuery(selectQuery, null)

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val webPageValues = ContentValues()
                    val webPageSettingsValues = ContentValues()

                    webPageValues.put(DBKeys.KEY_URL, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)))
                    webPageValues.put(DBKeys.KEY_CHAPTER, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER)))
                    webPageValues.put(DBKeys.KEY_NOVEL_ID, cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID)))
                    webPageValues.put(DBKeys.KEY_ORDER_ID, cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID)))
                    webPageValues.put(DBKeys.KEY_SOURCE_ID, cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_SOURCE_ID)))

                    webPageSettingsValues.put(DBKeys.KEY_URL, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)))
                    webPageSettingsValues.put(DBKeys.KEY_NOVEL_ID, cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID)))
                    webPageSettingsValues.put(DBKeys.KEY_REDIRECT_URL, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL)))
                    webPageSettingsValues.put(DBKeys.KEY_TITLE, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE)))
                    webPageSettingsValues.put(DBKeys.KEY_FILE_PATH, cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH)))
                    webPageSettingsValues.put(DBKeys.KEY_IS_READ, cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ)))
                    webPageSettingsValues.put(DBKeys.KEY_METADATA, "{}")

                    db.insert(DBKeys.TABLE_WEB_PAGE, null, webPageValues)
                    db.insert(DBKeys.TABLE_WEB_PAGE_SETTINGS, null, webPageSettingsValues)

                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    fun removeAll() {
        // db.delete(String tableName, String whereClause, String[] whereArgs);
        // If whereClause is null, it will delete all rows.
        val db = writableDatabase
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_NOVEL}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_WEB_PAGE}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_WEB_PAGE_SETTINGS}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_GENRE}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_NOVEL_GENRE}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_DOWNLOAD_QUEUE}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_SOURCE}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_DOWNLOAD}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_NOVEL_SECTION}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_LARGE_PREFERENCE}")

        db.execSQL("DROP INDEX IF EXISTS ${DBKeys.INDEX_WEB_PAGE}")
        db.execSQL("DROP INDEX IF EXISTS ${DBKeys.INDEX_WEB_PAGE_SETTINGS}")

        // create new tables
        onCreate(db)
    }

    // Custom Methods

    fun cleanupNovelData(novel: Novel) {
        deleteNovel(novel.id)
        deleteWebPages(novel.id)
        deleteWebPageSettings(novel.id)
        deleteNovelGenre(novel.id)
        deleteDownloads(novel.name)
    }
}

