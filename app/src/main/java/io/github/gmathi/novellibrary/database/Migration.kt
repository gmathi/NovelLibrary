package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import androidx.room.OnConflictStrategy
import androidx.room.migration.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.NovelLibraryApplication
import io.github.gmathi.novellibrary.model.other.GenericJsonMappedModel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.RuntimeException

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_ORDER_ID} INTEGER")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_ORDER_ID}=${DBKeys.KEY_ID}")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_NEW_RELEASES_COUNT} INTEGER")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NEW_RELEASES_COUNT}=0")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CURRENT_WEB_PAGE_ID}=-1")
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_WEB_PAGE}")
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_CHAPTERS_COUNT} INTEGER")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CHAPTERS_COUNT}=${DBKeys.KEY_NEW_RELEASES_COUNT}")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ${DBKeys.TABLE_WEB_PAGE} ADD COLUMN ${DBKeys.KEY_SOURCE_ID} INTEGER")
        db.execSQL("UPDATE ${DBKeys.TABLE_WEB_PAGE} SET ${DBKeys.KEY_SOURCE_ID}= -1")
        db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
        db.execSQL("INSERT INTO ${DBKeys.TABLE_SOURCE} (${DBKeys.KEY_ID}, ${DBKeys.KEY_NAME}) VALUES (-1, 'All')")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS ${DBKeys.TABLE_DOWNLOAD_QUEUE}")
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_CHAPTERS_COUNT}=${DBKeys.KEY_NEW_RELEASES_COUNT}")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NEW_RELEASES_COUNT}= 0")
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
        db.execSQL("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_NOVEL_SECTION_ID} INTEGER")
        db.execSQL("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NOVEL_SECTION_ID}= -1")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(DBKeys.CREATE_TABLE_LARGE_PREFERENCE)

        //Move Novel History Preferences to database because it is a huge data
        val values = ContentValues()
        values.put(DBKeys.KEY_NAME, Constants.LargePreferenceKeys.RVN_HISTORY)
        values.put(DBKeys.KEY_VALUE, "[]")
        db.insert(DBKeys.TABLE_LARGE_PREFERENCE, OnConflictStrategy.IGNORE, values)

        db.execSQL("CREATE INDEX web_pages_url_id_index ON ${DBKeys.TABLE_WEB_PAGE}(${DBKeys.KEY_ID}, ${DBKeys.KEY_URL})")
    }
}

val MIGRATION_8_9 = object :Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
    }

    private fun copyDataToDownloads(db: SupportSQLiteDatabase) {
        val selectQuery =
            "SELECT d.${DBKeys.KEY_NAME}, d.${DBKeys.KEY_CHAPTER}, d.${DBKeys.KEY_STATUS}, d.${DBKeys.KEY_ORDER_ID}, d.${DBKeys.KEY_METADATA}, w.${DBKeys.KEY_URL} FROM ${DBKeys.TABLE_DOWNLOAD}_old d, ${DBKeys.TABLE_WEB_PAGE} w WHERE d.${DBKeys.KEY_WEB_PAGE_ID} = w.${DBKeys.KEY_ID}"
        val cursor = db.query(selectQuery)

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

                    db.insert(DBKeys.TABLE_DOWNLOAD, OnConflictStrategy.IGNORE, values)
                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }

    private fun copyDataToWebPageSettings(db: SupportSQLiteDatabase) {
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE}_old"
        val cursor = db.query(selectQuery )

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

                    db.insert(DBKeys.TABLE_WEB_PAGE, OnConflictStrategy.IGNORE, webPageValues)
                    db.insert(DBKeys.TABLE_WEB_PAGE_SETTINGS, OnConflictStrategy.IGNORE, webPageSettingsValues)

                } while (cursor.moveToNext())
            }
            cursor.close()
        }
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val schema = getSchema(endVersion)
        val tables = getDatabaseEntitiesFromSchema(schema)
        tables.forEach() {
            Logs.debug("MIGRATION_9_10", "Upgrading table ${it.key}")
            MigrationTableHelper(db, it.value, it.key).apply() {
                replaceTable()
            }
        }
    }
}

fun getSchema(version: Int): JSONObject {
    val reader: BufferedReader
    val stringBuilder = StringBuilder()
    try {
        val filename = "schemas/io.github.gmathi.novellibrary.database.AppDatabase/$version.json"
        reader = BufferedReader(InputStreamReader(NovelLibraryApplication.context.assets.open(filename)))
        var mLine = reader.readLine()
        while (mLine != null) {
            stringBuilder.append(mLine)
            mLine = reader.readLine()
        }
        return JSONObject(stringBuilder.toString())
    } catch (e: IOException) {
        Logs.error("ContributionsActivity", e.localizedMessage, e)
        throw RuntimeException("Excepted database schema file")
    }
}

fun getDatabaseEntitiesFromSchema(schema: JSONObject): Map<String, JSONObject> {
    val entities = schema.getJSONObject("database").getJSONArray("entities")
    val result = HashMap<String, JSONObject>(entities.length())
    for (i in 0 until entities.length()) {
        val db = entities.getJSONObject(i)
        result[db.getString("tableName")] = db
    }
    
    return result
}

class MigrationTableHelper(val db: SupportSQLiteDatabase,
                           val settings: JSONObject,
                           val tableName: String,
                           val newTableName: String = "new_" + tableName) {
    /**
     * Creates new table (in standard configuration with 'new' prefix
     */
    fun createNewTable() {
        db.execSQL(settings.getString("createSql")
                .replace("\${TABLE_NAME}", newTableName))
    }
    
    fun getColumns(): Map<String, JSONObject> {
        val fields = settings.getJSONArray("fields")
        val result = HashMap<String, JSONObject>(fields.length())
        for (i in 0 until fields.length()) {
            val field = fields.getJSONObject(i)
            result[field.getString("columnName")] = field
        }
        
        return result
    }

    /**
     * Set column value if value is null to value
     * @param columnName Valid column name
     * @param value Value for column
     * @see setDefaultColumnValuesIfNull
     */
    fun setIfNull(columnName: String, value: Any) {
        db.execSQL("UPDATE $tableName SET $columnName = ? WHERE $columnName IS NOT NULL", arrayOf(value))
    }

    /**
     * Reads column properties and sets to default value if previous fields was null and
     * the new one requires not null
     *
     * Requires a defined defaultValue
     *
     * @param column One object of schema.database.entities.fields
     */
    fun setDefaultColumnValuesIfNull(column: JSONObject) {
        if (!column.has("defaultValue") || !column.getBoolean("notNull"))
            return

        val columnName = column.getString("columnName")!!
        setIfNull(columnName, column.getString("defaultValue"))
    }
    
    fun createIndex(index: JSONObject) {
        db.execSQL(index.getString("createSql")
                .replace("\${TABLE_NAME}", tableName))
    }
    
    fun createIndices(indices: JSONArray = settings.getJSONArray("indices")) {
        for (i in 0 until indices.length()) {
            createIndex(indices.getJSONObject(i))
        }
    }

    /**
     * Function createNewTable should have been executed before
     * @param columns Columns to copy to new table
     * @see createNewTable
     */
    fun copyOldToNewTable(columns: List<String>) {
        val columnsStr = columns.joinToString(", ")
        db.execSQL("INSERT INTO $newTableName ($columnsStr) SELECT $columnsStr FROM $tableName")
    }
    
    fun replaceOldTable() {
        db.execSQL("DROP TABLE $tableName")
        db.execSQL("ALTER TABLE $newTableName RENAME TO $tableName")
    }

    /**
     * Useful if a column type changed, new columns must not be added
     * (should have be added with addColumn before)
     */
    fun replaceTable() {
        createNewTable()
        val columns = getColumns()
        columns.forEach() {
            setDefaultColumnValuesIfNull(it.value)
        }
        copyOldToNewTable(columns.map {
            it.value.getString("columnName")
        })
        replaceOldTable()
        createIndices()
    }
    
    fun addColumn(columnName: String) {
        val columns = getColumns()
        val column = columns[columnName] ?: error("Column $columnName in schema expected")
        val notNull = if (column.getBoolean("notNull")) { "NOT NULL" } else { "" }
        db.execSQL("ALTER TABLE $tableName ADD COLUMN $columnName ${column.getString("affinitiy")} $notNull DEFAULT ?)", arrayOf(column.get("defaultValue") ?: ""))
    }
}

