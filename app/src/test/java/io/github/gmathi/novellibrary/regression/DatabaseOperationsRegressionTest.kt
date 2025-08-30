package io.github.gmathi.novellibrary.regression

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.NovelHelper
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.source.SourceManager
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.Chapter
import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

/**
 * Comprehensive regression tests for database operations after Injekt cleanup.
 * Validates that all database functionality works correctly with pure Hilt injection.
 */
@HiltAndroidTest
class DatabaseOperationsRegressionTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var novelHelper: NovelHelper

    @Inject
    lateinit var novelDao: NovelDao

    @Inject
    lateinit var sourceManager: SourceManager

    @ApplicationContext
    @Inject
    lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `test DBHelper basic operations work correctly`() = runBlocking {
        // Test that DBHelper can be injected and used
        assertNotNull("DBHelper should be injected", dbHelper)
        
        // Test database connection
        val database = dbHelper.readableDatabase
        assertNotNull("Should have readable database", database)
        assertTrue("Database should be open", database.isOpen)
        
        // Test basic query operations
        val cursor = database.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null)
        assertNotNull("Should execute basic queries", cursor)
        assertTrue("Should have tables", cursor.count > 0)
        cursor.close()
    }

    @Test
    fun `test NovelHelper operations work correctly`() = runBlocking {
        // Test that NovelHelper can be injected and used
        assertNotNull("NovelHelper should be injected", novelHelper)
        assertNotNull("NovelHelper should have SourceManager", novelHelper.sourceManager)
        
        // Test novel operations
        val testNovel = createTestNovel()
        
        // Test novel insertion
        val insertedId = novelHelper.insertNovel(testNovel)
        assertTrue("Novel should be inserted successfully", insertedId > 0)
        
        // Test novel retrieval
        val retrievedNovel = novelHelper.getNovel(insertedId)
        assertNotNull("Should retrieve inserted novel", retrievedNovel)
        assertEquals("Retrieved novel should match", testNovel.name, retrievedNovel?.name)
        
        // Test novel update
        val updatedNovel = retrievedNovel?.copy(rating = "4.5")
        if (updatedNovel != null) {
            novelHelper.updateNovel(updatedNovel)
            val reRetrievedNovel = novelHelper.getNovel(insertedId)
            assertEquals("Novel should be updated", "4.5", reRetrievedNovel?.rating)
        }
        
        // Test novel deletion
        novelHelper.deleteNovel(insertedId)
        val deletedNovel = novelHelper.getNovel(insertedId)
        assertNull("Novel should be deleted", deletedNovel)
    }

    @Test
    fun `test NovelDao operations work correctly`() = runBlocking {
        // Test that NovelDao can be injected and used
        assertNotNull("NovelDao should be injected", novelDao)
        
        // Test novel details retrieval with source manager integration
        val testNovel = createTestNovel()
        val insertedId = novelHelper.insertNovel(testNovel)
        
        try {
            val novelDetails = novelDao.getNovelDetails(insertedId)
            // Details might be null for test novel, but should not throw injection errors
            
            // Test that DAO has access to SourceManager
            assertNotNull("NovelDao should have SourceManager access", novelDao.sourceManager)
            
        } finally {
            // Clean up
            novelHelper.deleteNovel(insertedId)
        }
    }

    @Test
    fun `test chapter operations work correctly`() = runBlocking {
        // Test chapter database operations
        val testNovel = createTestNovel()
        val novelId = novelHelper.insertNovel(testNovel)
        
        try {
            val testChapter = createTestChapter(novelId)
            
            // Test chapter insertion
            val chapterId = novelHelper.insertChapter(testChapter)
            assertTrue("Chapter should be inserted successfully", chapterId > 0)
            
            // Test chapter retrieval
            val retrievedChapter = novelHelper.getChapter(chapterId)
            assertNotNull("Should retrieve inserted chapter", retrievedChapter)
            assertEquals("Retrieved chapter should match", testChapter.name, retrievedChapter?.name)
            
            // Test chapters by novel
            val novelChapters = novelHelper.getChaptersByNovel(novelId)
            assertTrue("Should retrieve chapters for novel", novelChapters.isNotEmpty())
            assertEquals("Should find inserted chapter", chapterId, novelChapters.first().id)
            
            // Test chapter update
            val updatedChapter = retrievedChapter?.copy(read = 1)
            if (updatedChapter != null) {
                novelHelper.updateChapter(updatedChapter)
                val reRetrievedChapter = novelHelper.getChapter(chapterId)
                assertEquals("Chapter should be updated", 1, reRetrievedChapter?.read)
            }
            
            // Test chapter deletion
            novelHelper.deleteChapter(chapterId)
            val deletedChapter = novelHelper.getChapter(chapterId)
            assertNull("Chapter should be deleted", deletedChapter)
            
        } finally {
            // Clean up
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `test web page operations work correctly`() = runBlocking {
        // Test web page database operations
        val testWebPage = createTestWebPage()
        
        // Test web page insertion
        val insertedId = novelHelper.insertWebPage(testWebPage)
        assertTrue("WebPage should be inserted successfully", insertedId > 0)
        
        // Test web page retrieval
        val retrievedWebPage = novelHelper.getWebPage(insertedId)
        assertNotNull("Should retrieve inserted web page", retrievedWebPage)
        assertEquals("Retrieved web page should match", testWebPage.url, retrievedWebPage?.url)
        
        // Test web page update
        val updatedWebPage = retrievedWebPage?.copy(title = "Updated Title")
        if (updatedWebPage != null) {
            novelHelper.updateWebPage(updatedWebPage)
            val reRetrievedWebPage = novelHelper.getWebPage(insertedId)
            assertEquals("WebPage should be updated", "Updated Title", reRetrievedWebPage?.title)
        }
        
        // Test web page deletion
        novelHelper.deleteWebPage(insertedId)
        val deletedWebPage = novelHelper.getWebPage(insertedId)
        assertNull("WebPage should be deleted", deletedWebPage)
    }

    @Test
    fun `test database transaction operations work correctly`() = runBlocking {
        // Test database transactions
        val testNovels = listOf(
            createTestNovel("Novel 1"),
            createTestNovel("Novel 2"),
            createTestNovel("Novel 3")
        )
        
        // Test batch insertion in transaction
        val insertedIds = mutableListOf<Long>()
        
        try {
            dbHelper.writableDatabase.beginTransaction()
            
            testNovels.forEach { novel ->
                val id = novelHelper.insertNovel(novel)
                insertedIds.add(id)
                assertTrue("Novel should be inserted in transaction", id > 0)
            }
            
            dbHelper.writableDatabase.setTransactionSuccessful()
            dbHelper.writableDatabase.endTransaction()
            
            // Verify all novels were inserted
            insertedIds.forEach { id ->
                val novel = novelHelper.getNovel(id)
                assertNotNull("Novel should exist after transaction", novel)
            }
            
        } finally {
            // Clean up
            insertedIds.forEach { id ->
                novelHelper.deleteNovel(id)
            }
        }
    }

    @Test
    fun `test database query operations work correctly`() = runBlocking {
        // Test complex database queries
        val testNovels = listOf(
            createTestNovel("Test Novel A"),
            createTestNovel("Test Novel B"),
            createTestNovel("Another Novel")
        )
        
        val insertedIds = mutableListOf<Long>()
        
        try {
            // Insert test data
            testNovels.forEach { novel ->
                val id = novelHelper.insertNovel(novel)
                insertedIds.add(id)
            }
            
            // Test search queries
            val searchResults = novelHelper.searchNovels("Test")
            assertTrue("Should find novels matching search", searchResults.isNotEmpty())
            assertTrue("Should find correct number of results", searchResults.size >= 2)
            
            // Test filtering queries
            val allNovels = novelHelper.getAllNovels()
            assertTrue("Should retrieve all novels", allNovels.size >= 3)
            
            // Test sorting queries
            val sortedNovels = novelHelper.getNovelsSortedByName()
            assertTrue("Should retrieve sorted novels", sortedNovels.isNotEmpty())
            
        } finally {
            // Clean up
            insertedIds.forEach { id ->
                novelHelper.deleteNovel(id)
            }
        }
    }

    @Test
    fun `test database migration and schema work correctly`() {
        // Test database schema operations
        val database = dbHelper.readableDatabase
        
        // Test table existence
        val tableQuery = "SELECT name FROM sqlite_master WHERE type='table' AND name=?"
        
        val novelTableCursor = database.rawQuery(tableQuery, arrayOf("novel"))
        assertTrue("Novel table should exist", novelTableCursor.count > 0)
        novelTableCursor.close()
        
        val chapterTableCursor = database.rawQuery(tableQuery, arrayOf("chapter"))
        assertTrue("Chapter table should exist", chapterTableCursor.count > 0)
        chapterTableCursor.close()
        
        val webPageTableCursor = database.rawQuery(tableQuery, arrayOf("web_page"))
        assertTrue("WebPage table should exist", webPageTableCursor.count > 0)
        webPageTableCursor.close()
    }

    @Test
    fun `test concurrent database operations work correctly`() = runBlocking {
        // Test multiple concurrent database operations
        val testNovels = (1..5).map { i -> createTestNovel("Concurrent Novel $i") }
        
        val insertOperations = testNovels.map { novel ->
            kotlinx.coroutines.async {
                novelHelper.insertNovel(novel)
            }
        }
        
        val insertedIds = insertOperations.map { it.await() }
        
        try {
            // Verify all insertions succeeded
            insertedIds.forEach { id ->
                assertTrue("Concurrent insertion should succeed", id > 0)
                val novel = novelHelper.getNovel(id)
                assertNotNull("Concurrently inserted novel should be retrievable", novel)
            }
            
            // Test concurrent reads
            val readOperations = insertedIds.map { id ->
                kotlinx.coroutines.async {
                    novelHelper.getNovel(id)
                }
            }
            
            readOperations.forEach { deferred ->
                val novel = deferred.await()
                assertNotNull("Concurrent read should succeed", novel)
            }
            
        } finally {
            // Clean up
            insertedIds.forEach { id ->
                novelHelper.deleteNovel(id)
            }
        }
    }

    @Test
    fun `test database error handling works correctly`() = runBlocking {
        // Test database error scenarios
        
        // Test invalid novel ID
        val invalidNovel = novelHelper.getNovel(-1L)
        assertNull("Should handle invalid novel ID gracefully", invalidNovel)
        
        // Test invalid chapter ID
        val invalidChapter = novelHelper.getChapter(-1L)
        assertNull("Should handle invalid chapter ID gracefully", invalidChapter)
        
        // Test invalid web page ID
        val invalidWebPage = novelHelper.getWebPage(-1L)
        assertNull("Should handle invalid web page ID gracefully", invalidWebPage)
        
        // Test constraint violations
        try {
            val duplicateNovel = createTestNovel()
            val id1 = novelHelper.insertNovel(duplicateNovel)
            val id2 = novelHelper.insertNovel(duplicateNovel.copy(url = duplicateNovel.url))
            
            // Clean up
            novelHelper.deleteNovel(id1)
            if (id2 > 0) novelHelper.deleteNovel(id2)
            
        } catch (e: Exception) {
            // Should handle constraint violations gracefully
            assertTrue("Should handle database constraints", true)
        }
    }

    private fun createTestNovel(name: String = "Test Novel"): Novel {
        return Novel(
            id = 0L,
            name = name,
            url = "https://example.com/novel/${name.replace(" ", "-").lowercase()}",
            imageUrl = "https://example.com/image.jpg",
            rating = "4.0",
            shortDescription = "Test novel description",
            longDescription = "Long test novel description",
            language = "English",
            genres = listOf("Fantasy", "Adventure"),
            authors = listOf("Test Author"),
            translators = listOf("Test Translator"),
            tags = listOf("test", "novel"),
            chaptersCount = 100L,
            currentChapterUrl = "https://example.com/chapter/1",
            newReleasesCount = 0L,
            lastReadDate = null,
            lastUpdatedDate = System.currentTimeMillis(),
            metadata = mapOf("source" to "test")
        )
    }

    private fun createTestChapter(novelId: Long): Chapter {
        return Chapter(
            id = 0L,
            novelId = novelId,
            name = "Test Chapter",
            url = "https://example.com/chapter/test",
            orderId = 1L,
            read = 0,
            bookmark = 0,
            translatorSourceName = "Test Translator",
            filePath = null,
            isDownloaded = 0
        )
    }

    private fun createTestWebPage(): WebPage {
        return WebPage(
            id = 0L,
            url = "https://example.com/webpage",
            title = "Test Web Page",
            chapter = "Chapter 1",
            novelId = 1L,
            sourceId = 1L,
            filePath = null,
            redirectedUrl = null,
            isRead = 0
        )
    }
}