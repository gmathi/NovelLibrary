@file:JvmName("GlobalNetworkHelper")
package io.github.gmathi.novellibrary.network

/**
 * Global access to NetworkHelper for external extensions.
 * This provides backward compatibility for extensions that expect global access.
 */

/**
 * Global NetworkHelper instance for external extensions.
 * This field can be accessed directly by external extensions.
 */
@JvmField
var networkHelper: NetworkHelper? = null

/**
 * Initialize the global NetworkHelper instance.
 * This should be called during app initialization.
 */
fun initializeGlobalNetworkHelper(helper: NetworkHelper) {
    networkHelper = helper
}

/**
 * Get the global NetworkHelper instance.
 * @return NetworkHelper instance or null if not initialized
 */
fun getGlobalNetworkHelper(): NetworkHelper? {
    return networkHelper
}