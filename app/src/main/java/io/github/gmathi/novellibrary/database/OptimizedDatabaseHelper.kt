package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Optimized database helper with prepared statements and caching integration.
 * Provides better performance through connection pooling, prepared statements, and result caching.
 */
class OptimizedDatabaseHelper private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "OptimizedDatabaseHelper"
        
        @Volatile
        private var instance: OptimizedDatabaseHelper? = null
        
        @Synchronized
        fun getInstance(context: Context): OptimizedDatabaseHelper {
            return instance ?: OptimizedDatabaseHelper(context.applicationContext).also {
                instance = it
            }
        }
    }
    
    private val databaseManager = DatabaseManager.getInstance(context)
    private val databaseCache = DatabaseCache.getInstance()
    
    // Prepared statements for frequently used INSERT/UPDATE operations
    private var insertNovelStmt: SQLiteStatement? = null
    private var updateNovelStmt: SQLiteStatement? = null
    private var insertWebPageStmt: SQLiteStatement? = null
    private var insertWebPageSettingsStmt: SQLiteStatement? = null
    
    init {
        initializePreparedStatements()
    }
    
    private fun initializePreparedStatements() {
        databaseManager.withDatabase { db ->
            try {
                // Note: SQLiteStatement doesn't support SELECT queries with cursors
                // We'll use prepared statements only for INSERT/UPDATE operations
                // SELECT operations will use raw queries with parameter binding
                
                insertNovelStmt = db.compileStatement(
                    "INSERT INTO ${DBKeys.TABLE_NOVEL} (${DBKeys.KEY_NAME}, ${DBKeys.KEY_URL}, ${DBKeys.KEY_SOURCE_ID}, ${DBKeys.KEY_METADATA}, ${DBKeys.KEY_IMAGE_URL}, ${DBKeys.KEY_RATING}, ${DBKeys.KEY_SHORT_DESCRIPTION}, ${DBKeys.KEY_LONG_DESCRIPTION}, ${DBKeys.KEY_EXTERNAL_NOVEL_ID}, ${DBKeys.KEY_IMAGE_FILE_PATH}, ${DBKeys.KEY_NEW_RELEASES_COUNT}, ${DBKeys.KEY_CHAPTERS_COUNT}, ${DBKeys.KEY_CURRENT_WEB_PAGE_URL}, ${DBKeys.KEY_NOVEL_SECTION_ID}) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                )
                
                updateNovelStmt = db.compileStatement(
                    "UPDATE ${DBKeys.TABLE_NOVEL} SET ${DBKeys.KEY_NAME} = ?, ${DBKeys.KEY_URL} = ?, ${DBKeys.KEY_METADATA} = ?, ${DBKeys.KEY_IMAGE_URL} = ?, ${DBKeys.KEY_RATING} = ?, ${DBKeys.KEY_SHORT_DESCRIPTION} = ?, ${DBKeys.KEY_LONG_DESCRIPTION} = ?, ${DBKeys.KEY_NEW_RELEASES_COUNT} = ?, ${DBKeys.KEY_CHAPTERS_COUNT} = ?, ${DBKeys.KEY_CURRENT_WEB_PAGE_URL} = ?, ${DBKeys.KEY_NOVEL_SECTION_ID} = ? WHERE ${DBKeys.KEY_ID} = ?"
                )
                
                insertWebPageStmt = db.compileStatement(
                    "INSERT INTO ${DBKeys.TABLE_WEB_PAGE} (${DBKeys.KEY_URL}, ${DBKeys.KEY_CHAPTER}, ${DBKeys.KEY_NOVEL_ID}, ${DBKeys.KEY_ORDER_ID}, ${DBKeys.KEY_TRANSLATOR_SOURCE_NAME}) VALUES (?, ?, ?, ?, ?)"
                )
                
                insertWebPageSettingsStmt = db.compileStatement(
                    "INSERT INTO ${DBKeys.TABLE_WEB_PAGE_SETTINGS} (${DBKeys.KEY_URL}, ${DBKeys.KEY_NOVEL_ID}, ${DBKeys.KEY_REDIRECT_URL}, ${DBKeys.KEY_TITLE}, ${DBKeys.KEY_FILE_PATH}, ${DBKeys.KEY_IS_READ}, ${DBKeys.KEY_METADATA}) VALUES (?, ?, ?, ?, ?, ?, ?)"
                )
                
                Logs.debug(TAG, "Prepared statements initialized successfully")
            } catch (e: Exception) {
                Logs.error(TAG, "Error initializing prepared statements", e)
            }
        }
    }
    
    // Novel operations with caching
    suspend fun getNovel(novelId: Long): Novel? = withContext(Dispatchers.IO) {
        // Check cache first
        databaseCache.getNovel(novelId)?.let {
            Logs.debug(TAG, "Novel found in cache: ${it.name}")
            return@withContext it
        }
        
        // Query database with raw query (prepared statements don't support SELECT with cursors)
        databaseManager.withDatabase { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_ID} = ?",
                arrayOf(novelId.toString())
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val novel = getNovelFromCursor(it)
                    // Cache the result
                    databaseCache.putNovel(novel)
                    novel
                } else null
            }
        }
    }
    
    suspend fun getNovelByUrl(url: String): Novel? = withContext(Dispatchers.IO) {
        databaseManager.withDatabase { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_URL} = ?",
                arrayOf(url)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val novel = getNovelFromCursor(it)
                    databaseCache.putNovel(novel)
                    novel
                } else null
            }
        }
    }
    
    suspend fun insertNovel(novel: Novel): Long = withContext(Dispatchers.IO) {
        databaseManager.withWritableDatabase { db ->
            try {
                insertNovelStmt?.let { stmt ->
                    stmt.bindString(1, novel.name)
                    stmt.bindString(2, novel.url)
                    stmt.bindLong(3, novel.sourceId)
                    stmt.bindString(4, novel.metadata.toString())
                    stmt.bindString(5, novel.imageUrl ?: "")
                    stmt.bindString(6, novel.rating ?: "")
                    stmt.bindString(7, novel.shortDescription ?: "")
                    stmt.bindString(8, novel.longDescription ?: "")
                    stmt.bindString(9, novel.externalNovelId ?: "")
                    stmt.bindString(10, novel.imageFilePath ?: "")
                    stmt.bindLong(11, novel.newReleasesCount)
                    stmt.bindLong(12, novel.chaptersCount)
                    stmt.bindString(13, novel.currentChapterUrl ?: "")
                    stmt.bindLong(14, novel.novelSectionId)
                    
                    val id = stmt.executeInsert()
                    novel.id = id
                    databaseCache.putNovel(novel)
                    id
                } ?: run {
                    val values = ContentValues().apply {
                        put(DBKeys.KEY_NAME, novel.name)
                        put(DBKeys.KEY_URL, novel.url)
                        put(DBKeys.KEY_SOURCE_ID, novel.sourceId)
                        put(DBKeys.KEY_METADATA, novel.metadata.toString())
                        put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
                        put(DBKeys.KEY_RATING, novel.rating)
                        put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
                        put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
                        put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
                        put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
                        put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
                        put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)
                        put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
                        put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)
                    }
                    
                    val id = db.insert(DBKeys.TABLE_NOVEL, null, values)
                    novel.id = id
                    databaseCache.putNovel(novel)
                    id
                }
            } finally {
                insertNovelStmt?.clearBindings()
            }
        }
    }
    
    // Chapter operations with caching
    suspend fun getChapters(novelId: Long): List<WebPage> = withContext(Dispatchers.IO) {
        // Check cache first
        databaseCache.getChapters(novelId)?.let {
            Logs.debug(TAG, "Chapters found in cache for novel $novelId: ${it.size} chapters")
            return@withContext it
        }
        
        databaseManager.withDatabase { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE} WHERE ${DBKeys.KEY_NOVEL_ID} = ? ORDER BY ${DBKeys.KEY_ORDER_ID} ASC",
                arrayOf(novelId.toString())
            )
            
            val chapters = mutableListOf<WebPage>()
            cursor.use {
                while (it.moveToNext()) {
                    chapters.add(getWebPageFromCursor(it))
                }
            }
            
            // Cache the result
            databaseCache.putChapters(novelId, chapters)
            chapters
        }
    }
    
    suspend fun insertChapters(chapters: List<WebPage>): Boolean = withContext(Dispatchers.IO) {
        if (chapters.isEmpty()) return@withContext true
        
        databaseManager.withTransaction { db ->
            chapters.forEach { chapter ->
                try {
                    insertWebPageStmt?.let { stmt ->
                        stmt.bindString(1, chapter.url)
                        stmt.bindString(2, chapter.chapterName)
                        stmt.bindLong(3, chapter.novelId)
                        stmt.bindLong(4, chapter.orderId)
                        stmt.bindString(5, chapter.translatorSourceName ?: "")
                        
                        stmt.executeInsert()
                    } ?: run {
                        val values = ContentValues().apply {
                            put(DBKeys.KEY_URL, chapter.url)
                            put(DBKeys.KEY_CHAPTER, chapter.chapterName)
                            put(DBKeys.KEY_NOVEL_ID, chapter.novelId)
                            put(DBKeys.KEY_ORDER_ID, chapter.orderId)
                            put(DBKeys.KEY_TRANSLATOR_SOURCE_NAME, chapter.translatorSourceName)
                        }
                        db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
                    }
                } finally {
                    insertWebPageStmt?.clearBindings()
                }
            }
            
            // Invalidate cache for this novel
            chapters.firstOrNull()?.novelId?.let { novelId ->
                databaseCache.invalidateNovel(novelId)
            }
        }
        
        true
    }
    
    // WebPage settings operations with caching
    suspend fun getWebPageSettings(url: String): WebPageSettings? = withContext(Dispatchers.IO) {
        // Check cache first
        databaseCache.getWebPageSettings(url)?.let {
            Logs.debug(TAG, "WebPage settings found in cache for URL: $url")
            return@withContext it
        }
        
        databaseManager.withDatabase { db ->
            val cursor = db.rawQuery(
                "SELECT * FROM ${DBKeys.TABLE_WEB_PAGE_SETTINGS} WHERE ${DBKeys.KEY_URL} = ?",
                arrayOf(url)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val settings = getWebPageSettingsFromCursor(it)
                    databaseCache.putWebPageSettings(settings)
                    settings
                } else null
            }
        }
    }
    
    suspend fun insertWebPageSettings(settings: WebPageSettings): Long = withContext(Dispatchers.IO) {
        databaseManager.withWritableDatabase { db ->
            try {
                insertWebPageSettingsStmt?.let { stmt ->
                    stmt.bindString(1, settings.url)
                    stmt.bindLong(2, settings.novelId)
                    stmt.bindString(3, settings.redirectedUrl ?: "")
                    stmt.bindString(4, settings.title ?: "")
                    stmt.bindString(5, settings.filePath ?: "")
                    stmt.bindLong(6, if (settings.isRead) 1L else 0L)
                    stmt.bindString(7, settings.metadata.toString())
                    
                    val id = stmt.executeInsert()
                    databaseCache.putWebPageSettings(settings)
                    id
                } ?: run {
                    val values = ContentValues().apply {
                        put(DBKeys.KEY_URL, settings.url)
                        put(DBKeys.KEY_NOVEL_ID, settings.novelId)
                        put(DBKeys.KEY_REDIRECT_URL, settings.redirectedUrl)
                        put(DBKeys.KEY_TITLE, settings.title)
                        put(DBKeys.KEY_FILE_PATH, settings.filePath)
                        put(DBKeys.KEY_IS_READ, if (settings.isRead) 1 else 0)
                        put(DBKeys.KEY_METADATA, settings.metadata.toString())
                    }
                    
                    val id = db.insert(DBKeys.TABLE_WEB_PAGE_SETTINGS, null, values)
                    databaseCache.putWebPageSettings(settings)
                    id
                }
            } finally {
                insertWebPageSettingsStmt?.clearBindings()
            }
        }
    }
    
    // Bulk operations
    suspend fun insertNovels(novels: List<Novel>): List<Long> = withContext(Dispatchers.IO) {
        if (novels.isEmpty()) return@withContext emptyList()
        
        val results = mutableListOf<Long>()
        databaseManager.withTransaction { db ->
            novels.forEach { novel ->
                // Use direct database operations instead of suspend functions
                val id = insertNovelDirect(db, novel)
                results.add(id)
            }
        }
        results
    }
    
    suspend fun insertChaptersForNovels(chaptersMap: Map<Long, List<WebPage>>): Boolean = withContext(Dispatchers.IO) {
        if (chaptersMap.isEmpty()) return@withContext true
        
        databaseManager.withTransaction { db ->
            chaptersMap.forEach { (novelId, chapters) ->
                insertChaptersDirect(db, chapters)
            }
        }
        
        true
    }
    
    // Direct database operations for use within transactions
    private fun insertNovelDirect(db: SQLiteDatabase, novel: Novel): Long {
        return try {
            insertNovelStmt?.let { stmt ->
                stmt.bindString(1, novel.name)
                stmt.bindString(2, novel.url)
                stmt.bindLong(3, novel.sourceId)
                stmt.bindString(4, novel.metadata.toString())
                stmt.bindString(5, novel.imageUrl ?: "")
                stmt.bindString(6, novel.rating ?: "")
                stmt.bindString(7, novel.shortDescription ?: "")
                stmt.bindString(8, novel.longDescription ?: "")
                stmt.bindString(9, novel.externalNovelId ?: "")
                stmt.bindString(10, novel.imageFilePath ?: "")
                stmt.bindLong(11, novel.newReleasesCount)
                stmt.bindLong(12, novel.chaptersCount)
                stmt.bindString(13, novel.currentChapterUrl ?: "")
                stmt.bindLong(14, novel.novelSectionId)
                
                val id = stmt.executeInsert()
                novel.id = id
                databaseCache.putNovel(novel)
                id
            } ?: run {
                val values = ContentValues().apply {
                    put(DBKeys.KEY_NAME, novel.name)
                    put(DBKeys.KEY_URL, novel.url)
                    put(DBKeys.KEY_SOURCE_ID, novel.sourceId)
                    put(DBKeys.KEY_METADATA, novel.metadata.toString())
                    put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
                    put(DBKeys.KEY_RATING, novel.rating)
                    put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
                    put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
                    put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
                    put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
                    put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
                    put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)
                    put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
                    put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)
                }
                
                val id = db.insert(DBKeys.TABLE_NOVEL, null, values)
                novel.id = id
                databaseCache.putNovel(novel)
                id
            }
        } finally {
            insertNovelStmt?.clearBindings()
        }
    }
    
    private fun insertChaptersDirect(db: SQLiteDatabase, chapters: List<WebPage>): Boolean {
        if (chapters.isEmpty()) return true
        
        chapters.forEach { chapter ->
            try {
                insertWebPageStmt?.let { stmt ->
                    stmt.bindString(1, chapter.url)
                    stmt.bindString(2, chapter.chapterName)
                    stmt.bindLong(3, chapter.novelId)
                    stmt.bindLong(4, chapter.orderId)
                    stmt.bindString(5, chapter.translatorSourceName ?: "")
                    
                    stmt.executeInsert()
                } ?: run {
                    val values = ContentValues().apply {
                        put(DBKeys.KEY_URL, chapter.url)
                        put(DBKeys.KEY_CHAPTER, chapter.chapterName)
                        put(DBKeys.KEY_NOVEL_ID, chapter.novelId)
                        put(DBKeys.KEY_ORDER_ID, chapter.orderId)
                        put(DBKeys.KEY_TRANSLATOR_SOURCE_NAME, chapter.translatorSourceName)
                    }
                    db.insert(DBKeys.TABLE_WEB_PAGE, null, values)
                }
            } finally {
                insertWebPageStmt?.clearBindings()
            }
        }
        
        // Invalidate cache for this novel
        chapters.firstOrNull()?.novelId?.let { novelId ->
            databaseCache.invalidateNovel(novelId)
        }
        
        return true
    }
    
    // Cache management
    fun invalidateNovelCache(novelId: Long) {
        databaseCache.invalidateNovel(novelId)
    }
    
    fun invalidateAllCaches() {
        databaseCache.invalidateAll()
    }
    
    fun getCacheStats() = databaseCache.getCacheStats()
    
    fun getDatabasePoolStats() = databaseManager.getPoolStats()
    
    // Helper methods for cursor operations
    private fun getNovelFromCursor(cursor: Cursor): Novel {
        val novel = Novel(
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)),
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)),
            cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_SOURCE_ID))
        )
        novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
        novel.metadata = parseMetadata(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)))
        novel.imageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_URL))
        novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
        novel.shortDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_SHORT_DESCRIPTION))
        novel.longDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_LONG_DESCRIPTION))
        novel.externalNovelId = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_EXTERNAL_NOVEL_ID))
        novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
        novel.newReleasesCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NEW_RELEASES_COUNT))
        novel.chaptersCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTERS_COUNT))
        novel.currentChapterUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CURRENT_WEB_PAGE_URL))
        novel.novelSectionId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_SECTION_ID))
        return novel
    }
    
    private fun getWebPageFromCursor(cursor: Cursor): WebPage {
        val webPage = WebPage(
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)),
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CHAPTER))
        )
        webPage.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
        webPage.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
        webPage.translatorSourceName = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TRANSLATOR_SOURCE_NAME))
        return webPage
    }
    
    private fun getWebPageSettingsFromCursor(cursor: Cursor): WebPageSettings {
        return WebPageSettings(
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)),
            cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
        ).apply {
            redirectedUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_REDIRECT_URL))
            title = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_TITLE))
            filePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_FILE_PATH))
            isRead = cursor.getInt(cursor.getColumnIndex(DBKeys.KEY_IS_READ)) == 1
            metadata = parseMetadata(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)))
        }
    }
    
    private fun parseMetadata(metadataString: String?): HashMap<String, String?> {
        return try {
            if (metadataString.isNullOrBlank()) {
                HashMap()
            } else {
                // Simple parsing - you might want to use JSON parsing here
                val map = HashMap<String, String?>()
                metadataString.split(",").forEach { 
                    val parts = it.split("=")
                    if (parts.size == 2) map[parts[0]] = parts[1] else map[""] = ""
                }
                map
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Error parsing metadata: $metadataString", e)
            HashMap()
        }
    }
} 