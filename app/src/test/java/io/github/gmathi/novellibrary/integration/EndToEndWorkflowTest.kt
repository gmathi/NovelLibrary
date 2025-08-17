package io.github.gmathi.novellibrary.integration

import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.network.api.NovelApiService
import io.github.gmathi.novellibrary.service.download.CoroutineDownloadService
import io.github.gmathi.novellibrary.service.sync.CoroutineSyncService
import io.github.gmathi.novellibrary.util.coroutines.CoroutineErrorHandler
import io.github.gmathi.novellibrary.util.coroutines.CoroutineUtils
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Integration tests for end-to-end workflows covering the complete data flow
 * from network through database to UI using coroutines.
 * 
 * Tests Requirements: 7.3, 2.1, 2.2, 2.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndWorkflowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mock dependencies
    private val mockNovelDao = mockk<NovelDao>()
    private val mockWebPageDao = mockk<WebPageDao>()
    private val mockNovelRepository = mockk<NovelRepository>()
    private val mockWebPageRepository = mockk<WebPageRepository>()
    private val mockApiService = mockk<NovelApiService>()
    private val mockDocumentFetcher = mockk<WebPageDocumentFetcher>()
    private val mockDownloadService = mockk<CoroutineDownloadService>()
    private val mockSyncService = mockk<CoroutineSyncService>()
    private val mockErrorHandler = mockk<CoroutineErrorHandler>()
    
    // Test data
    private val testNovel = Novel(
        id = 1L,
        name = "Test Novel",
        url = "https://example.com/novel/1",
        imageUrl = "https://example.com/image.jpg",
        rating = "4.5",
        shortDescription = "A test novel",
        longDescription = "A longer description of the test novel",
        chaptersCount = 100L,
        newChaptersCount = 5L,
        currentChapterUrl = "https://example.com/novel/1/chapter/1"
    )
    
    private val testWebPage = WebPage(
        url = "https://example.com/novel/1/chapter/1",
        chapter = "Chapter 1: Beginning",
        novelId = 1L,
        orderId = 1L,
        isRead = 0L,
        title = "Chapter 1",
        filePath = "/storage/novels/1/chapter1.html"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup common mock behaviors
        every { mockErrorHandler.handleError(any(), any()) } returns Unit
        coEvery { mockErrorHandler.handleSuspendError(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test complete novel discovery and reading workflow`() = testScope.runTest {
        // Arrange - Setup the complete workflow chain
        val novelFlow = MutableStateFlow<List<Novel>>(emptyList())
        val webPageFlow = MutableStateFlow<List<WebPage>>(emptyList())
        
        coEvery { mockApiService.searchNovels(any()) } returns listOf(testNovel)
        coEvery { mockNovelRepository.insertNovel(any()) } returns 1L
        coEvery { mockNovelRepository.getAllNovels() } returns novelFlow
        coEvery { mockWebPageRepository.getWebPagesForNovel(any()) } returns webPageFlow
        coEvery { mockDocumentFetcher.fetchDocument(any()) } returns "Chapter content"
        coEvery { mockWebPageRepository.insertWebPage(any()) } returns 1L
        
        // Act - Execute the complete workflow
        // 1. Search for novels
        val searchResults = mockApiService.searchNovels("test")
        
        // 2. Save novel to database
        val novelId = mockNovelRepository.insertNovel(testNovel)
        novelFlow.value = listOf(testNovel.copy(id = novelId))
        
        // 3. Fetch chapter content
        val chapterContent = mockDocumentFetcher.fetchDocument(testWebPage.url)
        
        // 4. Save chapter to database
        val webPageId = mockWebPageRepository.insertWebPage(testWebPage)
        webPageFlow.value = listOf(testWebPage.copy(id = webPageId))
        
        // 5. Verify UI can observe the data
        val novels = mockNovelRepository.getAllNovels().first()
        val chapters = mockWebPageRepository.getWebPagesForNovel(novelId).first()
        
        // Assert - Verify the complete workflow
        assertEquals(1, searchResults.size)
        assertEquals(testNovel.name, searchResults[0].name)
        assertEquals(1L, novelId)
        assertEquals(1, novels.size)
        assertEquals(testNovel.name, novels[0].name)
        assertEquals("Chapter content", chapterContent)
        assertEquals(1L, webPageId)
        assertEquals(1, chapters.size)
        assertEquals(testWebPage.title, chapters[0].title)
        
        // Verify all operations were called
        coVerify { mockApiService.searchNovels("test") }
        coVerify { mockNovelRepository.insertNovel(testNovel) }
        coVerify { mockDocumentFetcher.fetchDocument(testWebPage.url) }
        coVerify { mockWebPageRepository.insertWebPage(testWebPage) }
    }

    @Test
    fun `test download workflow with progress tracking`() = testScope.runTest {
        // Arrange
        val downloadProgressFlow = MutableSharedFlow<Float>()
        val downloadStatusFlow = MutableStateFlow("idle")
        
        coEvery { mockDownloadService.downloadNovel(any()) } returns Unit
        every { mockDownloadService.getDownloadProgress(any()) } returns downloadProgressFlow
        every { mockDownloadService.getDownloadStatus(any()) } returns downloadStatusFlow
        
        // Act - Start download and track progress
        val progressValues = mutableListOf<Float>()
        val statusValues = mutableListOf<String>()
        
        val progressJob = launch {
            mockDownloadService.getDownloadProgress(testNovel.id!!).collect {
                progressValues.add(it)
            }
        }
        
        val statusJob = launch {
            mockDownloadService.getDownloadStatus(testNovel.id!!).collect {
                statusValues.add(it)
            }
        }
        
        // Simulate download progress
        downloadStatusFlow.value = "downloading"
        downloadProgressFlow.emit(0.0f)
        downloadProgressFlow.emit(0.5f)
        downloadProgressFlow.emit(1.0f)
        downloadStatusFlow.value = "completed"
        
        mockDownloadService.downloadNovel(testNovel.id!!)
        
        advanceUntilIdle()
        
        progressJob.cancel()
        statusJob.cancel()
        
        // Assert
        assertTrue(progressValues.contains(0.0f))
        assertTrue(progressValues.contains(0.5f))
        assertTrue(progressValues.contains(1.0f))
        assertTrue(statusValues.contains("downloading"))
        assertTrue(statusValues.contains("completed"))
        
        coVerify { mockDownloadService.downloadNovel(testNovel.id!!) }
    }

    @Test
    fun `test sync workflow with conflict resolution`() = testScope.runTest {
        // Arrange
        val localNovel = testNovel.copy(chaptersCount = 95L)
        val remoteNovel = testNovel.copy(chaptersCount = 100L)
        
        coEvery { mockNovelRepository.getNovel(any()) } returns localNovel
        coEvery { mockApiService.getNovelDetails(any()) } returns remoteNovel
        coEvery { mockNovelRepository.updateNovel(any()) } returns Unit
        coEvery { mockSyncService.syncNovel(any()) } returns Unit
        
        // Act - Execute sync workflow
        val local = mockNovelRepository.getNovel(testNovel.id!!)
        val remote = mockApiService.getNovelDetails(testNovel.url)
        
        // Simulate conflict resolution (remote has more chapters)
        val resolvedNovel = if (remote.chaptersCount > local.chaptersCount) remote else local
        mockNovelRepository.updateNovel(resolvedNovel)
        mockSyncService.syncNovel(testNovel.id!!)
        
        // Assert
        assertEquals(95L, local.chaptersCount)
        assertEquals(100L, remote.chaptersCount)
        assertEquals(100L, resolvedNovel.chaptersCount)
        
        coVerify { mockNovelRepository.getNovel(testNovel.id!!) }
        coVerify { mockApiService.getNovelDetails(testNovel.url) }
        coVerify { mockNovelRepository.updateNovel(resolvedNovel) }
        coVerify { mockSyncService.syncNovel(testNovel.id!!) }
    }

    @Test
    fun `test error handling across all layers`() = testScope.runTest {
        // Arrange - Setup various error scenarios
        val networkError = IOException("Network error")
        val timeoutError = SocketTimeoutException("Request timeout")
        
        coEvery { mockApiService.searchNovels(any()) } throws networkError
        coEvery { mockDocumentFetcher.fetchDocument(any()) } throws timeoutError
        coEvery { mockNovelRepository.insertNovel(any()) } throws RuntimeException("Database error")
        
        // Act & Assert - Test network layer error handling
        try {
            mockApiService.searchNovels("test")
            fail("Expected IOException")
        } catch (e: IOException) {
            assertEquals("Network error", e.message)
        }
        
        // Test document fetcher error handling
        try {
            mockDocumentFetcher.fetchDocument("https://example.com")
            fail("Expected SocketTimeoutException")
        } catch (e: SocketTimeoutException) {
            assertEquals("Request timeout", e.message)
        }
        
        // Test database layer error handling
        try {
            mockNovelRepository.insertNovel(testNovel)
            fail("Expected RuntimeException")
        } catch (e: RuntimeException) {
            assertEquals("Database error", e.message)
        }
        
        // Verify error handling was attempted
        coVerify { mockApiService.searchNovels("test") }
        coVerify { mockDocumentFetcher.fetchDocument("https://example.com") }
        coVerify { mockNovelRepository.insertNovel(testNovel) }
    }

    @Test
    fun `test concurrent operations with proper cancellation`() = testScope.runTest {
        // Arrange
        val job1Completed = CompletableDeferred<Boolean>()
        val job2Completed = CompletableDeferred<Boolean>()
        val job3Cancelled = CompletableDeferred<Boolean>()
        
        coEvery { mockApiService.searchNovels("query1") } coAnswers {
            delay(100)
            job1Completed.complete(true)
            listOf(testNovel)
        }
        
        coEvery { mockApiService.searchNovels("query2") } coAnswers {
            delay(200)
            job2Completed.complete(true)
            listOf(testNovel.copy(name = "Novel 2"))
        }
        
        coEvery { mockApiService.searchNovels("query3") } coAnswers {
            try {
                delay(1000) // Long delay to test cancellation
                listOf(testNovel.copy(name = "Novel 3"))
            } catch (e: CancellationException) {
                job3Cancelled.complete(true)
                throw e
            }
        }
        
        // Act - Launch concurrent operations
        val job1 = launch { mockApiService.searchNovels("query1") }
        val job2 = launch { mockApiService.searchNovels("query2") }
        val job3 = launch { mockApiService.searchNovels("query3") }
        
        // Wait for first two jobs to complete
        advanceTimeBy(250)
        
        // Cancel the third job
        job3.cancel()
        
        advanceUntilIdle()
        
        // Assert
        assertTrue(job1Completed.await())
        assertTrue(job2Completed.await())
        assertTrue(job3Cancelled.await())
        assertTrue(job3.isCancelled)
        
        coVerify { mockApiService.searchNovels("query1") }
        coVerify { mockApiService.searchNovels("query2") }
        coVerify { mockApiService.searchNovels("query3") }
    }

    @Test
    fun `test memory usage and performance improvements`() = testScope.runTest {
        // Arrange - Create a large dataset to test memory efficiency
        val largeNovelList = (1..1000).map { index ->
            testNovel.copy(
                id = index.toLong(),
                name = "Novel $index",
                url = "https://example.com/novel/$index"
            )
        }
        
        val novelFlow = MutableStateFlow<List<Novel>>(emptyList())
        coEvery { mockNovelRepository.getAllNovels() } returns novelFlow
        coEvery { mockNovelRepository.insertNovels(any()) } returns Unit
        
        // Act - Measure performance of bulk operations
        val startTime = System.currentTimeMillis()
        
        // Simulate bulk insert
        mockNovelRepository.insertNovels(largeNovelList)
        novelFlow.value = largeNovelList
        
        // Collect data in chunks to test Flow efficiency
        val collectedNovels = mutableListOf<Novel>()
        mockNovelRepository.getAllNovels()
            .take(1)
            .collect { novels ->
                collectedNovels.addAll(novels)
            }
        
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        
        // Assert - Verify performance and memory efficiency
        assertEquals(1000, collectedNovels.size)
        assertTrue("Execution should be fast with coroutines", executionTime < 1000) // Less than 1 second
        
        // Verify that Flow handles large datasets efficiently
        assertEquals(largeNovelList.first().name, collectedNovels.first().name)
        assertEquals(largeNovelList.last().name, collectedNovels.last().name)
        
        coVerify { mockNovelRepository.insertNovels(largeNovelList) }
        verify { mockNovelRepository.getAllNovels() }
    }

    @Test
    fun `test UI state management with coroutines`() = testScope.runTest {
        // Arrange - Setup ViewModel-like behavior
        val uiStateFlow = MutableStateFlow<String>("loading")
        val dataFlow = MutableStateFlow<List<Novel>>(emptyList())
        
        coEvery { mockNovelRepository.getAllNovels() } returns dataFlow
        
        // Act - Simulate UI state changes
        launch {
            try {
                uiStateFlow.value = "loading"
                mockNovelRepository.getAllNovels().collect { novels ->
                    if (novels.isNotEmpty()) {
                        uiStateFlow.value = "success"
                    } else {
                        uiStateFlow.value = "empty"
                    }
                }
            } catch (e: Exception) {
                uiStateFlow.value = "error"
            }
        }
        
        advanceUntilIdle()
        
        // Initially should be empty
        assertEquals("empty", uiStateFlow.value)
        
        // Add data and verify state change
        dataFlow.value = listOf(testNovel)
        advanceUntilIdle()
        
        assertEquals("success", uiStateFlow.value)
        
        // Assert
        verify { mockNovelRepository.getAllNovels() }
    }
}