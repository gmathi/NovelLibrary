package io.github.gmathi.novellibrary.compose.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gmathi.novellibrary.compose.common.EmptyView
import io.github.gmathi.novellibrary.compose.common.URLImage
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.viewmodel.LibrarySort
import io.github.gmathi.novellibrary.viewmodel.NewLibraryViewModel
import io.github.gmathi.novellibrary.viewmodel.TagFilterMode

/** Callbacks the host (fragment/activity) supplies for navigation and background work. */
data class NewLibraryActions(
    val onMenuClick: () -> Unit,
    val onNovelClick: (Novel) -> Unit,
    val onNovelDetails: (Novel) -> Unit,
    val onNovelRead: (Novel) -> Unit,
    val onNovelRemove: (Novel) -> Unit,
    val onSync: () -> Unit,
    val onRefreshCovers: () -> Unit,
    val onSearchClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewLibraryScreen(
    viewModel: NewLibraryViewModel,
    actions: NewLibraryActions
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isSyncing = state.syncing

    var showSortSheet by remember { mutableStateOf(false) }
    var showTagManager by remember { mutableStateOf(false) }
    var showFilterMode by remember { mutableStateOf(false) }
    var assignTagsFor by remember { mutableStateOf<Novel?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                navigationIcon = {
                    IconButton(onClick = actions.onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open navigation drawer")
                    }
                },
                actions = {
                    IconButton(onClick = actions.onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Search library")
                    }
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    if (!isSyncing) {
                        IconButton(onClick = actions.onSync) {
                            Icon(Icons.Filled.Sync, contentDescription = "Sync library")
                        }
                    }
                    LibraryOverflowMenu(
                        onRefreshCovers = actions.onRefreshCovers,
                        onManageTags = { showTagManager = true },
                        syncing = isSyncing
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TagFilterBar(
                tags = state.tags,
                selected = state.selectedTagIds,
                filterMode = state.tagFilterMode,
                onToggle = viewModel::toggleTagFilter,
                onClear = viewModel::clearTagFilters,
                onFilterModeClick = { showFilterMode = true }
            )

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                state.isEmpty && state.selectedTagIds.isEmpty() && state.query.isBlank() ->
                    EmptyView(message = "Your library is empty!\nAdd novels from the search screen.")

                state.novels.isEmpty() ->
                    EmptyView(message = "No novels match the selected tags.")

                else -> NovelCoverGrid(
                    novels = state.novels,
                    novelTagIds = state.novelTagIds,
                    tagNames = state.tags.associate { it.tag.id to it.tag.name },
                    onClick = actions.onNovelClick,
                    onDetails = actions.onNovelDetails,
                    onRead = actions.onNovelRead,
                    onAssignTags = { assignTagsFor = it },
                    onRemove = actions.onNovelRemove
                )
            }
        }
    }

    if (showSortSheet) {
        LibrarySortSheet(
            current = state.sort,
            onSelect = {
                viewModel.setSort(it)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showFilterMode) {
        TagFilterModeDialog(
            current = state.tagFilterMode,
            onSelect = {
                viewModel.setTagFilterMode(it)
                showFilterMode = false
            },
            onDismiss = { showFilterMode = false }
        )
    }

    if (showTagManager) {
        TagManagerSheet(
            tags = state.tags,
            onCreate = viewModel::createTag,
            onRename = viewModel::renameTag,
            onDelete = viewModel::deleteTag,
            onDismiss = { showTagManager = false }
        )
    }

    assignTagsFor?.let { novel ->
        AssignTagsSheet(
            novel = novel,
            allTags = state.tags,
            appliedTagIds = state.novelTagIds[novel.id].orEmpty().toSet(),
            onToggle = { tagId, apply ->
                if (apply) viewModel.addTagToNovel(novel.id, tagId)
                else viewModel.removeTagFromNovel(novel.id, tagId)
            },
            onCreateAndApply = { name -> viewModel.createAndApplyTag(novel.id, name) },
            onDismiss = { assignTagsFor = null }
        )
    }
}

@Composable
private fun LibraryOverflowMenu(
    onRefreshCovers: () -> Unit,
    onManageTags: () -> Unit,
    syncing: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Manage tags") },
            onClick = { expanded = false; onManageTags() }
        )
        if (!syncing) {
            DropdownMenuItem(
                text = { Text("Refresh covers") },
                onClick = { expanded = false; onRefreshCovers() }
            )
        }
    }
}

@Composable
private fun TagFilterBar(
    tags: List<io.github.gmathi.novellibrary.viewmodel.LibraryTag>,
    selected: Set<Long>,
    filterMode: TagFilterMode,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit,
    onFilterModeClick: () -> Unit
) {
    if (tags.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssistChip(
            onClick = onFilterModeClick,
            label = { Text(if (filterMode == TagFilterMode.ANY) "Any" else "All") },
            leadingIcon = {
                Icon(Icons.Filled.FilterList, contentDescription = "Filter mode", modifier = Modifier.size(18.dp))
            }
        )
        if (selected.isNotEmpty()) {
            AssistChip(
                onClick = onClear,
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear filters", modifier = Modifier.size(18.dp))
                }
            )
        }
        tags.forEach { libraryTag ->
            FilterChip(
                selected = libraryTag.tag.id in selected,
                onClick = { onToggle(libraryTag.tag.id) },
                label = { Text("${libraryTag.tag.name} (${libraryTag.novelCount})") }
            )
        }
    }
}

@Composable
private fun NovelCoverGrid(
    novels: List<Novel>,
    novelTagIds: Map<Long, List<Long>>,
    tagNames: Map<Long, String>,
    onClick: (Novel) -> Unit,
    onDetails: (Novel) -> Unit,
    onRead: (Novel) -> Unit,
    onAssignTags: (Novel) -> Unit,
    onRemove: (Novel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(novels, key = { "${it.id}_${it.url}" }) { novel ->
            NovelGridItem(
                novel = novel,
                tagNames = novelTagIds[novel.id].orEmpty().mapNotNull { tagNames[it] },
                onClick = { onClick(novel) },
                onDetails = { onDetails(novel) },
                onRead = { onRead(novel) },
                onAssignTags = { onAssignTags(novel) },
                onRemove = { onRemove(novel) }
            )
        }
    }
}

@Composable
private fun NovelGridItem(
    novel: Novel,
    tagNames: List<String>,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onRead: () -> Unit,
    onAssignTags: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!novel.imageUrl.isNullOrBlank()) {
                URLImage(
                    imageUrl = novel.imageUrl,
                    contentDescription = novel.name,
                    modifier = Modifier.matchParentSize(),
                    width = 200.dp,
                    height = 286.dp,
                    shape = RoundedCornerShape(10.dp),
                    contentScale = ContentScale.Crop,
                    errorContent = { CoverPlaceholderGrid() }
                )
            } else {
                CoverPlaceholderGrid()
            }

            // New releases badge
            if (novel.newReleasesCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "${novel.newReleasesCount}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            }

            // Overflow menu button
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { showMenu = true }) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Novel options",
                            tint = Color.White,
                            modifier = Modifier.padding(2.dp).size(20.dp)
                        )
                    }
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Open chapters") }, onClick = { showMenu = false; onClick() })
                    DropdownMenuItem(text = { Text("Continue reading") }, onClick = { showMenu = false; onRead() })
                    DropdownMenuItem(text = { Text("Novel details") }, onClick = { showMenu = false; onDetails() })
                    DropdownMenuItem(text = { Text("Edit tags") }, onClick = { showMenu = false; onAssignTags() })
                    DropdownMenuItem(text = { Text("Remove from library") }, onClick = { showMenu = false; onRemove() })
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = novel.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        val progress = novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE]
        if (progress != null) {
            Text(
                text = "Read $progress",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (tagNames.isNotEmpty()) {
            Text(
                text = tagNames.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CoverPlaceholderGrid() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Book,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagFilterModeDialog(
    current: TagFilterMode,
    onSelect: (TagFilterMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Tag filter mode") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("Match any selected tag") },
                    supportingContent = { Text("Show novels with at least one selected tag") },
                    leadingContent = { RadioButton(selected = current == TagFilterMode.ANY, onClick = { onSelect(TagFilterMode.ANY) }) },
                    modifier = Modifier.clickable { onSelect(TagFilterMode.ANY) }
                )
                ListItem(
                    headlineContent = { Text("Match all selected tags") },
                    supportingContent = { Text("Show only novels that carry every selected tag") },
                    leadingContent = { RadioButton(selected = current == TagFilterMode.ALL, onClick = { onSelect(TagFilterMode.ALL) }) },
                    modifier = Modifier.clickable { onSelect(TagFilterMode.ALL) }
                )
            }
        }
    )
}
