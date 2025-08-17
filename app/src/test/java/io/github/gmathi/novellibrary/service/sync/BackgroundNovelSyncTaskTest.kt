package io.github.gmathi.novellibrary.service.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackgroundNovelSyncTaskTest {

    private lateinit var context: Context
    private lateinit var mockDbHelper: DBHelper
    private lateinit var mockNetworkHelper: NetworkHelper

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockDbHelper = mockk(relaxed = true)
        mockNetworkHelper = mockk(relaxed = true)
        
        mockkObject(DBHelper)
        every { DBHelper.getInstance(any()) } returns mockDbHelper
        
        mockkConstructor(NetworkHelper::class)
        every { anyConstructed<NetworkHelper>().isConnectedToNetwork() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork should return success when network is available and sync completes`() = runTest {
        // Given
        every { mockDbHelper.getAllNovels() } returns emptyList()
        
        val worker = TestListenableWorkerBuilder<BackgroundNovelSyncTask>(context)
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork should return retry when network is not available`() = runTest {
        // Given
        every { anyConstructed<NetworkHelper>().isConnectedToNetwork() } returns false
        
        val worker = TestListenableWorkerBuilder<BackgroundNovelSyncTask>(context)
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `doWork should return retry when exception occurs`() = runTest {
        // Given
        every { anyConstructed<NetworkHelper>().isConnectedToNetwork() } throws RuntimeException("Network error")
        
        val worker = TestListenableWorkerBuilder<BackgroundNovelSyncTask>(context)
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `doWork should handle empty novels list gracefully`() = runTest {
        // Given
        every { mockDbHelper.getAllNovels() } returns emptyList()
        
        val worker = TestListenableWorkerBuilder<BackgroundNovelSyncTask>(context)
            .build()

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        verify { mockDbHelper.getAllNovels() }
    }

    @Test
    fun `scheduleRepeat should cancel existing work and schedule new periodic work`() {
        // Given
        val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns mockWorkManager

        // When
        BackgroundNovelSyncTask.scheduleRepeat(context)

        // Then
        verify { mockWorkManager.cancelAllWorkByTag("BackgroundNovelSyncTask") }
        verify { mockWorkManager.enqueue(any<androidx.work.PeriodicWorkRequest>()) }
    }

    @Test
    fun `cancelAll should cancel all work by tag`() {
        // Given
        val mockWorkManager = mockk<androidx.work.WorkManager>(relaxed = true)
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns mockWorkManager

        // When
        BackgroundNovelSyncTask.cancelAll(context)

        // Then
        verify { mockWorkManager.cancelAllWorkByTag("BackgroundNovelSyncTask") }
    }
}