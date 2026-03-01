package io.github.gmathi.novellibrary.core.system

import android.content.Context

/**
 * Interface defining the contract for accessing injected dependencies.
 * Uses generic types (Any) to avoid dependencies on concrete implementations.
 * Concrete implementations are provided by app/settings modules through dependency injection.
 */
interface DataAccessor {
    /**
     * Firebase Analytics instance for tracking events.
     * Concrete type provided by app module.
     */
    val firebaseAnalytics: Any
    
    /**
     * Data center for application preferences and settings.
     * Concrete type provided by app module.
     */
    val dataCenter: Any
    
    /**
     * Database helper for data persistence.
     * Concrete type provided by app module.
     */
    val dbHelper: Any
    
    /**
     * Source manager for novel source management.
     * Concrete type provided by app module.
     */
    val sourceManager: Any
    
    /**
     * Network helper for network operations.
     * Concrete type provided by app module.
     */
    val networkHelper: Any
    
    /**
     * Returns the context for this accessor.
     * @return Context or null if not available
     */
    fun getContext(): Context?
}
