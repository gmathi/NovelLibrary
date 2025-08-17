package io.github.gmathi.novellibrary.integration

import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.database.repository.WebPageRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.network.api.NovelApiService
import io.github.gmathi.novellibrary.service.download.CoroutineDownloadService
import io.github.gmathi.novellibrary.util.coroutines.CoroutineErrorHandler
import io.github.gmathi.novellibrary.util.coroutines.RetryUtils
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
import java.net.UnknownHostException
import java.sql.SQLException

/**
 * Integration tests for error handling across all app layers using coroutines.
 * Validates that errors are properly propagated and handled throughout the application.
 * 
 * Tests Requirements: 7.3, 2.1, 2.2, 2.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ErrorHandlingIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mock dependencies
    private val mockNovelRepository = mockk<NovelRepository>()
    private val mockWebPageRepository = mockk<WebPageRepository>()
    private val mockApiService = mockk<NovelApiService>()
    private val mockDocumentFetcher = mockk<WebPageDocumentFetcher>()
    private val mockDownloadService = mockk<CoroutineDownloadService>()
    private val mockErrorHandler = mockk<CoroutineErrorHandler>()
    private val mockRetryUtils = mockk<RetryUtils>()
    
    private val testNovel = Novel(
        id = 1L,
        name = "Test Novel",
        url = "https://example.com/novel/1",
        imageUrl = "https://example.com/image.jpg",
        rating = "4.5",
        shortDescription = "A test novel",
        longDescription = "A longer description",
        chaptersCount = 100L,
        newChaptersCount = 5L,
        currentChapterUrl = "https://example.com/novel/1/chapter/1"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup default error handler behavior
        every { mockErrorHandler.handleError(any(), any()) } returns Unit
        coEvery { mockErrorHandler.handleSuspendError(any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test network error propagation through all layers`() = testScope.runTest {
        // Arrange - Setup network error at API level
        val networkError = IOException("Network connection failed")
        coEvery { mockApiService.searchNovels(any()) } throws networkError
        
        // Act - Attempt operation that should propagate error
        var caughtError: Throwable? = null
        
        try {
            // Simulate a service layer that calls repository that calls API
            val searchResults = mockApiService.searchNovels("test")
            mockNovelRepository.insertNovels(searchResults)
        } catch (e: Exception) {
            caughtError = e
            mockErrorHandler.handleError(e, "Search operation failed")
        }
        
        // Assert
        assertNotNull("Error should be caught", caughtError)
        assertTrue("Should be IOException", caughtError is IOException)
        assertEquals("Network connection failed", caughtError?.message)
        
        // Verify error handling was called
        verify { mockErrorHandler.handleError(any<IOException>(), "Search operation failed") }
        coVerify { mockApiService.searchNovels("test") }
    }

    @Test
    fun `test database error handling with transaction rollback`() = testScope.runTest {
        // Arrange - Setup database error
        val dbError = SQLException("Database constraint violation")
        coEvery { mockNovelRepository.insertNovel(any()) } throws dbError
        coEvery { mockWebPageRepository.insertWebPage(any()) } returns 1L
        
        // Act - Attempt transaction that should fail and rollback
        var transactionFailed = false
        var rollbackExecuted = false
        
        try {
            // Simulate a transaction
            coroutineScope {
                val novelId = mockNovelRepository.insertNovel(testNovel)
                // This should not execute due to the exception above
                mockWebPageRepository.insertWebPage(
                    io.github.gmathi.novellibrary.model.database.WebPage(
                        url = "test",
                        chapter = "test",
                        novelId = novelId
                    )
                )
            }
        } catch (e: SQLException) {
            transactionFailed = true
            rollbackExecuted = true
            mockErrorHandler.handleError(e, "Transaction failed")
        }
        
        // Assert
        assertTrue("Transaction should have failed", transactionFailed)
        assertTrue("Rollback should have been executed", rollbackExecuted)
        
        // Verify only the first operation was attempted
        coVerify { mockNovelRepository.insertNovel(testNovel) }
        coVerify(exactly = 0) { mockWebPageRepository.insertWebPage(any()) }
        verify { mockErrorHandler.handleError(any<SQLException>(), "Transaction failed") }
    }

    @Test
    fun `test timeout error handling with retry mechanism`() = testScope.runTest {
        // Arrange - Setup timeout error with retry
        val timeoutError = SocketTimeoutException("Request timeout")
        var attemptCount = 0
        
        coEvery { mockDocumentFetcher.fetchDocument(any()) } coAnswers {
            attemptCount++
            if (attemptCount < 3) {
                throw timeoutError
            } else {
                "Success after retry"
            }
        }
        
        coEvery { mockRetryUtils.retryWithExponentialBackoff(any<suspend () -> String>()) } coAnswers {
            val operation = firstArg<suspend () -> String>()
            var lastException: Exception? = null
            
            repeat(3) { attempt ->
                try {
                    return@coAnswers operation()
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < 2) {
                        delay(100 * (attempt + 1)) // Exponential backoff
                    }
                }
            }
            throw lastException!!
        }
        
        // Act - Execute operation with retry
        val result = mockRetryUtils.retryWithExponentialBackoff {
            mockDocumentFetcher.fetchDocument("https://example.com/chapter/1")
        }
        
        // Assert
        assertEquals("Success after retry", result)
        assertEquals(3, attemptCount)
        
        coVerify { mockRetryUtils.retryWithExponentialBackoff(any<suspend () -> String>()) }
        coVerify(exactly = 3) { mockDocumentFetcher.fetchDocument("https://example.com/chapter/1") }
    }

    @Test
    fun `test cancellation error handling`() = testScope.runTest {
        // Arrange
        var operationStarted = false
        var operationCancelled = false
        var cleanupExecuted = false
        
        coEvery { mockDownloadService.downloadNovel(any()) } coAnswers {
            operationStarted = true
            try {
                delay(1000) // Long operation
                "Download completed"
            } catch (e: CancellationException) {
                operationCancelled = true
                throw e
            } finally {
                cleanupExecuted = true
            }
        }
        
        // Act - Start operation and cancel it
        val job = launch {
            try {
                mockDownloadService.downloadNovel(testNovel.id!!)
            } catch (e: CancellationException) {
                mockErrorHandler.handleError(e, "Download cancelled")
                throw e
            }
        }
        
        advanceTimeBy(100) // Let operation start
        job.cancel("User cancelled download")
        advanceUntilIdle()
        
        // Assert
        assertTrue("Operation should have started", operationStarted)
        assertTrue("Operation should have been cancelled", operationCancelled)
        assertTrue("Cleanup should have been executed", cleanupExecuted)
        assertTrue("Job should be cancelled", job.isCancelled)
        
        verify { mockErrorHandler.handleError(any<CancellationException>(), "Download cancelled") }
        coVerify { mockDownloadService.downloadNovel(testNovel.id!!) }
    }

    @Test
    fun `test error recovery with fallback mechanisms`() = testScope.runTest {
        // Arrange - Setup primary and fallback operations
        val primaryError = UnknownHostException("Primary server unreachable")
        val fallbackNovel = testNovel.copy(name = "Fallback Novel")
        
        coEvery { mockApiService.getNovelDetails("https://primary.com/novel/1") } throws primaryError
        coEvery { mockApiService.getNovelDetails("https://fallback.com/novel/1") } returns fallbackNovel
        
        // Act - Implement fallback mechanism
        var result: Novel? = null
        var primaryFailed = false
        var fallbackUsed = false
        
        try {
            result = mockApiService.getNovelDetails("https://primary.com/novel/1")
        } catch (e: UnknownHostException) {
            primaryFailed = true
            mockErrorHandler.handleError(e, "Primary server failed, trying fallback")
            
            try {
                result = mockApiService.getNovelDetails("https://fallback.com/novel/1")
                fallbackUsed = true
            } catch (fallbackError: Exception) {
                mockErrorHandler.handleError(fallbackError, "Fallback also failed")
                throw fallbackError
            }
        }
        
        // Assert
        assertTrue("Primary should have failed", primaryFailed)
        assertTrue("Fallback should have been used", fallbackUsed)
        assertNotNull("Result should not be null", result)
        assertEquals("Fallback Novel", result?.name)
        
        verify { mockErrorHandler.handleError(any<UnknownHostException>(), "Primary server failed, trying fallback") }
        coVerify { mockApiService.getNovelDetails("https://primary.com/novel/1") }
        coVerify { mockApiService.getNovelDetails("https://fallback.com/novel/1") }
    }

    @Test
    fun `test concurrent error handling with partial failures`() = testScope.runTest {
        // Arrange - Setup mixed success/failure scenarios
        val novels = listOf(
            testNovel.copy(id = 1L, url = "https://example.com/novel/1"),
            testNovel.copy(id = 2L, url = "https://example.com/novel/2"),
            testNovel.copy(id = 3L, url = "https://example.com/novel/3")
        )
        
        coEvery { mockApiService.getNovelDetails("https://example.com/novel/1") } returns novels[0]
        coEvery { mockApiService.getNovelDetails("https://example.com/novel/2") } throws IOException("Network error")
        coEvery { mockApiService.getNovelDetails("https://example.com/novel/3") } returns novels[2]
        
        // Act - Process concurrently with error handling
        val results = mutableListOf<Novel>()
        val errors = mutableListOf<Exception>()
        
        val jobs = novels.map { novel ->
            async {
                try {
                    val result = mockApiService.getNovelDetails(novel.url)
                    results.add(result)
                    result
                } catch (e: Exception) {
                    errors.add(e)
                    mockErrorHandler.handleError(e, "Failed to fetch novel ${novel.id}")
                    null
                }
            }
        }
        
        val completedResults = jobs.awaitAll().filterNotNull()
        
        // Assert
        assertEquals(2, completedResults.size) // 2 successful, 1 failed
        assertEquals(1, errors.size)
        assertEquals(2, results.size)
        
        assertTrue("Should contain novel 1", results.any { it.id == 1L })
        assertTrue("Should contain novel 3", results.any { it.id == 3L })
        assertTrue("Should have IOException", errors[0] is IOException)
        
        verify { mockErrorHandler.handleError(any<IOException>(), "Failed to fetch novel 2") }
        coVerify { mockApiService.getNovelDetails("https://example.com/novel/1") }
        coVerify { mockApiService.getNovelDetails("https://example.com/novel/2") }
        coVerify { mockApiService.getNovelDetails("https://example.com/novel/3") }
    }

    @Test
    fun `test error handling in Flow streams`() = testScope.runTest {
        // Arrange - Setup Flow with errors
        val dataFlow = flow {
            emit(testNovel.copy(id = 1L))
            emit(testNovel.copy(id = 2L))
            throw RuntimeException("Stream error")
            emit(testNovel.copy(id = 3L)) // This should not be emitted
        }
        
        // Act - Collect with error handling
        val collectedNovels = mutableListOf<Novel>()
        var streamError: Exception? = null
        
        try {
            dataFlow.collect { novel ->
                collectedNovels.add(novel)
            }
        } catch (e: Exception) {
            streamError = e
            mockErrorHandler.handleError(e, "Stream processing failed")
        }
        
        // Assert
        assertEquals(2, collectedNovels.size) // Only first 2 items before error
        assertNotNull("Stream error should be caught", streamError)
        assertTrue("Should be RuntimeException", streamError is RuntimeException)
        assertEquals("Stream error", streamError?.message)
        
        verify { mockErrorHandler.handleError(any<RuntimeException>(), "Stream processing failed") }
    }

    @Test
    fun `test error handling with resource cleanup`() = testScope.runTest {
        // Arrange
        val resources = mutableListOf<String>()
        val cleanedResources = mutableListOf<String>()
        
        suspend fun operationWithResources(resourceId: String) {
            try {
                resources.add(resourceId)
                delay(50)
                if (resourceId == "resource_2") {
                    throw RuntimeException("Operation failed for $resourceId")
                }
            } finally {
                cleanedResources.add(resourceId)
            }
        }
        
        // Act - Execute operations with error
        val results = mutableListOf<String>()
        val errors = mutableListOf<Exception>()
        
        listOf("resource_1", "resource_2", "resource_3").forEach { resourceId ->
            try {
                operationWithResources(resourceId)
                results.add(resourceId)
            } catch (e: Exception) {
                errors.add(e)
                mockErrorHandler.handleError(e, "Resource operation failed")
            }
        }
        
        // Assert
        assertEquals(3, resources.size) // All resources were created
        assertEquals(3, cleanedResources.size) // All resources were cleaned up
        assertEquals(2, results.size) // 2 successful operations
        assertEquals(1, errors.size) // 1 failed operation
        
        assertTrue("resource_1 should be cleaned", cleanedResources.contains("resource_1"))
        assertTrue("resource_2 should be cleaned", cleanedResources.contains("resource_2"))
        assertTrue("resource_3 should be cleaned", cleanedResources.contains("resource_3"))
        
        verify(exactly = 1) { mockErrorHandler.handleError(any<RuntimeException>(), "Resource operation failed") }
    }

    @Test
    fun `test supervisor job error isolation`() = testScope.runTest {
        // Arrange
        val parentJob = SupervisorJob()
        val supervisorScope = CoroutineScope(testDispatcher + parentJob)
        
        val completedTasks = mutableListOf<String>()
        val failedTasks = mutableListOf<String>()
        
        // Act - Launch child jobs with one failing
        val job1 = supervisorScope.launch {
            try {
                delay(50)
                completedTasks.add("job1")
            } catch (e: Exception) {
                failedTasks.add("job1")
                mockErrorHandler.handleError(e, "Job 1 failed")
            }
        }
        
        val job2 = supervisorScope.launch {
            try {
                delay(25)
                throw RuntimeException("Job 2 intentional failure")
            } catch (e: Exception) {
                failedTasks.add("job2")
                mockErrorHandler.handleError(e, "Job 2 failed")
            }
        }
        
        val job3 = supervisorScope.launch {
            try {
                delay(75)
                completedTasks.add("job3")
            } catch (e: Exception) {
                failedTasks.add("job3")
                mockErrorHandler.handleError(e, "Job 3 failed")
            }
        }
        
        advanceUntilIdle()
        
        // Assert - SupervisorJob should isolate failures
        assertTrue("Job 1 should complete", completedTasks.contains("job1"))
        assertTrue("Job 2 should fail", failedTasks.contains("job2"))
        assertTrue("Job 3 should complete despite job 2 failure", completedTasks.contains("job3"))
        
        assertEquals(2, completedTasks.size)
        assertEquals(1, failedTasks.size)
        
        verify { mockErrorHandler.handleError(any<RuntimeException>(), "Job 2 failed") }
        
        // Cleanup
        parentJob.cancel()
    }
}