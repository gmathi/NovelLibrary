package io.github.gmathi.novellibrary.model

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.Source

/**
 * Sealed class representing different UI states for the Search screen.
 * Extends the base UiState pattern with search-specific functionality.
 */
sealed class SearchUiState {
    
    /**
     * Represents the initial state when no search has been performed.
     */
    object Initial : SearchUiState()
    
    /**
     * Represents a loading state during search.
     */
    object Loading : SearchUiState()
    
    /**
     * Represents a successful search state with results.
     * 
     * @param searchTerm The search term used
     * @param sources List of sources being searched
     * @param searchHistory List of previous search terms
     * @param searchMode Whether currently in search mode or browsing mode
     */
    data class Success(
        val searchTerm: String? = null,
        val sources: List<Source> = emptyList(),
        val searchHistory: List<String> = emptyList(),
        val searchMode: Boolean = false
    ) : SearchUiState()
    
    /**
     * Represents an error state during search.
     * 
     * @param message The error message
     * @param throwable Optional throwable for detailed error information
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : SearchUiState()
    
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
     * Check if currently in search mode.
     */
    val isSearchMode: Boolean
        get() = if (this is Success) searchMode else false
    
    /**
     * Get the current search term if available.
     */
    val searchTermOrNull: String?
        get() = if (this is Success) searchTerm else null
    
    /**
     * Get the error message if the state is an error, null otherwise.
     */
    val errorOrNull: String?
        get() = if (this is Error) message else null
}