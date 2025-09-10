package io.github.gmathi.novellibrary.model

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings

/**
 * UI state for chapters screen following the established UiState pattern.
 * 
 * Represents different states of the chapters loading and display process.
 */
sealed class ChaptersUiState {
    
    /**
     * Initial loading state when fetching chapters.
     */
    object Loading : ChaptersUiState()
    
    /**
     * Successful state with chapters data.
     * 
     * @param novel The novel for which chapters are displayed
     * @param chapters List of chapters/web pages
     * @param chapterSettings Settings for each chapter (read status, download status, etc.)
     * @param showSources Whether to show chapters grouped by translator sources
     * @param translatorSources List of available translator sources
     * @param isRefreshing Whether the data is being refreshed
     */
    data class Success(
        val novel: Novel,
        val chapters: List<WebPage>,
        val chapterSettings: List<WebPageSettings>,
        val showSources: Boolean = false,
        val translatorSources: List<String> = emptyList(),
        val isRefreshing: Boolean = false
    ) : ChaptersUiState() {
        
        /**
         * Get chapters filtered by translator source.
         */
        fun getChaptersForSource(sourceName: String): List<WebPage> {
            return if (sourceName == "All Translator Sources") {
                chapters
            } else {
                chapters.filter { it.translatorSourceName == sourceName }
            }
        }
        
        /**
         * Get chapter settings for a specific URL.
         */
        fun getSettingsForChapter(url: String): WebPageSettings? {
            return chapterSettings.firstOrNull { it.url == url }
        }
        
        /**
         * Check if any chapters are available.
         */
        val hasChapters: Boolean
            get() = chapters.isNotEmpty()
        
        /**
         * Get the total number of chapters.
         */
        val chaptersCount: Int
            get() = chapters.size
    }
    
    /**
     * Error state when chapters loading fails.
     * 
     * @param message Error message to display
     * @param throwable Optional throwable for detailed error information
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : ChaptersUiState()
    
    /**
     * Empty state when no chapters are found.
     * 
     * @param message Optional message to display
     */
    data class Empty(
        val message: String = "No chapters found"
    ) : ChaptersUiState()
    
    /**
     * No internet connection state.
     */
    object NoInternet : ChaptersUiState()
    
    /**
     * Network error state.
     */
    object NetworkError : ChaptersUiState()
}

/**
 * Extension functions for ChaptersUiState handling.
 */
inline fun ChaptersUiState.handle(
    onLoading: () -> Unit = {},
    onSuccess: (ChaptersUiState.Success) -> Unit = {},
    onError: (String, Throwable?) -> Unit = { _, _ -> },
    onEmpty: (String) -> Unit = {},
    onNoInternet: () -> Unit = {},
    onNetworkError: () -> Unit = {}
) {
    when (this) {
        is ChaptersUiState.Loading -> onLoading()
        is ChaptersUiState.Success -> onSuccess(this)
        is ChaptersUiState.Error -> onError(message, throwable)
        is ChaptersUiState.Empty -> onEmpty(message)
        is ChaptersUiState.NoInternet -> onNoInternet()
        is ChaptersUiState.NetworkError -> onNetworkError()
    }
}