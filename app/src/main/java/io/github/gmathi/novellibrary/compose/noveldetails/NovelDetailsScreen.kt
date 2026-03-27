package io.github.gmathi.novellibrary.compose.noveldetails

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.common.ErrorView
import io.github.gmathi.novellibrary.compose.common.LoadingView
import io.github.gmathi.novellibrary.compose.common.URLImage
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsUiState
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NovelDetailsScreen(
    viewModel: NovelDetailsViewModel,
    onBackClick: () -> Unit,
    onImageClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onMetadataClick: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onAuthorLinkClick: (title: String, url: String) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onShareClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.loadDetails(isManualRefresh = true) }
    )

    val novelName = when (val s = uiState) {
        is NovelDetailsUiState.Content -> s.novel.name
        else -> ""
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.confirm_remove)) },
            text = { Text(stringResource(R.string.confirm_remove_description_novel)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteConfirmed()
                }) { Text(stringResource(R.string.remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = novelName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState is NovelDetailsUiState.Content &&
                        (uiState as NovelDetailsUiState.Content).novel.id != -1L
                    ) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.confirm_remove))
                        }
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
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
            when (val state = uiState) {
                is NovelDetailsUiState.Loading -> LoadingView()

                is NovelDetailsUiState.NoInternet -> ErrorView(
                    message = stringResource(R.string.no_internet),
                    onRetry = { viewModel.loadDetails() }
                )

                is NovelDetailsUiState.MissingSource -> ErrorView(
                    message = "Missing Novel Source Id.\nPlease re-add the novel.",
                    onRetry = { viewModel.onDeleteAndFinish() },
                    buttonText = "Delete Novel"
                )

                is NovelDetailsUiState.Error -> ErrorView(
                    message = state.message,
                    onRetry = { viewModel.loadDetails() }
                )

                is NovelDetailsUiState.CloudflareRequired -> LoadingView()

                is NovelDetailsUiState.Content -> NovelDetailsContent(
                    novel = state.novel,
                    onImageClick = onImageClick,
                    onAddToLibrary = { viewModel.addToLibrary() },
                    onDeleteFromLibrary = { showDeleteDialog = true },
                    onChaptersClick = onChaptersClick,
                    onMetadataClick = onMetadataClick,
                    onOpenInBrowser = onOpenInBrowser,
                    onAuthorLinkClick = onAuthorLinkClick
                )
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
private fun NovelDetailsContent(
    novel: Novel,
    onImageClick: () -> Unit,
    onAddToLibrary: () -> Unit,
    onDeleteFromLibrary: () -> Unit,
    onChaptersClick: () -> Unit,
    onMetadataClick: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onAuthorLinkClick: (title: String, url: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: image + basic info
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            URLImage(
                imageUrl = novel.imageUrl,
                contentDescription = novel.name,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onImageClick),
                width = 120.dp,
                height = 160.dp,
                contentScale = ContentScale.Crop,
                showLoadingIndicator = true
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = novel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Author
                val author = novel.metadata["Author(s)"]
                if (author != null) {
                    HtmlLinkedText(
                        html = author,
                        style = MaterialTheme.typography.bodyMedium,
                        onLinkClick = onAuthorLinkClick
                    )
                } else {
                    val authors = novel.authors?.joinToString(", ")
                    if (!authors.isNullOrBlank()) {
                        Text(text = authors, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Year/status
                val year = novel.metadata["Year"]
                Text(
                    text = year ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Rating
                NovelRating(novel.rating)
            }
        }

        // Licensing warning
        val publisher = novel.metadata["English Publisher"] ?: ""
        val isLicensed = novel.metadata["Licensed (in English)"] == "Yes"
        if (publisher.isNotEmpty() || isLicensed) {
            val publisherName = publisher.ifEmpty { "an unknown publisher" }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                HtmlLinkedText(
                    html = "⚠️ Licensed by $publisherName. Reading may not be supported.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                    onLinkClick = onAuthorLinkClick
                )
            }
        }

        // Add to library button
        if (novel.id == -1L) {
            Button(
                onClick = onAddToLibrary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.add_to_library))
            }
        } else {
            Button(
                onClick = onDeleteFromLibrary,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.in_library))
            }
        }

        // Genres
        NovelGenres(novel.genres)

        HorizontalDivider()

        // Description
        if (!novel.longDescription.isNullOrBlank()) {
            NovelDescription(description = novel.longDescription!!)
        }

        HorizontalDivider()

        // Chapters row
        val chaptersCountText = novel.chaptersCount.takeIf { it > 0L }?.toString()
            ?: novel.metadata["Chapters"]?.takeIf { it.isNotBlank() }
        val chaptersLabel = if (chaptersCountText != null)
            "${stringResource(R.string.chapters)} ($chaptersCountText)"
        else
            stringResource(R.string.chapters)

        NavigationRow(label = chaptersLabel, onClick = onChaptersClick)
        NavigationRow(label = "Metadata", onClick = onMetadataClick)
        NavigationRow(label = "Open in Browser", onClick = onOpenInBrowser)
    }
}

@Composable
private fun NovelRating(rating: String?) {
    if (rating.isNullOrBlank()) return
    val ratingValue = try { rating.replace(",", ".").toFloat() } catch (e: Exception) { return }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(5) { index ->
            Text(
                text = if (index < ratingValue.toInt()) "★" else "☆",
                color = if (index < ratingValue.toInt()) Color(0xFFFFC107)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "(${String.format("%.1f", ratingValue)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NovelGenres(genres: List<String>?) {
    val list = if (!genres.isNullOrEmpty()) genres else listOf("N/A")
    Column {
        Text(
            text = "Genres",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            list.forEach { genre ->
                Text(
                    text = genre,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun NovelDescription(description: String) {
    var isExpanded by remember { mutableStateOf(false) }
    Column {
        Text(
            text = "Description",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (isExpanded || description.length <= 300) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        } else {
            val shortText = description.substring(0, min(300, description.length))
            Text(
                text = buildAnnotatedString {
                    append(shortText)
                    append("… ")
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) { append("Expand") }
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { isExpanded = true }
            )
        }
    }
}

@Composable
private fun NavigationRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = "›", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Renders HTML text with clickable links. Links in the HTML trigger [onLinkClick].
 */
@Composable
private fun HtmlLinkedText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    onLinkClick: (title: String, url: String) -> Unit
) {
    val spanned = remember(html) {
        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }
    // Extract link URLs and titles from spanned text
    val annotatedString = remember(spanned) {
        buildAnnotatedString {
            val raw = spanned.toString()
            if (color != Color.Unspecified) {
                withStyle(SpanStyle(color = color)) { append(raw) }
            } else {
                append(raw)
            }
        }
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style
    )
}
