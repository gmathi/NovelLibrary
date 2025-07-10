package io.github.gmathi.novellibrary.util

import android.content.Context
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility to track app performance metrics
 */
class PerformanceMonitor private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        
        @Volatile
        private var instance: PerformanceMonitor? = null
        
        fun getInstance(context: Context): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Performance metrics storage
    private val metrics = ConcurrentHashMap<String, MetricData>()
    private val operationCounters = ConcurrentHashMap<String, AtomicLong>()
    
    // Memory tracking
    private val memoryTracker = MemoryTracker()
    
    data class MetricData(
        val operation: String,
        var totalTime: Long = 0,
        var count: Long = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastExecutionTime: Long = 0
    ) {
        val averageTime: Long get() = if (count > 0) totalTime / count else 0
    }
    
    /**
     * Track operation execution time
     */
    inline fun <T> trackOperation(operation: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            val endTime = SystemClock.elapsedRealtime()
            recordMetric(operation, endTime - startTime)
        }
    }
    
    /**
     * Track suspend operation execution time
     */
    suspend inline fun <T> trackSuspendOperation(operation: String, block: () -> T): T {
        val startTime = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            val endTime = SystemClock.elapsedRealtime()
            recordMetric(operation, endTime - startTime)
        }
    }
    
    /**
     * Record a performance metric
     */
    private fun recordMetric(operation: String, executionTime: Long) {
        val metric = metrics.getOrPut(operation) { MetricData(operation) }
        
        metric.apply {
            totalTime += executionTime
            count++
            minTime = minOf(minTime, executionTime)
            maxTime = maxOf(maxTime, executionTime)
            lastExecutionTime = executionTime
        }
        
        // Log slow operations
        if (executionTime > SLOW_OPERATION_THRESHOLD) {
            Log.w(TAG, "Slow operation detected: $operation took ${executionTime}ms")
        }
    }
    
    /**
     * Increment operation counter
     */
    fun incrementCounter(operation: String) {
        operationCounters.getOrPut(operation) { AtomicLong(0) }.incrementAndGet()
    }
    
    /**
     * Get performance report
     */
    fun getPerformanceReport(): String {
        val report = StringBuilder()
        report.appendLine("=== Performance Report ===")
        
        metrics.values.sortedByDescending { it.averageTime }.forEach { metric ->
            report.appendLine("${metric.operation}:")
            report.appendLine("  Count: ${metric.count}")
            report.appendLine("  Average: ${metric.averageTime}ms")
            report.appendLine("  Min: ${metric.minTime}ms")
            report.appendLine("  Max: ${metric.maxTime}ms")
            report.appendLine("  Last: ${metric.lastExecutionTime}ms")
            report.appendLine()
        }
        
        report.appendLine("=== Operation Counters ===")
        operationCounters.forEach { (operation, counter) ->
            report.appendLine("$operation: ${counter.get()}")
        }
        
        report.appendLine("=== Memory Usage ===")
        report.appendLine(memoryTracker.getMemoryInfo())
        
        return report.toString()
    }
    
    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
        operationCounters.clear()
    }
    
    /**
     * Log performance report
     */
    fun logPerformanceReport() {
        Log.i(TAG, getPerformanceReport())
    }
    
    /**
     * Start memory tracking
     */
    fun startMemoryTracking() {
        memoryTracker.startTracking()
    }
    
    /**
     * Stop memory tracking
     */
    fun stopMemoryTracking() {
        memoryTracker.stopTracking()
    }
    
    companion object {
        private const val SLOW_OPERATION_THRESHOLD = 1000L // 1 second
    }
    
    /**
     * Memory tracking utility
     */
    private inner class MemoryTracker {
        private var isTracking = false
        private val memorySnapshots = mutableListOf<MemorySnapshot>()
        
        data class MemorySnapshot(
            val timestamp: Long,
            val usedMemory: Long,
            val maxMemory: Long,
            val freeMemory: Long
        )
        
        fun startTracking() {
            isTracking = true
            CoroutineScope(Dispatchers.Default).launch {
                while (isTracking) {
                    takeMemorySnapshot()
                    kotlinx.coroutines.delay(5000) // Every 5 seconds
                }
            }
        }
        
        fun stopTracking() {
            isTracking = false
        }
        
        private fun takeMemorySnapshot() {
            val runtime = Runtime.getRuntime()
            val snapshot = MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                usedMemory = runtime.totalMemory() - runtime.freeMemory(),
                maxMemory = runtime.maxMemory(),
                freeMemory = runtime.freeMemory()
            )
            memorySnapshots.add(snapshot)
            
            // Keep only last 100 snapshots
            if (memorySnapshots.size > 100) {
                memorySnapshots.removeAt(0)
            }
        }
        
        fun getMemoryInfo(): String {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val freeMemory = runtime.freeMemory()
            
            return """
                Current Memory Usage:
                Used: ${usedMemory / 1024 / 1024}MB
                Free: ${freeMemory / 1024 / 1024}MB
                Max: ${maxMemory / 1024 / 1024}MB
                Usage: ${(usedMemory * 100 / maxMemory)}%
            """.trimIndent()
        }
    }
}

/**
 * Extension function for easy performance tracking
 */
inline fun <T> Context.trackPerformance(operation: String, block: () -> T): T {
    return PerformanceMonitor.getInstance(this).trackOperation(operation, block)
}

/**
 * Extension function for suspend performance tracking
 */
suspend inline fun <T> Context.trackSuspendPerformance(operation: String, block: () -> T): T {
    return PerformanceMonitor.getInstance(this).trackSuspendOperation(operation, block)
}