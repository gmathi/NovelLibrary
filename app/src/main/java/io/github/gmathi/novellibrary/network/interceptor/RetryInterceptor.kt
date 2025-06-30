package io.github.gmathi.novellibrary.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import kotlin.math.min

/**
 * An OkHttp Interceptor that retries idempotent requests on transient errors with exponential backoff.
 * @param maxRetries Maximum number of retries (default: 3)
 * @param baseDelayMillis Initial backoff delay in ms (default: 500)
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMillis: Long = 500
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var lastException: IOException? = null
        val request = chain.request()
        val method = request.method.uppercase()
        val isIdempotent = method == "GET" || method == "HEAD" || method == "OPTIONS"
        if (!isIdempotent) return chain.proceed(request)
        while (attempt <= maxRetries) {
            try {
                val response = chain.proceed(request)
                if (response.isSuccessful || response.code in 400..499) {
                    return response
                }
                // Retry on 5xx
            } catch (e: IOException) {
                lastException = e
            }
            attempt++
            if (attempt > maxRetries) break
            val delay = min(baseDelayMillis * (1L shl (attempt - 1)), 5000L)
            Thread.sleep(delay)
        }
        throw lastException ?: IOException("Unknown network error after $maxRetries retries")
    }
} 