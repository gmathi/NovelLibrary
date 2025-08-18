package io.github.gmathi.novellibrary.util.migration

import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback mechanism for gradual migration from Injekt to Hilt.
 * Provides safety nets and rollback capabilities during the migration process.
 */
@Singleton
class MigrationFallback @Inject constructor(
    private val migrationLogger: MigrationLogger
) {
    
    companion object {
        private const val TAG = "MigrationFallback"
    }
    
    private val fallbackRegistry = mutableMapOf<String, () -> Any?>()
    private val activeFallbacks = mutableSetOf<String>()
    
    /**
     * Registers a fallback provider for a specific component.
     * @param componentName Name of the component
     * @param fallbackProvider Function that provides the fallback instance
     */
    fun registerFallback(componentName: String, fallbackProvider: () -> Any?) {
        fallbackRegistry[componentName] = fallbackProvider
        Logs.debug(TAG, "Registered fallback for $componentName")
    }
    
    /**
     * Attempts to get a component via Hilt injection, falls back to Injekt if needed.
     * @param componentName Name of the component
     * @param hiltProvider Function that attempts to get the component via Hilt
     * @return The component instance or null if both methods fail
     */
    fun <T> getComponentWithFallback(
        componentName: String,
        hiltProvider: () -> T?
    ): T? {
        return try {
            // First try Hilt injection
            val hiltComponent = hiltProvider()
            if (hiltComponent != null) {
                // Remove from active fallbacks if Hilt works
                if (activeFallbacks.remove(componentName)) {
                    migrationLogger.logComponentMigration(
                        componentName, 
                        "Injekt (Fallback)", 
                        "Hilt", 
                        "Recovered"
                    )
                }
                hiltComponent
            } else {
                // Fall back to registered fallback
                activateFallback(componentName)
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Hilt injection failed for $componentName", e)
            activateFallback(componentName)
        }
    }
    
    /**
     * Activates fallback mechanism for a component.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> activateFallback(componentName: String): T? {
        return try {
            val fallbackProvider = fallbackRegistry[componentName]
            if (fallbackProvider != null) {
                activeFallbacks.add(componentName)
                migrationLogger.logFallbackActivation(
                    componentName, 
                    "Hilt injection failed, using fallback"
                )
                fallbackProvider() as? T
            } else {
                Logs.error(TAG, "No fallback registered for $componentName")
                null
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Fallback failed for $componentName", e)
            null
        }
    }
    
    /**
     * Checks if a component is currently using fallback.
     */
    fun isUsingFallback(componentName: String): Boolean {
        return activeFallbacks.contains(componentName)
    }
    
    /**
     * Gets list of components currently using fallback.
     */
    fun getActiveFallbacks(): Set<String> {
        return activeFallbacks.toSet()
    }
    
    /**
     * Validates that fallback mechanisms are working correctly.
     */
    fun validateFallbacks(): Boolean {
        var allValid = true
        
        fallbackRegistry.forEach { (componentName, provider) ->
            try {
                val instance = provider()
                if (instance == null) {
                    Logs.error(TAG, "Fallback validation failed for $componentName - null instance")
                    allValid = false
                } else {
                    Logs.debug(TAG, "Fallback validation passed for $componentName")
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Fallback validation failed for $componentName", e)
                allValid = false
            }
        }
        
        return allValid
    }
    
    /**
     * Clears all fallback registrations (for testing or cleanup).
     */
    fun clearFallbacks() {
        fallbackRegistry.clear()
        activeFallbacks.clear()
        Logs.debug(TAG, "All fallbacks cleared")
    }
    
    /**
     * Gets migration status summary.
     */
    fun getMigrationStatus(): MigrationStatus {
        val totalComponents = fallbackRegistry.size
        val fallbackComponents = activeFallbacks.size
        val migratedComponents = totalComponents - fallbackComponents
        
        return MigrationStatus(
            totalComponents = totalComponents,
            migratedComponents = migratedComponents,
            fallbackComponents = fallbackComponents,
            activeFallbacks = activeFallbacks.toList()
        )
    }
}

/**
 * Data class representing migration status.
 */
data class MigrationStatus(
    val totalComponents: Int,
    val migratedComponents: Int,
    val fallbackComponents: Int,
    val activeFallbacks: List<String>
) {
    val migrationProgress: Float
        get() = if (totalComponents > 0) {
            (migratedComponents.toFloat() / totalComponents.toFloat()) * 100f
        } else {
            100f
        }
}