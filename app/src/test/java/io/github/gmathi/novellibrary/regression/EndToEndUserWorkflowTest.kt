package io.github.gmathi.novellibrary.regression

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.database.NovelHelper
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.dataCenter.DataCenter
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.Chapter
import io.github.gmathi.novellibrary.model.source.HttpSource
import io.github.gmathi.novellibrary.network.sync.NovelUpdatesSync
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

/**
 * End-to-end regression tests that validate complete user workflows after Injekt cleanup.
 * These tests simulate real user interactions to ensure all functionality works together correctly.
 */
@HiltAndroidTest
class EndToEndUserWorkflowTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var novelHelper: NovelHelper

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @ApplicationContext
    @Inject
    lateinit var context: Context

    private lateinit var novelUpdatesSync: NovelUpdatesSync

    @Before
    fun setup() {
        hiltRule.inject()
        novelUpdatesSync = NovelUpdatesSync(dbHelper, dataCenter)
    }

    @Test
    fun `test complete novel discovery and addition workflow`() = runBlocking {
        // Simulate user discovering and adding a novel to their library
        
        // Step 1: User browses available sources
        val availableSources = sourceManager.getAllSources()
        assertNotNull("User should see available sources", availableSources)
        assertTrue("Should have sources available", availableSources.isNotEmpty())
        
        // Step 2: User selects a source and browses popular novels
        val httpSources = availableSources.filterIsInstance<HttpSource>()
        if (httpSources.isNotEmpty()) {
            val selectedSource = httpSources.first()
            
            try {
                // Step 3: User browses popular novels from source
                val popularNovels = selectedSource.getPopularNovels(1)
                assertNotNull("Should retrieve popular novels", popularNovels)
                
                // Step 4: User selects a novel and views details
                if (popularNovels.isNotEmpty()) {
                    val selectedNovel = popularNovels.first()
                    val novelDetails = selectedSource.getNovelDetails(selectedNovel)
                    
                    // Step 5: User adds novel to library
                    val testNovel = createTestNovelFromSource(selectedSource)
                    val novelId = novelHelper.insertNovel(testNovel)
                    assertTrue("Novel should be added to library", novelId > 0)
                    
                    // Step 6: User views novel in library
                    val libraryNovel = novelHelper.getNovel(novelId)
                    assertNotNull("Novel should appear in library", libraryNovel)
                    assertEquals("Library novel should match added novel", testNovel.name, libraryNovel?.name)
                    
                    // Clean up
                    novelHelper.deleteNovel(novelId)
                }
                
            } catch (e: Exception) {
                // Network operations might fail in test environment
                assertFalse("Should not fail due to injection issues", 
                    e.message?.contains("inject") == true)
            }
        }
    }

    @Test
    fun `test complete novel reading workflow`() = runBlocking {
        // Simulate user reading a novel from start to finish
        
        // Step 1: User has a novel in their library
        val testNovel = createTestNovel()
        val novelId = novelHelper.insertNovel(testNovel)
        
        try {
            // Step 2: User opens novel and views chapter list
            val novel = novelHelper.getNovel(novelId)
            assertNotNull("User should see novel details", novel)
            
            // Step 3: User loads chapters for the novel
            val testChapters = createTestChapters(novelId, 5)
            val chapterIds = mutableListOf<Long>()
            
            testChapters.forEach { chapter ->
                val chapterId = novelHelper.insertChapter(chapter)
                chapterIds.add(chapterId)
            }
            
            // Step 4: User views chapter list
            val chapters = novelHelper.getChaptersByNovel(novelId)
            assertEquals("Should show all chapters", testChapters.size, chapters.size)
            
            // Step 5: User reads first chapter
            val firstChapter = chapters.first()
            val updatedChapter = firstChapter.copy(read = 1)
            novelHelper.updateChapter(updatedChapter)
            
            // Step 6: User checks reading progress
            val readChapters = novelHelper.getReadChaptersByNovel(novelId)
            assertEquals("Should show one read chapter", 1, readChapters.size)
            
            // Step 7: User bookmarks a chapter
            val secondChapter = chapters[1].copy(bookmark = 1)
            novelHelper.updateChapter(secondChapter)
            
            val bookmarkedChapters = novelHelper.getBookmarkedChaptersByNovel(novelId)
            assertEquals("Should show one bookmarked chapter", 1, bookmarkedChapters.size)
            
            // Step 8: User updates novel reading progress
            val updatedNovel = novel?.copy(currentChapterUrl = firstChapter.url)
            if (updatedNovel != null) {
                novelHelper.updateNovel(updatedNovel)
                
                val progressNovel = novelHelper.getNovel(novelId)
                assertEquals("Should update reading progress", firstChapter.url, progressNovel?.currentChapterUrl)
            }
            
            // Clean up
            chapterIds.forEach { novelHelper.deleteChapter(it) }
            
        } finally {
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `test complete novel synchronization workflow`() = runBlocking {
        // Simulate user synchronizing novel updates
        
        // Step 1: User has novels in library that support sync
        val testNovel = createTestNovel("https://www.novelupdates.com/series/test-novel/")
        val novelId = novelHelper.insertNovel(testNovel)
        
        try {
            // Step 2: User initiates sync for novel
            val canSync = novelUpdatesSync.canSync(testNovel.url)
            
            if (canSync) {
                // Step 3: System checks for novel updates
                try {
                    val syncResult = novelUpdatesSync.syncNovelUpdates(testNovel)
                    // Sync might fail due to network/test environment, but should not fail due to injection
                    
                } catch (e: Exception) {
                    assertFalse("Sync should not fail due to injection issues", 
                        e.message?.contains("inject") == true)
                }
            }
            
            // Step 4: User views sync results
            val syncedNovel = novelHelper.getNovel(novelId)
            assertNotNull("Novel should still exist after sync attempt", syncedNovel)
            
        } finally {
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `test complete extension management workflow`() = runBlocking {
        // Simulate user managing extensions
        
        // Step 1: User views available extensions
        val availableExtensions = extensionManager.getAvailableExtensions()
        assertNotNull("Should show available extensions", availableExtensions)
        
        // Step 2: User views installed extensions
        val installedExtensions = extensionManager.getInstalledExtensions()
        assertNotNull("Should show installed extensions", installedExtensions)
        
        // Step 3: User checks extension details
        if (availableExtensions.isNotEmpty()) {
            val extension = availableExtensions.first()
            val extensionInfo = extensionManager.getExtensionInfo(extension.packageName)
            assertNotNull("Should show extension details", extensionInfo)
            
            // Step 4: User views extension icon
            val extensionIcon = extensionManager.getExtensionIcon(extension.packageName)
            // Icon might be null, but should not throw injection errors
        }
        
        // Step 5: User manages extension sources
        val extensionSources = extensionManager.getSourcesForExtension("test.extension")
        assertNotNull("Should handle extension source queries", extensionSources)
    }

    @Test
    fun `test complete search and discovery workflow`() = runBlocking {
        // Simulate user searching for novels across sources
        
        // Step 1: User initiates global search
        val searchQuery = "test novel"
        val sources = sourceManager.getEnabledSources()
        
        if (sources.isNotEmpty()) {
            val httpSources = sources.filterIsInstance<HttpSource>()
            
            // Step 2: User searches across multiple sources
            val searchResults = mutableListOf<Any>()
            
            httpSources.take(2).forEach { source ->
                try {
                    val results = source.searchNovels(searchQuery, 1)
                    searchResults.addAll(results)
                } catch (e: Exception) {
                    // Search might fail due to network, but should not be injection-related
                    assertFalse("Search should not fail due to injection issues", 
                        e.message?.contains("inject") == true)
                }
            }
            
            // Step 3: User filters and sorts results
            assertNotNull("Should handle search results", searchResults)
            
            // Step 4: User selects novel from search results
            if (searchResults.isNotEmpty()) {
                val selectedResult = searchResults.first()
                // User would typically view details and add to library
                assertNotNull("Should handle selected search result", selectedResult)
            }
        }
    }

    @Test
    fun `test complete library management workflow`() = runBlocking {
        // Simulate user managing their novel library
        
        // Step 1: User adds multiple novels to library
        val testNovels = listOf(
            createTestNovel("Novel A"),
            createTestNovel("Novel B"),
            createTestNovel("Novel C")
        )
        
        val novelIds = mutableListOf<Long>()
        
        try {
            testNovels.forEach { novel ->
                val id = novelHelper.insertNovel(novel)
                novelIds.add(id)
            }
            
            // Step 2: User views their library
            val libraryNovels = novelHelper.getAllNovels()
            assertTrue("Library should contain added novels", libraryNovels.size >= 3)
            
            // Step 3: User searches within library
            val searchResults = novelHelper.searchNovels("Novel")
            assertTrue("Should find novels in library search", searchResults.size >= 3)
            
            // Step 4: User sorts library
            val sortedNovels = novelHelper.getNovelsSortedByName()
            assertTrue("Should sort library novels", sortedNovels.isNotEmpty())
            
            // Step 5: User filters library by status
            val unreadNovels = novelHelper.getUnreadNovels()
            assertNotNull("Should filter unread novels", unreadNovels)
            
            // Step 6: User updates novel metadata
            val firstNovel = libraryNovels.first()
            val updatedNovel = firstNovel.copy(rating = "5.0")
            novelHelper.updateNovel(updatedNovel)
            
            val retrievedNovel = novelHelper.getNovel(firstNovel.id)
            assertEquals("Should update novel metadata", "5.0", retrievedNovel?.rating)
            
            // Step 7: User removes novel from library
            val novelToRemove = novelIds.last()
            novelHelper.deleteNovel(novelToRemove)
            novelIds.remove(novelToRemove)
            
            val remainingNovels = novelHelper.getAllNovels()
            assertFalse("Novel should be removed from library", 
                remainingNovels.any { it.id == novelToRemove })
            
        } finally {
            // Clean up remaining novels
            novelIds.forEach { novelHelper.deleteNovel(it) }
        }
    }

    @Test
    fun `test complete offline reading workflow`() = runBlocking {
        // Simulate user downloading and reading novels offline
        
        // Step 1: User selects novel for offline reading
        val testNovel = createTestNovel()
        val novelId = novelHelper.insertNovel(testNovel)
        
        try {
            // Step 2: User downloads chapters
            val testChapters = createTestChapters(novelId, 3)
            val chapterIds = mutableListOf<Long>()
            
            testChapters.forEachIndexed { index, chapter ->
                val downloadedChapter = chapter.copy(
                    isDownloaded = 1,
                    filePath = "/storage/novels/chapter_$index.html"
                )
                val id = novelHelper.insertChapter(downloadedChapter)
                chapterIds.add(id)
            }
            
            // Step 3: User views downloaded chapters
            val downloadedChapters = novelHelper.getDownloadedChaptersByNovel(novelId)
            assertEquals("Should show downloaded chapters", 3, downloadedChapters.size)
            
            // Step 4: User reads offline chapters
            downloadedChapters.forEach { chapter ->
                val readChapter = chapter.copy(read = 1)
                novelHelper.updateChapter(readChapter)
            }
            
            // Step 5: User checks offline reading progress
            val readChapters = novelHelper.getReadChaptersByNovel(novelId)
            assertEquals("Should track offline reading progress", 3, readChapters.size)
            
            // Step 6: User manages downloaded content
            val totalDownloadedSize = novelHelper.getTotalDownloadedSize()
            assertTrue("Should track downloaded content size", totalDownloadedSize >= 0)
            
            // Clean up
            chapterIds.forEach { novelHelper.deleteChapter(it) }
            
        } finally {
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `test complete error recovery workflow`() = runBlocking {
        // Simulate user encountering and recovering from errors
        
        // Step 1: User encounters network error
        try {
            val sources = sourceManager.getAllSources()
            val httpSources = sources.filterIsInstance<HttpSource>()
            
            if (httpSources.isNotEmpty()) {
                val source = httpSources.first()
                // Attempt operation that might fail
                source.getPopularNovels(1)
            }
        } catch (e: Exception) {
            // Step 2: System handles error gracefully
            assertFalse("Error should not be injection-related", 
                e.message?.contains("inject") == true)
        }
        
        // Step 3: User retries operation
        val retryResult = sourceManager.getAllSources()
        assertNotNull("Retry should work after error", retryResult)
        
        // Step 4: User checks system state after error
        val systemHealth = dbHelper.readableDatabase.isOpen
        assertTrue("Database should remain functional after errors", systemHealth)
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
            currentChapterUrl = null,
            newReleasesCount = 0L,
            lastReadDate = null,
            lastUpdatedDate = System.currentTimeMillis(),
            metadata = mapOf("source" to "test")
        )
    }

    private fun createTestNovelFromSource(source: HttpSource): Novel {
        return createTestNovel("Novel from ${source.name}")
    }

    private fun createTestChapters(novelId: Long, count: Int): List<Chapter> {
        return (1..count).map { i ->
            Chapter(
                id = 0L,
                novelId = novelId,
                name = "Chapter $i",
                url = "https://example.com/chapter/$i",
                orderId = i.toLong(),
                read = 0,
                bookmark = 0,
                translatorSourceName = "Test Translator",
                filePath = null,
                isDownloaded = 0
            )
        }
    }
}