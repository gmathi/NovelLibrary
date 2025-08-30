@file:JvmName("NetworkHelperCompat")
package io.github.gmathi.novellibrary.network

/**
 * Compatibility layer for external extensions that expect NetworkHelper to be available
 * through specific static methods or global variables.
 */

/**
 * Static method to get NetworkHelper instance.
 * Provides multiple access patterns for maximum compatibility.
 */
fun getNetworkHelper(): NetworkHelper {
    return getGlobalNetworkHelper() ?: NetworkHelperProvider.getInstance()
}

/**
 * Initialize the compatibility layer.
 * This should be called when NetworkHelper is available.
 */
fun initNetworkHelperCompat(helper: NetworkHelper) {
    networkHelper = helper
    NetworkHelperProvider.initialize(helper)
    initializeGlobalNetworkHelper(helper)
}