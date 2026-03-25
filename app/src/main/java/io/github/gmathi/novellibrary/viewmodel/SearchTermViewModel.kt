package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.CatalogueSource
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.model.source.getPreferenceKey
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.online.NovelUpdatesSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.gmathi.novellibrary.BuildConfig
import uy.kohesive.injekt.injectLazy

sealed class SearchTermUiState {
    object Idle : SearchTermUiState()
    object Loading : SearchTermUiState()
    data class Success(val hasMore: Boolean) : SearchTermUiState()
    data class Error(val message: String, val isCloudflare: Boolean = false, val cloudflareUrl: String? = null) : SearchTermUiState()
    object NoInternet : SearchTermUiState()
    object Empty : SearchTermUiState()
}

data class SourceSearchState(
    val sourceName: String,
    val sourceId: Long,
    val novels: List<Novel> = emptyList(),
    val uiState: SearchTermUiState = SearchTermUiState.Loading,
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false
)

class SearchTermViewModel : ViewModel() {

    companion object {
        /**
         * Dev flag: skip NovelUpdates search API calls to reduce network usage during development.
         * Only effective in debug builds.
         */
        private const val SKIP_NOVEL_UPDATES_SEARCH = true

        /**
         * When true, search results are fetched lazily — only when the user views a source tab.
         * When false, all sources are fetched immediately on search.
         */
        const val LAZY_LOAD_SOURCES = true
    }

    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()

    private val _sourceStates = MutableStateFlow<List<SourceSearchState>>(emptyList())
    val sourceStates: StateFlow<List<SourceSearchState>> = _sourceStates.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var currentSearchTerm: String? = null

    fun search(searchTerm: String) {
        currentSearchTerm = searchTerm
        _isSearching.value = true

        val sources = sourceManager.getOnlineSources().filter {
            dataCenter.isSourceEnabled(it.getPreferenceKey())
        }

        if (LAZY_LOAD_SOURCES) {
            // Initialize all sources as Idle; only the visible tab will trigger a fetch
            _sourceStates.value = sources.map { source ->
                SourceSearchState(
                    sourceName = source.name,
                    sourceId = source.id,
                    uiState = SearchTermUiState.Idle
                )
            }
            // Fetch only the first source (initially visible tab)
            if (sources.isNotEmpty()) {
                updateSourceState(0) { it.copy(uiState = SearchTermUiState.Loading) }
                searchSource(0, sources[0], searchTerm, page = 1)
            }
        } else {
            // Fetch all sources immediately
            _sourceStates.value = sources.map { source ->
                SourceSearchState(
                    sourceName = source.name,
                    sourceId = source.id
                )
            }
            sources.forEachIndexed { index, source ->
                searchSource(index, source, searchTerm, page = 1)
            }
        }
    }

    /**
     * Fetch results for a specific source tab if it hasn't been fetched yet.
     * Called when the user switches to a tab.
     */
    fun fetchSourceIfNeeded(sourceIndex: Int) {
        val states = _sourceStates.value
        if (sourceIndex !in states.indices) return
        val state = states[sourceIndex]
        // Only fetch if still in Idle state (never fetched)
        if (state.uiState != SearchTermUiState.Idle) return

        val searchTerm = currentSearchTerm ?: return
        val source = sourceManager.getOnlineSources().filter {
            dataCenter.isSourceEnabled(it.getPreferenceKey())
        }.getOrNull(sourceIndex) ?: return

        updateSourceState(sourceIndex) { it.copy(uiState = SearchTermUiState.Loading) }
        searchSource(sourceIndex, source, searchTerm, page = 1)
    }

    private fun searchSource(index: Int, source: HttpSource, searchTerm: String, page: Int) {
        viewModelScope.launch {
            if (!networkHelper.isConnectedToNetwork()) {
                updateSourceState(index) { it.copy(uiState = SearchTermUiState.NoInternet) }
                return@launch
            }

            try {
                val novelsPage = withContext(Dispatchers.IO) {
                    if (source is NovelUpdatesSource) {
                        // Skip NovelUpdates search in debug builds when dev flag is enabled
                        if (BuildConfig.DEBUG && SKIP_NOVEL_UPDATES_SEARCH) {
                            NovelsPage(emptyList(), false)
                        } else {
                            // Use series-finder API for NovelUpdates
                            source.searchSeriesFinder(searchTerm, page)
                        }
                    } else {
                        val catalogueSource = source as? CatalogueSource
                            ?: throw Exception("Source ${source.name} is not a CatalogueSource")
                        catalogueSource.getSearchNovels(page, searchTerm)
                    }
                }

                updateSourceState(index) { state ->
                    val updatedNovels = if (page == 1) {
                        novelsPage.novels
                    } else {
                        val existingUrls = state.novels.map { it.url }.toSet()
                        state.novels + novelsPage.novels.filter { it.url !in existingUrls }
                    }
                    state.copy(
                        novels = updatedNovels,
                        currentPage = page,
                        isLoadingMore = false,
                        uiState = if (updatedNovels.isEmpty()) {
                            SearchTermUiState.Empty
                        } else {
                            SearchTermUiState.Success(hasMore = novelsPage.hasNextPage)
                        }
                    )
                }
            } catch (e: Exception) {
                updateSourceState(index) { state ->
                    if (page == 1) {
                        val isCloudflare = e.localizedMessage?.contains("503") == true ||
                                e.localizedMessage?.contains("403") == true ||
                                e.localizedMessage?.contains("cloudflare", ignoreCase = true) == true
                        val sourceBaseUrl = source.baseUrl
                        state.copy(
                            uiState = SearchTermUiState.Error(
                                message = e.localizedMessage ?: "Search failed",
                                isCloudflare = isCloudflare,
                                cloudflareUrl = if (isCloudflare) sourceBaseUrl else null
                            ),
                            isLoadingMore = false
                        )
                    } else {
                        // Keep existing results on load-more failure
                        state.copy(isLoadingMore = false)
                    }
                }
            }
        }
    }

    fun loadMore(sourceIndex: Int) {
        val states = _sourceStates.value
        if (sourceIndex !in states.indices) return
        val state = states[sourceIndex]
        if (state.isLoadingMore) return
        val uiState = state.uiState
        if (uiState !is SearchTermUiState.Success || !uiState.hasMore) return

        val searchTerm = currentSearchTerm ?: return
        val source = sourceManager.getOnlineSources().filter {
            dataCenter.isSourceEnabled(it.getPreferenceKey())
        }.getOrNull(sourceIndex) ?: return

        updateSourceState(sourceIndex) { it.copy(isLoadingMore = true) }
        searchSource(sourceIndex, source, searchTerm, page = state.currentPage + 1)
    }

    fun retrySource(sourceIndex: Int) {
        val searchTerm = currentSearchTerm ?: return
        val source = sourceManager.getOnlineSources().filter {
            dataCenter.isSourceEnabled(it.getPreferenceKey())
        }.getOrNull(sourceIndex) ?: return

        updateSourceState(sourceIndex) { it.copy(uiState = SearchTermUiState.Loading, novels = emptyList()) }
        searchSource(sourceIndex, source, searchTerm, page = 1)
    }

    fun clearSearch() {
        _isSearching.value = false
        _sourceStates.value = emptyList()
        currentSearchTerm = null
    }

    private fun updateSourceState(index: Int, update: (SourceSearchState) -> SourceSearchState) {
        val current = _sourceStates.value.toMutableList()
        if (index in current.indices) {
            current[index] = update(current[index])
            _sourceStates.value = current
        }
    }
}
