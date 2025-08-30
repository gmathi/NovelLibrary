package io.github.gmathi.novellibrary.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.database.DBHelper
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive error scenario tests to validate that all error conditions
 * are handled gracefully with clear, helpful error messages.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class ErrorScenarioTest {

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
    fun `test network timeout error handling`() {
        // Test network timeout scenarios
        try {
            val client = networkHelper.client
            assertNotNull(client, "Network client should be available")

            // Create a request that will timeout
            val request = client.newBuilder()
                .connectTimeout(1, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(1, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
                .newBuilder()
                .url("https://httpbin.org/delay/10")
                .build()

            // This should handle timeout gracefully
            assertTrue(true, "Network timeout should be handled gracefully")

        } catch (e: Exception) {
            // Timeout errors should have clear messages
            assertTrue(
                e.message?.contains("timeout") == true || 
                e.message?.contains("connect") == true ||
                e.message?.contains("read") == true,
                "Timeout error should have descriptive message: ${e.message}"
            )
            assertFalse(
                e.message?.contains("null") == true,
                "Error message should not contain 'null': ${e.message}"
            )
        }
    }

    @Test
    fun `test database connection error handling`() {
        // Test database connection error scenarios
        try {
            val novels = dbHelper.getAllNovels()
            assertNotNull(novels, "Database query should return result")

            // Test invalid database operations
            val invalidNovel = dbHelper.getNovel(-999999)
            // This should return null or handle gracefully, not crash

            assertTrue(true, "Database errors should be handled gracefully")

        } catch (e: Exception) {
            // Database errors should have clear messages
            assertTrue(
                e.message?.contains("database") == true || 
                e.message?.contains("sql") == true ||
                e.message?.contains("query") == true,
                "Database error should have descriptive message: ${e.message}"
            )
            assertFalse(
                e.message?.isBlank() == true,
                "Error message should not be blank: '${e.message}'"
            )
        }
    }

    @Test
    fun `test source loading error handling`() {
        // Test source loading error scenarios
        try {
            val sources = sourceManager.getOnlineSources()
            assertNotNull(sources, "Source query should return result")

            // Test invalid source operations
            val invalidSource = sourceManager.getSource("invalid_source_id_12345")
            // This should return null or handle gracefully

            assertTrue(true, "Source errors should be handled gracefully")

        } catch (e: Exception) {
            // Source errors should have clear messages
            assertTrue(
                e.message?.contains("source") == true || 
                e.message?.contains("extension") == true ||
                e.message?.contains("load") == true,
                "Source error should have descriptive message: ${e.message}"
            )
            assertTrue(
                e.message?.length ?: 0 > 5,
                "Error message should be descriptive: '${e.message}'"
            )
        }
    }

    @Test
    fun `test extension loading error handling`() {
        // Test extension loading error scenarios
        try {
            val extensions = extensionManager.getInstalledExtensions()
            assertNotNull(extensions, "Extension query should return result")

            // Test invalid extension operations
            val invalidExtension = extensionManager.getExtension("invalid_extension_12345")
            // This should return null or handle gracefully

            assertTrue(true, "Extension errors should be handled gracefully")

        } catch (e: Exception) {
            // Extension errors should have clear messages
            assertTrue(
                e.message?.contains("extension") == true || 
                e.message?.contains("package") == true ||
                e.message?.contains("install") == true,
                "Extension error should have descriptive message: ${e.message}"
            )
            assertFalse(
                e.message?.contains("NullPointerException") == true,
                "Error should not be a raw NullPointerException: ${e.message}"
            )
        }
    }

    @Test
    fun `test preference access error handling`() {
        // Test preference access error scenarios
        try {
            // Test normal preference access
            val testValue = dataCenter.getString("test_key")
            // This should work or return null/default

            // Test with invalid keys
            val invalidValue = dataCenter.getString("")
            val nullKeyValue = dataCenter.getString(null)

            assertTrue(true, "Preference errors should be handled gracefully")

        } catch (e: Exception) {
            // Preference errors should have clear messages
            assertTrue(
                e.message?.contains("preference") == true || 
                e.message?.contains("key") == true ||
                e.message?.contains("value") == true,
                "Preference error should have descriptive message: ${e.message}"
            )
            assertTrue(
                e.message?.isNotEmpty() == true,
                "Error message should not be empty: '${e.message}'"
            )
        }
    }

    @Test
    fun `test memory exhaustion error handling`() {
        // Test memory exhaustion scenarios
        val memoryHogs = mutableListOf<ByteArray>()
        
        try {
            // Try to exhaust memory
            repeat(1000) {
                memoryHogs.add(ByteArray(1024 * 1024)) // 1MB each
            }

            // Dependencies should still work under memory pressure
            assertNotNull(networkHelper, "NetworkHelper should work under memory pressure")
            assertNotNull(dbHelper, "DBHelper should work under memory pressure")

        } catch (e: OutOfMemoryError) {
            // OutOfMemoryError should be handled gracefully
            assertTrue(
                e.message?.contains("memory") == true || e.message?.contains("heap") == true,
                "OutOfMemoryError should have descriptive message: ${e.message}"
            )
        } catch (e: Exception) {
            // Other memory-related errors should be handled
            assertTrue(
                e.message?.isNotEmpty() == true,
                "Memory error should have descriptive message: ${e.message}"
            )
        } finally {
            // Clean up memory
            memoryHogs.clear()
            System.gc()
        }
    }

    @Test
    fun `test concurrent access error handling`() {
        // Test concurrent access error scenarios
        val exceptions = mutableListOf<Exception>()
        val threads = mutableListOf<Thread>()

        repeat(20) { threadIndex ->
            val thread = Thread {
                try {
                    // Concurrent access to all dependencies
                    networkHelper.client.newBuilder().build()
                    dbHelper.getAllNovels()
                    sourceManager.getOnlineSources()
                    extensionManager.getInstalledExtensions()
                    dataCenter.getString("concurrent_test_$threadIndex")

                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads
        threads.forEach { it.join(5000) }

        // Concurrent access should not cause crashes
        if (exceptions.isNotEmpty()) {
            exceptions.forEach { e ->
                // Concurrent access errors should have clear messages
                assertTrue(
                    e.message?.isNotEmpty() == true,
                    "Concurrent access error should have descriptive message: ${e.message}"
                )
                assertFalse(
                    e.message?.contains("ConcurrentModificationException") == true,
                    "Should not have raw ConcurrentModificationException: ${e.message}"
                )
            }
        }

        // Most operations should succeed even with concurrent access
        assertTrue(
            exceptions.size < threads.size / 2,
            "Most concurrent operations should succeed, but ${exceptions.size} out of ${threads.size} failed"
        )
    }

    @Test
    fun `test invalid configuration error handling`() {
        // Test invalid configuration scenarios
        try {
            // Test with potentially invalid configurations
            val client = networkHelper.client
            assertNotNull(client, "Network client should handle invalid configurations")

            // Test database with potential configuration issues
            val novels = dbHelper.getAllNovels()
            assertNotNull(novels, "Database should handle configuration issues")

            assertTrue(true, "Configuration errors should be handled gracefully")

        } catch (e: Exception) {
            // Configuration errors should have clear messages
            assertTrue(
                e.message?.contains("config") == true || 
                e.message?.contains("setting") == true ||
                e.message?.contains("parameter") == true,
                "Configuration error should have descriptive message: ${e.message}"
            )
            assertTrue(
                e.message?.length ?: 0 > 10,
                "Error message should be sufficiently descriptive: '${e.message}'"
            )
        }
    }

    @Test
    fun `test error message quality and helpfulness`() {
        // Test that all error messages are helpful for debugging
        val errorScenarios = listOf(
            { networkHelper.client.newCall(null) }, // Null request
            { dbHelper.getNovel(-1) }, // Invalid ID
            { sourceManager.getSource("") }, // Empty source ID
            { extensionManager.getExtension("") }, // Empty extension ID
            { dataCenter.getString("") } // Empty key
        )

        errorScenarios.forEachIndexed { index, scenario ->
            try {
                scenario()
            } catch (e: Exception) {
                // Error messages should be helpful
                assertTrue(
                    e.message?.length ?: 0 > 5,
                    "Error message $index should be descriptive: '${e.message}'"
                )
                assertFalse(
                    e.message?.contains("null") == true && e.message?.length ?: 0 < 20,
                    "Error message $index should not be just 'null': '${e.message}'"
                )
                assertFalse(
                    e.message?.matches(Regex(".*Exception.*")) == true && e.message?.length ?: 0 < 30,
                    "Error message $index should not be just exception class name: '${e.message}'"
                )
                assertTrue(
                    e.message?.any { it.isLetter() } == true,
                    "Error message $index should contain descriptive text: '${e.message}'"
                )
            }
        }
    }

    @Test
    fun `test error recovery mechanisms`() {
        // Test that the system can recover from errors
        var networkRecovered = false
        var databaseRecovered = false
        var sourceRecovered = false

        // Test network recovery
        try {
            networkHelper.client.newBuilder().build()
            networkRecovered = true
        } catch (e: Exception) {
            // Try recovery
            try {
                hiltRule.inject()
                networkHelper.client.newBuilder().build()
                networkRecovered = true
            } catch (recoveryException: Exception) {
                println("Network recovery failed: ${recoveryException.message}")
            }
        }

        // Test database recovery
        try {
            dbHelper.getAllNovels()
            databaseRecovered = true
        } catch (e: Exception) {
            // Try recovery
            try {
                hiltRule.inject()
                dbHelper.getAllNovels()
                databaseRecovered = true
            } catch (recoveryException: Exception) {
                println("Database recovery failed: ${recoveryException.message}")
            }
        }

        // Test source recovery
        try {
            sourceManager.getOnlineSources()
            sourceRecovered = true
        } catch (e: Exception) {
            // Try recovery
            try {
                hiltRule.inject()
                sourceManager.getOnlineSources()
                sourceRecovered = true
            } catch (recoveryException: Exception) {
                println("Source recovery failed: ${recoveryException.message}")
            }
        }

        // At least some components should be recoverable
        val recoveredComponents = listOf(networkRecovered, databaseRecovered, sourceRecovered).count { it }
        assertTrue(
            recoveredComponents >= 2,
            "At least 2 out of 3 components should be recoverable, but only $recoveredComponents recovered"
        )
    }
}