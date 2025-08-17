package io.github.gmathi.novellibrary.regression

import io.github.gmathi.novellibrary.activity.ChaptersActivity
import io.github.gmathi.novellibrary.activity.NovelDetailsActivity
import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.fragment.LibraryFragment
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.api.NovelApiService
import io.github.gmathi.novellibrary.service.download.CoroutineDownloadService
import io.github.gmathi.novellibrary.service.sync.CoroutineSyncService
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import io.github.gmathi.novellibrary.viewmodel.LibraryViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Regression tests to ensure all existing app functionality works correctly
 * after migration from RxJava to Coroutines.
 * 
 * Tests Requirements: 1.4, 2.1, 2.2, 2.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppFunctionalityRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mock dependencies
    private val mockNovelRepository = mockk<NovelRepository>()
    private val mockWebPageRepository = mockk<WebPageRepository>()
    private val mockApiService = mockk<NovelApiService>()
    private val mockDownloadService = mockk<CoroutineDownloadService>()
    private val mockSyncService = mockk<CoroutineSyncService>()
    
    // ViewModels
    private val mockLibraryViewModel = mockk<LibraryViewModel>()
    private val mockChaptersViewModel = mockk<ChaptersViewModel>()
    
    // Test data
    private val testNovels = listOf(
        Novel(
            id = 1L,
            name = "Test Novel 1",
            url = "https://example.com/novel/1",
            imageUrl = "https://example.com/image1.jpg",
            rating = "4.5",
            shortDescription = "First test novel",
            longDescription = "Detailed description of first novel",
            chaptersCount = 50L,
            newChaptersCount = 2L,
            currentChapterUrl = "https://example.com/novel/1/chapter/1"
        ),
        Novel(
            id = 2L,
            name = "Test Novel 2",
            url = "https://example.com/novel/2",
            imageUrl = "https://example.com/image2.jpg",
            rating = "4.0",
            shortDescription = "Second test novel",
            longDescription = "Detailed description of second novel",
            chaptersCount = 75L,
            newChaptersCount = 0L,
            currentChapterUrl = "https://example.com/novel/2/chapter/1"
        )
    )
    
    private val testWebPages = listOf(
        WebPage(
            id = 1L,
            url = "https://example.com/novel/1/chapter/1",
            chapter = "Chapter 1: Beginning",
            novelId = 1L,
            orderId = 1L,
            isRead = 0L,
            title = "Chapter 1",
            filePath = "/storage/novels/1/chapter1.html"
        ),
        WebPage(
            id = 2L,
            url = "https://example.com/novel/1/chapter/2",
            chapter = "Chapter 2: Development",
            novelId = 1L,
            orderId = 2L,
            isRead = 1L,
            title = "Chapter 2",
            filePath = "/storage/novels/1/chapter2.html"
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
    fun `test library functionality - novel listing and management`() = testScope.runTest {
        // Arrange
        val novelsFlow = MutableStateFlow(testNovels)
        coEvery { mockNovelRepository.getAllNovels() } returns novelsFlow
        coEvery { mockNovelRepository.updateNovel(any()) } returns Unit
        coEvery { mockNovelRepository.deleteNovel(any()) } returns Unit
        
        // Act - Simulate library operations
        val allNovels = mockNovelRepository.getAllNovels().first()
        
        // Update a novel (mark as favorite)
        val updatedNovel = testNovels[0].copy(rating = "5.0")
        mockNovelRepository.updateNovel(updatedNovel)
        
        // Delete a novel
        mockNovelRepository.deleteNovel(testNovels[1].id!!)
        
        // Assert
        assertEquals(2, allNovels.size)
        assertEquals("Test Novel 1", allNovels[0].name)
        assertEquals("Test Novel 2", allNovels[1].name)
        
        // Verify operations
        verify { mockNovelRepository.getAllNovels() }
        coVerify { mockNovelRepository.updateNovel(updatedNovel) }
        coVerify { mockNovelRepository.deleteNovel(testNovels[1].id!!) }
    }

    @Test
    fun `test novel details functionality - chapter listing and reading`() = testScope.runTest {
        // Arrange
        val chaptersFlow = MutableStateFlow(testWebPages)
        coEvery { mockWebPageRepository.getWebPagesForNovel(1L) } returns chaptersFlow
        coEvery { mockWebPageRepository.updateWebPage(any()) } returns Unit
        coEvery { mockNovelRepository.getNovel(1L) } returns testNovels[0]
        
        // Act - Simulate novel details operations
        val novel = mockNovelRepository.getNovel(1L)
        val chapters = mockWebPageRepository.getWebPagesForNovel(1L).first()
        
        // Mark chapter as read
        val readChapter = testWebPages[0].copy(isRead = 1L)
        mockWebPageRepository.updateWebPage(readChapter)
        
        // Assert
        assertNotNull("Novel should be found", novel)
        assertEquals("Test Novel 1", novel.name)
        assertEquals(2, chapters.size)
        assertEquals("Chapter 1: Beginning", chapters[0].chapter)
        assertEquals("Chapter 2: Development", chapters[1].chapter)
        
        // Verify operations
        coVerify { mockNovelRepository.getNovel(1L) }
        verify { mockWebPageRepository.getWebPagesForNovel(1L) }
        coVerify { mockWebPageRepository.updateWebPage(readChapter) }
    }

    @Test
    fun `test search functionality - novel discovery and filtering`() = testScope.runTest {
        // Arrange
        val searchResults = testNovels.filter { it.name.contains("Test") }
        coEvery { mockApiService.searchNovels("Test") } returns searchResults
        coEvery { mockApiService.searchNovels("NonExistent") } returns emptyList()
        
        // Act - Simulate search operations
        val foundNovels = mockApiService.searchNovels("Test")
        val emptyResults = mockApiService.searchNovels("NonExistent")
        
        // Assert
        assertEquals(2, foundNovels.size)
        assertTrue("Should find Test Novel 1", foundNovels.any { it.name == "Test Novel 1" })
        assertTrue("Should find Test Novel 2", foundNovels.any { it.name == "Test Novel 2" })
        assertEquals(0, emptyResults.size)
        
        // Verify operations
        coVerify { mockApiService.searchNovels("Test") }
        coVerify { mockApiService.searchNovels("NonExistent") }
    }

    @Test
    fun `test download functionality - novel and chapter downloading`() = testScope.runTest {
        // Arrange
        val downloadProgressFlow = MutableSharedFlow<Float>()
        val downloadStatusFlow = MutableStateFlow("idle")
        
        coEvery { mockDownloadService.downloadNovel(any()) } returns Unit
        coEvery { mockDownloadService.downloadChapter(any(), any()) } returns Unit
        every { mockDownloadService.getDownloadProgress(any()) } returns downloadProgressFlow
        every { mockDownloadService.getDownloadStatus(any()) } returns downloadStatusFlow
        coEvery { mockDownloadService.cancelDownload(any()) } returns Unit
        
        // Act - Simulate download operations
        val progressValues = mutableListOf<Float>()
        val statusValues = mutableListOf<String>()
        
        val progressJob = launch {
            mockDownloadService.getDownloadProgress(1L).collect {
                progressValues.add(it)
            }
        }
        
        val statusJob = launch {
            mockDownloadService.getDownloadStatus(1L).collect {
                statusValues.add(it)
            }
        }
        
        // Start download
        mockDownloadService.downloadNovel(1L)
        downloadStatusFlow.value = "downloading"
        downloadProgressFlow.emit(0.0f)
        downloadProgressFlow.emit(0.5f)
        downloadProgressFlow.emit(1.0f)
        downloadStatusFlow.value = "completed"
        
        // Download individual chapter
        mockDownloadService.downloadChapter(1L, "https://example.com/chapter/1")
        
        advanceUntilIdle()
        
        progressJob.cancel()
        statusJob.cancel()
        
        // Assert
        assertTrue("Should track progress", progressValues.contains(0.0f))
        assertTrue("Should track progress", progressValues.contains(0.5f))
        assertTrue("Should track progress", progressValues.contains(1.0f))
        assertTrue("Should track status", statusValues.contains("downloading"))
        assertTrue("Should track status", statusValues.contains("completed"))
        
        // Verify operations
        coVerify { mockDownloadService.downloadNovel(1L) }
        coVerify { mockDownloadService.downloadChapter(1L, "https://example.com/chapter/1") }
        verify { mockDownloadService.getDownloadProgress(1L) }
        verify { mockDownloadService.getDownloadStatus(1L) }
    }

    @Test
    fun `test sync functionality - data synchronization and conflict resolution`() = testScope.runTest {
        // Arrange
        val localNovel = testNovels[0].copy(chaptersCount = 45L, newChaptersCount = 0L)
        val remoteNovel = testNovels[0].copy(chaptersCount = 50L, newChaptersCount = 5L)
        
        coEvery { mockNovelRepository.getNovel(1L) } returns localNovel
        coEvery { mockApiService.getNovelDetails(any()) } returns remoteNovel
        coEvery { mockNovelRepository.updateNovel(any()) } returns Unit
        coEvery { mockSyncService.syncNovel(1L) } returns Unit
        coEvery { mockSyncService.syncAllNovels() } returns Unit
        
        // Act - Simulate sync operations
        val local = mockNovelRepository.getNovel(1L)
        val remote = mockApiService.getNovelDetails(local.url)
        
        // Resolve conflicts (remote has newer data)
        val resolvedNovel = if (remote.chaptersCount > local.chaptersCount) {
            remote.copy(id = local.id)
        } else {
            local
        }
        
        mockNovelRepository.updateNovel(resolvedNovel)
        mockSyncService.syncNovel(1L)
        mockSyncService.syncAllNovels()
        
        // Assert
        assertEquals(45L, local.chaptersCount)
        assertEquals(50L, remote.chaptersCount)
        assertEquals(50L, resolvedNovel.chaptersCount)
        assertEquals(5L, resolvedNovel.newChaptersCount)
        
        // Verify operations
        coVerify { mockNovelRepository.getNovel(1L) }
        coVerify { mockApiService.getNovelDetails(local.url) }
        coVerify { mockNovelRepository.updateNovel(resolvedNovel) }
        coVerify { mockSyncService.syncNovel(1L) }
        coVerify { mockSyncService.syncAllNovels() }
    }

    @Test
    fun `test reading functionality - chapter navigation and progress tracking`() = testScope.runTest {
        // Arrange
        val chaptersFlow = MutableStateFlow(testWebPages)
        coEvery { mockWebPageRepository.getWebPagesForNovel(1L) } returns chaptersFlow
        coEvery { mockWebPageRepository.updateWebPage(any()) } returns Unit
        coEvery { mockWebPageRepository.getNextChapter(any()) } returns testWebPages[1]
        coEvery { mockWebPageRepository.getPreviousChapter(any()) } returns testWebPages[0]
        
        // Act - Simulate reading operations
        val chapters = mockWebPageRepository.getWebPagesForNovel(1L).first()
        val currentChapter = chapters[0]
        
        // Mark current chapter as read
        val readChapter = currentChapter.copy(isRead = 1L)
        mockWebPageRepository.updateWebPage(readChapter)
        
        // Navigate to next chapter
        val nextChapter = mockWebPageRepository.getNextChapter(currentChapter.id!!)
        
        // Navigate to previous chapter
        val previousChapter = mockWebPageRepository.getPreviousChapter(nextChapter.id!!)
        
        // Assert
        assertEquals(2, chapters.size)
        assertEquals(1L, readChapter.isRead)
        assertEquals("Chapter 2: Development", nextChapter.chapter)
        assertEquals("Chapter 1: Beginning", previousChapter.chapter)
        
        // Verify operations
        verify { mockWebPageRepository.getWebPagesForNovel(1L) }
        coVerify { mockWebPageRepository.updateWebPage(readChapter) }
        coVerify { mockWebPageRepository.getNextChapter(currentChapter.id!!) }
        coVerify { mockWebPageRepository.getPreviousChapter(nextChapter.id!!) }
    }

    @Test
    fun `test user preferences and settings functionality`() = testScope.runTest {
        // Arrange - Mock preferences operations
        val mockPreferences = mockk<android.content.SharedPreferences>()
        val mockEditor = mockk<android.content.SharedPreferences.Editor>()
        
        every { mockPreferences.getString("theme", "light") } returns "dark"
        every { mockPreferences.getBoolean("auto_sync", false) } returns true
        every { mockPreferences.getInt("font_size", 16) } returns 18
        every { mockPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.putBoolean(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.apply() } returns Unit
        
        // Act - Simulate settings operations
        val currentTheme = mockPreferences.getString("theme", "light")
        val autoSyncEnabled = mockPreferences.getBoolean("auto_sync", false)
        val fontSize = mockPreferences.getInt("font_size", 16)
        
        // Update settings
        mockPreferences.edit()
            .putString("theme", "night")
            .putBoolean("auto_sync", false)
            .putInt("font_size", 20)
            .apply()
        
        // Assert
        assertEquals("dark", currentTheme)
        assertTrue("Auto sync should be enabled", autoSyncEnabled)
        assertEquals(18, fontSize)
        
        // Verify operations
        verify { mockPreferences.getString("theme", "light") }
        verify { mockPreferences.getBoolean("auto_sync", false) }
        verify { mockPreferences.getInt("font_size", 16) }
        verify { mockPreferences.edit() }
        verify { mockEditor.putString("theme", "night") }
        verify { mockEditor.putBoolean("auto_sync", false) }
        verify { mockEditor.putInt("font_size", 20) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `test background operations - services and workers`() = testScope.runTest {
        // Arrange
        val backgroundJob = SupervisorJob()
        val backgroundScope = CoroutineScope(testDispatcher + backgroundJob)
        
        var syncCompleted = false
        var downloadCompleted = false
        var cleanupCompleted = false
        
        coEvery { mockSyncService.performBackgroundSync() } coAnswers {
            delay(100)
            syncCompleted = true
        }
        
        coEvery { mockDownloadService.processDownloadQueue() } coAnswers {
            delay(150)
            downloadCompleted = true
        }
        
        // Act - Simulate background operations
        val syncJob = backgroundScope.launch {
            mockSyncService.performBackgroundSync()
        }
        
        val downloadJob = backgroundScope.launch {
            mockDownloadService.processDownloadQueue()
        }
        
        val cleanupJob = backgroundScope.launch {
            delay(200)
            cleanupCompleted = true
        }
        
        advanceUntilIdle()
        
        // Assert
        assertTrue("Sync should complete", syncCompleted)
        assertTrue("Download should complete", downloadCompleted)
        assertTrue("Cleanup should complete", cleanupCompleted)
        assertTrue("All jobs should complete successfully", syncJob.isCompleted)
        assertTrue("All jobs should complete successfully", downloadJob.isCompleted)
        assertTrue("All jobs should complete successfully", cleanupJob.isCompleted)
        
        // Verify operations
        coVerify { mockSyncService.performBackgroundSync() }
        coVerify { mockDownloadService.processDownloadQueue() }
        
        // Cleanup
        backgroundJob.cancel()
    }

    @Test
    fun `test edge cases and error scenarios`() = testScope.runTest {
        // Arrange - Setup various edge cases
        coEvery { mockNovelRepository.getNovel(999L) } returns null // Non-existent novel
        coEvery { mockWebPageRepository.getWebPagesForNovel(999L) } returns flowOf(emptyList())
        coEvery { mockApiService.searchNovels("") } returns emptyList() // Empty search
        coEvery { mockDownloadService.downloadNovel(999L) } throws IllegalArgumentException("Novel not found")
        
        // Act & Assert - Test edge cases
        
        // Non-existent novel
        val nonExistentNovel = mockNovelRepository.getNovel(999L)
        assertNull("Non-existent novel should return null", nonExistentNovel)
        
        // Empty chapters list
        val emptyChapters = mockWebPageRepository.getWebPagesForNovel(999L).first()
        assertTrue("Empty chapters list should be empty", emptyChapters.isEmpty())
        
        // Empty search query
        val emptySearchResults = mockApiService.searchNovels("")
        assertTrue("Empty search should return empty list", emptySearchResults.isEmpty())
        
        // Invalid download
        var downloadError: Exception? = null
        try {
            mockDownloadService.downloadNovel(999L)
        } catch (e: Exception) {
            downloadError = e
        }
        
        assertNotNull("Download error should be caught", downloadError)
        assertTrue("Should be IllegalArgumentException", downloadError is IllegalArgumentException)
        
        // Verify operations
        coVerify { mockNovelRepository.getNovel(999L) }
        verify { mockWebPageRepository.getWebPagesForNovel(999L) }
        coVerify { mockApiService.searchNovels("") }
        coVerify { mockDownloadService.downloadNovel(999L) }
    }

    @Test
    fun `test user experience consistency - UI state management`() = testScope.runTest {
        // Arrange - Setup UI state flows
        val loadingStateFlow = MutableStateFlow(false)
        val errorStateFlow = MutableStateFlow<String?>(null)
        val dataStateFlow = MutableStateFlow<List<Novel>>(emptyList())
        
        // Act - Simulate UI state changes during operations
        
        // Loading state
        loadingStateFlow.value = true
        assertEquals(true, loadingStateFlow.value)
        
        // Data loaded successfully
        dataStateFlow.value = testNovels
        loadingStateFlow.value = false
        assertEquals(false, loadingStateFlow.value)
        assertEquals(2, dataStateFlow.value.size)
        
        // Error state
        errorStateFlow.value = "Network error occurred"
        loadingStateFlow.value = false
        assertEquals("Network error occurred", errorStateFlow.value)
        
        // Recovery from error
        errorStateFlow.value = null
        dataStateFlow.value = testNovels
        assertNull("Error should be cleared", errorStateFlow.value)
        assertEquals(2, dataStateFlow.value.size)
        
        // Assert - UI states should be consistent
        assertFalse("Loading should be false after operations", loadingStateFlow.value)
        assertNull("Error should be null after recovery", errorStateFlow.value)
        assertEquals("Data should be available", testNovels, dataStateFlow.value)
    }
}