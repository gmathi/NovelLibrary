package io.github.gmathi.novellibrary.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.common.EmptyView
import io.github.gmathi.novellibrary.compose.common.ErrorView
import io.github.gmathi.novellibrary.compose.common.LoadingView
import io.github.gmathi.novellibrary.compose.components.NovelItem
import io.github.gmathi.novellibrary.compose.components.RecentlyUpdatedItemView
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.util.Constants

// Preview Data Providers
private class RecentlyUpdatedItemsProvider : PreviewParameterProvider<List<RecentlyUpdatedItem>> {
    override val values = sequenceOf(
        listOf(
            RecentlyUpdatedItem().apply {
                novelName = "The Beginning After The End"
                chapterName = "Chapter 456: The Final Battle"
                publisherName = "Tapas"
                novelUrl = "https://example.com/novel1"
            },
            RecentlyUpdatedItem().apply {
                novelName = "Solo Leveling"
                chapterName = "Chapter 270: Epilogue"
                publisherName = "Webnovel"
                novelUrl = "https://example.com/novel2"
            },
            RecentlyUpdatedItem().apply {
                novelName = "Omniscient Reader's Viewpoint"
                chapterName = "Chapter 551: Epilogue (End)"
                publisherName = "Munpia"
                novelUrl = "https://example.com/novel3"
            },
            RecentlyUpdatedItem().apply {
                novelName = "Reverend Insanity"
                chapterName = "Chapter 2334: Fang Yuan's Victory"
                publisherName = "Qidian"
                novelUrl = "https://example.com/novel4"
            },
            RecentlyUpdatedItem().apply {
                novelName = "Lord of the Mysteries"
                chapterName = "Chapter 1394: The End"
                publisherName = "Webnovel"
                novelUrl = "https://example.com/novel5"
            }
        )
    )
}

private class RecentlyViewedNovelsProvider : PreviewParameterProvider<List<Novel>> {
    override val values = sequenceOf(
        listOf(
            Novel("The Beginning After The End", "https://example.com/novel1", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image1.jpg"
                rating = "4.8"
                metadata["OriginMarker"] = "Korean"
            },
            Novel("Solo Leveling", "https://example.com/novel2", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image2.jpg"
                rating = "4.9"
                metadata["OriginMarker"] = "Korean"
            },
            Novel("Omniscient Reader's Viewpoint", "https://example.com/novel3", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image3.jpg"
                rating = "4.7"
                metadata["OriginMarker"] = "Korean"
            }
        )
    )
}

// Recently Updated Tab Previews
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Updated Tab - Success", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyUpdatedTabSuccessPreview(
    @PreviewParameter(RecentlyUpdatedItemsProvider::class) items: List<RecentlyUpdatedItem>
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(items) { index, item ->
                        RecentlyUpdatedItemView(
                            item = item,
                            onClick = {},
                            isEvenPosition = index % 2 == 0
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Updated Tab - Loading", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyUpdatedTabLoadingPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LoadingView()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Updated Tab - Error", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyUpdatedTabErrorPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                ErrorView(
                    message = "Failed to load recently updated novels",
                    onRetry = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Updated Tab - No Internet", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyUpdatedTabNoInternetPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                ErrorView(
                    message = "No internet connection",
                    onRetry = {}
                )
            }
        }
    }
}

// Recently Viewed Tab Previews
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Viewed Tab - Success", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyViewedTabSuccessPreview(
    @PreviewParameter(RecentlyViewedNovelsProvider::class) novels: List<Novel>
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Delete, "Clear history")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 1) {
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(novels) { _, novel ->
                        NovelItem(
                            novel = novel,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Viewed Tab - Empty", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyViewedTabEmptyPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Delete, "Clear history")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 1) {
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                EmptyView(message = "No recently viewed novels")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Recently Viewed Tab - Loading", showBackground = true, heightDp = 600)
@Composable
private fun RecentlyViewedTabLoadingPreview() {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Delete, "Clear history")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 1) {
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LoadingView()
            }
        }
    }
}

// Dark Mode Previews
@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Recently Updated Tab - Dark Mode",
    showBackground = true,
    heightDp = 600,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RecentlyUpdatedTabDarkPreview(
    @PreviewParameter(RecentlyUpdatedItemsProvider::class) items: List<RecentlyUpdatedItem>
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 0) {
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(items) { index, item ->
                        RecentlyUpdatedItemView(
                            item = item,
                            onClick = {},
                            isEvenPosition = index % 2 == 0
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Recently Viewed Tab - Dark Mode",
    showBackground = true,
    heightDp = 600,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RecentlyViewedTabDarkPreview(
    @PreviewParameter(RecentlyViewedNovelsProvider::class) novels: List<Novel>
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Recent Novels") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Delete, "Clear history")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues)) {
                TabRow(selectedTabIndex = 1) {
                    Tab(selected = false, onClick = {}, text = { Text("RECENTLY UPDATED") })
                    Tab(selected = true, onClick = {}, text = { Text("RECENTLY VIEWED") })
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(novels) { _, novel ->
                        NovelItem(
                            novel = novel,
                            onClick = {}
                        )
                    }
                }
            }
        }
    }
}
