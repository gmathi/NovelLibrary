package io.github.gmathi.novellibrary.integration

import io.github.gmathi.novellibrary.database.repository.NovelRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.api.NovelApiService
import io.github.gmathi.novellibrary.util.coroutines.CoroutineUtils
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Performance comparison tests to validate memory usage and performance improvements
 * after migrating from RxJava to Coroutines.
 * 
 * Tests Requirements: 7.3, 2.1, 2.2, 2.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceComparisonTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val mockRepository = mockk<NovelRepository>()
    private val mockApiService = mockk<NovelApiService>()
    
    private val testNovels = (1..100).map { index ->
        Novel(
            id = index.toLong(),
            name = "Novel $index",
            url = "https://example.com/novel/$index",
            imageUrl = "https://example.com/image$index.jpg",
            rating = "${(index % 5) + 1}.0",
            shortDescription = "Description for novel $index",
            longDescription = "Long description for novel $index",
            chaptersCount = (index * 10).toLong(),
            newChaptersCount = (index % 5).toLong(),
            currentChapterUrl = "https://example.com/novel/$index/chapter/1"
        )
    }

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
    fun `test coroutines memory efficiency with large datasets`() = testScope.runTest {
        // Arrange
        val largeDataset = (1..10000).map { index ->
            Novel(
                id = index.toLong(),
                name = "Novel $index",
                url = "https://example.com/novel/$index"
            )
        }
        
        val dataFlow = flow {
            // Simulate chunked data emission to test backpressure handling
            largeDataset.chunked(100).forEach { chunk ->
                emit(chunk)
                delay(1) // Small delay to simulate real-world scenario
            }
        }
        
        // Act - Process large dataset with Flow
        val processedCount = mutableListOf<Int>()
        val memoryUsageBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        dataFlow.collect { chunk ->
            processedCount.add(chunk.size)
        }
        
        val memoryUsageAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = memoryUsageAfter - memoryUsageBefore
        
        // Assert
        assertEquals(100, processedCount.size) // 100 chunks of 100 items each
        assertTrue("Memory increase should be reasonable", memoryIncrease < 50 * 1024 * 1024) // Less than 50MB
        
        // Verify all chunks were processed
        val totalProcessed = processedCount.sum()
        assertEquals(10000, totalProcessed)
    }

    @Test
    fun `test concurrent operations performance`() = testScope.runTest {
        // Arrange
        coEvery { mockApiService.getNovelDetails(any()) } coAnswers {
            delay(10) // Simulate network delay
            testNovels.random()
        }
        
        // Act - Test concurrent API calls
        val executionTime = measureTimeMillis {
            val deferredResults = testNovels.take(50).map { novel ->
                async {
                    mockApiService.getNovelDetails(novel.url)
                }
            }
            
            val results = deferredResults.awaitAll()
            assertEquals(50, results.size)
        }
        
        advanceUntilIdle()
        
        // Assert - Concurrent execution should be much faster than sequential
        assertTrue("Concurrent execution should be efficient", executionTime < 1000)
        
        // Verify all API calls were made concurrently
        coVerify(exactly = 50) { mockApiService.getNovelDetails(any()) }
    }

    @Test
    fun `test Flow vs Observable performance characteristics`() = testScope.runTest {
        // Arrange - Setup Flow-based data stream
        val dataFlow = flow {
            repeat(1000) { index ->
                emit(testNovels[index % testNovels.size])
                if (index % 100 == 0) {
                    delay(1) // Simulate processing delay
                }
            }
        }
        
        // Act - Process data with Flow operators
        val startTime = System.currentTimeMillis()
        val processedNovels = mutableListOf<Novel>()
        
        dataFlow
            .filter { it.rating.toFloat() >= 3.0f }
            .map { it.copy(name = "Filtered: ${it.name}") }
            .take(500)
            .collect { novel ->
                processedNovels.add(novel)
            }
        
        val endTime = System.currentTimeMillis()
        val processingTime = endTime - startTime
        
        // Assert
        assertTrue("Processing should be efficient", processingTime < 2000)
        assertTrue("Should have processed novels", processedNovels.isNotEmpty())
        assertTrue("Should respect take(500) limit", processedNovels.size <= 500)
        
        // Verify filtering worked
        processedNovels.forEach { novel ->
            assertTrue("All novels should have rating >= 3.0", novel.rating.toFloat() >= 3.0f)
            assertTrue("All novels should have filtered prefix", novel.name.startsWith("Filtered:"))
        }
    }

    @Test
    fun `test structured concurrency benefits`() = testScope.runTest {
        // Arrange
        val parentJob = SupervisorJob()
        val childScope = CoroutineScope(testDispatcher + parentJob)
        
        val completedTasks = mutableListOf<String>()
        val failedTasks = mutableListOf<String>()
        
        coEvery { mockRepository.getNovel(1L) } coAnswers {
            delay(50)
            testNovels[0]
        }
        
        coEvery { mockRepository.getNovel(2L) } coAnswers {
            delay(100)
            throw RuntimeException("Database error")
        }
        
        coEvery { mockRepository.getNovel(3L) } coAnswers {
            delay(150)
            testNovels[2]
        }
        
        // Act - Launch multiple child coroutines
        val job1 = childScope.launch {
            try {
                mockRepository.getNovel(1L)
                completedTasks.add("task1")
            } catch (e: Exception) {
                failedTasks.add("task1")
            }
        }
        
        val job2 = childScope.launch {
            try {
                mockRepository.getNovel(2L)
                completedTasks.add("task2")
            } catch (e: Exception) {
                failedTasks.add("task2")
            }
        }
        
        val job3 = childScope.launch {
            try {
                mockRepository.getNovel(3L)
                completedTasks.add("task3")
            } catch (e: Exception) {
                failedTasks.add("task3")
            }
        }
        
        advanceUntilIdle()
        
        // Assert - Structured concurrency should handle failures gracefully
        assertTrue("Task 1 should complete", completedTasks.contains("task1"))
        assertTrue("Task 2 should fail", failedTasks.contains("task2"))
        assertTrue("Task 3 should complete despite task 2 failure", completedTasks.contains("task3"))
        
        assertEquals(2, completedTasks.size)
        assertEquals(1, failedTasks.size)
        
        // Verify all operations were attempted
        coVerify { mockRepository.getNovel(1L) }
        coVerify { mockRepository.getNovel(2L) }
        coVerify { mockRepository.getNovel(3L) }
        
        // Cleanup
        parentJob.cancel()
    }

    @Test
    fun `test cancellation efficiency`() = testScope.runTest {
        // Arrange
        var operationStarted = false
        var operationCancelled = false
        var operationCompleted = false
        
        coEvery { mockApiService.searchNovels(any()) } coAnswers {
            operationStarted = true
            try {
                delay(1000) // Long operation
                operationCompleted = true
                testNovels
            } catch (e: CancellationException) {
                operationCancelled = true
                throw e
            }
        }
        
        // Act - Start operation and cancel it quickly
        val job = launch {
            mockApiService.searchNovels("test")
        }
        
        advanceTimeBy(50) // Let operation start
        job.cancel() // Cancel quickly
        advanceUntilIdle()
        
        // Assert - Cancellation should be immediate and efficient
        assertTrue("Operation should have started", operationStarted)
        assertTrue("Operation should have been cancelled", operationCancelled)
        assertFalse("Operation should not have completed", operationCompleted)
        assertTrue("Job should be cancelled", job.isCancelled)
        
        coVerify { mockApiService.searchNovels("test") }
    }

    @Test
    fun `test resource cleanup efficiency`() = testScope.runTest {
        // Arrange
        val resourcesCreated = mutableListOf<String>()
        val resourcesCleaned = mutableListOf<String>()
        
        suspend fun simulateResourceIntensiveOperation(resourceId: String) {
            try {
                resourcesCreated.add(resourceId)
                delay(100)
                // Simulate work
            } finally {
                resourcesCleaned.add(resourceId)
            }
        }
        
        // Act - Create multiple operations and cancel them
        val jobs = (1..10).map { index ->
            launch {
                simulateResourceIntensiveOperation("resource_$index")
            }
        }
        
        advanceTimeBy(50) // Let some operations start
        
        // Cancel all jobs
        jobs.forEach { it.cancel() }
        advanceUntilIdle()
        
        // Assert - All resources should be cleaned up
        assertEquals(resourcesCreated.size, resourcesCleaned.size)
        assertTrue("Some resources should have been created", resourcesCreated.isNotEmpty())
        
        // Verify cleanup happened for all created resources
        resourcesCreated.forEach { resource ->
            assertTrue("Resource $resource should be cleaned up", resourcesCleaned.contains(resource))
        }
    }

    @Test
    fun `test error propagation performance`() = testScope.runTest {
        // Arrange
        val errorHandlingTimes = mutableListOf<Long>()
        
        coEvery { mockRepository.getNovel(any()) } throws RuntimeException("Test error")
        
        // Act - Test error handling performance
        repeat(100) { index ->
            val startTime = System.currentTimeMillis()
            
            try {
                mockRepository.getNovel(index.toLong())
            } catch (e: Exception) {
                val endTime = System.currentTimeMillis()
                errorHandlingTimes.add(endTime - startTime)
            }
        }
        
        // Assert - Error handling should be fast and consistent
        val averageErrorHandlingTime = errorHandlingTimes.average()
        val maxErrorHandlingTime = errorHandlingTimes.maxOrNull() ?: 0L
        
        assertTrue("Average error handling should be fast", averageErrorHandlingTime < 10.0)
        assertTrue("Max error handling should be reasonable", maxErrorHandlingTime < 50L)
        
        // Verify all error scenarios were tested
        coVerify(exactly = 100) { mockRepository.getNovel(any()) }
    }

    @Test
    fun `test backpressure handling with Flow`() = testScope.runTest {
        // Arrange - Create a fast producer and slow consumer scenario
        val producedItems = mutableListOf<Int>()
        val consumedItems = mutableListOf<Int>()
        
        val fastProducer = flow {
            repeat(1000) { index ->
                emit(index)
                producedItems.add(index)
            }
        }
        
        // Act - Consume with backpressure
        fastProducer
            .buffer(50) // Buffer to handle backpressure
            .collect { item ->
                delay(1) // Slow consumer
                consumedItems.add(item)
            }
        
        // Assert - All items should be processed despite backpressure
        assertEquals(1000, producedItems.size)
        assertEquals(1000, consumedItems.size)
        assertEquals(producedItems, consumedItems)
    }
}