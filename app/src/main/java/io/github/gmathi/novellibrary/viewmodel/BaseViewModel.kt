package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import io.github.gmathi.novellibrary.util.Logs

/**
 * Base ViewModel class that provides common functionality for all ViewModels in the app.
 * 
 * Features:
 * - Common error handling with CoroutineExceptionHandler
 * - Loading state management
 * - Safe coroutine launching with error handling
 * - Consistent logging patterns
 * 
 * Usage:
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val repository: MyRepository
 * ) : BaseViewModel() {
 *     
 *     fun loadData() {
 *         launchSafely {
 *             setLoading(true)
 *             val data = repository.getData()
 *             // Handle success
 *             setLoading(false)
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel : ViewModel() {

    companion object {
        private const val TAG = "BaseViewModel"
    }

    // Common loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Common error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Logs.error(getTag(), "Coroutine exception occurred", exception)
        handleError(exception)
    }

    /**
     * Get the tag for logging. Override in subclasses to provide specific tags.
     */
    protected open fun getTag(): String = this::class.java.simpleName

    /**
     * Launch a coroutine with safe error handling.
     * 
     * @param block The coroutine block to execute
     */
    protected fun launchSafely(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(exceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                Logs.error(getTag(), "Exception in launchSafely", e)
                handleError(e)
            }
        }
    }

    /**
     * Set the loading state.
     * 
     * @param loading True if loading, false otherwise
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Set an error message.
     * 
     * @param message The error message to set
     */
    protected fun setError(message: String?) {
        _error.value = message
    }

    /**
     * Clear the current error.
     */
    protected fun clearError() {
        _error.value = null
    }

    /**
     * Handle errors that occur in the ViewModel.
     * Override in subclasses to provide custom error handling.
     * 
     * @param throwable The error that occurred
     */
    protected open fun handleError(throwable: Throwable) {
        setLoading(false)
        val errorMessage = throwable.message ?: "An unknown error occurred"
        setError(errorMessage)
        Logs.error(getTag(), "Error handled: $errorMessage", throwable)
    }

    /**
     * Execute a block with loading state management.
     * Automatically sets loading to true at start and false at end.
     * 
     * @param block The block to execute
     */
    protected fun executeWithLoading(block: suspend CoroutineScope.() -> Unit) {
        launchSafely {
            setLoading(true)
            try {
                block()
            } finally {
                setLoading(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Logs.debug(getTag(), "ViewModel cleared")
    }
}