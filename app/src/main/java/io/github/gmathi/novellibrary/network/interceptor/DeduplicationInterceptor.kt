package io.github.gmathi.novellibrary.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Interceptor that prevents duplicate requests by caching ongoing requests
 * and returning the same response for identical requests made within a short time window.
 */
class DeduplicationInterceptor : Interceptor {
    
    private val ongoingRequests = ConcurrentHashMap<String, OngoingRequest>()
    private val requestCounter = AtomicInteger(0)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val cacheKey = generateCacheKey(request)
        
        // Check if there's already an ongoing request for this URL
        val existingRequest = ongoingRequests[cacheKey]
        if (existingRequest != null && !existingRequest.isExpired()) {
            // Wait for the existing request to complete
            return existingRequest.waitForResponse()
        }
        
        // Create a new ongoing request
        val ongoingRequest = OngoingRequest()
        ongoingRequests[cacheKey] = ongoingRequest
        
        try {
            // Execute the request
            val response = chain.proceed(request)
            ongoingRequest.setResponse(response)
            return response
        } catch (e: Exception) {
            ongoingRequest.setException(e)
            throw e
        } finally {
            // Clean up after a delay to allow for potential duplicate requests
            ongoingRequest.scheduleCleanup {
                ongoingRequests.remove(cacheKey)
            }
        }
    }
    
    private fun generateCacheKey(request: okhttp3.Request): String {
        // Create a cache key based on method, URL, and headers
        val headers = request.headers.toString()
        return "${request.method}:${request.url}:$headers"
    }
    
    private class OngoingRequest {
        private var response: Response? = null
        private var exception: Exception? = null
        private var completed = false
        private val startTime = System.currentTimeMillis()
        private val cleanupJobs = mutableListOf<() -> Unit>()
        
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - startTime > EXPIRY_TIME_MS
        }
        
        fun setResponse(response: Response) {
            this.response = response
            this.completed = true
            notifyWaiters()
        }
        
        fun setException(exception: Exception) {
            this.exception = exception
            this.completed = true
            notifyWaiters()
        }
        
        fun waitForResponse(): Response {
            while (!completed) {
                try {
                    Thread.sleep(10) // Small delay to avoid busy waiting
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
            
            exception?.let { throw it }
            return response ?: throw IllegalStateException("No response available")
        }
        
        fun scheduleCleanup(cleanup: () -> Unit) {
            cleanupJobs.add(cleanup)
            Thread {
                try {
                    Thread.sleep(CLEANUP_DELAY_MS)
                    cleanupJobs.forEach { it.invoke() }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
        
        private fun notifyWaiters() {
            // Notify any waiting threads
        }
        
        companion object {
            private const val EXPIRY_TIME_MS = 5000L // 5 seconds
            private const val CLEANUP_DELAY_MS = 1000L // 1 second
        }
    }
} 