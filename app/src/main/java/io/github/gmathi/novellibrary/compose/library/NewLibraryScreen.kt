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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
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
import io.github.gmathi.novellibrary.model.database.NovelTag
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.viewmodel.NewLibraryViewModel
import io.github.gmathi.novellibrary.viewmodel.SortDirection
import io.github.gmathi.novellibrary.viewmodel.SortKey
import io.github.gmathi.novellibrary.viewmodel.TagFilterMode

/** Callbacks the host (fragment/activity) supplies for navigation and background work. */
data class NewLibraryActions(
    val onMenuClick: () -> Unit,
    val onNovelClick: (Novel) -> Unit,
    val onNovelDetails: (Novel) -> Unit,
    val onNovelRead: (Novel) -> Unit,
    val onNovelRemove: (Novel) -> Unit,
    val onNovelHardReset: (Novel) -> Unit,
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
                    tagsById = state.tags.associate { it.tag.id to it.tag },
                    onClick = actions.onNovelClick,
                    onDetails = actions.onNovelDetails,
                    onRead = actions.onNovelRead,
                    onAssignTags = { assignTagsFor = it },
                    onRemove = actions.onNovelRemove,
                    onHardReset = actions.onNovelHardReset
                )
            }
        }
    }

    if (showSortSheet) {
        LibrarySortSheet(
            currentKey = state.sortKey,
            currentDirection = state.sortDirection,
            onSelectKey = viewModel::setSortKey,
            onSelectDirection = viewModel::setSortDirection,
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
            val isSelected = libraryTag.tag.id in selected
            val base = TagColors.resolveTagColor(libraryTag.tag.color, libraryTag.tag.name)
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(libraryTag.tag.id) },
                label = { Text("${libraryTag.tag.name} (${libraryTag.novelCount})") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(base)
                    )
                },
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = base,
                    selectedBorderColor = base,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 2.dp
                )
            )
        }
    }
}

@Composable
private fun NovelCoverGrid(
    novels: List<Novel>,
    novelTagIds: Map<Long, List<Long>>,
    tagsById: Map<Long, NovelTag>,
    onClick: (Novel) -> Unit,
    onDetails: (Novel) -> Unit,
    onRead: (Novel) -> Unit,
    onAssignTags: (Novel) -> Unit,
    onRemove: (Novel) -> Unit,
    onHardReset: (Novel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(novels, key = { "${it.id}_${it.url}" }) { novel ->
            NovelGridItem(
                novel = novel,
                tags = novelTagIds[novel.id].orEmpty().mapNotNull { tagsById[it] },
                onClick = { onClick(novel) },
                onDetails = { onDetails(novel) },
                onRead = { onRead(novel) },
                onAssignTags = { onAssignTags(novel) },
                onRemove = { onRemove(novel) },
                onHardReset = { onHardReset(novel) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovelGridItem(
    novel: Novel,
    tags: List<NovelTag>,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onRead: () -> Unit,
    onAssignTags: () -> Unit,
    onRemove: () -> Unit,
    onHardReset: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Tapping the cover opens the reader (continue reading); the side strip gives quick access to
    // the chapter list (contents) and the novel details, with More for the rest.
    Column(modifier = Modifier.clickable(onClick = onRead)) {
        // Cover on the left, quick actions in a strip to its side.
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
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
            }

            // Side quick-action strip.
            Column(
                modifier = Modifier.padding(start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QuickAction(icon = Icons.AutoMirrored.Filled.List, label = "Contents", onClick = onClick)
                QuickAction(icon = Icons.Outlined.Info, label = "Details", onClick = onDetails)
                Box {
                    QuickAction(icon = Icons.Filled.MoreVert, label = "More", onClick = { showMenu = true })
                    NovelContextMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        onOpen = onClick,
                        onRead = onRead,
                        onDetails = onDetails,
                        onAssignTags = onAssignTags,
                        onRemove = onRemove,
                        onHardReset = onHardReset
                    )
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

        if (tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.forEach { tag -> TagChip(tag = tag) }
            }
        }
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NovelContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRead: () -> Unit,
    onDetails: () -> Unit,
    onAssignTags: () -> Unit,
    onRemove: () -> Unit,
    onHardReset: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Open chapters") }, onClick = { onDismiss(); onOpen() })
        DropdownMenuItem(text = { Text("Continue reading") }, onClick = { onDismiss(); onRead() })
        DropdownMenuItem(text = { Text("Novel details") }, onClick = { onDismiss(); onDetails() })
        DropdownMenuItem(text = { Text("Edit tags") }, onClick = { onDismiss(); onAssignTags() })
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Hard reset", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onHardReset() }
        )
        DropdownMenuItem(
            text = { Text("Remove from library", color = MaterialTheme.colorScheme.error) },
            onClick = { onDismiss(); onRemove() }
        )
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
