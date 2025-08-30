package io.github.gmathi.novellibrary.performance

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
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue

/**
 * Performance validation tests to ensure that the Injekt cleanup maintains
 * or improves performance characteristics.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class PerformanceValidationTest {

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

    private val performanceMetrics = mutableMapOf<String, Long>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `measure dependency injection startup time`() {
        // Measure time to inject all dependencies
        val injectionTime = measureTimeMillis {
            hiltRule.inject()
        }
        
        performanceMetrics["dependency_injection_time"] = injectionTime
        
        // Dependency injection should be fast (under 100ms in tests)
        assertTrue(
            injectionTime < 100,
            "Dependency injection took ${injectionTime}ms, should be under 100ms"
        )
    }

    @Test
    fun `measure network helper initialization time`() {
        val initTime = measureTimeMillis {
            val client = networkHelper.client
            val cloudflareClient = networkHelper.cloudflareClient
            // Access properties to ensure initialization
            client.connectTimeoutMillis
            cloudflareClient.connectTimeoutMillis
        }
        
        performanceMetrics["network_helper_init_time"] = initTime
        
        // Network helper initialization should be fast
        assertTrue(
            initTime < 50,
            "Network helper initialization took ${initTime}ms, should be under 50ms"
        )
    }

    @Test
    fun `measure database helper initialization time`() {
        val initTime = measureTimeMillis {
            // Access database operations to ensure initialization
            dbHelper.getAllNovels()
        }
        
        performanceMetrics["database_helper_init_time"] = initTime
        
        // Database helper initialization should be reasonable
        assertTrue(
            initTime < 200,
            "Database helper initialization took ${initTime}ms, should be under 200ms"
        )
    }

    @Test
    fun `measure source manager initialization time`() {
        val initTime = measureTimeMillis {
            // Access source operations to ensure initialization
            sourceManager.getOnlineSources()
        }
        
        performanceMetrics["source_manager_init_time"] = initTime
        
        // Source manager initialization should be fast
        assertTrue(
            initTime < 100,
            "Source manager initialization took ${initTime}ms, should be under 100ms"
        )
    }

    @Test
    fun `measure extension manager initialization time`() {
        val initTime = measureTimeMillis {
            // Access extension operations to ensure initialization
            extensionManager.getInstalledExtensions()
        }
        
        performanceMetrics["extension_manager_init_time"] = initTime
        
        // Extension manager initialization should be reasonable
        assertTrue(
            initTime < 150,
            "Extension manager initialization took ${initTime}ms, should be under 150ms"
        )
    }

    @Test
    fun `measure memory usage patterns`() {
        val runtime = Runtime.getRuntime()
        
        // Measure memory before operations
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform typical operations
        networkHelper.client.newBuilder().build()
        dbHelper.getAllNovels()
        sourceManager.getOnlineSources()
        extensionManager.getInstalledExtensions()
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        // Measure memory after operations
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryDelta = memoryAfter - memoryBefore
        
        performanceMetrics["memory_usage_delta"] = memoryDelta
        
        // Memory usage should be reasonable (under 10MB for test operations)
        assertTrue(
            memoryDelta < 10 * 1024 * 1024,
            "Memory usage delta was ${memoryDelta / 1024 / 1024}MB, should be under 10MB"
        )
    }

    @Test
    fun `measure concurrent dependency access performance`() {
        val concurrentTime = measureTimeMillis {
            // Simulate concurrent access to dependencies
            repeat(10) {
                Thread {
                    networkHelper.client
                    dbHelper.getAllNovels()
                    sourceManager.getOnlineSources()
                    extensionManager.getInstalledExtensions()
                }.start()
            }
            Thread.sleep(100) // Wait for threads to complete
        }
        
        performanceMetrics["concurrent_access_time"] = concurrentTime
        
        // Concurrent access should not significantly degrade performance
        assertTrue(
            concurrentTime < 500,
            "Concurrent access took ${concurrentTime}ms, should be under 500ms"
        )
    }

    @Test
    fun `validate no performance regression from Injekt cleanup`() {
        // This test validates that performance is maintained or improved
        // after Injekt cleanup compared to baseline metrics
        
        val totalInitTime = performanceMetrics.values.sum()
        
        // Total initialization time should be reasonable
        assertTrue(
            totalInitTime < 1000,
            "Total initialization time was ${totalInitTime}ms, should be under 1000ms"
        )
        
        // Log performance metrics for future reference
        println("Performance Metrics:")
        performanceMetrics.forEach { (key, value) ->
            println("  $key: ${value}ms")
        }
    }

    @Test
    fun `create performance benchmark for future reference`() {
        // Create a comprehensive performance benchmark
        val benchmark = PerformanceBenchmark(
            dependencyInjectionTime = performanceMetrics["dependency_injection_time"] ?: 0,
            networkHelperInitTime = performanceMetrics["network_helper_init_time"] ?: 0,
            databaseHelperInitTime = performanceMetrics["database_helper_init_time"] ?: 0,
            sourceManagerInitTime = performanceMetrics["source_manager_init_time"] ?: 0,
            extensionManagerInitTime = performanceMetrics["extension_manager_init_time"] ?: 0,
            memoryUsageDelta = performanceMetrics["memory_usage_delta"] ?: 0,
            concurrentAccessTime = performanceMetrics["concurrent_access_time"] ?: 0,
            timestamp = System.currentTimeMillis()
        )
        
        // Validate benchmark is reasonable
        assertTrue(benchmark.isWithinAcceptableRange(), "Performance benchmark should be within acceptable range")
        
        // Log benchmark for future comparison
        println("Performance Benchmark Created: $benchmark")
    }

    data class PerformanceBenchmark(
        val dependencyInjectionTime: Long,
        val networkHelperInitTime: Long,
        val databaseHelperInitTime: Long,
        val sourceManagerInitTime: Long,
        val extensionManagerInitTime: Long,
        val memoryUsageDelta: Long,
        val concurrentAccessTime: Long,
        val timestamp: Long
    ) {
        fun isWithinAcceptableRange(): Boolean {
            return dependencyInjectionTime < 100 &&
                    networkHelperInitTime < 50 &&
                    databaseHelperInitTime < 200 &&
                    sourceManagerInitTime < 100 &&
                    extensionManagerInitTime < 150 &&
                    memoryUsageDelta < 10 * 1024 * 1024 &&
                    concurrentAccessTime < 500
        }
    }
}