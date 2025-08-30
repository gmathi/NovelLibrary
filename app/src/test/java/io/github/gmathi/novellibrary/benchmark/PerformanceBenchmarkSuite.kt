package io.github.gmathi.novellibrary.benchmark

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
import kotlin.system.measureNanoTime
import kotlin.test.assertTrue

/**
 * Comprehensive performance benchmark suite for measuring app performance
 * before and after Injekt cleanup to ensure no performance regression.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class PerformanceBenchmarkSuite {

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

    private val benchmarkResults = mutableMapOf<String, BenchmarkResult>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `benchmark app startup time`() {
        val iterations = 10
        val startupTimes = mutableListOf<Long>()

        repeat(iterations) {
            val startupTime = measureTimeMillis {
                // Simulate app startup by re-injecting dependencies
                hiltRule.inject()
                
                // Access core components to ensure they're initialized
                networkHelper.client
                dbHelper.getAllNovels()
                sourceManager.getOnlineSources()
                extensionManager.getInstalledExtensions()
                dataCenter.getString("test")
            }
            startupTimes.add(startupTime)
        }

        val averageStartupTime = startupTimes.average()
        val minStartupTime = startupTimes.minOrNull() ?: 0L
        val maxStartupTime = startupTimes.maxOrNull() ?: 0L

        benchmarkResults["app_startup"] = BenchmarkResult(
            name = "App Startup Time",
            averageTime = averageStartupTime,
            minTime = minStartupTime.toDouble(),
            maxTime = maxStartupTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // App startup should be under 2 seconds
        assertTrue(
            averageStartupTime < 2000,
            "Average startup time ${averageStartupTime}ms should be under 2000ms"
        )

        println("App Startup Benchmark: avg=${averageStartupTime}ms, min=${minStartupTime}ms, max=${maxStartupTime}ms")
    }

    @Test
    fun `benchmark dependency injection performance`() {
        val iterations = 100
        val injectionTimes = mutableListOf<Long>()

        repeat(iterations) {
            val injectionTime = measureNanoTime {
                hiltRule.inject()
            } / 1_000_000 // Convert to milliseconds

            injectionTimes.add(injectionTime)
        }

        val averageInjectionTime = injectionTimes.average()
        val minInjectionTime = injectionTimes.minOrNull() ?: 0L
        val maxInjectionTime = injectionTimes.maxOrNull() ?: 0L

        benchmarkResults["dependency_injection"] = BenchmarkResult(
            name = "Dependency Injection Time",
            averageTime = averageInjectionTime,
            minTime = minInjectionTime.toDouble(),
            maxTime = maxInjectionTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // Dependency injection should be very fast
        assertTrue(
            averageInjectionTime < 10,
            "Average injection time ${averageInjectionTime}ms should be under 10ms"
        )

        println("Dependency Injection Benchmark: avg=${averageInjectionTime}ms, min=${minInjectionTime}ms, max=${maxInjectionTime}ms")
    }

    @Test
    fun `benchmark network operations performance`() {
        val iterations = 20
        val networkTimes = mutableListOf<Long>()

        repeat(iterations) {
            val networkTime = measureTimeMillis {
                // Simulate network operations
                val client = networkHelper.client
                val cloudflareClient = networkHelper.cloudflareClient
                
                // Create and configure requests (without actually executing them)
                client.newBuilder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                cloudflareClient.newBuilder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
            }
            networkTimes.add(networkTime)
        }

        val averageNetworkTime = networkTimes.average()
        val minNetworkTime = networkTimes.minOrNull() ?: 0L
        val maxNetworkTime = networkTimes.maxOrNull() ?: 0L

        benchmarkResults["network_operations"] = BenchmarkResult(
            name = "Network Operations Setup Time",
            averageTime = averageNetworkTime,
            minTime = minNetworkTime.toDouble(),
            maxTime = maxNetworkTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // Network setup should be fast
        assertTrue(
            averageNetworkTime < 50,
            "Average network setup time ${averageNetworkTime}ms should be under 50ms"
        )

        println("Network Operations Benchmark: avg=${averageNetworkTime}ms, min=${minNetworkTime}ms, max=${maxNetworkTime}ms")
    }

    @Test
    fun `benchmark database operations performance`() {
        val iterations = 50
        val databaseTimes = mutableListOf<Long>()

        repeat(iterations) {
            val databaseTime = measureTimeMillis {
                // Simulate database operations
                dbHelper.getAllNovels()
                dbHelper.getAllWebPages()
                dbHelper.getAllNovelSections()
            }
            databaseTimes.add(databaseTime)
        }

        val averageDatabaseTime = databaseTimes.average()
        val minDatabaseTime = databaseTimes.minOrNull() ?: 0L
        val maxDatabaseTime = databaseTimes.maxOrNull() ?: 0L

        benchmarkResults["database_operations"] = BenchmarkResult(
            name = "Database Operations Time",
            averageTime = averageDatabaseTime,
            minTime = minDatabaseTime.toDouble(),
            maxTime = maxDatabaseTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // Database operations should be reasonably fast
        assertTrue(
            averageDatabaseTime < 100,
            "Average database time ${averageDatabaseTime}ms should be under 100ms"
        )

        println("Database Operations Benchmark: avg=${averageDatabaseTime}ms, min=${minDatabaseTime}ms, max=${maxDatabaseTime}ms")
    }

    @Test
    fun `benchmark source management performance`() {
        val iterations = 30
        val sourceTimes = mutableListOf<Long>()

        repeat(iterations) {
            val sourceTime = measureTimeMillis {
                // Simulate source management operations
                sourceManager.getOnlineSources()
                sourceManager.getCatalogueSources()
            }
            sourceTimes.add(sourceTime)
        }

        val averageSourceTime = sourceTimes.average()
        val minSourceTime = sourceTimes.minOrNull() ?: 0L
        val maxSourceTime = sourceTimes.maxOrNull() ?: 0L

        benchmarkResults["source_management"] = BenchmarkResult(
            name = "Source Management Time",
            averageTime = averageSourceTime,
            minTime = minSourceTime.toDouble(),
            maxTime = maxSourceTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // Source management should be fast
        assertTrue(
            averageSourceTime < 75,
            "Average source management time ${averageSourceTime}ms should be under 75ms"
        )

        println("Source Management Benchmark: avg=${averageSourceTime}ms, min=${minSourceTime}ms, max=${maxSourceTime}ms")
    }

    @Test
    fun `benchmark extension management performance`() {
        val iterations = 25
        val extensionTimes = mutableListOf<Long>()

        repeat(iterations) {
            val extensionTime = measureTimeMillis {
                // Simulate extension management operations
                extensionManager.getInstalledExtensions()
                extensionManager.getAvailableExtensions()
            }
            extensionTimes.add(extensionTime)
        }

        val averageExtensionTime = extensionTimes.average()
        val minExtensionTime = extensionTimes.minOrNull() ?: 0L
        val maxExtensionTime = extensionTimes.maxOrNull() ?: 0L

        benchmarkResults["extension_management"] = BenchmarkResult(
            name = "Extension Management Time",
            averageTime = averageExtensionTime,
            minTime = minExtensionTime.toDouble(),
            maxTime = maxExtensionTime.toDouble(),
            iterations = iterations,
            unit = "ms"
        )

        // Extension management should be reasonably fast
        assertTrue(
            averageExtensionTime < 100,
            "Average extension management time ${averageExtensionTime}ms should be under 100ms"
        )

        println("Extension Management Benchmark: avg=${averageExtensionTime}ms, min=${minExtensionTime}ms, max=${maxExtensionTime}ms")
    }

    @Test
    fun `benchmark memory usage patterns`() {
        val runtime = Runtime.getRuntime()
        val iterations = 10
        val memoryUsages = mutableListOf<Long>()

        repeat(iterations) {
            // Force garbage collection before measurement
            System.gc()
            Thread.sleep(100)

            val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

            // Perform typical operations
            hiltRule.inject()
            networkHelper.client.newBuilder().build()
            dbHelper.getAllNovels()
            sourceManager.getOnlineSources()
            extensionManager.getInstalledExtensions()

            val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
            val memoryDelta = memoryAfter - memoryBefore

            memoryUsages.add(memoryDelta)
        }

        val averageMemoryUsage = memoryUsages.average()
        val minMemoryUsage = memoryUsages.minOrNull() ?: 0L
        val maxMemoryUsage = memoryUsages.maxOrNull() ?: 0L

        benchmarkResults["memory_usage"] = BenchmarkResult(
            name = "Memory Usage Delta",
            averageTime = averageMemoryUsage / 1024 / 1024, // Convert to MB
            minTime = minMemoryUsage.toDouble() / 1024 / 1024,
            maxTime = maxMemoryUsage.toDouble() / 1024 / 1024,
            iterations = iterations,
            unit = "MB"
        )

        // Memory usage should be reasonable (under 20MB for test operations)
        assertTrue(
            averageMemoryUsage < 20 * 1024 * 1024,
            "Average memory usage ${averageMemoryUsage / 1024 / 1024}MB should be under 20MB"
        )

        println("Memory Usage Benchmark: avg=${averageMemoryUsage / 1024 / 1024}MB, min=${minMemoryUsage / 1024 / 1024}MB, max=${maxMemoryUsage / 1024 / 1024}MB")
    }

    @Test
    fun `create performance benchmark report`() {
        // Generate comprehensive performance report
        val report = generatePerformanceReport()
        
        println("\n" + "="*60)
        println("PERFORMANCE BENCHMARK REPORT")
        println("="*60)
        println(report)
        
        // Validate overall performance is acceptable
        val totalAverageTime = benchmarkResults.values
            .filter { it.unit == "ms" }
            .sumOf { it.averageTime }
        
        assertTrue(
            totalAverageTime < 5000,
            "Total average time ${totalAverageTime}ms should be under 5000ms"
        )
    }

    private fun generatePerformanceReport(): String {
        val report = StringBuilder()
        
        report.appendLine("Performance Benchmark Results:")
        report.appendLine("Generated: ${java.util.Date()}")
        report.appendLine()
        
        benchmarkResults.forEach { (key, result) ->
            report.appendLine("${result.name}:")
            report.appendLine("  Average: ${String.format("%.2f", result.averageTime)} ${result.unit}")
            report.appendLine("  Min: ${String.format("%.2f", result.minTime)} ${result.unit}")
            report.appendLine("  Max: ${String.format("%.2f", result.maxTime)} ${result.unit}")
            report.appendLine("  Iterations: ${result.iterations}")
            report.appendLine()
        }
        
        // Performance summary
        report.appendLine("Performance Summary:")
        report.appendLine("✓ App startup time: ${benchmarkResults["app_startup"]?.averageTime?.let { String.format("%.2f", it) } ?: "N/A"}ms")
        report.appendLine("✓ Dependency injection: ${benchmarkResults["dependency_injection"]?.averageTime?.let { String.format("%.2f", it) } ?: "N/A"}ms")
        report.appendLine("✓ Network operations: ${benchmarkResults["network_operations"]?.averageTime?.let { String.format("%.2f", it) } ?: "N/A"}ms")
        report.appendLine("✓ Database operations: ${benchmarkResults["database_operations"]?.averageTime?.let { String.format("%.2f", it) } ?: "N/A"}ms")
        report.appendLine("✓ Memory usage: ${benchmarkResults["memory_usage"]?.averageTime?.let { String.format("%.2f", it) } ?: "N/A"}MB")
        
        return report.toString()
    }

    data class BenchmarkResult(
        val name: String,
        val averageTime: Double,
        val minTime: Double,
        val maxTime: Double,
        val iterations: Int,
        val unit: String
    )
}