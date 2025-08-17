package io.github.gmathi.novellibrary.util.coroutines

import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utilities for coroutines.
 * Provides structured exception handling patterns and logging.
 */
object CoroutineErrorHandler {
    
    /**
     * Global exception handler for unhandled coroutine exceptions.
     * Logs errors and prevents app crashes from background operations.
     */
    val globalExceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logs.error("CoroutineErrorHandler", "Unhandled coroutine exception", exception)
        
        when (exception) {
            is NetworkException -> handleNetworkError(exception)
            is DatabaseException -> handleDatabaseError(exception)
            else -> handleGenericError(exception)
        }
    }
    
    /**
     * Executes a suspend function with proper error handling.
     * Returns a Result object containing success value or exception.
     */
    suspend inline fun <T> safeCall(
        crossinline action: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(action())
        } catch (exception: Exception) {
            Logs.error("CoroutineErrorHandler", "Safe call failed", exception)
            Result.failure(mapException(exception))
        }
    }
    
    /**
     * Executes a coroutine with error handling in the given scope.
     * Catches and handles exceptions without crashing the app.
     */
    inline fun CoroutineScope.launchSafely(
        crossinline onError: (Exception) -> Unit = { },
        crossinline action: suspend () -> Unit
    ) {
        launch(globalExceptionHandler) {
            try {
                action()
            } catch (exception: Exception) {
                Logs.error("CoroutineErrorHandler", "Launch safely failed", exception)
                onError(mapException(exception))
            }
        }
    }
    
    /**
     * Maps common exceptions to domain-specific exceptions.
     */
    fun mapException(exception: Exception): Exception {
        return when (exception) {
            is SocketTimeoutException, is UnknownHostException -> 
                NetworkException("Network connection failed", exception)
            is IOException -> 
                if (exception.message?.contains("database", ignoreCase = true) == true) {
                    DatabaseException("Database operation failed", exception)
                } else {
                    NetworkException("I/O operation failed", exception)
                }
            else -> exception
        }
    }
    
    private fun handleNetworkError(exception: NetworkException) {
        Logs.error("CoroutineErrorHandler", "Network error: ${exception.message}")
        // Additional network error handling can be added here
    }
    
    private fun handleDatabaseError(exception: DatabaseException) {
        Logs.error("CoroutineErrorHandler", "Database error: ${exception.message}")
        // Additional database error handling can be added here
    }
    
    private fun handleGenericError(exception: Throwable) {
        Logs.error("CoroutineErrorHandler", "Generic error: ${exception.message}")
        // Additional generic error handling can be added here
    }
}

/**
 * Custom exception for network-related errors.
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Custom exception for database-related errors.
 */
class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)