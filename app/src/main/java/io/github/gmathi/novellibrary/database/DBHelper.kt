package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.model.Novel
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
        db.execSQL("INSERT INTO ${DBKeys.TABLE_SOURCE} (${DBKeys.KEY_ID}, ${DBKeys.KEY_NAME}) VALUES (-1, 'All')")
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        var version = oldVersion

        if (version == DBKeys.INITIAL_VERSION) {
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_ORDER_ID} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_ORDER_ID}=${DBKeys.KEY_ID}")
            version = DBKeys.VER_NOVEL_ORDER_ID
        }

        if (version == DBKeys.VER_NOVEL_ORDER_ID) {
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_NEW_RELEASES_COUNT} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NEW_RELEASES_COUNT}=0")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CURRENT_WEB_PAGE_ID}=-1")
            db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_WEB_PAGE}")
            db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
            version = DBKeys.VER_WEB_PAGE_ORDER_ID
        }

        if (version == DBKeys.VER_WEB_PAGE_ORDER_ID) {
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_CHAPTERS_COUNT} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CHAPTERS_COUNT}=${DBKeys.KEY_NEW_RELEASES_COUNT}")
            version = DBKeys.VER_NOVEL_SYNC
        }

        if (version == DBKeys.VER_NOVEL_SYNC) {
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_WEB_PAGE} ADD COLUMN ${DBKeys.KEY_SOURCE_ID} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_WEB_PAGE} SET ${DBKeys.KEY_SOURCE_ID}= -1")
            db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
            db.execSQL("INSERT INTO ${DBKeys.TABLE_SOURCE} (${DBKeys.KEY_ID}, ${DBKeys.KEY_NAME}) VALUES (-1, 'All')")
            version = DBKeys.VER_CHAPTER_SOURCE
        }

        if (version == DBKeys.VER_CHAPTER_SOURCE) {
            db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_DOWNLOAD_QUEUE}")
            db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
            version = DBKeys.VER_DOWNLOADS
        }

        if (version == DBKeys.VER_DOWNLOADS) {
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CHAPTERS_COUNT}=${DBKeys.KEY_NEW_RELEASES_COUNT}")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NEW_RELEASES_COUNT}= 0")
            db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_NOVEL_SECTION_ID} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NOVEL_SECTION_ID}= -1")
            version = DBKeys.VER_NEW_RELEASES
        }

        if (version == DBKeys.VER_NEW_RELEASES) {
            db.execSQL(DBKeys.CREATE_TABLE_LARGE_PREFERENCE)

            //Move Novel History Preferences to database because it is a huge data
            val values = ContentValues()
            values.put(DBKeys.KEY_NAME, Constants.LargePreferenceKeys.RVN_HISTORY)
            values.put(DBKeys.KEY_VALUE, "[]")
            db.insert(DBKeys.TABLE_LARGE_PREFERENCE, null, values)

            db.execSQL("CREATE INDEX web_pages_url_id_index ON ${DBKeys.TABLE_WEB_PAGE}(${DBKeys.KEY_ID}, ${DBKeys.KEY_URL})")

            version = DBKeys.VER_LARGE_PREFERENCE
        }

        if (version == DBKeys.VER_LARGE_PREFERENCE) {
            db.execSQL("DROP INDEX web_pages_url_id_index")

            //Update the bookmark to be a web_page.url instead of web_page.id & showSources to false
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_CURRENT_WEB_PAGE_URL} TEXT")
            db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CURRENT_WEB_PAGE_URL} = (SELECT ${DBKeys.KEY_URL} FROM ${DBKeys.TABLE_WEB_PAGE} WHERE  ${DBKeys.TABLE_NOVEL}.${DBKeys.KEY_CURRENT_WEB_PAGE_ID} = ${DBKeys.TABLE_WEB_PAGE}.${DBKeys.KEY_ID})")

            //Modify Downloads Table to use WebPage_URL instead of WebPage_ID
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_DOWNLOAD} RENAME TO ${DBKeys.TABLE_DOWNLOAD}_old")
            db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
            copyDataToDownloads(db)
            db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_DOWNLOAD}_old")

            //Create a new table web_page & web_page_settings, copy data and delete the old one
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_WEB_PAGE} RENAME TO ${DBKeys.TABLE_WEB_PAGE}_old")
            db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
            db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE_SETTINGS)
            copyDataToWebPageSettings(db)
            db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_WEB_PAGE}_old")

            //Create indexes
            db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE)
            db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE_SETTINGS)

            version = DBKeys.VER_WEB_PAGE_SETTINGS
        }

        if (version == DBKeys.VER_WEB_PAGE_SETTINGS) {



            //version = DBKeys.VER_WEB_PAGE_SETTINGS
        }


    }

    private fun copyDataToDownloads(db: SQLiteDatabase) {
        val selectQuery = "SELECT d.${DBKeys.KEY_NAME}, d.${DBKeys.KEY_CHAPTER}, d.${DBKeys.KEY_STATUS}, d.${DBKeys.KEY_ORDER_ID}, d.${DBKeys.KEY_METADATA}, w.${DBKeys.KEY_URL} FROM ${DBKeys.TABLE_DOWNLOAD}_old d, ${DBKeys.TABLE_WEB_PAGE} w WHERE d.${DBKeys.KEY_WEB_PAGE_ID} = w.${DBKeys.KEY_ID}"
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

