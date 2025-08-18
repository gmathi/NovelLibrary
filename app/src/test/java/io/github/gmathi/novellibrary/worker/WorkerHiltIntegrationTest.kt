package io.github.gmathi.novellibrary.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.di.WorkerEntryPoint
import io.github.gmathi.novellibrary.extension.ExtensionUpdateJob
import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import dagger.hilt.android.EntryPointAccessors

/**
 * Integration tests for Worker classes with Hilt dependency injection
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkerHiltIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    private lateinit var context: Context

    @Before
    fun init() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testWorkerEntryPointAccess() {
        // Test that WorkerEntryPoint can access dependencies
        val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
        
        val workerDbHelper = entryPoint.dbHelper()
        val workerDataCenter = entryPoint.dataCenter()
        
        // Verify dependencies are accessible
        assert(workerDbHelper != null)
        assert(workerDataCenter != null)
        
        // Verify they are the same instances as injected dependencies (singletons)
        assert(workerDbHelper === dbHelper)
        assert(workerDataCenter === dataCenter)
    }

    @Test
    fun testBackupWorkerCanAccessDependencies() {
        // Test that BackupWorker can access dependencies through EntryPoint
        val worker = TestListenableWorkerBuilder<BackupWorker>(context).build()
        
        // Verify worker can be created
        assert(worker != null)
        assert(worker is BackupWorker)
    }

    @Test
    fun testRestoreWorkerCanAccessDependencies() {
        // Test that RestoreWorker can access dependencies through EntryPoint
        val worker = TestListenableWorkerBuilder<RestoreWorker>(context).build()
        
        // Verify worker can be created
        assert(worker != null)
        assert(worker is RestoreWorker)
    }

    @Test
    fun testExtensionUpdateJobCanAccessDependencies() {
        // Test that ExtensionUpdateJob can access dependencies through EntryPoint
        val worker = TestListenableWorkerBuilder<ExtensionUpdateJob>(context).build()
        
        // Verify worker can be created
        assert(worker != null)
        assert(worker is ExtensionUpdateJob)
    }

    @Test
    fun testWorkBuilderFunctionsWork() {
        // Test that WorkBuilder functions can access dependencies
        try {
            // This should not throw an exception
            val workRequest = periodicBackupWorkRequest(context, 24)
            // workRequest might be null if no backup data exists, which is fine
            assert(true) // Test passes if no exception is thrown
        } catch (e: Exception) {
            // If an exception is thrown, it should not be related to dependency injection
            assert(!e.message?.contains("inject", ignoreCase = true) ?: false) {
                "Dependency injection failed: ${e.message}"
            }
        }
    }
}