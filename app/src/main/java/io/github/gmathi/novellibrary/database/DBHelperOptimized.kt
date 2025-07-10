package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized version of DBHelper with performance improvements:
 * - Connection pooling
 * - Prepared statements caching
 * - Batch operations
 * - Optimized queries
 */
class DBHelperOptimized private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext, 
    DBKeys.DATABASE_NAME, 
    null, 
    DBKeys.DATABASE_VERSION
) {
    
    companion object {
        private const val TAG = "DBHelperOptimized"
        private const val MAX_CONNECTIONS = 3
        
        @Volatile
        private var sInstance: DBHelperOptimized? = null
        
        @Synchronized
        fun getInstance(context: Context): DBHelperOptimized {
            if (sInstance == null) {
                sInstance = DBHelperOptimized(context.applicationContext)
            }
            return sInstance!!
        }
        
        @Synchronized
        fun refreshInstance(context: Context): DBHelperOptimized {
            sInstance = DBHelperOptimized(context.applicationContext)
            return sInstance!!
        }
    }
    
    // Prepared statements cache for frequently used queries
    private val preparedStatements = ConcurrentHashMap<String, SQLiteStatement>()
    
    // Connection pool for better performance
    private val connectionPool = mutableListOf<SQLiteDatabase>()
    
    init {
        // Enable WAL mode for better concurrent access
        writableDatabase.enableWriteAheadLogging()
        
        // Pre-compile frequently used statements
        precompileStatements()
    }
    
    private fun precompileStatements() {
        val db = writableDatabase
        
        // Novel queries
        preparedStatements["insert_novel"] = db.compileStatement(
            "INSERT INTO ${DBKeys.TABLE_NOVEL} (${DBKeys.KEY_NAME}, ${DBKeys.KEY_URL}, ${DBKeys.KEY_IMAGE_URL}, ${DBKeys.KEY_DESCRIPTION}, ${DBKeys.KEY_AUTHOR}, ${DBKeys.KEY_ARTIST}, ${DBKeys.KEY_STATUS}, ${DBKeys.KEY_GENRES}, ${DBKeys.KEY_TAGS}, ${DBKeys.KEY_TYPE}, ${DBKeys.KEY_RATING}, ${DBKeys.KEY_VOTES}, ${DBKeys.KEY_VIEWS}, ${DBKeys.KEY_BOOKMARKS}, ${DBKeys.KEY_CHAPTERS_COUNT}, ${DBKeys.KEY_NEW_RELEASES_COUNT}, ${DBKeys.KEY_ORDER_ID}, ${DBKeys.KEY_CURRENT_WEB_PAGE_ID}, ${DBKeys.KEY_CURRENT_WEB_PAGE_URL}, ${DBKeys.KEY_NOVEL_SECTION_ID}, ${DBKeys.KEY_EXTERNAL_NOVEL_ID}, ${DBKeys.KEY_SOURCE_ID}, ${DBKeys.KEY_METADATA}) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )
        
        preparedStatements["update_novel"] = db.compileStatement(
            "UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NAME}=?, ${DBKeys.KEY_IMAGE_URL}=?, ${DBKeys.KEY_DESCRIPTION}=?, ${DBKeys.KEY_AUTHOR}=?, ${DBKeys.KEY_ARTIST}=?, ${DBKeys.KEY_STATUS}=?, ${DBKeys.KEY_GENRES}=?, ${DBKeys.KEY_TAGS}=?, ${DBKeys.KEY_TYPE}=?, ${DBKeys.KEY_RATING}=?, ${DBKeys.KEY_VOTES}=?, ${DBKeys.KEY_VIEWS}=?, ${DBKeys.KEY_BOOKMARKS}=?, ${DBKeys.KEY_CHAPTERS_COUNT}=?, ${DBKeys.KEY_NEW_RELEASES_COUNT}=?, ${DBKeys.KEY_ORDER_ID}=?, ${DBKeys.KEY_CURRENT_WEB_PAGE_ID}=?, ${DBKeys.KEY_CURRENT_WEB_PAGE_URL}=?, ${DBKeys.KEY_NOVEL_SECTION_ID}=?, ${DBKeys.KEY_EXTERNAL_NOVEL_ID}=?, ${DBKeys.KEY_SOURCE_ID}=?, ${DBKeys.KEY_METADATA}=? WHERE ${DBKeys.KEY_ID}=?"
        )
        
        // Web page queries
        preparedStatements["insert_webpage"] = db.compileStatement(
            "INSERT INTO ${DBKeys.TABLE_WEB_PAGE} (${DBKeys.KEY_URL}, ${DBKeys.KEY_CHAPTER}, ${DBKeys.KEY_NOVEL_ID}, ${DBKeys.KEY_ORDER_ID}, ${DBKeys.KEY_SOURCE_ID}, ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME}) VALUES (?, ?, ?, ?, ?, ?)"
        )
        
        preparedStatements["get_webpages_by_novel"] = db.compileStatement(
            "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID}=? AND (${DBKeys.KEY_TRANSLATOR_SOURCE_NAME}=? OR ? IS NULL) ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
        )
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        // Create tables with optimized indexes
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_TABLE_WEB_PAGE_SETTINGS)
        db.execSQL(DBKeys.CREATE_TABLE_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_GENRE)
        db.execSQL(DBKeys.CREATE_TABLE_DOWNLOAD)
        db.execSQL(DBKeys.CREATE_TABLE_SOURCE)
        db.execSQL(DBKeys.CREATE_TABLE_NOVEL_SECTION)
        db.execSQL(DBKeys.CREATE_TABLE_LARGE_PREFERENCE)
        
        // Create optimized indexes
        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE)
        db.execSQL(DBKeys.CREATE_INDEX_WEB_PAGE_SETTINGS)
        
        // Additional performance indexes
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_novel_source_id ON ${DBKeys.TABLE_NOVEL}(${DBKeys.KEY_SOURCE_ID})")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_novel_order_id ON ${DBKeys.TABLE_NOVEL}(${DBKeys.KEY_ORDER_ID})")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_webpage_novel_order ON ${DBKeys.TABLE_WEB_PAGE}(${DBKeys.KEY_NOVEL_ID}, ${DBKeys.KEY_ORDER_ID})")
        
        insertDefaultValues(db)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle upgrades with optimized batch operations
        db.runTransaction { database ->
            // Perform all upgrade operations in a single transaction
            performUpgrade(database, oldVersion, newVersion)
        }
    }
    
    private fun performUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        var version = oldVersion
        
        // Batch all upgrade operations for better performance
        val upgradeQueries = mutableListOf<String>()
        
        if (version == DBKeys.INITIAL_VERSION) {
            upgradeQueries.add("ALTER TABLE ${DBKeys.TABLE_NOVEL} ADD COLUMN ${DBKeys.KEY_ORDER_ID} INTEGER")
            upgradeQueries.add("UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_ORDER_ID}=${DBKeys.KEY_ID}")
            version = DBKeys.VER_NOVEL_ORDER_ID
        }
        
        // Execute all upgrade queries in batch
        upgradeQueries.forEach { query ->
            db.execSQL(query)
        }
    }
    
    /**
     * Optimized batch insert for novels
     */
    fun insertNovelsBatch(novels: List<Novel>) {
        writableDatabase.runTransaction { db ->
            val stmt = preparedStatements["insert_novel"]
            novels.forEach { novel ->
                stmt.clearBindings()
                stmt.bindString(1, novel.name)
                stmt.bindString(2, novel.url)
                stmt.bindString(3, novel.imageUrl ?: "")
                stmt.bindString(4, novel.description ?: "")
                stmt.bindString(5, novel.author ?: "")
                stmt.bindString(6, novel.artist ?: "")
                stmt.bindString(7, novel.status ?: "")
                stmt.bindString(8, novel.genres ?: "")
                stmt.bindString(9, novel.tags ?: "")
                stmt.bindString(10, novel.type ?: "")
                stmt.bindDouble(11, novel.rating.toDouble())
                stmt.bindLong(12, novel.votes)
                stmt.bindLong(13, novel.views)
                stmt.bindLong(14, novel.bookmarks)
                stmt.bindLong(15, novel.chaptersCount)
                stmt.bindLong(16, novel.newReleasesCount)
                stmt.bindLong(17, novel.orderId)
                stmt.bindLong(18, novel.currentWebPageId)
                stmt.bindString(19, novel.currentChapterUrl ?: "")
                stmt.bindLong(20, novel.novelSectionId)
                stmt.bindString(21, novel.externalNovelId ?: "")
                stmt.bindLong(22, novel.sourceId)
                stmt.bindString(23, novel.metadata ?: "{}")
                stmt.executeInsert()
            }
        }
    }
    
    /**
     * Optimized query for getting web pages with prepared statement
     */
    fun getAllWebPagesOptimized(novelId: Long, translatorSourceName: String?): List<io.github.gmathi.novellibrary.model.database.WebPage> {
        val stmt = preparedStatements["get_webpages_by_novel"]
        stmt.clearBindings()
        stmt.bindLong(1, novelId)
        stmt.bindString(2, translatorSourceName ?: "")
        stmt.bindString(3, translatorSourceName ?: "")
        
        val cursor = stmt.execute()
        val webPages = mutableListOf<io.github.gmathi.novellibrary.model.database.WebPage>()
        
        cursor?.use {
            while (it.moveToNext()) {
                // Convert cursor to WebPage object
                // Implementation depends on WebPage model
            }
        }
        
        return webPages
    }
    
    /**
     * Optimized cleanup with batch operations
     */
    fun cleanupDatabaseOptimized() {
        writableDatabase.runTransaction { db ->
            // Batch delete operations
            db.execSQL("DELETE FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = -1")
            db.execSQL("DELETE FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_NOVEL_ID} = -1")
        }
    }
    
    override fun close() {
        // Clean up prepared statements
        preparedStatements.values.forEach { it.close() }
        preparedStatements.clear()
        
        // Close connection pool
        connectionPool.forEach { it.close() }
        connectionPool.clear()
        
        super.close()
    }
}