package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Utility functions for common coroutine patterns.
 */
object CoroutineUtils {
    
    /**
     * Executes an operation with retry logic.
     * Retries the operation up to maxRetries times with exponential backoff.
     * 
     * @deprecated Use RetryUtils.withRetry() for more advanced retry strategies
     */
    @Deprecated("Use RetryUtils.withRetry() for more advanced retry strategies")
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Duration = 1.seconds,
        maxDelay: Duration = 10.seconds,
        factor: Double = 2.0,
        operation: suspend () -> T
    ): T {
        return RetryUtils.withRetry(
            maxRetries = maxRetries,
            retryStrategy = { attempt ->
                val delay = (initialDelay.inWholeMilliseconds * Math.pow(factor, (attempt - 1).toDouble())).toLong()
                minOf(delay, maxDelay.inWholeMilliseconds)
            },
            operation = operation
        )
    }
    
    /**
     * Creates a Flow that emits values with retry logic.
     * 
     * @deprecated Use RetryUtils.flowWithRetry() for more advanced retry strategies
     */
    @Deprecated("Use RetryUtils.flowWithRetry() for more advanced retry strategies")
    fun <T> flowWithRetry(
        maxRetries: Long = 3,
        operation: suspend () -> T
    ): Flow<T> = RetryUtils.flowWithRetry(
        maxRetries = maxRetries,
        shouldRetry = { cause ->
            cause is NetworkException || cause is DatabaseException
        },
        operation = operation
    ).catch { exception ->
        throw CoroutineErrorHandler.mapException(exception as Exception)
    }
    
    /**
     * Launches a coroutine with automatic cancellation tracking.
     * Returns a Job that can be used to cancel the operation.
     */
    fun CoroutineScope.launchWithTracking(
        onStart: () -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        action: suspend () -> Unit
    ): Job {
        return launch(CoroutineErrorHandler.globalExceptionHandler) {
            try {
                onStart()
                action()
                onComplete()
            } catch (exception: Exception) {
                onError(exception)
                throw exception
            }
        }
    }
    
    /**
     * Debounces coroutine execution to prevent rapid successive calls.
     */
    class CoroutineDebouncer(
        private val scope: CoroutineScope,
        private val delayMs: Long = 300L
    ) {
        private var debounceJob: Job? = null
        
        fun debounce(action: suspend () -> Unit) {
            debounceJob?.cancel()
            debounceJob = scope.launch {
                delay(delayMs)
                action()
            }
        }
        
        fun cancel() {
            debounceJob?.cancel()
        }
    }
    
    /**
     * Throttles coroutine execution to limit frequency of calls.
     */
    class CoroutineThrottler(
        private val scope: CoroutineScope,
        private val intervalMs: Long = 1000L
    ) {
        private var lastExecutionTime = 0L
        private var throttleJob: Job? = null
        
        fun throttle(action: suspend () -> Unit) {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastExecution = currentTime - lastExecutionTime
            
            if (timeSinceLastExecution >= intervalMs) {
                lastExecutionTime = currentTime
                throttleJob?.cancel()
                throttleJob = scope.launch { action() }
            }
        }
        
        fun cancel() {
            throttleJob?.cancel()
        }
    }
}