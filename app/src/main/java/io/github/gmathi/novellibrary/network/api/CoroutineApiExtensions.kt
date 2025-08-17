package io.github.gmathi.novellibrary.network.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import okhttp3.ResponseBody
import java.net.SocketTimeoutException

/**
 * Extension functions for converting API responses to Flow patterns.
 * These extensions help migrate from RxJava Observable patterns to Kotlin Flow.
 */

/**
 * Converts a suspend API call to a Flow.
 * Replaces RxJava Single.toObservable() pattern.
 * 
 * Usage:
 * apiService.getData().asFlow()
 */
fun <T> (suspend () -> Response<T>).asFlow(): Flow<T> = flow {
    val response = this@asFlow()
    emit(ApiErrorHandler.handleResponse(response))
}.flowOn(Dispatchers.IO)

/**
 * Converts a suspend API call to a Flow with error handling.
 * Replaces RxJava error handling chains.
 */
fun <T> (suspend () -> Response<T>).asFlowWithErrorHandling(): Flow<T> = flow {
    try {
        val response = this@asFlowWithErrorHandling()
        emit(ApiErrorHandler.handleResponse(response))
    } catch (e: Exception) {
        throw ApiErrorHandler.mapException(e)
    }
}.flowOn(Dispatchers.IO)

/**
 * Creates a polling Flow from a suspend API call.
 * Replaces RxJava Observable.interval() patterns.
 * 
 * Usage:
 * { apiService.getUpdates() }.asPollingFlow(5000L)
 */
fun <T> (suspend () -> Response<T>).asPollingFlow(intervalMs: Long): Flow<T> = flow {
    while (currentCoroutineContext().isActive) {
        try {
            val response = this@asPollingFlow()
            emit(ApiErrorHandler.handleResponse(response))
            delay(intervalMs)
        } catch (e: Exception) {
            // Log error but continue polling
            delay(intervalMs)
        }
    }
}.flowOn(Dispatchers.IO)

/**
 * Creates a Flow that retries on failure.
 * Replaces RxJava retry operators and RetryWithDelay class.
 */
fun <T> (suspend () -> Response<T>).asRetryFlow(
    maxRetries: Int = 3,
    retryStrategy: (Int) -> Long = { attempt -> 1000L * attempt }
): Flow<T> = flow {
    var lastException: Exception? = null
    var success = false
    for (attempt in 0..maxRetries) {
        if (success) break
        try {
            val response = this@asRetryFlow()
            emit(ApiErrorHandler.handleResponse(response))
            success = true
        } catch (e: Exception) {
            lastException = e
            if (attempt >= maxRetries) {
                throw ApiErrorHandler.mapException(e)
            }
            delay(retryStrategy(attempt + 1))
        }
    }
}.flowOn(Dispatchers.IO)

/**
 * Creates a Flow with timeout handling.
 * Replaces RxJava timeout operators.
 */
fun <T> (suspend () -> Response<T>).asFlowWithTimeout(timeoutMs: Long = 30000L): Flow<T> = flow {
    try {
        withTimeout(timeoutMs) {
            val response = this@asFlowWithTimeout()
            emit(ApiErrorHandler.handleResponse(response))
        }
    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
        throw ApiErrorHandler.mapException(SocketTimeoutException("Request timeout after ${timeoutMs}ms"))
    } catch (e: Exception) {
        throw ApiErrorHandler.mapException(e)
    }
}.flowOn(Dispatchers.IO)

/**
 * Creates a Flow with both retry and timeout handling.
 * Combines multiple error handling strategies.
 */
fun <T> (suspend () -> Response<T>).asFlowWithRetryAndTimeout(
    maxRetries: Int = 3,
    timeoutMs: Long = 30000L,
    retryStrategy: (Int) -> Long = { attempt -> 1000L * attempt }
): Flow<T> = flow {
    var lastException: Exception? = null
    var success = false
    for (attempt in 0..maxRetries) {
        if (success) break
        try {
            withTimeout(timeoutMs) {
                val response = this@asFlowWithRetryAndTimeout()
                emit(ApiErrorHandler.handleResponse(response))
                success = true
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            lastException = e
            if (attempt >= maxRetries) {
                throw ApiErrorHandler.mapException(SocketTimeoutException("Request timeout after ${timeoutMs}ms"))
            }
            delay(retryStrategy(attempt + 1))
        } catch (e: Exception) {
            lastException = e
            if (attempt >= maxRetries) {
                throw ApiErrorHandler.mapException(e)
            }
            delay(retryStrategy(attempt + 1))
        }
    }
}.flowOn(Dispatchers.IO)

/**
 * Extension for ResponseBody to convert to Flow<String>.
 * Useful for streaming content.
 */
fun ResponseBody.asStringFlow(): Flow<String> = flow {
    emit(string())
}.flowOn(Dispatchers.IO)

/**
 * Extension for paginated API calls.
 * Replaces RxJava pagination patterns.
 */
fun <T> createPaginatedFlow(
    apiCall: suspend (page: Int) -> Response<List<T>>
): Flow<List<T>> = flow {
    var page = 1
    do {
        val response = apiCall(page)
        if (response.isSuccessful) {
            val data = response.body()
            if (!data.isNullOrEmpty()) {
                emit(data)
                page++
            } else {
                break
            }
        } else {
            throw ApiErrorHandler.mapException(
                ApiException("Pagination failed", response.code())
            )
        }
    } while (currentCoroutineContext().isActive)
}.flowOn(Dispatchers.IO)

/**
 * Creates a Flow that emits cached data first, then fresh data.
 * Replaces RxJava concat patterns.
 */
fun <T> createCacheFirstFlow(
    getCachedData: suspend () -> T?,
    getFreshData: suspend () -> Response<T>
): Flow<T> = flow {
    // Emit cached data first if available
    getCachedData()?.let { cachedData ->
        emit(cachedData)
    }
    
    // Then emit fresh data
    try {
        val response = getFreshData()
        emit(ApiErrorHandler.handleResponse(response))
    } catch (e: Exception) {
        // If we already emitted cached data, we can ignore the error
        // Otherwise, propagate it
        if (getCachedData() == null) {
            throw ApiErrorHandler.mapException(e)
        }
    }
}.flowOn(Dispatchers.IO)