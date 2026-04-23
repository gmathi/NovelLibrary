package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.source.online.NovelUpdatesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.gmathi.novellibrary.BuildConfig
import uy.kohesive.injekt.injectLazy

sealed class SearchUrlUiState {
    object Loading : SearchUrlUiState()
    data class Success(val novels: List<Novel>, val hasMore: Boolean) : SearchUrlUiState()
    data class Error(val message: String, val isCloudflare: Boolean = false, val cloudflareUrl: String? = null) : SearchUrlUiState()
    object NoInternet : SearchUrlUiState()
    object Empty : SearchUrlUiState()
}

class SearchUrlViewModel : ViewModel() {

    companion object {
        /**
         * Dev flag: skip popular novels API calls to reduce network usage during development.
         * Only effective in debug builds.
         */
        private const val SKIP_POPULAR_NOVELS_FETCH = false
    }

    private val networkHelper: NetworkHelper by injectLazy()
    
    private val _uiState = MutableStateFlow<SearchUrlUiState>(SearchUrlUiState.Loading)
    val uiState: StateFlow<SearchUrlUiState> = _uiState.asStateFlow()
    
    private val _novels = MutableStateFlow<List<Novel>>(emptyList())
    val novels: StateFlow<List<Novel>> = _novels.asStateFlow()
    
    private var currentPage = 1
    private var isLoadingMore = false
    private var rank: String? = null
    private var url: String? = null

    fun initialize(rank: String?, url: String?) {
        this.rank = rank
        this.url = url

        // Skip API calls in debug builds when dev flag is enabled
        if (BuildConfig.DEBUG && SKIP_POPULAR_NOVELS_FETCH) {
            _novels.value = emptyList()
            _uiState.value = SearchUrlUiState.Empty
            return
        }

        // Only NovelUpdates URLs are supported for genre/rank browsing.
        // Other sources (e.g. libread, novelfull) use different HTML structures
        // that NovelUpdatesSource cannot parse.
        if (rank == null && url != null && !url.contains(HostNames.NOVEL_UPDATES)) {
            _novels.value = emptyList()
            _uiState.value = SearchUrlUiState.Error(
                message = "Browsing by genre is only supported for Novel Updates sources."
            )
            return
        }

        loadNovels(reset = true)
    }

    fun loadNovels(reset: Boolean = false) {
        if (reset) {
            currentPage = 1
            _novels.value = emptyList()
        }
        
        if (isLoadingMore) return
        isLoadingMore = true
        
        viewModelScope.launch {
            try {
                if (!networkHelper.isConnectedToNetwork()) {
                    _uiState.value = SearchUrlUiState.NoInternet
                    isLoadingMore = false
                    return@launch
                }
                
                if (reset) {
                    _uiState.value = SearchUrlUiState.Loading
                }
                
                val novelsPage = withContext(Dispatchers.IO) {
                    NovelUpdatesSource().getPopularNovels(rank, url, currentPage)
                }
                
                val updatedNovels = if (reset) {
                    novelsPage.novels
                } else {
                    val existingUrls = _novels.value.map { it.url }.toSet()
                    _novels.value + novelsPage.novels.filter { it.url !in existingUrls }
                }
                
                _novels.value = updatedNovels
                
                _uiState.value = when {
                    updatedNovels.isEmpty() -> SearchUrlUiState.Empty
                    else -> SearchUrlUiState.Success(updatedNovels, novelsPage.hasNextPage)
                }
                
                isLoadingMore = false
                
            } catch (e: Exception) {
                val isCloudflare = e.localizedMessage?.contains("503") == true || 
                                  e.localizedMessage?.contains("403") == true ||
                                  e.localizedMessage?.contains("cloudflare", ignoreCase = true) == true
                _uiState.value = SearchUrlUiState.Error(
                    message = e.localizedMessage ?: "Connection error",
                    isCloudflare = isCloudflare,
                    cloudflareUrl = if (isCloudflare) url ?: "https://${io.github.gmathi.novellibrary.network.HostNames.NOVEL_UPDATES}" else null
                )
                isLoadingMore = false
            }
        }
    }

    fun loadMore() {
        if (_uiState.value is SearchUrlUiState.Success && 
            (_uiState.value as SearchUrlUiState.Success).hasMore) {
            currentPage++
            loadNovels(reset = false)
        }
    }

    fun retry() {
        loadNovels(reset = true)
    }
}
