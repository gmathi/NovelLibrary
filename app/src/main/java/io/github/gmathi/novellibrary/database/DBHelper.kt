package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.logging.Logs

class DBHelper
private constructor(private val appContext: Context) : SQLiteOpenHelper(
    appContext,
    DBKeys.DATABASE_NAME,
    null,
    DBKeys.DATABASE_VERSION,
    DatabaseErrorHandler { dbObj ->
        Logs.error(TAG, "Database corruption detected! Deleting corrupt database to allow recreation.")
        try {
            dbObj.close()
        } catch (_: Exception) {}
        appContext.deleteDatabase(DBKeys.DATABASE_NAME)
        sInstance = null
    }
) {

    companion object {
        private const val TAG = "DBHelper"

        @Volatile
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
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG_CROSS_REF)

        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE_SETTINGS)
        db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_NAME)
        db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_CROSS_REF_TAG)

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

            // Adds new column for the novel id which will be used instead of novel name.
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_DOWNLOAD} ADD COLUMN ${DBKeys.KEY_NOVEL_ID} INTEGER")
            db.execSQL("UPDATE ${DBKeys.TABLE_DOWNLOAD} SET ${DBKeys.KEY_NOVEL_ID} = (SELECT ${DBKeys.KEY_ID} FROM ${DBKeys.TABLE_NOVEL} WHERE  ${DBKeys.TABLE_NOVEL}.${DBKeys.KEY_NAME} = ${DBKeys.TABLE_DOWNLOAD}.${DBKeys.KEY_NAME})")

            // Sets the external ids for the novels. This is currently stored in metadata.
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_EXTERNAL_NOVEL_ID} TEXT")
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_SOURCE_ID} INTEGER")

            //Set the source id for all the novels
            updateNovelsWithSourceId(db)

            // Deprecate the sourceId and start using translatorSourceName in table `web_page`
            db.execSQL("ALTER TABLE ${DBKeys.TABLE_WEB_PAGE} ADD COLUMN ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} TEXT")
            db.execSQL("UPDATE ${DBKeys.TABLE_WEB_PAGE} SET ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME} = (SELECT ${DBKeys.TABLE_SOURCE}.${DBKeys.KEY_NAME} FROM ${DBKeys.TABLE_SOURCE} WHERE  ${DBKeys.TABLE_SOURCE}.${DBKeys.KEY_ID} = ${DBKeys.TABLE_WEB_PAGE}.${DBKeys.KEY_SOURCE_ID})")

            version = DBKeys.VER_SOURCES_REFACTOR
        }

//        if (version == DBKeys.VER_SOURCES_REFACTOR) {
//
//
//            //version = DBKeys.VER_SOURCES_REFACTOR
//        }

        if (version == DBKeys.VER_ROOM_MIGRATION) {
            migrateSectionsToTags(db)
            version = DBKeys.VER_NOVEL_TAGS
        }


    }

    /**
     * Migration 11 -> 12: introduce the tag model.
     *
     * Creates `novel_tag` and `novel_tag_cross_ref`, then seeds one tag per existing novel-section
     * name (including the implicit default section "Currently Reading", id -1L, which has no row in
     * `novel_section`). Every novel is linked to the tag that matches the section it currently
     * belongs to. The `novel_section` schema and `novel.novel_section_id` are left untouched so the
     * legacy library keeps working unchanged — tags coexist with sections rather than replacing the
     * column.
     */
    private fun migrateSectionsToTags(db: SQLiteDatabase) {
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_TAG_CROSS_REF)
        db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_NAME)
        db.execSQL(DBKeys.CREATE_INDEX_NOVEL_TAG_CROSS_REF_TAG)

        // Idempotency guard: if Room's MIGRATION_11_12 reached the file first it already seeded the
        // tags, so skip re-seeding (and avoid duplicate-link constraint failures).
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM ${DBKeys.TABLE_NOVEL_TAG}", null)
        var existingTags = 0
        if (countCursor != null) {
            if (countCursor.moveToFirst()) existingTags = countCursor.getInt(0)
            countCursor.close()
        }
        if (existingTags > 0) return

        // Map every section id (including -1L default) to its display name.
        val sectionNames = HashMap<Long, String>()
        sectionNames[-1L] = defaultSectionName()
        val sectionCursor = db.rawQuery(
            "SELECT ${DBKeys.KEY_ID}, ${DBKeys.KEY_NAME} FROM ${DBKeys.TABLE_NOVEL_SECTION}", null
        )
        if (sectionCursor != null) {
            if (sectionCursor.moveToFirst()) {
                do {
                    val id = sectionCursor.getLong(sectionCursor.getColumnIndex(DBKeys.KEY_ID))
                    val name = sectionCursor.getString(sectionCursor.getColumnIndex(DBKeys.KEY_NAME))
                    if (!name.isNullOrBlank()) sectionNames[id] = name
                } while (sectionCursor.moveToNext())
            }
            sectionCursor.close()
        }

        // Only seed a tag for a section that actually has novels in it, so we don't create empty
        // tags for sections the user left unused. Collect the sections that are referenced first.
        val usedSections = HashSet<Long>()
        val usedCursor = db.rawQuery(
            "SELECT DISTINCT ${DBKeys.KEY_NOVEL_SECTION_ID} FROM ${DBKeys.TABLE_NOVEL}", null
        )
        if (usedCursor != null) {
            if (usedCursor.moveToFirst()) {
                do {
                    usedSections.add(usedCursor.getLong(0))
                } while (usedCursor.moveToNext())
            }
            usedCursor.close()
        }
        // Always provide the default tag so a novel pointing at a since-deleted section (orphan)
        // still gets a home instead of silently losing its organization.
        if (usedSections.isNotEmpty()) usedSections.add(-1L)

        // Create a tag per used section and remember section-id -> new tag-id.
        val sectionToTagId = HashMap<Long, Long>()
        var order = 0L
        for (sectionId in usedSections) {
            val name = sectionNames[sectionId] ?: continue
            val values = ContentValues()
            values.put(DBKeys.KEY_NAME, name)
            values.put(DBKeys.KEY_ORDER_ID, order++)
            // CONFLICT_IGNORE keeps the unique-name index happy if two section ids share a name;
            // the existing tag id is then looked up below.
            var tagId = db.insertWithOnConflict(
                DBKeys.TABLE_NOVEL_TAG, null, values, SQLiteDatabase.CONFLICT_IGNORE
            )
            if (tagId == -1L) {
                val c = db.rawQuery(
                    "SELECT ${DBKeys.KEY_ID} FROM ${DBKeys.TABLE_NOVEL_TAG} WHERE ${DBKeys.KEY_NAME} = ?",
                    arrayOf(name)
                )
                if (c != null) {
                    if (c.moveToFirst()) tagId = c.getLong(0)
                    c.close()
                }
            }
            if (tagId != -1L) sectionToTagId[sectionId] = tagId
        }

        // Link every novel to the tag for its current section.
        val novelCursor = db.rawQuery(
            "SELECT ${DBKeys.KEY_ID}, ${DBKeys.KEY_NOVEL_SECTION_ID} FROM ${DBKeys.TABLE_NOVEL}", null
        )
        if (novelCursor != null) {
            if (novelCursor.moveToFirst()) {
                db.beginTransaction()
                try {
                    do {
                        val novelId = novelCursor.getLong(novelCursor.getColumnIndex(DBKeys.KEY_ID))
                        val sectionId = novelCursor.getLong(novelCursor.getColumnIndex(DBKeys.KEY_NOVEL_SECTION_ID))
                        val tagId = sectionToTagId[sectionId] ?: sectionToTagId[-1L] ?: continue
                        val link = ContentValues()
                        link.put(DBKeys.KEY_NOVEL_ID, novelId)
                        link.put(DBKeys.KEY_TAG_ID, tagId)
                        db.insertWithOnConflict(
                            DBKeys.TABLE_NOVEL_TAG_CROSS_REF, null, link, SQLiteDatabase.CONFLICT_IGNORE
                        )
                    } while (novelCursor.moveToNext())
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            novelCursor.close()
        }
    }

    /**
     * Default-section display name used during migration. Mirrors R.string.default_novel_section_name
     * ("Currently Reading") but read from resources so a localized install keeps its label.
     */
    private fun defaultSectionName(): String = try {
        appContext.getString(io.github.gmathi.novellibrary.R.string.default_novel_section_name)
    } catch (e: Exception) {
        "Currently Reading"
    }

    /**
     * Update the existing database novels with sourceIds
     */
    private fun updateNovelsWithSourceId(db: SQLiteDatabase) {
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL}"
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    val novelUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
                    val novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                    val sourceId = getSourceId(novelUrl)
                    val values = ContentValues()
                    values.put(DBKeys.KEY_SOURCE_ID, sourceId)
                    db.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    /**
     * Get the sourceId for the novel based on the novel URL
     */
    fun getSourceId(novelUrl: String): Long {
        return when {
            novelUrl.contains(HostNames.NOVEL_UPDATES) -> Constants.SourceId.NOVEL_UPDATES
            novelUrl.contains(HostNames.ROYAL_ROAD) -> Constants.SourceId.ROYAL_ROAD
            novelUrl.contains(HostNames.ROYAL_ROAD_OLD) -> Constants.SourceId.ROYAL_ROAD
            novelUrl.contains(HostNames.NEOVEL) -> Constants.SourceId.NEOVEL
            novelUrl.contains(HostNames.SCRIBBLE_HUB) -> Constants.SourceId.SCRIBBLE_HUB
            novelUrl.contains(HostNames.LNMTL) -> Constants.SourceId.LNMTL
            novelUrl.contains(HostNames.NOVEL_FULL) -> Constants.SourceId.NOVEL_FULL
            else -> 0L // 0L is considered a invalid source.
        }
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
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_NOVEL_TAG}")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_NOVEL_TAG_CROSS_REF}")

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
        deleteDownloads(novel.id)
    }
}

fun SQLiteDatabase.runTransaction(fn: (SQLiteDatabase) -> Unit) {
    try {
        this.beginTransaction()
        fn(this)
        this.setTransactionSuccessful()
    } finally {
        this.endTransaction()
    }
}

