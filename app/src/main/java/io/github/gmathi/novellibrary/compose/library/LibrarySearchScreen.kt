package io.github.gmathi.novellibrary.compose.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.common.EmptyView
import io.github.gmathi.novellibrary.compose.common.URLImage
import io.github.gmathi.novellibrary.compose.search.*
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants

/**
 * Library search screen composable — displays the user's library novels in a rich card layout
 * with cover images, ratings, chapter counts, genres, and reading progress.
 */
@Composable
fun LibrarySearchScreen(
    allNovels: List<Novel>,
    searchHistory: List<String>,
    searchHint: String,
    onSearch: (String) -> Unit,
    onBackClick: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onNovelDetailsClick: (Novel) -> Unit,
    onNovelReadClick: (Novel) -> Unit
) {
    var searchResults by remember(allNovels) { mutableStateOf(allNovels) }
    val searchState = rememberPersistentSearchState()
    val suggestionBuilder = remember(searchHistory) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PersistentSearchView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    state = searchState,
                    hint = searchHint,
                    homeButtonMode = HomeButtonMode.Arrow,
                    onHomeButtonClick = onBackClick,
                    onSearch = { query -> onSearch(query) },
                    onSearchTermChanged = { term ->
                        searchResults = if (term.isEmpty()) {
                            allNovels
                        } else {
                            allNovels.filter { novel ->
                                novel.name.contains(term, ignoreCase = true)
                            }
                        }
                    },
                    suggestionBuilder = suggestionBuilder,
                    elevation = 4
                )

                if (searchResults.isEmpty()) {
                    EmptyView(message = stringResource(R.string.no_novels_found))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(searchResults, key = { "${it.id}_${it.url}" }) { novel ->
                            LibraryNovelCard(
                                novel = novel,
                                onClick = { onNovelClick(novel) },
                                onDetails = { onNovelDetailsClick(novel) },
                                onRead = { onNovelReadClick(novel) }
                            )
                        }
                    }
                }
            }

            SearchBackgroundTint(
                visible = searchState.isEditing,
                onClick = { searchState.closeSearch() }
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryNovelCard(
    novel: Novel,
    onClick: () -> Unit,
    onDetails: () -> Unit,
    onRead: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cover image
            Box {
                if (!novel.imageUrl.isNullOrBlank()) {
                    URLImage(
                        imageUrl = novel.imageUrl,
                        contentDescription = novel.name,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                        width = 72.dp,
                        height = 104.dp,
                        shape = RoundedCornerShape(8.dp),
                        contentScale = ContentScale.Crop,
                        errorContent = {
                            CoverPlaceholder()
                        }
                    )
                } else {
                    CoverPlaceholder()
                }

                // New releases badge
                if (novel.newReleasesCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = "${novel.newReleasesCount}",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            // Novel details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 104.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Title + overflow menu
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = novel.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { showMenu = true }
                            )
                        ) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_activity_chapters)) },
                                    onClick = {
                                        showMenu = false
                                        onClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.title_activity_novel_details)) },
                                    onClick = {
                                        showMenu = false
                                        onDetails()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.read)) },
                                    onClick = {
                                        showMenu = false
                                        onRead()
                                    }
                                )
                            }
                        }
                    }

                    // Rating + chapter count row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating
                        novel.rating?.let { ratingStr ->
                            val ratingValue = try { ratingStr.toFloat() } catch (_: Exception) { 0f }
                            if (ratingValue > 0f) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFFFFB300)
                                    )
                                    Text(
                                        text = String.format("%.1f", ratingValue),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Chapter count
                        if (novel.chaptersCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${novel.chaptersCount} ${stringResource(R.string.chapters).lowercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Genres
                    val genres = novel.genres
                    if (!genres.isNullOrEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            genres.take(4).forEach { genre ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = genre,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            if (genres.size > 4) {
                                Text(
                                    text = "+${genres.size - 4}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }

                // Bottom row: last read / last updated
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val lastRead = novel.metadata[Constants.MetaDataKeys.LAST_READ_DATE]
                    val lastUpdated = novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE]

                    if (lastRead != null) {
                        Text(
                            text = stringResource(R.string.last_read_label, lastRead),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (lastUpdated != null) {
                        Text(
                            text = stringResource(R.string.updated_label, lastUpdated),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverPlaceholder() {
    Box(
        modifier = Modifier
            .size(72.dp, 104.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Book,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
