package io.github.gmathi.novellibrary.compose.recentnovels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import io.github.gmathi.novellibrary.compose.components.RecentlyUpdatedItemView
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.viewmodel.RecentNovelsViewModel
import io.github.gmathi.novellibrary.viewmodel.RecentlyUpdatedUiState
import io.github.gmathi.novellibrary.viewmodel.RecentlyViewedUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNovelsScreen(
    viewModel: RecentNovelsViewModel,
    onNovelClick: (Novel) -> Unit,
    onRecentlyUpdatedItemClick: (RecentlyUpdatedItem) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Recently Updated", "Recently Viewed")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Novels") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectedTab == 1) {
                        IconButton(onClick = { viewModel.clearRecentlyViewedNovels() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear history"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title.uppercase()) }
                    )
                }
            }

            when (selectedTab) {
                0 -> RecentlyUpdatedTab(
                    viewModel = viewModel,
                    onItemClick = onRecentlyUpdatedItemClick
                )
                1 -> RecentlyViewedTab(
                    viewModel = viewModel,
                    onNovelClick = onNovelClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RecentlyUpdatedTab(
    viewModel: RecentNovelsViewModel,
    onItemClick: (RecentlyUpdatedItem) -> Unit
) {
    val uiState by viewModel.recentlyUpdatedState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadRecentlyUpdatedNovels()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.retryRecentlyUpdated() }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when (val state = uiState) {
            is RecentlyUpdatedUiState.Loading -> {
                LoadingView()
            }
            is RecentlyUpdatedUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    itemsIndexed(
                        items = state.items,
                        key = { index, item -> "updated_${index}_${item.novelUrl}_${item.chapterName}" }
                    ) { index, item ->
                        RecentlyUpdatedItemView(
                            item = item,
                            onClick = { onItemClick(item) },
                            isEvenPosition = index % 2 == 0
                        )
                    }
                }
            }
            is RecentlyUpdatedUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetry = { viewModel.retryRecentlyUpdated() }
                )
            }
            is RecentlyUpdatedUiState.NoInternet -> {
                ErrorView(
                    message = "No internet connection",
                    onRetry = { viewModel.retryRecentlyUpdated() }
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

@Composable
private fun RecentlyViewedTab(
    viewModel: RecentNovelsViewModel,
    onNovelClick: (Novel) -> Unit
) {
    val uiState by viewModel.recentlyViewedState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadRecentlyViewedNovels()
    }

    when (val state = uiState) {
        is RecentlyViewedUiState.Loading -> {
            LoadingView()
        }
        is RecentlyViewedUiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                itemsIndexed(
                    items = state.novels,
                    key = { index, novel -> "viewed_${index}_${novel.id}_${novel.url}" }
                ) { _, novel ->
                    NovelItem(
                        novel = novel,
                        onClick = { onNovelClick(novel) }
                    )
                }
            }
        }
        is RecentlyViewedUiState.Empty -> {
            EmptyView(message = "No recently viewed novels")
        }
    }
}
