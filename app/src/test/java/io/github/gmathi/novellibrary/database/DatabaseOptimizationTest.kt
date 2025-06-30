package io.github.gmathi.novellibrary.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test class for database optimization components.
 */
class DatabaseOptimizationTest {
    
    private lateinit var context: Context
    private lateinit var databaseManager: DatabaseManager
    private lateinit var databaseCache: DatabaseCache
    private lateinit var optimizedHelper: OptimizedDatabaseHelper
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        databaseManager = DatabaseManager.getInstance(context)
        databaseCache = DatabaseCache.getInstance()
        optimizedHelper = OptimizedDatabaseHelper.getInstance(context)
    }
    
    @After
    fun cleanup() {
        // Clean up test data
        databaseCache.invalidateAll()
        databaseManager.close()
    }
    
    @Test
    fun testDatabaseManagerConnectionPooling() {
        // Test that we can get database connections from the pool
        val db1 = databaseManager.getDatabase()
        val db2 = databaseManager.getDatabase()
        
        assertNotNull(db1)
        assertNotNull(db2)
        assertTrue(db1.isOpen)
        assertTrue(db2.isOpen)
        
        // Return connections to pool
        databaseManager.returnDatabase(db1)
        databaseManager.returnDatabase(db2)
        
        // Check pool stats
        val stats = databaseManager.getPoolStats()
        assertTrue(stats.poolSize <= stats.maxPoolSize)
    }
    
    @Test
    fun testDatabaseCacheOperations() {
        // Test novel caching
        val novel = Novel("Test Novel", "http://test.com", 1L)
        novel.id = 1L
        
        databaseCache.putNovel(novel)
        val cachedNovel = databaseCache.getNovel(1L)
        
        assertNotNull(cachedNovel)
        assertEquals(novel.name, cachedNovel?.name)
        assertEquals(novel.url, cachedNovel?.url)
        
        // Test cache invalidation
        databaseCache.invalidateNovel(1L)
        val invalidatedNovel = databaseCache.getNovel(1L)
        assertNull(invalidatedNovel)
    }
    
    @Test
    fun testDatabaseCacheStatistics() {
        // Perform some cache operations
        val novel = Novel("Test Novel", "http://test.com", 1L)
        novel.id = 1L
        
        databaseCache.putNovel(novel)
        databaseCache.getNovel(1L) // Hit
        databaseCache.getNovel(2L) // Miss
        
        val stats = databaseCache.getCacheStats()
        assertTrue(stats.containsKey("novel"))
        
        val novelStats = stats["novel"]
        assertNotNull(novelStats)
        assertEquals(1, novelStats?.hits)
        assertEquals(1, novelStats?.misses)
        assertEquals(0.5, novelStats?.hitRate, 0.01)
    }
    
    @Test
    fun testDatabaseCacheMemoryUsage() {
        val memoryUsage = databaseCache.getMemoryUsage()
        
        assertTrue(memoryUsage.novelCacheSize >= 0)
        assertTrue(memoryUsage.novelCacheMaxSize > 0)
        assertTrue(memoryUsage.totalSize >= 0)
        assertTrue(memoryUsage.totalMaxSize > 0)
        assertTrue(memoryUsage.utilization >= 0.0)
        assertTrue(memoryUsage.utilization <= 1.0)
    }
    
    @Test
    fun testOptimizedDatabaseHelperNovelOperations() = runBlocking {
        // Test novel insertion and retrieval
        val novel = Novel("Test Novel", "http://test.com", 1L)
        
        val novelId = optimizedHelper.insertNovel(novel)
        assertTrue(novelId > 0)
        
        val retrievedNovel = optimizedHelper.getNovel(novelId)
        assertNotNull(retrievedNovel)
        assertEquals(novel.name, retrievedNovel?.name)
        assertEquals(novel.url, retrievedNovel?.url)
    }
    
    @Test
    fun testOptimizedDatabaseHelperChapterOperations() = runBlocking {
        // Test chapter operations
        val novel = Novel("Test Novel", "http://test.com", 1L)
        val novelId = optimizedHelper.insertNovel(novel)
        
        val chapters = listOf(
            WebPage("http://test.com/ch1", "Chapter 1", novelId),
            WebPage("http://test.com/ch2", "Chapter 2", novelId),
            WebPage("http://test.com/ch3", "Chapter 3", novelId)
        )
        
        val success = optimizedHelper.insertChapters(chapters)
        assertTrue(success)
        
        val retrievedChapters = optimizedHelper.getChapters(novelId)
        assertEquals(chapters.size, retrievedChapters.size)
        assertEquals(chapters[0].chapterName, retrievedChapters[0].chapterName)
    }
    
    @Test
    fun testOptimizedDatabaseHelperWebPageSettingsOperations() = runBlocking {
        // Test WebPage settings operations
        val novel = Novel("Test Novel", "http://test.com", 1L)
        val novelId = optimizedHelper.insertNovel(novel)
        
        val settings = WebPageSettings("http://test.com/ch1", novelId)
        settings.title = "Test Chapter"
        settings.isRead = true
        
        val settingsId = optimizedHelper.insertWebPageSettings(settings)
        assertTrue(settingsId > 0)
        
        val retrievedSettings = optimizedHelper.getWebPageSettings(settings.url)
        assertNotNull(retrievedSettings)
        assertEquals(settings.title, retrievedSettings?.title)
        assertEquals(settings.isRead, retrievedSettings?.isRead)
    }
    
    @Test
    fun testDatabaseManagerTransactionSupport() {
        // Test transaction support
        val result = databaseManager.withTransaction { db ->
            // Perform some operations within transaction
            db.execSQL("SELECT 1")
            "Transaction completed"
        }
        
        assertEquals("Transaction completed", result)
    }
    
    @Test
    fun testDatabaseCacheBulkOperations() {
        // Test bulk cache operations
        val novels = listOf(
            Novel("Novel 1", "http://test1.com", 1L).apply { id = 1L },
            Novel("Novel 2", "http://test2.com", 1L).apply { id = 2L },
            Novel("Novel 3", "http://test3.com", 1L).apply { id = 3L }
        )
        
        databaseCache.putNovels(novels)
        
        // Verify all novels are cached
        novels.forEach { novel ->
            val cached = databaseCache.getNovel(novel.id)
            assertNotNull(cached)
            assertEquals(novel.name, cached?.name)
        }
    }
    
    @Test
    fun testDatabaseCacheCleanup() {
        // Test cache cleanup
        val novel = Novel("Test Novel", "http://test.com", 1L)
        novel.id = 1L
        
        databaseCache.putNovel(novel)
        assertNotNull(databaseCache.getNovel(1L))
        
        databaseCache.cleanup()
        // After cleanup, cache should be empty
        assertNull(databaseCache.getNovel(1L))
    }
    
    @Test
    fun testDatabaseManagerWithDatabaseExtension() {
        // Test extension function
        val result = databaseManager.withDatabase { db ->
            db.isOpen
        }
        
        assertTrue(result)
    }
    
    @Test
    fun testDatabaseManagerWithWritableDatabaseExtension() {
        // Test writable database extension function
        val result = databaseManager.withWritableDatabase { db ->
            !db.isReadOnly
        }
        
        assertTrue(result)
    }
    
    @Test
    fun testDatabaseManagerWithTransactionExtension() {
        // Test transaction extension function
        val result = databaseManager.withTransaction { db ->
            db.isOpen && !db.isReadOnly
        }
        
        assertTrue(result)
    }
} 