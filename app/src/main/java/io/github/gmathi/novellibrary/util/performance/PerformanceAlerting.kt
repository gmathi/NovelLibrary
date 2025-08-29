package io.github.gmathi.novellibrary.util.performance

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance alerting system for Hilt migration
 * 
 * Monitors performance metrics and sends alerts when thresholds are exceeded
 */
@Singleton
class PerformanceAlerting @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: RuntimePerformanceMonitor,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    
    companion object {
        private const val TAG = "PerformanceAlerting"
        
        // Performance thresholds
        private const val MEMORY_CRITICAL_THRESHOLD_MB = 300L
        private const val MEMORY_WARNING_THRESHOLD_MB = 200L
        private const val STARTUP_CRITICAL_THRESHOLD_MS = 5000L
        private const val STARTUP_WARNING_THRESHOLD_MS = 3000L
        private const val DI_CRITICAL_THRESHOLD_MS = 1000L
        private const val DI_WARNING_THRESHOLD_MS = 500L
    }
    
    private val alertingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false
    
    /**
     * Starts continuous performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Log.d(TAG, "Starting performance monitoring...")
        
        alertingScope.launch {
            while (isMonitoring) {
                checkPerformanceMetrics()
                delay(30000) // Check every 30 seconds
            }
        }
    }
    
    /**
     * Stops performance monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        alertingScope.cancel()
        Log.d(TAG, "Performance monitoring stopped")
    }
    
    /**
     * Checks all performance metrics and triggers alerts if needed
     */
    private suspend fun checkPerformanceMetrics() {
        checkMemoryUsage()
        checkStartupPerformance()
    }
    
    /**
     * Monitors memory usage and triggers alerts
     */
    private fun checkMemoryUsage() {
        val memoryUsage = performanceMonitor.getCurrentMemoryUsage()
        
        when {
            memoryUsage.usedMemoryMB > MEMORY_CRITICAL_THRESHOLD_MB -> {
                triggerCriticalMemoryAlert(memoryUsage)
            }
            memoryUsage.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB -> {
                triggerMemoryWarningAlert(memoryUsage)
            }
        }
        
        if (memoryUsage.isLowMemory) {
            triggerLowMemoryAlert(memoryUsage)
        }
    }
    
    /**
     * Monitors startup performance
     */
    private fun checkStartupPerformance() {
        val startupMetrics = performanceMonitor.getStartupMetrics()
        
        when {
            startupMetrics.totalStartupTime > STARTUP_CRITICAL_THRESHOLD_MS -> {
                triggerCriticalStartupAlert(startupMetrics)
            }
            startupMetrics.totalStartupTime > STARTUP_WARNING_THRESHOLD_MS -> {
                triggerStartupWarningAlert(startupMetrics)
            }
        }
    }
    
    /**
     * Monitors dependency injection performance
     */
    fun checkDependencyInjectionPerformance(componentName: String, duration: Long) {
        when {
            duration > DI_CRITICAL_THRESHOLD_MS -> {
                triggerCriticalDIAlert(componentName, duration)
            }
            duration > DI_WARNING_THRESHOLD_MS -> {
                triggerDIWarningAlert(componentName, duration)
            }
        }
    }
    
    /**
     * Triggers critical memory usage alert
     */
    private fun triggerCriticalMemoryAlert(memoryUsage: RuntimePerformanceMonitor.MemoryUsage) {
        Log.e(TAG, "CRITICAL: High memory usage - ${memoryUsage.usedMemoryMB}MB")
        
        // Log to Firebase Analytics
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "memory_critical")
            putLong("memory_used_mb", memoryUsage.usedMemoryMB)
            putLong("memory_max_mb", memoryUsage.maxMemoryMB)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
        
        // Suggest immediate actions
        Log.e(TAG, "Suggested actions: Force garbage collection, clear caches, review memory leaks")
    }
    
    /**
     * Triggers memory warning alert
     */
    private fun triggerMemoryWarningAlert(memoryUsage: RuntimePerformanceMonitor.MemoryUsage) {
        Log.w(TAG, "WARNING: Elevated memory usage - ${memoryUsage.usedMemoryMB}MB")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "memory_warning")
            putLong("memory_used_mb", memoryUsage.usedMemoryMB)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Triggers low memory alert
     */
    private fun triggerLowMemoryAlert(memoryUsage: RuntimePerformanceMonitor.MemoryUsage) {
        Log.w(TAG, "WARNING: System low memory detected")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "low_memory")
            putLong("available_memory_mb", memoryUsage.availableMemoryMB)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Triggers critical startup performance alert
     */
    private fun triggerCriticalStartupAlert(startupMetrics: RuntimePerformanceMonitor.StartupMetrics) {
        Log.e(TAG, "CRITICAL: Slow startup - ${startupMetrics.totalStartupTime}ms")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "startup_critical")
            putLong("startup_time_ms", startupMetrics.totalStartupTime)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Triggers startup warning alert
     */
    private fun triggerStartupWarningAlert(startupMetrics: RuntimePerformanceMonitor.StartupMetrics) {
        Log.w(TAG, "WARNING: Slow startup - ${startupMetrics.totalStartupTime}ms")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "startup_warning")
            putLong("startup_time_ms", startupMetrics.totalStartupTime)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Triggers critical dependency injection alert
     */
    private fun triggerCriticalDIAlert(componentName: String, duration: Long) {
        Log.e(TAG, "CRITICAL: Slow dependency injection for $componentName - ${duration}ms")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "di_critical")
            putString("component_name", componentName)
            putLong("injection_time_ms", duration)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Triggers dependency injection warning alert
     */
    private fun triggerDIWarningAlert(componentName: String, duration: Long) {
        Log.w(TAG, "WARNING: Slow dependency injection for $componentName - ${duration}ms")
        
        val bundle = android.os.Bundle().apply {
            putString("alert_type", "di_warning")
            putString("component_name", componentName)
            putLong("injection_time_ms", duration)
        }
        firebaseAnalytics.logEvent("performance_alert", bundle)
    }
    
    /**
     * Gets current alerting status
     */
    fun getAlertingStatus(): AlertingStatus {
        val memoryUsage = performanceMonitor.getCurrentMemoryUsage()
        val startupMetrics = performanceMonitor.getStartupMetrics()
        
        return AlertingStatus(
            isMonitoring = isMonitoring,
            memoryStatus = when {
                memoryUsage.usedMemoryMB > MEMORY_CRITICAL_THRESHOLD_MB -> AlertLevel.CRITICAL
                memoryUsage.usedMemoryMB > MEMORY_WARNING_THRESHOLD_MB -> AlertLevel.WARNING
                else -> AlertLevel.NORMAL
            },
            startupStatus = when {
                startupMetrics.totalStartupTime > STARTUP_CRITICAL_THRESHOLD_MS -> AlertLevel.CRITICAL
                startupMetrics.totalStartupTime > STARTUP_WARNING_THRESHOLD_MS -> AlertLevel.WARNING
                else -> AlertLevel.NORMAL
            }
        )
    }
    
    enum class AlertLevel {
        NORMAL, WARNING, CRITICAL
    }
    
    data class AlertingStatus(
        val isMonitoring: Boolean,
        val memoryStatus: AlertLevel,
        val startupStatus: AlertLevel
    )
}