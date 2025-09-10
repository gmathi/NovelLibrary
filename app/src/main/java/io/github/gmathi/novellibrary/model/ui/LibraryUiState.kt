package io.github.gmathi.novellibrary.model.ui

import io.github.gmathi.novellibrary.model.database.Novel

/**
 * UI state specific to the Library screen.
 * This follows the architecture guide pattern for feature-specific UI states.
 * 
 * Usage in ViewModel:
 * ```kotlin
 * private val _libraryUiState = MutableLiveData<LibraryUiState>()
 * val libraryUiState: LiveData<LibraryUiState> = _libraryUiState
 * 
 * fun loadNovels() {
 *     _libraryUiState.value = LibraryUiState.Loading
 *     // ... load data
 *     _libraryUiState.value = LibraryUiState.Success(novels, selectedNovels)
 * }
 * ```
 * 
 * Usage in Fragment:
 * ```kotlin
 * viewModel.libraryUiState.observe(viewLifecycleOwner) { state ->
 *     when (state) {
 *         is LibraryUiState.Loading -> showLoading()
 *         is LibraryUiState.Success -> showContent(state.novels)
 *         is LibraryUiState.Error -> showError(state.message)
 *     }
 * }
 * ```
 */
sealed class LibraryUiState {
    
    /**
     * Loading state for library
     */
    object Loading : LibraryUiState()
    
    /**
     * Success state with library data
     */
    data class Success(
        val novels: List<Novel>,
        val selectedNovels: Set<Novel> = emptySet(),
        val isRefreshing: Boolean = false,
        val isSyncing: Boolean = false,
        val syncProgress: String? = null
    ) : LibraryUiState()
    
    /**
     * Error state with error message
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : LibraryUiState()
    
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
     * Get the novels if the state is successful, empty list otherwise
     */
    val novelsOrEmpty: List<Novel>
        get() = if (this is Success) novels else emptyList()
    
    /**
     * Get the selected novels if the state is successful, empty set otherwise
     */
    val selectedNovelsOrEmpty: Set<Novel>
        get() = if (this is Success) selectedNovels else emptySet()
    
    /**
     * Get the error message if the state is an error, null otherwise
     */
    val errorOrNull: String?
        get() = if (this is Error) message else null
}