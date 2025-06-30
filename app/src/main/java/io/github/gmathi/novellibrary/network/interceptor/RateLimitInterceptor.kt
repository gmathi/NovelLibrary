package io.github.gmathi.novellibrary.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * An OkHttp Interceptor that enforces a global rate limit (requests per second) using a token bucket algorithm.
 * @param maxRequestsPerSecond The maximum number of requests allowed per second.
 */
class RateLimitInterceptor(
    private val maxRequestsPerSecond: Int = 5
) : Interceptor {
    private val semaphore = Semaphore(0, true)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        val refillInterval = 1000L / maxRequestsPerSecond
        scheduler.scheduleAtFixedRate({
            val toRelease = maxRequestsPerSecond - semaphore.availablePermits()
            if (toRelease > 0) semaphore.release(toRelease)
        }, 0, refillInterval, TimeUnit.MILLISECONDS)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        semaphore.acquire()
        try {
            return chain.proceed(chain.request())
        } finally {
            // Do not release here; tokens are refilled by the scheduler
        }
    }
} 