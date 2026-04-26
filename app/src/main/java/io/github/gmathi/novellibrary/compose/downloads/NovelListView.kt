package io.github.gmathi.novellibrary.compose.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.common.EmptyView
import io.github.gmathi.novellibrary.compose.common.LoadingView
import io.github.gmathi.novellibrary.compose.common.URLImage
import io.github.gmathi.novellibrary.model.ui.DownloadsUiState
import io.github.gmathi.novellibrary.model.ui.NovelDownloadStatus
import io.github.gmathi.novellibrary.model.ui.NovelDownloadUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelListView(
    uiState: DownloadsUiState,
    onNovelClick: (Long) -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onBackClick: () -> Unit
) {
    var novelToDelete by remember { mutableStateOf<NovelDownloadUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
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
            when {
                uiState.isLoading -> {
                    LoadingView()
                }
                uiState.novels.isEmpty() -> {
                    EmptyView(message = "No active downloads")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.novels,
                            key = { it.novelId }
                        ) { novel ->
                            NovelDownloadItem(
                                novel = novel,
                                onClick = { onNovelClick(novel.novelId) },
                                onPauseResume = {
                                    when (novel.status) {
                                        NovelDownloadStatus.DOWNLOADING -> onPause(novel.novelId)
                                        NovelDownloadStatus.PAUSED -> onResume(novel.novelId)
                                    }
                                },
                                onDelete = { novelToDelete = novel }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm delete dialog
    novelToDelete?.let { novel ->
        ConfirmDeleteDialog(
            novelName = novel.novelName,
            onConfirm = {
                onDelete(novel.novelId)
                novelToDelete = null
            },
            onDismiss = { novelToDelete = null }
        )
    }
}

@Composable
private fun NovelDownloadItem(
    novel: NovelDownloadUiModel,
    onClick: () -> Unit,
    onPauseResume: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Novel cover image
            URLImage(
                imageUrl = novel.imageUrl,
                contentDescription = novel.novelName,
                width = 56.dp,
                height = 80.dp,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Novel info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = novel.novelName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${novel.remainingDownloads} chapters remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = when (novel.status) {
                        NovelDownloadStatus.DOWNLOADING -> "Downloading"
                        NovelDownloadStatus.PAUSED -> "Paused"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (novel.status) {
                        NovelDownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
                        NovelDownloadStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Pause / Resume button
            IconButton(onClick = onPauseResume) {
                Icon(
                    imageVector = when (novel.status) {
                        NovelDownloadStatus.DOWNLOADING -> Icons.Default.Pause
                        NovelDownloadStatus.PAUSED -> Icons.Default.PlayArrow
                    },
                    contentDescription = when (novel.status) {
                        NovelDownloadStatus.DOWNLOADING -> "Pause downloads"
                        NovelDownloadStatus.PAUSED -> "Resume downloads"
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete downloads",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    novelName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Downloads") },
        text = {
            Text("Are you sure you want to delete all downloads for \"$novelName\"?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
