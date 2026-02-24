package io.github.gmathi.novellibrary.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gmathi.novellibrary.compose.common.EmptyView
import io.github.gmathi.novellibrary.compose.common.ErrorView
import io.github.gmathi.novellibrary.compose.common.LoadingView
import io.github.gmathi.novellibrary.compose.components.NovelItem
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.viewmodel.SearchUrlUiState
import io.github.gmathi.novellibrary.viewmodel.SearchUrlViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun SearchUrlScreen(
    viewModel: SearchUrlViewModel,
    title: String,
    onNovelClick: (Novel) -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val novels by viewModel.novels.collectAsStateWithLifecycle()
    
    val isRefreshing = uiState is SearchUrlUiState.Loading && novels.isEmpty()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.retry() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search: $title") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            when (uiState) {
                is SearchUrlUiState.Loading -> {
                    if (novels.isEmpty()) {
                        LoadingView()
                    } else {
                        NovelList(
                            novels = novels,
                            onNovelClick = onNovelClick,
                            onLoadMore = { viewModel.loadMore() },
                            hasMore = false
                        )
                    }
                }
                is SearchUrlUiState.Success -> {
                    NovelList(
                        novels = novels,
                        onNovelClick = onNovelClick,
                        onLoadMore = { viewModel.loadMore() },
                        hasMore = (uiState as SearchUrlUiState.Success).hasMore
                    )
                }
                is SearchUrlUiState.Error -> {
                    val error = uiState as SearchUrlUiState.Error
                    ErrorView(
                        message = if (error.isCloudflare) {
                            "Cloudflare protection detected. Please try again."
                        } else {
                            "Connection error. Please try again."
                        },
                        onRetry = { viewModel.retry() }
                    )
                }
                is SearchUrlUiState.NoInternet -> {
                    ErrorView(
                        message = "No internet connection",
                        onRetry = { viewModel.retry() }
                    )
                }
                is SearchUrlUiState.Empty -> {
                    EmptyView(
                        message = "No Novels Found!",
                        onRetry = { viewModel.retry() }
                    )
                }
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun NovelList(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onLoadMore: () -> Unit,
    hasMore: Boolean
) {
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(novels, key = { it.url }) { novel ->
            NovelItem(
                novel = novel,
                onClick = { onNovelClick(novel) }
            )
        }
        
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
                
                LaunchedEffect(Unit) {
                    onLoadMore()
                }
            }
        }
    }
}
