package io.github.gmathi.novellibrary.compose.downloads

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.model.ui.ChapterDownloadStatus
import io.github.gmathi.novellibrary.model.ui.ChapterDownloadUiModel
import io.github.gmathi.novellibrary.model.ui.ChapterListUiState
import io.github.gmathi.novellibrary.compose.common.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListView(
    chapterListState: ChapterListUiState,
    onCancelChapter: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = chapterListState.novelName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        ) {
            if (chapterListState.isLoading) {
                LoadingView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = chapterListState.chapters,
                        key = { it.webPageUrl }
                    ) { chapter ->
                        ChapterDownloadItem(
                            chapter = chapter,
                            onCancel = { onCancelChapter(chapter.webPageUrl) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterDownloadItem(
    chapter: ChapterDownloadUiModel,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (chapter.status) {
                ChapterDownloadStatus.IN_QUEUE -> {
                    Icon(
                        imageVector = Icons.Default.HourglassEmpty,
                        contentDescription = "In queue",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ChapterDownloadStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                ChapterDownloadStatus.PAUSED -> {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Paused",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Chapter name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chapter.chapterName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when (chapter.status) {
                    ChapterDownloadStatus.IN_QUEUE -> "In queue"
                    ChapterDownloadStatus.RUNNING -> "Downloading"
                    ChapterDownloadStatus.PAUSED -> "Paused"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (chapter.status) {
                    ChapterDownloadStatus.RUNNING -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Cancel button — visible for IN_QUEUE or RUNNING chapters
        if (chapter.status == ChapterDownloadStatus.IN_QUEUE ||
            chapter.status == ChapterDownloadStatus.RUNNING
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Cancel download",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
