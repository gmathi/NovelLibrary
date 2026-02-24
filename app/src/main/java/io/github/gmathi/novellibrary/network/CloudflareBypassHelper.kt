package io.github.gmathi.novellibrary.network

import android.content.Context
import android.webkit.WebView
import io.github.gmathi.novellibrary.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhanced helper for Cloudflare bypass operations with retry logic and caching
 */
object CloudflareBypassHelper {
    
    private val bypassCache = ConcurrentHashMap<String, BypassResult>()
    private const val CACHE_DURATION = 30 * 60 * 1000L // 30 minutes
    
    data class BypassResult(
        val success: Boolean,
        val timestamp: Long,
        val cookies: Map<String, String> = emptyMap()
    )
    
    /**
     * Check if a URL needs Cloudflare bypass
     */
    suspend fun needsBypass(client: OkHttpClient, url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            val needsBypass = response.code == 503 && 
                (response.header("Server")?.contains("cloudflare", ignoreCase = true) == true ||
                 response.header("CF-RAY") != null)
            
            response.close()
            needsBypass
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get cached bypass result if still valid
     */
    fun getCachedBypass(host: String): BypassResult? {
        val cached = bypassCache[host] ?: return null
        val age = System.currentTimeMillis() - cached.timestamp
        
        return if (age < CACHE_DURATION && cached.success) {
            cached
        } else {
            bypassCache.remove(host)
            null
        }
    }
    
    /**
     * Cache bypass result
     */
    fun cacheBypassResult(host: String, success: Boolean, cookies: Map<String, String> = emptyMap()) {
        bypassCache[host] = BypassResult(
            success = success,
            timestamp = System.currentTimeMillis(),
            cookies = cookies
        )
    }
    
    /**
     * Clear cached bypass for a host
     */
    fun clearCache(host: String) {
        bypassCache.remove(host)
    }
    
    /**
     * Clear all cached bypasses
     */
    fun clearAllCache() {
        bypassCache.clear()
    }
    
    /**
     * Check if WebView is available and up to date
     */
    fun isWebViewAvailable(context: Context): Boolean {
        return try {
            WebView(context)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Show user-friendly error message
     */
    fun showBypassError(context: Context, message: String) {
        context.toast(message)
    }
}
