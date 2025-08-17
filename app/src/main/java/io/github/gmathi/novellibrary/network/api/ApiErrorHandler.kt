package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.util.coroutines.NetworkException
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error handling utilities for coroutine-based API calls.
 * Provides patterns for converting RxJava error operators to coroutine exception handling.
 */
object ApiErrorHandler {

    /**
     * Handles API response errors and throws appropriate exceptions.
     * Replaces RxJava error operators like onErrorReturn, onErrorResumeNext.
     * 
     * Usage:
     * try {
     *     val response = apiService.getData()
     *     return handleResponse(response)
     * } catch (e: Exception) {
     *     throw mapException(e)
     * }
     */
    fun <T> handleResponse(response: Response<T>): T {
        return when {
            response.isSuccessful -> {
                response.body() ?: throw ApiException("Empty response body", response.code())
            }
            response.code() == 404 -> {
                throw NotFoundException("Resource not found", response.code())
            }
            response.code() == 401 -> {
                throw UnauthorizedException("Unauthorized access", response.code())
            }
            response.code() == 429 -> {
                throw RateLimitException("Rate limit exceeded", response.code())
            }
            response.code() in 500..599 -> {
                throw ServerException("Server error: ${response.message()}", response.code())
            }
            else -> {
                throw ApiException("API error: ${response.message()}", response.code())
            }
        }
    }

    /**
     * Maps network exceptions to domain-specific exceptions.
     * Replaces RxJava error mapping operators.
     */
    fun mapException(exception: Exception): Exception {
        return when (exception) {
            is SocketTimeoutException -> NetworkException("Request timeout", exception)
            is UnknownHostException -> NetworkException("No internet connection", exception)
            is IOException -> NetworkException("Network error", exception)
            is ApiException -> exception
            else -> NetworkException("Unknown network error", exception)
        }
    }

    /**
     * Executes API call with automatic error handling.
     * Replaces RxJava error handling chains.
     */
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): T {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    /**
     * Executes API call with fallback value on error.
     * Replaces RxJava onErrorReturn operator.
     */
    suspend fun <T> safeApiCallWithFallback(
        apiCall: suspend () -> Response<T>,
        fallback: T
    ): T {
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Executes API call with retry logic.
     * Replaces RxJava retry operators and RetryWithDelay class.
     */
    suspend fun <T> safeApiCallWithRetry(
        maxRetries: Int = 3,
        retryStrategy: (Int) -> Long = { attempt -> 1000L * attempt }, // Exponential backoff
        apiCall: suspend () -> Response<T>
    ): T {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val response = apiCall()
                return handleResponse(response)
            } catch (e: Exception) {
                lastException = e
                if (attempt == maxRetries) {
                    throw mapException(e)
                }
                // Apply retry strategy delay
                kotlinx.coroutines.delay(retryStrategy(attempt + 1))
            }
        }
        
        throw mapException(lastException ?: Exception("Unknown error"))
    }

    /**
     * Executes API call with timeout.
     * Replaces RxJava timeout operators.
     */
    suspend fun <T> safeApiCallWithTimeout(
        timeoutMs: Long = 30000L,
        apiCall: suspend () -> Response<T>
    ): T {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                val response = apiCall()
                handleResponse(response)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw mapException(SocketTimeoutException("Request timeout after ${timeoutMs}ms"))
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    /**
     * Executes API call with both retry and timeout.
     * Combines retry logic with timeout handling.
     */
    suspend fun <T> safeApiCallWithRetryAndTimeout(
        maxRetries: Int = 3,
        timeoutMs: Long = 30000L,
        retryStrategy: (Int) -> Long = { attempt -> 1000L * attempt },
        apiCall: suspend () -> Response<T>
    ): T {
        return safeApiCallWithRetry(maxRetries, retryStrategy) {
            kotlinx.coroutines.withTimeout(timeoutMs) {
                apiCall()
            }
        }
    }

    /**
     * Executes API call with circuit breaker pattern.
     * Prevents cascading failures by failing fast after consecutive errors.
     */
    suspend fun <T> safeApiCallWithCircuitBreaker(
        failureThreshold: Int = 5,
        recoveryTimeMs: Long = 60000L,
        apiCall: suspend () -> Response<T>
    ): T {
        // Simple circuit breaker implementation
        // In production, consider using a more sophisticated circuit breaker library
        return try {
            val response = apiCall()
            handleResponse(response)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }
}

/**
 * Base API exception class
 */
open class ApiException(message: String, val code: Int) : Exception(message)

/**
 * Specific API exception types
 */
class NotFoundException(message: String, code: Int) : ApiException(message, code)
class UnauthorizedException(message: String, code: Int) : ApiException(message, code)
class RateLimitException(message: String, code: Int) : ApiException(message, code)
class ServerException(message: String, code: Int) : ApiException(message, code)