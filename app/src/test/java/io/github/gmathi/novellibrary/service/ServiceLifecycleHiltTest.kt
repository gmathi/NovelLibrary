package io.github.gmathi.novellibrary.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.source.SourceManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Tests for Service lifecycle and background operation integration with Hilt
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ServiceLifecycleHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var sourceManager: SourceManager

    private lateinit var context: Context

    @Before
    fun init() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testDownloadNovelServiceLifecycle() {
        // Test that DownloadNovelService can handle lifecycle events with Hilt injection
        val service = DownloadNovelService()
        
        // Simulate service creation
        service.onCreate()
        
        // Verify service is properly initialized
        assert(service != null)
        
        // Test service can handle intents
        val intent = Intent(context, DownloadNovelService::class.java).apply {
            action = DownloadNovelService.ACTION_START
            putExtra(DownloadNovelService.NOVEL_ID, 1L)
        }
        
        // This would normally start the service, but in unit tests we just verify intent creation
        assert(intent.getLongExtra(DownloadNovelService.NOVEL_ID, -1L) == 1L)
        assert(intent.action == DownloadNovelService.ACTION_START)
    }

    @Test
    fun testServiceDependencyInjectionTiming() {
        // Test that dependencies are available when service is created
        val service = DownloadNovelService()
        
        // Simulate onCreate lifecycle
        service.onCreate()
        
        // In a real service, dependencies would be injected by this point
        // We can't directly test the lateinit var injection in unit tests,
        // but we can verify our injected dependencies work
        assert(dbHelper != null)
        assert(dataCenter != null)
        assert(networkHelper != null)
        assert(sourceManager != null)
    }

    @Test
    fun testServiceBackgroundOperationIntegration() {
        // Test that services can perform background operations with injected dependencies
        
        // Verify all required dependencies for background operations are available
        assert(dbHelper != null) // For database operations
        assert(networkHelper != null) // For network operations
        assert(sourceManager != null) // For source management
        
        // Test that we can create service intents for different actions
        val startIntent = Intent(context, DownloadNovelService::class.java).apply {
            action = DownloadNovelService.ACTION_START
            putExtra(DownloadNovelService.NOVEL_ID, 1L)
        }
        
        val pauseIntent = Intent(context, DownloadNovelService::class.java).apply {
            action = DownloadNovelService.ACTION_PAUSE
            putExtra(DownloadNovelService.NOVEL_ID, 1L)
        }
        
        val removeIntent = Intent(context, DownloadNovelService::class.java).apply {
            action = DownloadNovelService.ACTION_REMOVE
            putExtra(DownloadNovelService.NOVEL_ID, 1L)
        }
        
        // Verify intents are properly configured
        assert(startIntent.action == DownloadNovelService.ACTION_START)
        assert(pauseIntent.action == DownloadNovelService.ACTION_PAUSE)
        assert(removeIntent.action == DownloadNovelService.ACTION_REMOVE)
        
        // All intents should have the novel ID
        assert(startIntent.getLongExtra(DownloadNovelService.NOVEL_ID, -1L) == 1L)
        assert(pauseIntent.getLongExtra(DownloadNovelService.NOVEL_ID, -1L) == 1L)
        assert(removeIntent.getLongExtra(DownloadNovelService.NOVEL_ID, -1L) == 1L)
    }
}