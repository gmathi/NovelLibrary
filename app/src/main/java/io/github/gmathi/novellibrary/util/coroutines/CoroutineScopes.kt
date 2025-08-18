package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Provides coroutine scopes for different app layers following structured concurrency principles.
 * Each scope has a SupervisorJob to prevent child failures from cancelling siblings.
 */
class CoroutineScopes {
    
    /**
     * Application-wide scope for long-running operations that should survive activity/fragment lifecycle.
     * Uses SupervisorJob to prevent child failures from affecting other operations.
     */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * IO scope for network and database operations.
     * Optimized for blocking I/O operations.
     */
    val ioScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Background scope for CPU-intensive operations.
     * Uses Default dispatcher for computational work.
     */
    val backgroundScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Service scope for background services.
     * Separate from application scope for better lifecycle management.
     */
    val serviceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}