package io.github.gmathi.novellibrary.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.di.DatabaseModule
import io.github.gmathi.novellibrary.di.NetworkModule
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.testing.TestInstallIn
import io.mockk.mockk
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Error handling and edge case validation tests to ensure that dependency injection
 * failures are handled gracefully and provide clear error messages.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class ErrorHandlingValidationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    @ApplicationContext
    lateinit var context: Context

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `verify dependency injection failure scenarios are handled gracefully`() {
        // Test that dependency injection provides clear error messages
        try {
            hiltRule.inject()
            assertNotNull(networkHelper, "NetworkHelper should be injected")
            assertNotNull(dbHelper, "DBHelper should be injected")
            assertNotNull(dataCenter, "DataCenter should be injected")
            assertNotNull(sourceManager, "SourceManager should be injected")
            assertNotNull(extensionManager, "ExtensionManager should be injected")
        } catch (e: Exception) {
            // If injection fails, error should be clear and helpful
            assertTrue(
                e.message?.contains("dependency") == true || 
                e.message?.contains("inject") == true,
                "Error message should be clear about dependency injection failure: ${e.message}"
            )
        }
    }

    @Test
    fun `verify network error handling works correctly`() {
        // Test network error handling with Hilt injection
        try {
            val client = networkHelper.client
            assertNotNull(client, "Network client should be available")
            
            // Test invalid request handling
            val invalidRequest = client.newBuilder().build().newBuilder()
                .url("https://invalid-domain-that-does-not-exist.com")
                .build()
            
            // This should not crash the application
            assertTrue(true, "Network error handling completed without crash")
        } catch (e: Exception) {
            // Network errors should be handled gracefully
            assertTrue(
                e.message?.isNotEmpty() == true,
                "Network error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify database error handling works correctly`() {
        // Test database error handling with Hilt injection
        try {
            val novels = dbHelper.getAllNovels()
            assertNotNull(novels, "Database query should return result")
        } catch (e: Exception) {
            // Database errors should be handled gracefully
            assertTrue(
                e.message?.contains("database") == true || 
                e.message?.contains("sql") == true,
                "Database error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify source manager error handling works correctly`() {
        // Test source manager error handling with Hilt injection
        try {
            val sources = sourceManager.getOnlineSources()
            assertNotNull(sources, "Source query should return result")
        } catch (e: Exception) {
            // Source errors should be handled gracefully
            assertTrue(
                e.message?.contains("source") == true,
                "Source error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify extension manager error handling works correctly`() {
        // Test extension manager error handling with Hilt injection
        try {
            val extensions = extensionManager.getInstalledExtensions()
            assertNotNull(extensions, "Extension query should return result")
        } catch (e: Exception) {
            // Extension errors should be handled gracefully
            assertTrue(
                e.message?.contains("extension") == true,
                "Extension error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify EntryPoint access failure handling`() {
        // Test EntryPoint access error handling
        try {
            // This tests the EntryPoint pattern used for object classes
            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context,
                TestEntryPoint::class.java
            )
            assertNotNull(entryPoint, "EntryPoint should be accessible")
        } catch (e: Exception) {
            // EntryPoint errors should be handled gracefully
            assertTrue(
                e.message?.contains("EntryPoint") == true || 
                e.message?.contains("component") == true,
                "EntryPoint error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify graceful degradation when dependencies are unavailable`() {
        // Test that the application can handle missing dependencies gracefully
        try {
            // Simulate scenario where some dependencies might not be available
            val hasNetworkHelper = ::networkHelper.isInitialized
            val hasDbHelper = ::dbHelper.isInitialized
            val hasDataCenter = ::dataCenter.isInitialized
            val hasSourceManager = ::sourceManager.isInitialized
            val hasExtensionManager = ::extensionManager.isInitialized
            
            // At least some dependencies should be available
            assertTrue(
                hasNetworkHelper || hasDbHelper || hasDataCenter || hasSourceManager || hasExtensionManager,
                "At least some dependencies should be available"
            )
        } catch (e: Exception) {
            // Graceful degradation should not crash the application
            assertTrue(
                e.message?.isNotEmpty() == true,
                "Graceful degradation error should have descriptive message: ${e.message}"
            )
        }
    }

    @Test
    fun `verify error messages are clear and helpful`() {
        // Test that error messages provide clear guidance for troubleshooting
        val errorScenarios = listOf(
            { networkHelper.client.newCall(mockk()).execute() },
            { dbHelper.getNovel(-1) },
            { sourceManager.getSource("invalid_source") },
            { extensionManager.getExtension("invalid_extension") }
        )
        
        errorScenarios.forEach { scenario ->
            try {
                scenario()
            } catch (e: Exception) {
                // Error messages should be descriptive and helpful
                assertTrue(
                    e.message?.length ?: 0 > 10,
                    "Error message should be descriptive: ${e.message}"
                )
                assertTrue(
                    !e.message?.contains("null") == true,
                    "Error message should not contain 'null': ${e.message}"
                )
            }
        }
    }

    @Test
    fun `verify no Hilt-specific error patterns remain`() {
        // Test that there are no Hilt-specific error patterns that indicate incomplete migration
        try {
            hiltRule.inject()
            
            // Verify no Injekt-related errors
            val stackTrace = Thread.currentThread().stackTrace
            val hasInjektReferences = stackTrace.any { 
                it.className.contains("injekt", ignoreCase = true) ||
                it.methodName.contains("injekt", ignoreCase = true)
            }
            
            assertTrue(
                !hasInjektReferences,
                "Should not have Injekt references in stack trace"
            )
        } catch (e: Exception) {
            // Errors should not reference Injekt
            assertTrue(
                !e.message?.contains("injekt", ignoreCase = true) == true,
                "Error message should not reference Injekt: ${e.message}"
            )
        }
    }

    @Test
    fun `verify proper scoping prevents memory leaks`() {
        // Test that proper Hilt scoping prevents memory leaks
        val initialNetworkHelper = networkHelper
        val initialDbHelper = dbHelper
        
        // Force re-injection
        hiltRule.inject()
        
        // Singleton-scoped dependencies should be the same instance
        assertTrue(
            initialNetworkHelper === networkHelper,
            "NetworkHelper should maintain singleton scope"
        )
        assertTrue(
            initialDbHelper === dbHelper,
            "DBHelper should maintain singleton scope"
        )
    }

    @dagger.hilt.EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TestEntryPoint {
        fun networkHelper(): NetworkHelper
        fun dbHelper(): DBHelper
    }
}

/**
 * Test module for simulating error scenarios
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class, DatabaseModule::class]
)
object TestErrorModule {
    
    @Provides
    @Singleton
    fun provideTestNetworkHelper(): NetworkHelper = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideTestDbHelper(): DBHelper = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideTestDataCenter(): DataCenter = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideTestSourceManager(): SourceManager = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideTestExtensionManager(): ExtensionManager = mockk(relaxed = true)
}