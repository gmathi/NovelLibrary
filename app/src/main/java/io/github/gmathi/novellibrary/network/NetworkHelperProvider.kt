package io.github.gmathi.novellibrary.network

/**
 * Global provider for NetworkHelper to support external extensions.
 * This provides backward compatibility for extensions that expect global access to NetworkHelper.
 */
object NetworkHelperProvider {
    
    @Volatile
    private var networkHelper: NetworkHelper? = null
    
    /**
     * Initialize the provider with NetworkHelper instance.
     * This should be called during app initialization.
     */
    fun initialize(helper: NetworkHelper) {
        networkHelper = helper
    }
    
    /**
     * Get the NetworkHelper instance.
     * @return NetworkHelper instance
     * @throws IllegalStateException if not initialized
     */
    fun getInstance(): NetworkHelper {
        return networkHelper ?: throw IllegalStateException("NetworkHelper not initialized")
    }
    
    /**
     * Check if the provider is initialized.
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return networkHelper != null
    }
}