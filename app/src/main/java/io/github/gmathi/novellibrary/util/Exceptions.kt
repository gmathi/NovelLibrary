package io.github.gmathi.novellibrary.util

import android.content.Context
import io.github.gmathi.novellibrary.R

object Exceptions {

    const val INVALID_NOVEL = "Invalid Novel"
    const val MISSING_IMPLEMENTATION = "Missing Implementation"
    const val INVALID_PARAMETERS = "Invalid Parameters"
    const val MISSING_EXTERNAL_ID = "Missing External Id"
    const val NETWORK_ERROR = "Network Error"
    const val PARSING_ERROR = "Parsing Error"
    const val MISSING_SOURCE_ID = "Missing Source Id"

    /**
     * Parses an exception and returns a user-friendly error message
     * @param exception The exception to parse
     * @param context Context to access string resources
     * @return User-friendly error message
     */
    fun getErrorMessage(exception: Exception, context: Context): String {
        val message = exception.message?.lowercase() ?: ""
        return when {
            message.contains("503") || message.contains("cloudflare") ->
                context.getString(R.string.connection_error_cloudflare)
            exception.message?.contains("404") == true -> 
                "Endpoint not found. The website may have changed their functionality."
            exception.message?.contains("403") == true -> 
                "Access denied. The website may be blocking requests."
            exception.message?.contains("500") == true -> 
                "Server error. Please try again later."
            exception.message?.contains("timeout") == true -> 
                "Request timed out. Please check your connection."
            exception.message?.contains("SSL") == true || exception.message?.contains("certificate") == true -> 
                "SSL/TLS error. The website's security certificate may have changed."
            exception.message?.contains("DNS") == true -> 
                "DNS resolution failed. Please check your internet connection."
            exception.message?.contains("connection") == true -> 
                "Connection failed. Please check your internet connection."
            exception.message?.contains("host") == true -> 
                "Host unreachable. The website may be down or the URL has changed."
            else -> context.getString(R.string.connection_error)
        }
    }
}