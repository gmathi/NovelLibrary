package io.github.gmathi.novellibrary.compose.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.github.gmathi.novellibrary.viewmodel.SearchUrlViewModel
import io.github.gmathi.novellibrary.viewmodel.SearchUrlUiState
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

/**
 * Main search screen composable - uses SearchDemoActivity composables for UI
 */
@Composable
fun SearchScreen(
    searchHistory: List<String>,
    onHomeClick: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchExit: () -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner
) {
    var searchResults by remember { mutableStateOf<List<DbNovel>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // ViewModels for each tab - create separate instances
    val viewModelProvider = remember { ViewModelProvider(viewModelStoreOwner) }
    val popularMonthViewModel = remember { viewModelProvider[SearchUrlViewModel::class.java] }
    
    // Initialize ViewModel with rank based on selected tab
    LaunchedEffect(selectedTab) {
        val rank = when (selectedTab) {
            0 -> "popmonth"
            1 -> "popular"
            else -> "sixmonths"
        }
        popularMonthViewModel.initialize(rank, null)
    }
    
    val uiState by popularMonthViewModel.uiState.collectAsState()
    val novels by popularMonthViewModel.novels.collectAsState()

    val searchState = rememberPersistentSearchState(initialLogoText = "Search Novels")
    val suggestionBuilder = remember(searchHistory) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    // Use search results if available, otherwise use novels from ViewModel
    val displayNovels = remember(novels, searchResults) {
        if (searchResults.isEmpty()) novels else searchResults
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search View
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search novels...",
                homeButtonMode = HomeButtonMode.Burger,
                onHomeButtonClick = onHomeClick,
                onSearch = { query ->
                    onSearch(query)
                    searchResults = novels.filter { novel ->
                        novel.name?.contains(query, ignoreCase = true) == true ||
                        novel.genres?.any { it.contains(query, ignoreCase = true) } == true
                    }
                },
                onSearchTermChanged = { term ->
                    if (term.isNotEmpty()) {
                        searchResults = novels.filter { novel ->
                            novel.name?.contains(term, ignoreCase = true) == true ||
                            novel.genres?.any { it.contains(term, ignoreCase = true) } == true
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                onSearchCleared = {
                    searchResults = emptyList()
                },
                onSearchExit = {
                    searchResults = emptyList()
                    onSearchExit()
                },
                suggestionBuilder = suggestionBuilder,
                elevation = 4
            )

            // Tabs
            SearchTabs(
                selectedTab = selectedTab,
                onTabSelected = { 
                    selectedTab = it
                    searchResults = emptyList()
                }
            )

            // Results Header
            ResultsHeader(
                count = displayNovels.size,
                isSearching = searchState.isSearching || searchResults.isNotEmpty()
            )

            // Content based on UI state
            when (uiState) {
                is SearchUrlUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUrlUiState.Error -> {
                    val error = uiState as SearchUrlUiState.Error
                    ErrorState(
                        message = error.message,
                        isCloudflare = error.isCloudflare,
                        onRetry = { popularMonthViewModel.retry() }
                    )
                }
                is SearchUrlUiState.NoInternet -> {
                    ErrorState(
                        message = "No internet connection",
                        isCloudflare = false,
                        onRetry = { popularMonthViewModel.retry() }
                    )
                }
                is SearchUrlUiState.Empty -> {
                    EmptyState(
                        message = "No novels found",
                        icon = Icons.Filled.SearchOff
                    )
                }
                is SearchUrlUiState.Success -> {
                    if (displayNovels.isNotEmpty()) {
                        val success = uiState as SearchUrlUiState.Success
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                items = displayNovels,
                                key = { index, novel -> 
                                    "novel_${index}_${novel.id}_${novel.name?.hashCode() ?: 0}"
                                }
                            ) { _, novel ->
                                SearchResultsNovelItem(novel = novel)
                            }
                            
                            if (success.hasMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Button(onClick = { popularMonthViewModel.loadMore() }) {
                                            Text("Load More")
                                        }
                                    }
                                }
                            }
                        }
                    } else if (searchResults.isEmpty() && searchState.searchText.isEmpty()) {
                        WelcomeContent()
                    } else {
                        EmptyState(
                            message = "No novels found",
                            icon = Icons.Filled.SearchOff
                        )
                    }
                }
            }
        }

        // Background tint overlay
        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}

@Composable
fun SearchTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("Popular Now", "Best Rated", "Active Now")
    
    TabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            )
        }
    }
}




@Composable
private fun ErrorState(
    message: String,
    isCloudflare: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = if (isCloudflare) "Cloudflare Error" else "Connection Error",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}
