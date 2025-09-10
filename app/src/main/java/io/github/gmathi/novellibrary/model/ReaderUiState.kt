package io.github.gmathi.novellibrary.model

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage

/**
 * UI state for the Reader screen
 */
sealed class ReaderUiState {
    object Loading : ReaderUiState()
    
    data class Success(
        val novel: Novel,
        val webPages: List<WebPage>,
        val currentPageIndex: Int = 0,
        val isRefreshing: Boolean = false
    ) : ReaderUiState()
    
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ReaderUiState()
}