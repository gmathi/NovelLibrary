package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.preference.DataCenter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration class for network timeout settings
 * Allows dynamic adjustment based on network conditions and user preferences
 */
@Singleton
class NetworkTimeoutConfig @Inject constructor(
    private val dataCenter: DataCenter
) {
    
    companion object {
        // Default timeout values in seconds
        const val DEFAULT_CONNECT_TIMEOUT = 45L
        const val DEFAULT_READ_TIMEOUT = 60L
        const val DEFAULT_WRITE_TIMEOUT = 45L
        const val DEFAULT_CALL_TIMEOUT = 120L
        
        // Retry configuration
        const val DEFAULT_MAX_RETRIES = 3
        const val DEFAULT_BASE_DELAY_MS = 1000L
        
        // Timeout multipliers for different network conditions
        const val SLOW_NETWORK_MULTIPLIER = 1.5f
        const val VERY_SLOW_NETWORK_MULTIPLIER = 2.0f
    }
    
    /**
     * Get connect timeout based on current settings and network conditions
     */
    fun getConnectTimeout(): Long {
        val baseTimeout = dataCenter.networkConnectTimeout.takeIf { it > 0 } ?: DEFAULT_CONNECT_TIMEOUT
        return applyNetworkConditionMultiplier(baseTimeout)
    }
    
    /**
     * Get read timeout based on current settings and network conditions
     */
    fun getReadTimeout(): Long {
        val baseTimeout = dataCenter.networkReadTimeout.takeIf { it > 0 } ?: DEFAULT_READ_TIMEOUT
        return applyNetworkConditionMultiplier(baseTimeout)
    }
    
    /**
     * Get write timeout based on current settings and network conditions
     */
    fun getWriteTimeout(): Long {
        val baseTimeout = dataCenter.networkWriteTimeout.takeIf { it > 0 } ?: DEFAULT_WRITE_TIMEOUT
        return applyNetworkConditionMultiplier(baseTimeout)
    }
    
    /**
     * Get call timeout based on current settings and network conditions
     */
    fun getCallTimeout(): Long {
        val baseTimeout = dataCenter.networkCallTimeout.takeIf { it > 0 } ?: DEFAULT_CALL_TIMEOUT
        return applyNetworkConditionMultiplier(baseTimeout)
    }
    
    /**
     * Get maximum number of retries for failed requests
     */
    fun getMaxRetries(): Int {
        return dataCenter.networkMaxRetries.takeIf { it > 0 } ?: DEFAULT_MAX_RETRIES
    }
    
    /**
     * Get base delay between retries in milliseconds
     */
    fun getBaseRetryDelay(): Long {
        return dataCenter.networkRetryDelay.takeIf { it > 0 } ?: DEFAULT_BASE_DELAY_MS
    }
    
    /**
     * Apply network condition multiplier to timeout values
     * This can be enhanced to detect actual network conditions
     */
    private fun applyNetworkConditionMultiplier(baseTimeout: Long): Long {
        val multiplier = when {
            // Could be enhanced to detect actual network speed
            // For now, use a conservative approach for novel sites
            isSlowNetworkDetected() -> VERY_SLOW_NETWORK_MULTIPLIER
            else -> 1.0f
        }
        return (baseTimeout * multiplier).toLong()
    }
    
    /**
     * Detect if network is slow based on recent failures or user settings
     * This is a placeholder for more sophisticated network condition detection
     */
    private fun isSlowNetworkDetected(): Boolean {
        // Could implement actual network speed detection here
        // For now, return true for novel sites that are known to be slow
        return dataCenter.enableSlowNetworkMode
    }
}