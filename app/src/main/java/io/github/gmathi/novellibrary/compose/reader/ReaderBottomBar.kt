package io.github.gmathi.novellibrary.compose.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.viewmodel.ReaderUiState

@Composable
fun ReaderBottomBar(
    uiState: ReaderUiState,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSettingsClick: () -> Unit,
    onFontClick: () -> Unit,
    onReadAloudClick: () -> Unit,
    onBrowserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Chapter navigation row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Always enabled: on the first/last chapter the activity jumps to the start/end
                // of the current chapter instead (first/last page in page mode).
                IconButton(onClick = onPreviousChapter) {
                    Icon(
                        Icons.Filled.ChevronLeft,
                        contentDescription = "Previous chapter",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (uiState.chapterTitle.isNotBlank()) {
                        Text(
                            text = uiState.chapterTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (uiState.totalChapters > 0) {
                        Text(
                            text = "${uiState.currentChapterIndex + 1} / ${uiState.totalChapters}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isPageMode && uiState.totalPages > 0) {
                        Text(
                            text = "Page ${uiState.currentPage + 1} / ${uiState.totalPages}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onNextChapter) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = "Next chapter",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarAction(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    onClick = onSettingsClick
                )
                BottomBarAction(
                    icon = Icons.Outlined.TextFields,
                    label = "Font",
                    onClick = onFontClick
                )
                BottomBarAction(
                    icon = Icons.Outlined.RecordVoiceOver,
                    label = "Read Aloud",
                    onClick = onReadAloudClick
                )
                BottomBarAction(
                    icon = Icons.Outlined.OpenInBrowser,
                    label = "Browser",
                    onClick = onBrowserClick
                )
            }
        }
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
