package io.github.gmathi.novellibrary.util.system

import android.app.ActivityManager
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryMonitor @Inject constructor(
    private val context: Context
) {
    private val memoryInfo = ActivityManager.MemoryInfo()
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    fun getMemoryUsage(): MemoryUsage {
        activityManager.getMemoryInfo(memoryInfo)
        return MemoryUsage(
            availableMemory = memoryInfo.availMem,
            totalMemory = memoryInfo.totalMem,
            threshold = memoryInfo.threshold,
            lowMemory = memoryInfo.lowMemory
        )
    }
    
    fun shouldCleanupCache(): Boolean {
        val usage = getMemoryUsage()
        return usage.availableMemory < usage.threshold * 2
    }
    
    fun getMemoryUsagePercentage(): Float {
        val usage = getMemoryUsage()
        return ((usage.totalMemory - usage.availableMemory) * 100.0f / usage.totalMemory)
    }
    
    fun isLowMemory(): Boolean {
        return getMemoryUsage().lowMemory
    }
    
    fun getAvailableMemoryMB(): Long {
        return getMemoryUsage().availableMemory / (1024 * 1024)
    }
    
    fun getTotalMemoryMB(): Long {
        return getMemoryUsage().totalMemory / (1024 * 1024)
    }
}

data class MemoryUsage(
    val availableMemory: Long,
    val totalMemory: Long,
    val threshold: Long,
    val lowMemory: Boolean
) 