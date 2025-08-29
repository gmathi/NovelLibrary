package io.github.gmathi.novellibrary.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.util.Logs
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles network errors and provides user-friendly error messages
 */
@Singleton
class NetworkErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Convert network exceptions to user-friendly error messages
     */
    fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is InterruptedIOException -> {
                if (exception.message?.contains("timeout") == true) {
                    context.getString(R.string.error_network_timeout)
                } else {
                    context.getString(R.string.error_network_interrupted)
                }
            }
            is SocketTimeoutException -> context.getString(R.string.error_network_timeout)
            is ConnectException -> context.getString(R.string.error_network_connection_failed)
            is UnknownHostException -> context.getString(R.string.error_network_no_internet)
            else -> {
                Logs.warning("NetworkErrorHandler", "Unhandled network error: ${exception.javaClass.simpleName}: ${exception.message}")
                exception.message ?: context.getString(R.string.error_network_unknown)
            }
        }
    }
    
    /**
     * Check if an error is retryable
     */
    fun isRetryableError(exception: Throwable): Boolean {
        return when (exception) {
            is InterruptedIOException -> !exception.message?.contains("Canceled", ignoreCase = true).orElse(false)
            is SocketTimeoutException -> true
            is ConnectException -> true
            is UnknownHostException -> false // DNS issues usually aren't retryable immediately
            else -> false
        }
    }
    
    /**
     * Get suggested retry delay based on error type
     */
    fun getRetryDelay(exception: Throwable, attempt: Int): Long {
        val baseDelay = when (exception) {
            is InterruptedIOException, is SocketTimeoutException -> 2000L // Longer delay for timeouts
            is ConnectException -> 1000L
            else -> 1000L
        }
        
        // Exponential backoff with jitter
        return baseDelay * (attempt + 1) + (Math.random() * 500).toLong()
    }
    
    /**
     * Log network error with appropriate level
     */
    fun logError(url: String, exception: Throwable, attempt: Int, maxRetries: Int) {
        val message = "Network error for $url (attempt $attempt/$maxRetries): ${exception.message}"
        
        when {
            attempt < maxRetries && isRetryableError(exception) -> {
                Logs.warning("NetworkErrorHandler", message)
            }
            else -> {
                Logs.error("NetworkErrorHandler", message, exception)
            }
        }
    }
}

private fun Boolean?.orElse(default: Boolean): Boolean = this ?: default