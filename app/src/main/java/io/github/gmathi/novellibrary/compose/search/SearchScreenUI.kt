package io.github.gmathi.novellibrary.compose.search

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import io.github.gmathi.novellibrary.activity.CloudflareResolverActivity
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.viewmodel.SearchUrlViewModel
import io.github.gmathi.novellibrary.viewmodel.SearchUrlUiState
import io.github.gmathi.novellibrary.viewmodel.SearchTermViewModel
import io.github.gmathi.novellibrary.viewmodel.SearchTermUiState
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

@Composable
fun SearchScreen(
    searchHistory: List<String>,
    onHomeClick: () -> Unit,
    onSearch: (String) -> Unit,
    onSearchExit: () -> Unit,
    viewModelStoreOwner: ViewModelStoreOwner
) {
    var selectedBrowseTab by remember { mutableStateOf(0) }
    val activity = LocalContext.current as? Activity
    val viewModelProvider = remember { ViewModelProvider(viewModelStoreOwner) }
    val popularMonthViewModel = remember { viewModelProvider[SearchUrlViewModel::class.java] }
    val searchTermViewModel = remember { viewModelProvider[SearchTermViewModel::class.java] }

    LaunchedEffect(selectedBrowseTab) {
        val rank = when (selectedBrowseTab) {
            0 -> "popmonth"
            1 -> "popular"
            else -> "sixmonths"
        }
        popularMonthViewModel.initialize(rank, null)
    }

    val browseUiState by popularMonthViewModel.uiState.collectAsState()
    val browseNovels by popularMonthViewModel.novels.collectAsState()
    val isSearching by searchTermViewModel.isSearching.collectAsState()
    val sourceStates by searchTermViewModel.sourceStates.collectAsState()
    var selectedSourceTab by remember { mutableStateOf(0) }

    LaunchedEffect(sourceStates.size) {
        if (selectedSourceTab >= sourceStates.size) selectedSourceTab = 0
    }
    LaunchedEffect(selectedSourceTab, sourceStates.size) {
        if (sourceStates.isNotEmpty() && selectedSourceTab in sourceStates.indices) {
            searchTermViewModel.fetchSourceIfNeeded(selectedSourceTab)
        }
    }

    val searchState = rememberPersistentSearchState(initialLogoText = "Search Novels")
    var currentSearchHistory by remember { mutableStateOf(searchHistory) }
    val suggestionBuilder = remember(currentSearchHistory) {
        HistorySearchSuggestionsBuilder(currentSearchHistory)
    }

    // Cloudflare dialog state
    var showCloudflareDialog by remember { mutableStateOf(false) }
    var cloudflareUrl by remember { mutableStateOf<String?>(null) }
    var cloudflareRetrySource by remember { mutableStateOf<String?>(null) }

    val cloudflareResolverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            when {
                cloudflareRetrySource == "browse" -> popularMonthViewModel.retry()
                cloudflareRetrySource?.startsWith("search:") == true -> {
                    val idx = cloudflareRetrySource!!.removePrefix("search:").toIntOrNull()
                    if (idx != null) searchTermViewModel.retrySource(idx)
                }
            }
        }
        showCloudflareDialog = false
        cloudflareRetrySource = null
    }

    BackHandler {
        when {
            searchState.isEditing -> searchState.closeSearch()
            searchState.isSearching -> {
                searchState.resetSearch()
                searchTermViewModel.clearSearch()
            }
            else -> onHomeClick()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.zIndex(1f)) {
                PersistentSearchView(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    state = searchState,
                    hint = "Search novels...",
                    homeButtonMode = HomeButtonMode.Burger,
                    onHomeButtonClick = onHomeClick,
                    onSearch = { query ->
                        onSearch(query)
                        currentSearchHistory = listOf(query) + currentSearchHistory.filter { it != query }
                        selectedSourceTab = 0
                        searchTermViewModel.search(query)
                    },
                    onSearchCleared = { searchTermViewModel.clearSearch() },
                    onSearchExit = { if (!isSearching) onSearchExit() },
                    onSearchReset = {
                        searchTermViewModel.clearSearch()
                        onSearchExit()
                    },
                    suggestionBuilder = suggestionBuilder,
                    elevation = 4
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSearching && sourceStates.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ScrollableTabRow(
                            selectedTabIndex = selectedSourceTab.coerceIn(0, (sourceStates.size - 1).coerceAtLeast(0)),
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            edgePadding = 8.dp
                        ) {
                            sourceStates.forEachIndexed { index, state ->
                                Tab(
                                    selected = selectedSourceTab == index,
                                    onClick = { selectedSourceTab = index },
                                    text = { Text(text = state.sourceName, style = MaterialTheme.typography.titleSmall) }
                                )
                            }
                        }
                        val currentSource = sourceStates.getOrNull(selectedSourceTab)
                        if (currentSource != null) {
                            SourceSearchContent(
                                state = currentSource,
                                sourceIndex = selectedSourceTab,
                                onRetry = { searchTermViewModel.retrySource(selectedSourceTab) },
                                onLoadMore = { searchTermViewModel.loadMore(selectedSourceTab) },
                                onNovelClick = { novel -> activity?.startNovelDetailsActivity(novel, false) },
                                onResolveCloudflare = { url ->
                                    cloudflareUrl = url
                                    cloudflareRetrySource = "search:$selectedSourceTab"
                                    showCloudflareDialog = true
                                }
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchTabs(selectedTab = selectedBrowseTab, onTabSelected = { selectedBrowseTab = it })
                        BrowseContent(
                            uiState = browseUiState,
                            novels = browseNovels,
                            onRetry = { popularMonthViewModel.retry() },
                            onLoadMore = { popularMonthViewModel.loadMore() },
                            onNovelClick = { novel -> activity?.startNovelDetailsActivity(novel, false) },
                            onResolveCloudflare = { url ->
                                cloudflareUrl = url
                                cloudflareRetrySource = "browse"
                                showCloudflareDialog = true
                            }
                        )
                    }
                }
                SearchBackgroundTint(visible = searchState.isEditing, onClick = { searchState.closeSearch() })
            }
        }

        if (showCloudflareDialog) {
            CloudflareDialog(
                onResolveManually = {
                    showCloudflareDialog = false
                    val url = cloudflareUrl ?: "https://www.novelupdates.com"
                    val intent = CloudflareResolverActivity.createIntent(activity ?: return@CloudflareDialog, url)
                    cloudflareResolverLauncher.launch(intent)
                },
                onRetry = {
                    showCloudflareDialog = false
                    when {
                        cloudflareRetrySource == "browse" -> popularMonthViewModel.retry()
                        cloudflareRetrySource?.startsWith("search:") == true -> {
                            val idx = cloudflareRetrySource!!.removePrefix("search:").toIntOrNull()
                            if (idx != null) searchTermViewModel.retrySource(idx)
                        }
                    }
                    cloudflareRetrySource = null
                },
                onDismiss = {
                    showCloudflareDialog = false
                    cloudflareRetrySource = null
                }
            )
        }
    }
}

@Composable
private fun SourceSearchContent(
    state: io.github.gmathi.novellibrary.viewmodel.SourceSearchState,
    sourceIndex: Int,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNovelClick: (DbNovel) -> Unit,
    onResolveCloudflare: (String) -> Unit
) {
    when (state.uiState) {
        is SearchTermUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is SearchTermUiState.NoInternet -> {
            ErrorState(message = "No internet connection", isCloudflare = false, onRetry = onRetry, onResolveCloudflare = null)
        }
        is SearchTermUiState.Error -> {
            val error = state.uiState as SearchTermUiState.Error
            ErrorState(
                message = error.message,
                isCloudflare = error.isCloudflare,
                onRetry = onRetry,
                onResolveCloudflare = if (error.isCloudflare) {
                    { onResolveCloudflare(error.cloudflareUrl ?: "https://www.novelupdates.com") }
                } else null
            )
        }
        is SearchTermUiState.Empty -> { EmptyState(message = "No novels found", icon = Icons.Filled.SearchOff) }
        is SearchTermUiState.Success -> {
            val hasMore = (state.uiState as SearchTermUiState.Success).hasMore
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                itemsIndexed(
                    items = state.novels,
                    key = { index, novel -> "search_${sourceIndex}_${index}_${novel.id}_${novel.name?.hashCode() ?: 0}" }
                ) { _, novel ->
                    SearchTermResultItem(novel = novel, onClick = { onNovelClick(novel) })
                }
                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (hasMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Button(onClick = onLoadMore) { Text("Load More") }
                        }
                    }
                }
            }
        }
        is SearchTermUiState.Idle -> { WelcomeContent() }
    }
}

@Composable
private fun BrowseContent(
    uiState: SearchUrlUiState,
    novels: List<DbNovel>,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNovelClick: (DbNovel) -> Unit,
    onResolveCloudflare: (String) -> Unit
) {
    when (uiState) {
        is SearchUrlUiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
        is SearchUrlUiState.Error -> {
            ErrorState(
                message = uiState.message,
                isCloudflare = uiState.isCloudflare,
                onRetry = onRetry,
                onResolveCloudflare = if (uiState.isCloudflare) {
                    { onResolveCloudflare(uiState.cloudflareUrl ?: "https://www.novelupdates.com") }
                } else null
            )
        }
        is SearchUrlUiState.NoInternet -> {
            ErrorState(message = "No internet connection", isCloudflare = false, onRetry = onRetry, onResolveCloudflare = null)
        }
        is SearchUrlUiState.Empty -> { EmptyState(message = "No novels found", icon = Icons.Filled.SearchOff) }
        is SearchUrlUiState.Success -> {
            if (novels.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(
                        items = novels,
                        key = { index, novel -> "browse_${index}_${novel.id}_${novel.name.hashCode() ?: 0}" }
                    ) { _, novel ->
                        SearchUrlNovelItemWrapper(novel = novel, onClick = { onNovelClick(novel) })
                    }
                    if (uiState.hasMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Button(onClick = onLoadMore) { Text("Load More") }
                            }
                        }
                    }
                }
            } else {
                WelcomeContent()
            }
        }
    }
}

@Composable
fun SearchTabs(selectedTab: Int, onTabSelected: (Int) -> Unit) {
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
                text = { Text(text = title, style = MaterialTheme.typography.titleSmall) }
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    isCloudflare: Boolean,
    onRetry: () -> Unit,
    onResolveCloudflare: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isCloudflare) Icons.Filled.Security else Icons.Filled.Error,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = if (isCloudflare) "Cloudflare Verification Required" else "Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isCloudflare && onResolveCloudflare != null) {
                Button(onClick = onResolveCloudflare) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Resolve Cloudflare")
                }
            }
            OutlinedButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                Text("Try Again")
            }
        }
    }
}
