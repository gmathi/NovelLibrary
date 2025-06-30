package io.github.gmathi.novellibrary.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Cache control interceptor that ensures only 200 responses are cached.
 * Adds no-cache headers to non-200 responses to prevent them from being cached by OkHttp.
 */
class CacheControlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Only allow 200 responses to be cached
        return if (response.code == 200) {
            response.newBuilder()
                .header("X-Cache", if (response.cacheResponse != null) "HIT" else "MISS")
                .build()
        } else {
            // Add no-cache headers to prevent caching of non-200 responses
            response.newBuilder()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .header("Pragma", "no-cache")
                .header("X-Cache", "NO-CACHE")
                .build()
        }
    }
} 