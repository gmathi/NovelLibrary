package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.model.SearchUiState
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.model.source.getPreferenceKey

import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.lang.addToNovelSearchHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for SearchFragment that manages search state and operations.
 * Follows the established architecture patterns with Hilt injection and SearchUiState management.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val dataCenter: DataCenter,
    private val sourceManager: SourceManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "SearchViewModel"
    }

    // UI State for search
    private val _uiState = MutableLiveData<SearchUiState>(SearchUiState.Initial)
    val uiState: LiveData<SearchUiState> = _uiState

    override fun getTag(): String = TAG

    /**
     * Initialize the search screen with available sources and search history
     */
    fun initializeSearch() {
        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                val searchHistory = withContext(Dispatchers.IO) {
                    dataCenter.loadNovelSearchHistory()
                }
                val enabledSources = getEnabledSources()
                
                _uiState.value = SearchUiState.Success(
                    searchHistory = searchHistory,
                    sources = enabledSources,
                    searchMode = false
                )
            } catch (e: Exception) {
                Logs.error(TAG, "Error initializing search", e)
                _uiState.value = SearchUiState.Error(e.message ?: "Failed to initialize search")
            }
        }
    }

    /**
     * Perform a search with the given term
     */
    fun searchNovels(searchTerm: String) {
        if (searchTerm.isBlank()) {
            exitSearchMode()
            return
        }

        viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            try {
                // Add to search history
                withContext(Dispatchers.IO) {
                    searchTerm.addToNovelSearchHistory(dataCenter)
                }
                
                val enabledSources = getEnabledSources()
                val updatedSearchHistory = withContext(Dispatchers.IO) {
                    dataCenter.loadNovelSearchHistory()
                }
                
                _uiState.value = SearchUiState.Success(
                    searchTerm = searchTerm,
                    sources = enabledSources,
                    searchHistory = updatedSearchHistory,
                    searchMode = true
                )
            } catch (e: Exception) {
                Logs.error(TAG, "Error performing search", e)
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    /**
     * Exit search mode and return to browse mode
     */
    fun exitSearchMode() {
        val currentState = _uiState.value
        if (currentState is SearchUiState.Success) {
            _uiState.value = currentState.copy(
                searchTerm = null,
                searchMode = false
            )
        } else {
            initializeSearch()
        }
    }

    /**
     * Get the list of enabled sources for searching
     */
    private suspend fun getEnabledSources(): List<Source> {
        return withContext(Dispatchers.IO) {
            val allOnlineSources = sourceManager.getOnlineSources()
            allOnlineSources.filter { source ->
                dataCenter.isSourceEnabled(source.getPreferenceKey())
            }
        }
    }

    /**
     * Refresh search history
     */
    fun refreshSearchHistory() {
        viewModelScope.launch {
            try {
                val searchHistory = withContext(Dispatchers.IO) {
                    dataCenter.loadNovelSearchHistory()
                }
                val currentState = _uiState.value
                if (currentState is SearchUiState.Success) {
                    _uiState.value = currentState.copy(searchHistory = searchHistory)
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Error refreshing search history", e)
            }
        }
    }

    /**
     * Handle search state restoration from saved instance state
     */
    fun restoreSearchState(searchTerm: String?, isSearchMode: Boolean) {
        if (isSearchMode && !searchTerm.isNullOrBlank()) {
            searchNovels(searchTerm)
        } else {
            initializeSearch()
        }
    }
}