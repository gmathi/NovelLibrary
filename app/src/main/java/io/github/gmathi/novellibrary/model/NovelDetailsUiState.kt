package io.github.gmathi.novellibrary.model

import io.github.gmathi.novellibrary.model.database.Novel

/**
 * UI state for NovelDetailsFragment following the established UiState pattern.
 * 
 * Usage:
 * ```kotlin
 * private val _uiState = MutableLiveData<NovelDetailsUiState>()
 * val uiState: LiveData<NovelDetailsUiState> = _uiState
 * 
 * fun loadNovelDetails(novelId: Long) {
 *     _uiState.value = NovelDetailsUiState.Loading
 *     try {
 *         val novel = repository.getNovelDetails(novelId)
 *         _uiState.value = NovelDetailsUiState.Success(novel)
 *     } catch (e: Exception) {
 *         _uiState.value = NovelDetailsUiState.Error(e.message ?: "Unknown error")
 *     }
 * }
 * ```
 */
sealed class NovelDetailsUiState {
    
    /**
     * Loading state when fetching novel details
     */
    object Loading : NovelDetailsUiState()
    
    /**
     * Success state with novel details data
     * 
     * @param novel The novel with complete details
     * @param isInLibrary Whether the novel is in user's library
     * @param isRefreshing Whether the data is being refreshed
     */
    data class Success(
        val novel: Novel,
        val isInLibrary: Boolean = false,
        val isRefreshing: Boolean = false
    ) : NovelDetailsUiState()
    
    /**
     * Error state when loading fails
     * 
     * @param message The error message to display
     * @param throwable Optional throwable for detailed error information
     * @param canRetry Whether the user can retry the operation
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : NovelDetailsUiState()
    
    /**
     * Check if the current state is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if the current state is successful
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Check if the current state is an error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Get the novel if the state is successful, null otherwise
     */
    val novelOrNull: Novel?
        get() = if (this is Success) novel else null
    
    /**
     * Get the error message if the state is an error, null otherwise
     */
    val errorOrNull: String?
        get() = if (this is Error) message else null
}