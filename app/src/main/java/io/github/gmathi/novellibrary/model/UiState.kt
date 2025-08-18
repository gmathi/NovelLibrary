package io.github.gmathi.novellibrary.model

/**
 * Sealed class representing different UI states for consistent state management across the app.
 * 
 * Usage:
 * ```kotlin
 * private val _uiState = MutableLiveData<UiState<List<Novel>>>()
 * val uiState: LiveData<UiState<List<Novel>>> = _uiState
 * 
 * fun loadNovels() {
 *     _uiState.value = UiState.Loading
 *     try {
 *         val novels = repository.getNovels()
 *         _uiState.value = UiState.Success(novels)
 *     } catch (e: Exception) {
 *         _uiState.value = UiState.Error(e.message ?: "Unknown error")
 *     }
 * }
 * ```
 * 
 * In Fragment:
 * ```kotlin
 * viewModel.uiState.observe(viewLifecycleOwner) { state ->
 *     when (state) {
 *         is UiState.Loading -> showLoading()
 *         is UiState.Success -> showContent(state.data)
 *         is UiState.Error -> showError(state.message)
 *     }
 * }
 * ```
 */
sealed class UiState<out T> {
    
    /**
     * Represents a loading state.
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Represents a successful state with data.
     * 
     * @param data The successful result data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Represents an error state.
     * 
     * @param message The error message
     * @param throwable Optional throwable for detailed error information
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : UiState<Nothing>()
    
    /**
     * Check if the current state is loading.
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if the current state is successful.
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the current state is an error.
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Get the data if the state is successful, null otherwise.
     */
    val dataOrNull: T?
        get() = if (this is Success) data else null
    
    /**
     * Get the error message if the state is an error, null otherwise.
     */
    val errorOrNull: String?
        get() = if (this is Error) message else null
}

/**
 * Extension function to map the data of a successful UiState.
 * 
 * @param transform Function to transform the data
 * @return New UiState with transformed data or the same state if not successful
 */
inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> {
    return when (this) {
        is UiState.Loading -> UiState.Loading
        is UiState.Success -> UiState.Success(transform(data))
        is UiState.Error -> UiState.Error(message, throwable)
    }
}

/**
 * Extension function to handle each state with specific actions.
 * 
 * @param onLoading Action to perform when loading
 * @param onSuccess Action to perform when successful
 * @param onError Action to perform when error occurs
 */
inline fun <T> UiState<T>.handle(
    onLoading: () -> Unit = {},
    onSuccess: (T) -> Unit = {},
    onError: (String, Throwable?) -> Unit = { _, _ -> }
) {
    when (this) {
        is UiState.Loading -> onLoading()
        is UiState.Success -> onSuccess(data)
        is UiState.Error -> onError(message, throwable)
    }
}