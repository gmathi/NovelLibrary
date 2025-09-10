package io.github.gmathi.novellibrary.model

import io.github.gmathi.novellibrary.model.database.Novel

/**
 * Sealed class representing different UI states for the Library screen.
 * Extends the base UiState pattern with library-specific functionality.
 */
sealed class LibraryUiState {
    
    /**
     * Represents a loading state for the library.
     */
    object Loading : LibraryUiState()
    
    /**
     * Represents a successful state with library data.
     * 
     * @param novels List of novels in the library
     * @param isRefreshing Whether the library is currently being refreshed
     * @param selectedNovels Set of currently selected novels for action mode
     */
    data class Success(
        val novels: List<Novel>,
        val isRefreshing: Boolean = false,
        val selectedNovels: Set<Novel> = emptySet()
    ) : LibraryUiState()
    
    /**
     * Represents an error state for the library.
     * 
     * @param message The error message
     * @param throwable Optional throwable for detailed error information
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : LibraryUiState()
    
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
     * Get the novels if the state is successful, empty list otherwise.
     */
    val novelsOrEmpty: List<Novel>
        get() = if (this is Success) novels else emptyList()
    
    /**
     * Get the selected novels if the state is successful, empty set otherwise.
     */
    val selectedNovelsOrEmpty: Set<Novel>
        get() = if (this is Success) selectedNovels else emptySet()
    
    /**
     * Get the error message if the state is an error, null otherwise.
     */
    val errorOrNull: String?
        get() = if (this is Error) message else null
}