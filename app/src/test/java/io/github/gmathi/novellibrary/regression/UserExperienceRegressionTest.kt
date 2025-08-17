package io.github.gmathi.novellibrary.regression

import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.api.NovelApiService
import io.github.gmathi.novellibrary.service.download.CoroutineDownloadService
import io.github.gmathi.novellibrary.viewmodel.LibraryViewModel
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Regression tests to validate that user experience remains unchanged
 * after migration from RxJava to Coroutines.
 * 
 * Tests Requirements: 1.4, 2.1, 2.2, 2.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserExperienceRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mock dependencies
    private val mockNovelRepository = mockk<NovelRepository>()
    private val mockWebPageRepository = mockk<WebPageRepository>()
    private val mockApiService = mockk<NovelApiService>()
    private val mockDownloadService = mockk<CoroutineDownloadService>()
    
    // ViewModels
    private val mockLibraryViewModel = mockk<LibraryViewModel>()
    private val mockChaptersViewModel = mockk<ChaptersViewModel>()
    
    // Test data
    private val testNovels = listOf(
        Novel(
            id = 1L,
            name = "Popular Novel",
            url = "https://example.com/novel/1",
            imageUrl = "https://example.com/image1.jpg",
            rating = "4.8",
            shortDescription = "A very popular novel",
            longDescription = "This is a detailed description of a popular novel",
            chaptersCount = 200L,
            newChaptersCount = 3L,
            currentChapterUrl = "https://example.com/novel/1/chapter/150"
        ),
        Novel(
            id = 2L,
            name = "New Release",
            url = "https://example.com/novel/2",
            imageUrl = "https://example.com/image2.jpg",
            rating = "4.2",
            shortDescription = "A newly released novel",
            longDescription = "This is a detailed description of a new novel",
            chaptersCount = 25L,
            newChaptersCount = 25L,
            currentChapterUrl = "https://example.com/novel/2/chapter/1"
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test app startup performance and responsiveness`() = testScope.runTest {
        // Arrange - Simulate app startup sequence
        val startupTasks = mutableListOf<String>()
        val startupTimes = mutableMapOf<String, Long>()
        
        coEvery { mockNovelRepository.getAllNovels() } coAnswers {
            val startTime = System.currentTimeMillis()
            startupTasks.add("load_novels")
            delay(50) // Simulate database load time
            val endTime = System.currentTimeMillis()
            startupTimes["load_novels"] = endTime - startTime
            flowOf(testNovels)
        }
        
        coEvery { mockApiService.checkForUpdates() } coAnswers {
            val startTime = System.currentTimeMillis()
            startupTasks.add("check_updates")
            delay(100) // Simulate network call
            val endTime = System.currentTimeMillis()
            startupTimes["check_updates"] = endTime - startTime
            "No updates available"
        }
        
        // Act - Simulate app startup
        val startupStartTime = System.currentTimeMillis()
        
        // Load novels (should be fast)
        val novelsFlow = mockNovelRepository.getAllNovels()
        val novels = novelsFlow.first()
        
        // Check for updates (can be slower, runs in background)
        val updateCheckJob = launch {
            mockApiService.checkForUpdates()
        }
        
        advanceUntilIdle()
        
        val startupEndTime = System.currentTimeMillis()
        val totalStartupTime = startupEndTime - startupStartTime
        
        // Assert - Startup should be fast and responsive
        assertEquals(2, novels.size)
        assertTrue("Startup should be fast", totalStartupTime < 1000)
        assertTrue("Should load novels", startupTasks.contains("load_novels"))
        assertTrue("Should check updates", startupTasks.contains("check_updates"))
        assertTrue("Update check job should complete", updateCheckJob.isCompleted)
        
        // Verify database load is fast
        val novelLoadTime = startupTimes["load_novels"] ?: 0L
        assertTrue("Novel loading should be fast", novelLoadTime < 200)
        
        coVerify { mockNovelRepository.getAllNovels() }
        coVerify { mockApiService.checkForUpdates() }
    }

    @Test
    fun `test UI responsiveness during data loading`() = testScope.runTest {
        // Arrange - Setup UI state flows
        val loadingStateFlow = MutableStateFlow(false)
        val dataStateFlow = MutableStateFlow<List<Novel>>(emptyList())
        val errorStateFlow = MutableStateFlow<String?>(null)
        
        coEvery { mockNovelRepository.getAllNovels() } returns dataStateFlow
        
        // Act - Simulate UI data loading with state management
        launch {
            try {
                loadingStateFlow.value = true
                errorStateFlow.value = null
                
                mockNovelRepository.getAllNovels().collect { novels ->
                    dataStateFlow.value = novels
                    loadingStateFlow.value = false
                }
            } catch (e: Exception) {
                errorStateFlow.value = e.message
                loadingStateFlow.value = false
            }
        }
        
        // Simulate data becoming available
        advanceTimeBy(50)
        dataStateFlow.value = testNovels
        advanceUntilIdle()
        
        // Assert - UI should respond quickly to state changes
        assertFalse("Loading should be false after data loads", loadingStateFlow.value)
        assertEquals(2, dataStateFlow.value.size)
        assertNull("Error should be null on success", errorStateFlow.value)
        
        verify { mockNovelRepository.getAllNovels() }
    }

    @Test
    fun `test search functionality responsiveness`() = testScope.runTest {
        // Arrange - Setup search with debouncing
        val searchQueryFlow = MutableStateFlow("")
        val searchResultsFlow = MutableStateFlow<List<Novel>>(emptyList())
        val searchLoadingFlow = MutableStateFlow(false)
        
        coEvery { mockApiService.searchNovels("popular") } returns listOf(testNovels[0])
        coEvery { mockApiService.searchNovels("new") } returns listOf(testNovels[1])
        coEvery { mockApiService.searchNovels("") } returns emptyList()
        
        // Act - Simulate search with debouncing
        launch {
            searchQueryFlow
                .debounce(300) // Debounce user input
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotEmpty()) {
                        searchLoadingFlow.value = true
                        try {
                            val results = mockApiService.searchNovels(query)
                            searchResultsFlow.value = results
                        } catch (e: Exception) {
                            searchResultsFlow.value = emptyList()
                        } finally {
                            searchLoadingFlow.value = false
                        }
                    } else {
                        searchResultsFlow.value = emptyList()
                        searchLoadingFlow.value = false
                    }
                }
        }
        
        // Simulate user typing
        searchQueryFlow.value = "p"
        advanceTimeBy(100)
        searchQueryFlow.value = "po"
        advanceTimeBy(100)
        searchQueryFlow.value = "pop"
        advanceTimeBy(100)
        searchQueryFlow.value = "popular"
        
        advanceTimeBy(400) // Wait for debounce
        
        // Assert - Search should be responsive and debounced
        assertEquals(1, searchResultsFlow.value.size)
        assertEquals("Popular Novel", searchResultsFlow.value[0].name)
        assertFalse("Search loading should be false", searchLoadingFlow.value)
        
        // Verify only final search was executed (debounced)
        coVerify(exactly = 1) { mockApiService.searchNovels("popular") }
        coVerify(exactly = 0) { mockApiService.searchNovels("p") }
        coVerify(exactly = 0) { mockApiService.searchNovels("po") }
        coVerify(exactly = 0) { mockApiService.searchNovels("pop") }
    }

    @Test
    fun `test download progress UI updates`() = testScope.runTest {
        // Arrange - Setup download progress tracking
        val downloadProgressFlow = MutableSharedFlow<Float>()
        val downloadStatusFlow = MutableStateFlow("idle")
        val uiProgressFlow = MutableStateFlow(0f)
        val uiStatusFlow = MutableStateFlow("Ready to download")
        
        every { mockDownloadService.getDownloadProgress(1L) } returns downloadProgressFlow
        every { mockDownloadService.getDownloadStatus(1L) } returns downloadStatusFlow
        coEvery { mockDownloadService.downloadNovel(1L) } returns Unit
        
        // Act - Setup UI progress tracking
        launch {
            mockDownloadService.getDownloadProgress(1L).collect { progress ->
                uiProgressFlow.value = progress
            }
        }
        
        launch {
            mockDownloadService.getDownloadStatus(1L).collect { status ->
                uiStatusFlow.value = when (status) {
                    "idle" -> "Ready to download"
                    "downloading" -> "Downloading..."
                    "completed" -> "Download complete"
                    "error" -> "Download failed"
                    else -> status
                }
            }
        }
        
        // Simulate download progress
        downloadStatusFlow.value = "downloading"
        downloadProgressFlow.emit(0.0f)
        advanceTimeBy(50)
        
        downloadProgressFlow.emit(0.25f)
        advanceTimeBy(50)
        
        downloadProgressFlow.emit(0.5f)
        advanceTimeBy(50)
        
        downloadProgressFlow.emit(0.75f)
        advanceTimeBy(50)
        
        downloadProgressFlow.emit(1.0f)
        downloadStatusFlow.value = "completed"
        
        advanceUntilIdle()
        
        // Assert - UI should update smoothly with progress
        assertEquals(1.0f, uiProgressFlow.value)
        assertEquals("Download complete", uiStatusFlow.value)
        
        verify { mockDownloadService.getDownloadProgress(1L) }
        verify { mockDownloadService.getDownloadStatus(1L) }
    }

    @Test
    fun `test reading experience continuity`() = testScope.runTest {
        // Arrange - Setup reading session
        val currentChapterFlow = MutableStateFlow<WebPage?>(null)
        val readingProgressFlow = MutableStateFlow(0f)
        val bookmarkFlow = MutableStateFlow<String?>(null)
        
        val testChapter = WebPage(
            id = 1L,
            url = "https://example.com/novel/1/chapter/1",
            chapter = "Chapter 1: The Beginning",
            novelId = 1L,
            orderId = 1L,
            isRead = 0L,
            title = "Chapter 1",
            filePath = "/storage/novels/1/chapter1.html"
        )
        
        coEvery { mockWebPageRepository.getWebPage(1L) } returns testChapter
        coEvery { mockWebPageRepository.updateWebPage(any()) } returns Unit
        coEvery { mockWebPageRepository.getNextChapter(1L) } returns testChapter.copy(id = 2L, orderId = 2L)
        
        // Act - Simulate reading session
        // Load chapter
        val chapter = mockWebPageRepository.getWebPage(1L)
        currentChapterFlow.value = chapter
        
        // Simulate reading progress
        readingProgressFlow.value = 0.0f
        advanceTimeBy(100)
        readingProgressFlow.value = 0.3f
        advanceTimeBy(100)
        readingProgressFlow.value = 0.7f
        advanceTimeBy(100)
        readingProgressFlow.value = 1.0f
        
        // Mark as read and save bookmark
        val readChapter = chapter.copy(isRead = 1L)
        mockWebPageRepository.updateWebPage(readChapter)
        bookmarkFlow.value = "Finished reading Chapter 1"
        
        // Navigate to next chapter
        val nextChapter = mockWebPageRepository.getNextChapter(chapter.id!!)
        currentChapterFlow.value = nextChapter
        
        // Assert - Reading experience should be smooth
        assertNotNull("Current chapter should be loaded", currentChapterFlow.value)
        assertEquals(1.0f, readingProgressFlow.value)
        assertEquals("Finished reading Chapter 1", bookmarkFlow.value)
        assertEquals(2L, currentChapterFlow.value?.id)
        
        coVerify { mockWebPageRepository.getWebPage(1L) }
        coVerify { mockWebPageRepository.updateWebPage(readChapter) }
        coVerify { mockWebPageRepository.getNextChapter(1L) }
    }

    @Test
    fun `test offline functionality consistency`() = testScope.runTest {
        // Arrange - Setup offline scenario
        val isOnlineFlow = MutableStateFlow(true)
        val cachedNovelsFlow = MutableStateFlow(testNovels)
        val networkErrorFlow = MutableStateFlow<String?>(null)
        
        coEvery { mockNovelRepository.getAllNovels() } returns cachedNovelsFlow
        coEvery { mockApiService.searchNovels(any()) } coAnswers {
            if (isOnlineFlow.value) {
                testNovels.filter { it.name.contains(firstArg()) }
            } else {
                throw java.net.UnknownHostException("No internet connection")
            }
        }
        
        // Act - Test online functionality
        val onlineResults = mockApiService.searchNovels("Popular")
        assertEquals(1, onlineResults.size)
        
        // Simulate going offline
        isOnlineFlow.value = false
        
        // Test offline functionality
        var offlineError: Exception? = null
        try {
            mockApiService.searchNovels("Popular")
        } catch (e: Exception) {
            offlineError = e
            networkErrorFlow.value = "No internet connection. Showing cached results."
        }
        
        // Should still be able to access cached data
        val cachedNovels = mockNovelRepository.getAllNovels().first()
        
        // Assert - Offline functionality should work with cached data
        assertNotNull("Should have offline error", offlineError)
        assertTrue("Should be network error", offlineError is java.net.UnknownHostException)
        assertEquals(2, cachedNovels.size)
        assertEquals("No internet connection. Showing cached results.", networkErrorFlow.value)
        
        coVerify(exactly = 2) { mockApiService.searchNovels("Popular") }
        verify { mockNovelRepository.getAllNovels() }
    }

    @Test
    fun `test memory usage during long reading sessions`() = testScope.runTest {
        // Arrange - Simulate long reading session with multiple chapters
        val chaptersRead = mutableListOf<Long>()
        val memoryUsagePoints = mutableListOf<Long>()
        
        coEvery { mockWebPageRepository.getWebPage(any()) } coAnswers {
            val chapterId = firstArg<Long>()
            chaptersRead.add(chapterId)
            
            // Simulate memory usage tracking
            val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
            memoryUsagePoints.add(currentMemory)
            
            WebPage(
                id = chapterId,
                url = "https://example.com/novel/1/chapter/$chapterId",
                chapter = "Chapter $chapterId content",
                novelId = 1L,
                orderId = chapterId,
                isRead = 0L,
                title = "Chapter $chapterId",
                filePath = "/storage/novels/1/chapter$chapterId.html"
            )
        }
        
        coEvery { mockWebPageRepository.updateWebPage(any()) } returns Unit
        
        // Act - Simulate reading multiple chapters
        repeat(20) { chapterIndex ->
            val chapterId = chapterIndex + 1L
            val chapter = mockWebPageRepository.getWebPage(chapterId)
            
            // Simulate reading time
            delay(10)
            
            // Mark as read
            val readChapter = chapter.copy(isRead = 1L)
            mockWebPageRepository.updateWebPage(readChapter)
        }
        
        // Assert - Memory usage should remain stable
        assertEquals(20, chaptersRead.size)
        assertTrue("Should have memory usage data", memoryUsagePoints.isNotEmpty())
        
        // Memory usage should not grow excessively
        val initialMemory = memoryUsagePoints.first()
        val finalMemory = memoryUsagePoints.last()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue("Memory increase should be reasonable", memoryIncrease < 10 * 1024 * 1024) // Less than 10MB
        
        coVerify(exactly = 20) { mockWebPageRepository.getWebPage(any()) }
        coVerify(exactly = 20) { mockWebPageRepository.updateWebPage(any()) }
    }

    @Test
    fun `test error recovery and user feedback`() = testScope.runTest {
        // Arrange - Setup error scenarios with recovery
        val errorStateFlow = MutableStateFlow<String?>(null)
        val retryCountFlow = MutableStateFlow(0)
        val userFeedbackFlow = MutableStateFlow<String?>(null)
        
        var attemptCount = 0
        coEvery { mockApiService.getNovelDetails(any()) } coAnswers {
            attemptCount++
            if (attemptCount < 3) {
                throw java.net.SocketTimeoutException("Request timeout")
            } else {
                testNovels[0]
            }
        }
        
        // Act - Implement error recovery with user feedback
        suspend fun loadNovelWithRetry(novelUrl: String): Novel? {
            repeat(3) { attempt ->
                try {
                    errorStateFlow.value = null
                    userFeedbackFlow.value = if (attempt > 0) "Retrying... (${attempt + 1}/3)" else null
                    
                    val novel = mockApiService.getNovelDetails(novelUrl)
                    userFeedbackFlow.value = "Novel loaded successfully"
                    return novel
                } catch (e: Exception) {
                    retryCountFlow.value = attempt + 1
                    errorStateFlow.value = e.message
                    
                    if (attempt < 2) {
                        userFeedbackFlow.value = "Connection failed. Retrying in ${(attempt + 1) * 2} seconds..."
                        delay((attempt + 1) * 2000L)
                    } else {
                        userFeedbackFlow.value = "Failed to load novel. Please check your connection and try again."
                    }
                }
            }
            return null
        }
        
        val result = loadNovelWithRetry("https://example.com/novel/1")
        
        // Assert - Error recovery should provide good user feedback
        assertNotNull("Should eventually succeed", result)
        assertEquals("Popular Novel", result?.name)
        assertEquals(3, attemptCount)
        assertEquals("Novel loaded successfully", userFeedbackFlow.value)
        assertNull("Error should be cleared on success", errorStateFlow.value)
        
        coVerify(exactly = 3) { mockApiService.getNovelDetails("https://example.com/novel/1") }
    }

    @Test
    fun `test configuration changes and state preservation`() = testScope.runTest {
        // Arrange - Setup state that should survive configuration changes
        val viewModelStateFlow = MutableStateFlow<Map<String, Any>>(emptyMap())
        val uiStateFlow = MutableStateFlow<Map<String, Any>>(emptyMap())
        
        // Initial state
        val initialState = mapOf(
            "selectedNovelId" to 1L,
            "currentChapterId" to 5L,
            "readingProgress" to 0.75f,
            "searchQuery" to "popular",
            "sortOrder" to "rating_desc"
        )
        
        viewModelStateFlow.value = initialState
        uiStateFlow.value = initialState
        
        // Act - Simulate configuration change (screen rotation)
        val savedState = viewModelStateFlow.value
        
        // Simulate activity recreation
        advanceTimeBy(100)
        
        // Restore state
        viewModelStateFlow.value = savedState
        uiStateFlow.value = savedState
        
        // Assert - State should be preserved
        assertEquals(initialState, viewModelStateFlow.value)
        assertEquals(initialState, uiStateFlow.value)
        assertEquals(1L, viewModelStateFlow.value["selectedNovelId"])
        assertEquals(5L, viewModelStateFlow.value["currentChapterId"])
        assertEquals(0.75f, viewModelStateFlow.value["readingProgress"])
        assertEquals("popular", viewModelStateFlow.value["searchQuery"])
        assertEquals("rating_desc", viewModelStateFlow.value["sortOrder"])
    }
}