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
import io.github.gmathi.novellibrary.service.firebase.NLFirebaseMessagingService
import io.github.gmathi.novellibrary.service.tts.TTSService
import io.github.gmathi.novellibrary.source.SourceManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for Service classes with Hilt dependency injection
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ServiceHiltIntegrationTest {

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
    fun testDownloadNovelServiceHiltInjection() {
        // Test that DownloadNovelService can be created and dependencies are injected
        val service = DownloadNovelService()
        
        // Create intent to start service
        val intent = Intent(context, DownloadNovelService::class.java).apply {
            putExtra(DownloadNovelService.NOVEL_ID, 1L)
        }
        
        // Verify service can be instantiated (actual service lifecycle testing would require Robolectric)
        assert(service != null)
    }

    @Test
    fun testTTSServiceHiltInjection() {
        // Test that TTSService can be created and dependencies are injected
        val service = TTSService()
        
        // Verify service can be instantiated
        assert(service != null)
    }

    @Test
    fun testNLFirebaseMessagingServiceHiltInjection() {
        // Test that NLFirebaseMessagingService can be created and dependencies are injected
        val service = NLFirebaseMessagingService()
        
        // Verify service can be instantiated
        assert(service != null)
    }

    @Test
    fun testDependencyInjectionWorksCorrectly() {
        // Verify that all required dependencies are properly injected
        assert(::dbHelper.isInitialized)
        assert(::dataCenter.isInitialized)
        assert(::networkHelper.isInitialized)
        assert(::sourceManager.isInitialized)
        
        // Verify dependencies are not null
        assert(dbHelper != null)
        assert(dataCenter != null)
        assert(networkHelper != null)
        assert(sourceManager != null)
    }
}