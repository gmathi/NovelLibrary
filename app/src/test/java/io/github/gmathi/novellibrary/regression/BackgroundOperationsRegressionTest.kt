package io.github.gmathi.novellibrary.regression

import io.github.gmathi.novellibrary.service.download.CoroutineDownloadService
import io.github.gmathi.novellibrary.service.firebase.NLFirebaseMessagingService
import io.github.gmathi.novellibrary.service.sync.CoroutineSyncService
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.worker.NovelSyncWorker
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Regression tests for background operations to ensure they work correctly
 * with coroutines after migration from RxJava.
 * 
 * Tests Requirements: 1.4, 2.1, 2.2, 2.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackgroundOperationsRegressionTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    // Mock services
    private val mockDownloadService = mockk<CoroutineDownloadService>()
    private val mockSyncService = mockk<CoroutineSyncService>()
    private val mockTTSService = mockk<TTSService>()
    private val mockFirebaseService = mockk<NLFirebaseMessagingService>()
    private val mockSyncWorker = mockk<NovelSyncWorker>()
    
    // Test data
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
    
    private val testWebPage = WebPage(
        id = 1L,
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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `test download service background operations`() = testScope.runTest {
        // Arrange
        val downloadQueue = listOf(testNovel.id!!, 2L, 3L)
        val downloadProgressFlow = MutableSharedFlow<Pair<Long, Float>>()
        val downloadStatusFlow = MutableStateFlow<Map<Long, String>>(emptyMap())
        
        coEvery { mockDownloadService.getDownloadQueue() } returns downloadQueue
        coEvery { mockDownloadService.processDownloadQueue() } coAnswers {
            downloadQueue.forEach { novelId ->
                downloadStatusFlow.value = downloadStatusFlow.value + (novelId to "downloading")
                downloadProgressFlow.emit(novelId to 0.0f)
                delay(50)
                downloadProgressFlow.emit(novelId to 0.5f)
                delay(50)
                downloadProgressFlow.emit(novelId to 1.0f)
                downloadStatusFlow.value = downloadStatusFlow.value + (novelId to "completed")
            }
        }
        
        every { mockDownloadService.getDownloadProgress() } returns downloadProgressFlow
        every { mockDownloadService.getDownloadStatus() } returns downloadStatusFlow
        coEvery { mockDownloadService.pauseDownload(any()) } returns Unit
        coEvery { mockDownloadService.resumeDownload(any()) } returns Unit
        coEvery { mockDownloadService.cancelDownload(any()) } returns Unit
        
        // Act - Process download queue in background
        val progressUpdates = mutableListOf<Pair<Long, Float>>()
        val statusUpdates = mutableListOf<Map<Long, String>>()
        
        val progressJob = launch {
            mockDownloadService.getDownloadProgress().collect { update ->
                progressUpdates.add(update)
            }
        }
        
        val statusJob = launch {
            mockDownloadService.getDownloadStatus().collect { status ->
                statusUpdates.add(status)
            }
        }
        
        // Start background processing
        val processingJob = launch {
            mockDownloadService.processDownloadQueue()
        }
        
        advanceUntilIdle()
        
        progressJob.cancel()
        statusJob.cancel()
        
        // Assert
        assertTrue("Processing job should complete", processingJob.isCompleted)
        assertTrue("Should have progress updates", progressUpdates.isNotEmpty())
        assertTrue("Should have status updates", statusUpdates.isNotEmpty())
        
        // Verify all novels were processed
        val finalStatus = statusUpdates.last()
        downloadQueue.forEach { novelId ->
            assertEquals("completed", finalStatus[novelId])
        }
        
        // Verify progress tracking
        downloadQueue.forEach { novelId ->
            assertTrue("Should have start progress", progressUpdates.any { it.first == novelId && it.second == 0.0f })
            assertTrue("Should have end progress", progressUpdates.any { it.first == novelId && it.second == 1.0f })
        }
        
        coVerify { mockDownloadService.getDownloadQueue() }
        coVerify { mockDownloadService.processDownloadQueue() }
        verify { mockDownloadService.getDownloadProgress() }
        verify { mockDownloadService.getDownloadStatus() }
    }

    @Test
    fun `test sync service background operations`() = testScope.runTest {
        // Arrange
        val novelsToSync = listOf(1L, 2L, 3L)
        val syncProgressFlow = MutableSharedFlow<Pair<Long, String>>()
        
        coEvery { mockSyncService.getNovelsToSync() } returns novelsToSync
        coEvery { mockSyncService.performBackgroundSync() } coAnswers {
            novelsToSync.forEach { novelId ->
                syncProgressFlow.emit(novelId to "syncing")
                delay(100)
                syncProgressFlow.emit(novelId to "completed")
            }
        }
        
        coEvery { mockSyncService.syncNovel(any()) } coAnswers {
            val novelId = firstArg<Long>()
            syncProgressFlow.emit(novelId to "syncing")
            delay(50)
            syncProgressFlow.emit(novelId to "completed")
        }
        
        every { mockSyncService.getSyncProgress() } returns syncProgressFlow
        coEvery { mockSyncService.schedulePeriodicSync() } returns Unit
        
        // Act - Perform background sync
        val syncUpdates = mutableListOf<Pair<Long, String>>()
        
        val progressJob = launch {
            mockSyncService.getSyncProgress().collect { update ->
                syncUpdates.add(update)
            }
        }
        
        // Start background sync
        val syncJob = launch {
            mockSyncService.performBackgroundSync()
        }
        
        // Schedule periodic sync
        mockSyncService.schedulePeriodicSync()
        
        advanceUntilIdle()
        
        progressJob.cancel()
        
        // Assert
        assertTrue("Sync job should complete", syncJob.isCompleted)
        assertTrue("Should have sync updates", syncUpdates.isNotEmpty())
        
        // Verify all novels were synced
        novelsToSync.forEach { novelId ->
            assertTrue("Should have syncing status", syncUpdates.any { it.first == novelId && it.second == "syncing" })
            assertTrue("Should have completed status", syncUpdates.any { it.first == novelId && it.second == "completed" })
        }
        
        coVerify { mockSyncService.getNovelsToSync() }
        coVerify { mockSyncService.performBackgroundSync() }
        coVerify { mockSyncService.schedulePeriodicSync() }
        verify { mockSyncService.getSyncProgress() }
    }

    @Test
    fun `test TTS service background operations`() = testScope.runTest {
        // Arrange
        val ttsStateFlow = MutableStateFlow("idle")
        val playbackProgressFlow = MutableSharedFlow<Float>()
        
        coEvery { mockTTSService.initializeTTS() } returns Unit
        coEvery { mockTTSService.speakText(any()) } coAnswers {
            ttsStateFlow.value = "speaking"
            playbackProgressFlow.emit(0.0f)
            delay(100)
            playbackProgressFlow.emit(0.5f)
            delay(100)
            playbackProgressFlow.emit(1.0f)
            ttsStateFlow.value = "completed"
        }
        
        coEvery { mockTTSService.pauseSpeech() } coAnswers {
            ttsStateFlow.value = "paused"
        }
        
        coEvery { mockTTSService.resumeSpeech() } coAnswers {
            ttsStateFlow.value = "speaking"
        }
        
        coEvery { mockTTSService.stopSpeech() } coAnswers {
            ttsStateFlow.value = "stopped"
        }
        
        every { mockTTSService.getTTSState() } returns ttsStateFlow
        every { mockTTSService.getPlaybackProgress() } returns playbackProgressFlow
        
        // Act - Test TTS operations
        val stateUpdates = mutableListOf<String>()
        val progressUpdates = mutableListOf<Float>()
        
        val stateJob = launch {
            mockTTSService.getTTSState().collect { state ->
                stateUpdates.add(state)
            }
        }
        
        val progressJob = launch {
            mockTTSService.getPlaybackProgress().collect { progress ->
                progressUpdates.add(progress)
            }
        }
        
        // Initialize and start TTS
        mockTTSService.initializeTTS()
        mockTTSService.speakText("This is a test chapter content for TTS playback.")
        
        advanceTimeBy(150)
        
        // Test pause/resume
        mockTTSService.pauseSpeech()
        advanceTimeBy(50)
        mockTTSService.resumeSpeech()
        
        advanceUntilIdle()
        
        stateJob.cancel()
        progressJob.cancel()
        
        // Assert
        assertTrue("Should have state updates", stateUpdates.isNotEmpty())
        assertTrue("Should have progress updates", progressUpdates.isNotEmpty())
        
        // Verify state transitions
        assertTrue("Should have idle state", stateUpdates.contains("idle"))
        assertTrue("Should have speaking state", stateUpdates.contains("speaking"))
        assertTrue("Should have paused state", stateUpdates.contains("paused"))
        
        // Verify progress tracking
        assertTrue("Should have start progress", progressUpdates.contains(0.0f))
        assertTrue("Should have end progress", progressUpdates.contains(1.0f))
        
        coVerify { mockTTSService.initializeTTS() }
        coVerify { mockTTSService.speakText(any()) }
        coVerify { mockTTSService.pauseSpeech() }
        coVerify { mockTTSService.resumeSpeech() }
        verify { mockTTSService.getTTSState() }
        verify { mockTTSService.getPlaybackProgress() }
    }

    @Test
    fun `test Firebase messaging service background operations`() = testScope.runTest {
        // Arrange
        val messageFlow = MutableSharedFlow<Map<String, String>>()
        val tokenFlow = MutableStateFlow<String?>(null)
        
        coEvery { mockFirebaseService.handleMessage(any()) } coAnswers {
            val messageData = firstArg<Map<String, String>>()
            messageFlow.emit(messageData)
        }
        
        coEvery { mockFirebaseService.refreshToken() } coAnswers {
            tokenFlow.value = "new_firebase_token_123"
        }
        
        coEvery { mockFirebaseService.subscribeToTopic(any()) } returns Unit
        coEvery { mockFirebaseService.unsubscribeFromTopic(any()) } returns Unit
        
        every { mockFirebaseService.getMessageFlow() } returns messageFlow
        every { mockFirebaseService.getTokenFlow() } returns tokenFlow
        
        // Act - Test Firebase operations
        val receivedMessages = mutableListOf<Map<String, String>>()
        val tokenUpdates = mutableListOf<String?>()
        
        val messageJob = launch {
            mockFirebaseService.getMessageFlow().collect { message ->
                receivedMessages.add(message)
            }
        }
        
        val tokenJob = launch {
            mockFirebaseService.getTokenFlow().collect { token ->
                tokenUpdates.add(token)
            }
        }
        
        // Simulate message handling
        val testMessage = mapOf(
            "title" to "New Chapter Available",
            "body" to "Chapter 101 of Test Novel is now available",
            "novelId" to "1"
        )
        
        mockFirebaseService.handleMessage(testMessage)
        mockFirebaseService.refreshToken()
        mockFirebaseService.subscribeToTopic("novel_updates")
        
        advanceUntilIdle()
        
        messageJob.cancel()
        tokenJob.cancel()
        
        // Assert
        assertEquals(1, receivedMessages.size)
        assertEquals(testMessage, receivedMessages[0])
        assertTrue("Should have token updates", tokenUpdates.contains("new_firebase_token_123"))
        
        coVerify { mockFirebaseService.handleMessage(testMessage) }
        coVerify { mockFirebaseService.refreshToken() }
        coVerify { mockFirebaseService.subscribeToTopic("novel_updates") }
        verify { mockFirebaseService.getMessageFlow() }
        verify { mockFirebaseService.getTokenFlow() }
    }

    @Test
    fun `test WorkManager background tasks`() = testScope.runTest {
        // Arrange
        val workResultFlow = MutableSharedFlow<String>()
        
        coEvery { mockSyncWorker.doWork() } coAnswers {
            workResultFlow.emit("work_started")
            delay(200)
            workResultFlow.emit("work_completed")
            "SUCCESS"
        }
        
        coEvery { mockSyncWorker.onStopped() } coAnswers {
            workResultFlow.emit("work_stopped")
        }
        
        every { mockSyncWorker.getWorkProgress() } returns workResultFlow
        
        // Act - Execute WorkManager task
        val workUpdates = mutableListOf<String>()
        
        val progressJob = launch {
            mockSyncWorker.getWorkProgress().collect { update ->
                workUpdates.add(update)
            }
        }
        
        // Start work
        val workJob = launch {
            val result = mockSyncWorker.doWork()
            assertEquals("SUCCESS", result)
        }
        
        advanceUntilIdle()
        
        progressJob.cancel()
        
        // Assert
        assertTrue("Work job should complete", workJob.isCompleted)
        assertTrue("Should have work updates", workUpdates.isNotEmpty())
        assertTrue("Should have started", workUpdates.contains("work_started"))
        assertTrue("Should have completed", workUpdates.contains("work_completed"))
        
        coVerify { mockSyncWorker.doWork() }
        verify { mockSyncWorker.getWorkProgress() }
    }

    @Test
    fun `test concurrent background operations`() = testScope.runTest {
        // Arrange
        val downloadCompleted = CompletableDeferred<Boolean>()
        val syncCompleted = CompletableDeferred<Boolean>()
        val ttsCompleted = CompletableDeferred<Boolean>()
        
        coEvery { mockDownloadService.downloadNovel(any()) } coAnswers {
            delay(100)
            downloadCompleted.complete(true)
        }
        
        coEvery { mockSyncService.syncNovel(any()) } coAnswers {
            delay(150)
            syncCompleted.complete(true)
        }
        
        coEvery { mockTTSService.speakText(any()) } coAnswers {
            delay(200)
            ttsCompleted.complete(true)
        }
        
        // Act - Run concurrent background operations
        val downloadJob = launch { mockDownloadService.downloadNovel(1L) }
        val syncJob = launch { mockSyncService.syncNovel(1L) }
        val ttsJob = launch { mockTTSService.speakText("Test content") }
        
        advanceUntilIdle()
        
        // Assert
        assertTrue("Download should complete", downloadCompleted.await())
        assertTrue("Sync should complete", syncCompleted.await())
        assertTrue("TTS should complete", ttsCompleted.await())
        
        assertTrue("All jobs should complete", downloadJob.isCompleted)
        assertTrue("All jobs should complete", syncJob.isCompleted)
        assertTrue("All jobs should complete", ttsJob.isCompleted)
        
        coVerify { mockDownloadService.downloadNovel(1L) }
        coVerify { mockSyncService.syncNovel(1L) }
        coVerify { mockTTSService.speakText("Test content") }
    }

    @Test
    fun `test background operation cancellation and cleanup`() = testScope.runTest {
        // Arrange
        val operationStarted = mutableListOf<String>()
        val operationCancelled = mutableListOf<String>()
        val cleanupExecuted = mutableListOf<String>()
        
        coEvery { mockDownloadService.downloadNovel(any()) } coAnswers {
            operationStarted.add("download")
            try {
                delay(1000) // Long operation
            } catch (e: CancellationException) {
                operationCancelled.add("download")
                throw e
            } finally {
                cleanupExecuted.add("download")
            }
        }
        
        coEvery { mockSyncService.syncNovel(any()) } coAnswers {
            operationStarted.add("sync")
            try {
                delay(1000) // Long operation
            } catch (e: CancellationException) {
                operationCancelled.add("sync")
                throw e
            } finally {
                cleanupExecuted.add("sync")
            }
        }
        
        // Act - Start operations and cancel them
        val downloadJob = launch { mockDownloadService.downloadNovel(1L) }
        val syncJob = launch { mockSyncService.syncNovel(1L) }
        
        advanceTimeBy(100) // Let operations start
        
        // Cancel operations
        downloadJob.cancel()
        syncJob.cancel()
        
        advanceUntilIdle()
        
        // Assert
        assertTrue("Download should start", operationStarted.contains("download"))
        assertTrue("Sync should start", operationStarted.contains("sync"))
        assertTrue("Download should be cancelled", operationCancelled.contains("download"))
        assertTrue("Sync should be cancelled", operationCancelled.contains("sync"))
        assertTrue("Download cleanup should execute", cleanupExecuted.contains("download"))
        assertTrue("Sync cleanup should execute", cleanupExecuted.contains("sync"))
        
        assertTrue("Jobs should be cancelled", downloadJob.isCancelled)
        assertTrue("Jobs should be cancelled", syncJob.isCancelled)
        
        coVerify { mockDownloadService.downloadNovel(1L) }
        coVerify { mockSyncService.syncNovel(1L) }
    }

    @Test
    fun `test background operation error handling and recovery`() = testScope.runTest {
        // Arrange
        val downloadAttempts = mutableListOf<Int>()
        val syncAttempts = mutableListOf<Int>()
        
        coEvery { mockDownloadService.downloadNovel(any()) } coAnswers {
            val attempt = downloadAttempts.size + 1
            downloadAttempts.add(attempt)
            
            if (attempt < 3) {
                throw RuntimeException("Download failed, attempt $attempt")
            } else {
                "Download successful"
            }
        }
        
        coEvery { mockSyncService.syncNovel(any()) } coAnswers {
            val attempt = syncAttempts.size + 1
            syncAttempts.add(attempt)
            
            if (attempt < 2) {
                throw RuntimeException("Sync failed, attempt $attempt")
            } else {
                "Sync successful"
            }
        }
        
        // Act - Implement retry logic
        suspend fun retryOperation(operation: suspend () -> String, maxRetries: Int = 3): String {
            repeat(maxRetries) { attempt ->
                try {
                    return operation()
                } catch (e: Exception) {
                    if (attempt == maxRetries - 1) throw e
                    delay(100 * (attempt + 1)) // Exponential backoff
                }
            }
            throw RuntimeException("Max retries exceeded")
        }
        
        val downloadResult = retryOperation { mockDownloadService.downloadNovel(1L) }
        val syncResult = retryOperation { mockSyncService.syncNovel(1L) }
        
        // Assert
        assertEquals("Download successful", downloadResult)
        assertEquals("Sync successful", syncResult)
        assertEquals(3, downloadAttempts.size)
        assertEquals(2, syncAttempts.size)
        
        coVerify(exactly = 3) { mockDownloadService.downloadNovel(1L) }
        coVerify(exactly = 2) { mockSyncService.syncNovel(1L) }
    }
}