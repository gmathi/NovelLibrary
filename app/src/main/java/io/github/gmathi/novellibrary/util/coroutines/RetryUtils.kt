package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Coroutine-based retry utilities.
 * Replaces the RxJava RetryWithDelay class and provides modern retry patterns.
 */
object RetryUtils {

    /**
     * Retry strategies for different scenarios.
     */
    object RetryStrategies {
        /**
         * Fixed delay between retries.
         */
        fun fixedDelay(delayMs: Long): (Int) -> Long = { delayMs }

        /**
         * Linear backoff - delay increases linearly.
         */
        fun linearBackoff(baseDelayMs: Long = 1000L): (Int) -> Long = { attempt ->
            baseDelayMs * attempt
        }

        /**
         * Exponential backoff - delay doubles each time.
         */
        fun exponentialBackoff(
            baseDelayMs: Long = 1000L,
            maxDelayMs: Long = 30000L
        ): (Int) -> Long = { attempt ->
            minOf(baseDelayMs * (1L shl (attempt - 1)), maxDelayMs)
        }

        /**
         * Fibonacci backoff - delay follows Fibonacci sequence.
         */
        fun fibonacciBackoff(baseDelayMs: Long = 1000L): (Int) -> Long {
            return { attempt ->
                when (attempt) {
                    1 -> baseDelayMs
                    2 -> baseDelayMs
                    else -> {
                        var a = baseDelayMs
                        var b = baseDelayMs
                        repeat(attempt - 2) {
                            val temp = a + b
                            a = b
                            b = temp
                        }
                        b
                    }
                }
            }
        }
    }

    /**
     * Executes a suspend function with retry logic.
     * Replaces RxJava retry operators.
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        retryStrategy: (Int) -> Long = RetryStrategies.exponentialBackoff(),
        shouldRetry: (Exception) -> Boolean = { true },
        operation: suspend () -> T
    ): T {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e
                
                // Don't retry on the last attempt or if shouldRetry returns false
                if (attempt >= maxRetries || !shouldRetry(e)) {
                    throw e
                }
                
                // Apply retry delay
                delay(retryStrategy(attempt + 1))
            }
        }
        
        throw lastException ?: Exception("Unknown error")
    }

    /**
     * Creates a Flow with retry logic.
     * Replaces RxJava retry operators for reactive streams.
     */
    fun <T> flowWithRetry(
        maxRetries: Long = 3,
        retryStrategy: (Int) -> Duration = { attempt -> (attempt * 1000).seconds },
        shouldRetry: (Throwable) -> Boolean = { true },
        operation: suspend () -> T
    ): Flow<T> = flow {
        emit(operation())
    }.retry(maxRetries) { cause ->
        shouldRetry(cause)
    }

    /**
     * Predefined retry conditions for common scenarios.
     */
    object RetryConditions {
        /**
         * Retry on network-related exceptions.
         */
        val networkErrors: (Exception) -> Boolean = { exception ->
            exception is NetworkException ||
            exception is java.net.SocketTimeoutException ||
            exception is java.net.UnknownHostException ||
            exception is java.io.IOException
        }

        /**
         * Retry on server errors (5xx) but not client errors (4xx).
         */
        val serverErrors: (Exception) -> Boolean = { exception ->
            when (exception) {
                is io.github.gmathi.novellibrary.network.api.ServerException -> true
                is io.github.gmathi.novellibrary.network.api.ApiException -> exception.code >= 500
                else -> networkErrors(exception)
            }
        }

        /**
         * Retry on transient errors only.
         */
        val transientErrors: (Exception) -> Boolean = { exception ->
            when (exception) {
                is io.github.gmathi.novellibrary.network.api.RateLimitException -> true
                is java.net.SocketTimeoutException -> true
                is java.net.ConnectException -> true
                else -> false
            }
        }
    }
}