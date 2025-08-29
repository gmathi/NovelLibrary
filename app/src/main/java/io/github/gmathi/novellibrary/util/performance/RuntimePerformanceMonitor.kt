package io.github.gmathi.novellibrary.util.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime performance monitoring utility for Hilt migration
 * 
 * Monitors memory usage, startup time, and dependency injection performance
 */
@Singleton
class RuntimePerformanceMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "RuntimePerformanceMonitor"
        private const val MEMORY_WARNING_THRESHOLD_MB = 200L
        private const val STARTUP_WARNING_THRESHOLD_MS = 3000L
    }
    
    private var appStartTime: Long = 0
    private var hiltInitTime: Long = 0
    
    /**
     * Records app startup time
     */
    fun recordAppStartTime() {
        appStartTime = System.currentTimeMillis()
        Log.d(TAG, "App startup recorded at: $appStartTime")
    }
    
    /**
     * Records Hilt initialization completion time
     */
    fun recordHiltInitTime() {
        hiltInitTime = System.currentTimeMillis()
        val initDuration = hiltInitTime - appStartTime
        Log.d(TAG, "Hilt initialization completed in: ${initDuration}ms")
        
        if (initDuration > STARTUP_WARNING_THRESHOLD_MS) {
            Log.w(TAG, "Hilt initialization took longer than expected: ${initDuration}ms")
        }
    }
    
    /**
     * Gets current memory usage information
     */
    fun getCurrentMemoryUsage(): MemoryUsage {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemoryMB = runtime.maxMemory() / 1024 / 1024
        val availableMemoryMB = memoryInfo.availMem / 1024 / 1024
        
        return MemoryUsage(
            usedMemoryMB = usedMemoryMB,
            maxMemoryMB = maxMemoryMB,
            availableMemoryMB = availableMemoryMB,
            isLowMemory = memoryInfo.lowMemory
        )
    }
    
    /**
     * Monitors memory usage and logs warnings if thresholds are exceeded
     */
    fun checkMemoryUsage() {
        val memoryUsage = getCurrentMemoryUsage()
        
        Log.d(TAG, "Memory usage: ${memoryUsage.usedMemoryMB}MB / ${memoryUsage.maxMemoryMB}MB")
        
        if (memoryUsage.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB) {
            Log.w(TAG, "High memory usage detected: ${memoryUsage.usedMemoryMB}MB")
        }
        
        if (memoryUsage.isLowMemory) {
            Log.w(TAG, "System is in low memory state")
        }
    }
    
    /**
     * Measures dependency injection performance
     */
    fun measureDependencyInjectionTime(componentName: String, action: () -> Unit): Long {
        val startTime = System.currentTimeMillis()
        action()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        Log.d(TAG, "Dependency injection for $componentName took: ${duration}ms")
        return duration
    }
    
    /**
     * Gets startup performance metrics
     */
    fun getStartupMetrics(): StartupMetrics {
        val currentTime = System.currentTimeMillis()
        return StartupMetrics(
            appStartTime = appStartTime,
            hiltInitTime = hiltInitTime,
            totalStartupTime = if (hiltInitTime > 0) hiltInitTime - appStartTime else currentTime - appStartTime,
            currentTime = currentTime
        )
    }
    
    /**
     * Logs comprehensive performance report
     */
    fun logPerformanceReport() {
        val memoryUsage = getCurrentMemoryUsage()
        val startupMetrics = getStartupMetrics()
        
        Log.i(TAG, "=== Runtime Performance Report ===")
        Log.i(TAG, "Startup Time: ${startupMetrics.totalStartupTime}ms")
        Log.i(TAG, "Memory Usage: ${memoryUsage.usedMemoryMB}MB / ${memoryUsage.maxMemoryMB}MB")
        Log.i(TAG, "Available Memory: ${memoryUsage.availableMemoryMB}MB")
        Log.i(TAG, "Low Memory: ${memoryUsage.isLowMemory}")
        Log.i(TAG, "================================")
    }
    
    data class MemoryUsage(
        val usedMemoryMB: Long,
        val maxMemoryMB: Long,
        val availableMemoryMB: Long,
        val isLowMemory: Boolean
    )
    
    data class StartupMetrics(
        val appStartTime: Long,
        val hiltInitTime: Long,
        val totalStartupTime: Long,
        val currentTime: Long
    )
}