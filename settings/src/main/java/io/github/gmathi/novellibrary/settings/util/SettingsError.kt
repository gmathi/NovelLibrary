package io.github.gmathi.novellibrary.settings.util

/**
 * Common error types for settings operations.
 * 
 * Defines a sealed hierarchy of errors that can occur during settings
 * operations, providing type-safe error handling.
 */
sealed class SettingsError(
    open val message: String,
    open val cause: Throwable? = null
) {
    
    /**
     * Error reading a setting value from storage.
     */
    data class ReadError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Error writing a setting value to storage.
     */
    data class WriteError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Error validating a setting value.
     */
    data class ValidationError(
        override val message: String,
        val fieldName: String,
        val invalidValue: Any?
    ) : SettingsError(message)
    
    /**
     * Error during backup operation.
     */
    data class BackupError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Error during restore operation.
     */
    data class RestoreError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Error during sync operation.
     */
    data class SyncError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Network error during remote operations.
     */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
    
    /**
     * Unknown or unexpected error.
     */
    data class UnknownError(
        override val message: String,
        override val cause: Throwable? = null
    ) : SettingsError(message, cause)
}

/**
 * Result type for settings operations that can fail.
 * 
 * Provides a type-safe way to handle success and failure cases.
 */
sealed class SettingsResult<T> {
    data class Success<T>(val value: T) : SettingsResult<T>()
    data class Failure<T>(val error: SettingsError) : SettingsResult<T>()
    
    /**
     * Returns the value if successful, or null if failed.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }
    
    /**
     * Returns the value if successful, or the default value if failed.
     */
    fun getOrDefault(default: T): T = when (this) {
        is Success -> value
        is Failure -> default
    }
    
    /**
     * Returns the value if successful, or throws the error if failed.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw error.cause ?: Exception(error.message)
    }
    
    /**
     * Executes the given block if the result is successful.
     */
    inline fun onSuccess(block: (T) -> Unit): SettingsResult<T> {
        if (this is Success) block(value)
        return this
    }
    
    /**
     * Executes the given block if the result is a failure.
     */
    inline fun onFailure(block: (SettingsError) -> Unit): SettingsResult<T> {
        if (this is Failure) block(error)
        return this
    }
}

/**
 * Common error handlers for settings operations.
 * 
 * Provides reusable error handling logic to ensure consistent error
 * handling across all settings screens.
 */
object SettingsErrorHandler {
    
    /**
     * Handles a settings error by logging and returning a user-friendly message.
     * 
     * @param error The error to handle
     * @return User-friendly error message
     */
    fun handleError(error: SettingsError): String {
        // Log the error (in production, this would use a proper logging framework)
        logError(error)
        
        // Return user-friendly message
        return when (error) {
            is SettingsError.ReadError -> "Failed to read setting. Please try again."
            is SettingsError.WriteError -> "Failed to save setting. Please try again."
            is SettingsError.ValidationError -> "Invalid value for ${error.fieldName}. ${error.message}"
            is SettingsError.BackupError -> "Backup failed. ${error.message}"
            is SettingsError.RestoreError -> "Restore failed. ${error.message}"
            is SettingsError.SyncError -> "Sync failed. Please check your connection and try again."
            is SettingsError.NetworkError -> "Network error. Please check your connection."
            is SettingsError.UnknownError -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Wraps a suspend function with error handling.
     * 
     * @param block Suspend function to execute
     * @return Result containing the value or error
     */
    suspend fun <T> withErrorHandling(block: suspend () -> T): SettingsResult<T> {
        return try {
            SettingsResult.Success(block())
        } catch (e: Exception) {
            SettingsResult.Failure(
                SettingsError.UnknownError(
                    message = e.message ?: "Unknown error occurred",
                    cause = e
                )
            )
        }
    }
    
    /**
     * Wraps a regular function with error handling.
     * 
     * @param block Function to execute
     * @return Result containing the value or error
     */
    fun <T> withErrorHandlingSync(block: () -> T): SettingsResult<T> {
        return try {
            SettingsResult.Success(block())
        } catch (e: Exception) {
            SettingsResult.Failure(
                SettingsError.UnknownError(
                    message = e.message ?: "Unknown error occurred",
                    cause = e
                )
            )
        }
    }
    
    /**
     * Logs an error (placeholder for actual logging implementation).
     * 
     * @param error The error to log
     */
    private fun logError(error: SettingsError) {
        // In production, this would use a proper logging framework
        // For now, we'll just print to console
        println("SettingsError: ${error.message}")
        error.cause?.printStackTrace()
    }
    
    /**
     * Creates a validation error for an invalid value.
     * 
     * @param fieldName Name of the field that failed validation
     * @param value The invalid value
     * @param reason Reason why the value is invalid
     * @return ValidationError instance
     */
    fun createValidationError(
        fieldName: String,
        value: Any?,
        reason: String
    ): SettingsError.ValidationError {
        return SettingsError.ValidationError(
            message = reason,
            fieldName = fieldName,
            invalidValue = value
        )
    }
}
