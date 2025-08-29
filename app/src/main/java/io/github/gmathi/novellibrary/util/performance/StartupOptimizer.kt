package io.github.gmathi.novellibrary.util.performance

import android.app.Application
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Startup optimization utility for Hilt migration
 * 
 * Optimizes app startup by deferring expensive operations and using lazy initialization
 */
@Singleton
class StartupOptimizer @Inject constructor(
    @ApplicationContext private val context: Application,
    private val performanceMonitor: RuntimePerformanceMonitor,
    private val dbHelperProvider: Provider<DBHelper>,
    private val networkHelperProvider: Provider<NetworkHelper>,
    private val sourceManagerProvider: Provider<SourceManager>,
    private val extensionManagerProvider: Provider<ExtensionManager>
) {
    
    companion object {
        private const val TAG = "StartupOptimizer"
    }
    
    private val startupScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    /**
     * Optimizes app startup by deferring non-critical initializations
     */
    fun optimizeStartup() {
        performanceMonitor.recordAppStartTime()
        
        // Defer expensive operations to background
        startupScope.launch {
            deferExpensiveOperations()
        }
        
        // Initialize critical components immediately
        initializeCriticalComponents()
        
        performanceMonitor.recordHiltInitTime()
    }
    
    /**
     * Initializes only critical components needed for app startup
     */
    private fun initializeCriticalComponents() {
        Log.d(TAG, "Initializing critical components...")
        
        // Only initialize components that are absolutely necessary for startup
        // Other components will be lazily initialized when first accessed
        
        Log.d(TAG, "Critical components initialized")
    }
    
    /**
     * Defers expensive operations to background threads
     */
    private suspend fun deferExpensiveOperations() {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "Starting deferred initialization...")
            
            // Pre-warm database connection in background
            launch {
                performanceMonitor.measureDependencyInjectionTime("DBHelper") {
                    dbHelperProvider.get()
                }
            }
            
            // Pre-warm network helper in background
            launch {
                performanceMonitor.measureDependencyInjectionTime("NetworkHelper") {
                    networkHelperProvider.get()
                }
            }
            
            // Defer source manager initialization
            launch {
                delay(2000) // Wait 2 seconds after startup
                performanceMonitor.measureDependencyInjectionTime("SourceManager") {
                    sourceManagerProvider.get()
                }
            }
            
            // Defer extension manager initialization
            launch {
                delay(3000) // Wait 3 seconds after startup
                performanceMonitor.measureDependencyInjectionTime("ExtensionManager") {
                    extensionManagerProvider.get()
                }
            }
            
            Log.d(TAG, "Deferred initialization completed")
        }
    }
    
    /**
     * Pre-warms specific components for faster access
     */
    fun preWarmComponent(componentName: String) {
        startupScope.launch(Dispatchers.IO) {
            when (componentName) {
                "database" -> {
                    performanceMonitor.measureDependencyInjectionTime("DBHelper-PreWarm") {
                        dbHelperProvider.get()
                    }
                }
                "network" -> {
                    performanceMonitor.measureDependencyInjectionTime("NetworkHelper-PreWarm") {
                        networkHelperProvider.get()
                    }
                }
                "sources" -> {
                    performanceMonitor.measureDependencyInjectionTime("SourceManager-PreWarm") {
                        sourceManagerProvider.get()
                    }
                }
                "extensions" -> {
                    performanceMonitor.measureDependencyInjectionTime("ExtensionManager-PreWarm") {
                        extensionManagerProvider.get()
                    }
                }
            }
        }
    }
    
    /**
     * Monitors startup performance and logs metrics
     */
    fun monitorStartupPerformance() {
        startupScope.launch {
            delay(5000) // Wait 5 seconds after startup
            
            performanceMonitor.logPerformanceReport()
            performanceMonitor.checkMemoryUsage()
            
            val startupMetrics = performanceMonitor.getStartupMetrics()
            if (startupMetrics.totalStartupTime > 3000) {
                Log.w(TAG, "Slow startup detected: ${startupMetrics.totalStartupTime}ms")
                suggestOptimizations()
            }
        }
    }
    
    /**
     * Suggests optimizations based on performance metrics
     */
    private fun suggestOptimizations() {
        val memoryUsage = performanceMonitor.getCurrentMemoryUsage()
        
        Log.i(TAG, "=== Startup Optimization Suggestions ===")
        
        if (memoryUsage.usedMemoryMB > 150) {
            Log.i(TAG, "Consider lazy loading more components to reduce memory usage")
        }
        
        Log.i(TAG, "Consider pre-warming frequently used components")
        Log.i(TAG, "Review component initialization order")
        Log.i(TAG, "=======================================")
    }
    
    /**
     * Cleans up startup resources
     */
    fun cleanup() {
        startupScope.cancel()
    }
}