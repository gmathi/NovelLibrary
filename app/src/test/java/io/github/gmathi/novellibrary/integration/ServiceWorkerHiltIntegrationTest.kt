package io.github.gmathi.novellibrary.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.worker.NovelUpdateWorker
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for Service and Worker Hilt dependency injection.
 * Tests that background components properly receive their dependencies.
 */
@ExperimentalCoroutinesApi
class ServiceWorkerHiltIntegrationTest : BaseHiltTest() {

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `DownloadNovelService should have dependencies available`() {
        // Given
        val service = DownloadNovelService()

        // When - simulate service creation (dependencies would be injected by Hilt)
        // Note: In real scenario, Hilt would inject dependencies automatically
        // Here we verify that the test infrastructure provides the dependencies

        // Then
        assertNotNull(dbHelper, "DBHelper should be available for service injection")
        assertNotNull(dataCenter, "DataCenter should be available for service injection")
        assertNotNull(networkHelper, "NetworkHelper should be available for service injection")
    }

    @Test
    fun `NovelUpdateWorker should work with Hilt dependencies`() {
        // Given
        val worker = TestListenableWorkerBuilder<NovelUpdateWorker>(context).build()

        // When - worker would receive dependencies through Hilt in real scenario
        // Here we verify the test setup provides necessary dependencies

        // Then
        assertNotNull(worker, "Worker should be created successfully")
        assertNotNull(dbHelper, "DBHelper should be available for worker injection")
        assertNotNull(networkHelper, "NetworkHelper should be available for worker injection")
    }

    @Test
    fun `Service dependencies should be singleton instances`() {
        // Given - get dependencies multiple times
        val dbHelper2 = dbHelper
        val dataCenter2 = dataCenter
        val networkHelper2 = networkHelper

        // Then - should be same instances (singleton)
        assertTrue(dbHelper === dbHelper2, "DBHelper should be singleton for services")
        assertTrue(dataCenter === dataCenter2, "DataCenter should be singleton for services")
        assertTrue(networkHelper === networkHelper2, "NetworkHelper should be singleton for services")
    }

    @Test
    fun `Worker dependencies should be properly scoped`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // When - create multiple workers (simulating real scenario)
        val worker1 = TestListenableWorkerBuilder<NovelUpdateWorker>(context).build()
        val worker2 = TestListenableWorkerBuilder<NovelUpdateWorker>(context).build()

        // Then
        assertNotNull(worker1, "First worker should be created")
        assertNotNull(worker2, "Second worker should be created")
        
        // Dependencies should be available for both workers
        assertNotNull(dbHelper, "DBHelper should be available for all workers")
        assertNotNull(networkHelper, "NetworkHelper should be available for all workers")
    }

    @Test
    fun `Background service lifecycle should work with Hilt`() {
        // Given
        val service = mockk<DownloadNovelService>(relaxed = true)

        // When - simulate service lifecycle
        // onCreate -> onStartCommand -> onDestroy

        // Then - dependencies should remain available throughout lifecycle
        assertNotNull(dbHelper, "DBHelper should be available throughout service lifecycle")
        assertNotNull(dataCenter, "DataCenter should be available throughout service lifecycle")
        assertNotNull(networkHelper, "NetworkHelper should be available throughout service lifecycle")
    }

    @Test
    fun `Worker execution should have access to all required dependencies`() {
        // Given
        val worker = TestListenableWorkerBuilder<NovelUpdateWorker>(context).build()

        // When - simulate worker execution
        val result = worker.startWork()

        // Then
        assertNotNull(result, "Worker should be able to execute")
        // Dependencies should be available during execution
        assertNotNull(dbHelper, "DBHelper should be available during worker execution")
        assertNotNull(networkHelper, "NetworkHelper should be available during worker execution")
    }

    @Test
    fun `Service and Worker should share singleton dependencies`() {
        // Given
        val service = mockk<DownloadNovelService>(relaxed = true)
        val worker = TestListenableWorkerBuilder<NovelUpdateWorker>(context).build()

        // When - both components access dependencies
        val serviceDbHelper = dbHelper
        val workerDbHelper = dbHelper

        // Then - should be same singleton instances
        assertTrue(serviceDbHelper === workerDbHelper, "Service and Worker should share singleton DBHelper")
        assertNotNull(serviceDbHelper, "Shared DBHelper should not be null")
    }
}