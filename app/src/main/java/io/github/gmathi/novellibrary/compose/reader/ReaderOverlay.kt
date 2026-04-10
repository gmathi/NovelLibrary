package io.github.gmathi.novellibrary.compose.reader

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.viewmodel.ReaderViewModel

/**
 * Full-screen overlay for the reader that provides top bar, bottom bar,
 * and settings panel. This is meant to be placed on top of the existing
 * ViewPager/WebView content via a ComposeView.
 *
 * When the bars are hidden, the center area passes touches through
 * (no background scrim) so the WebView underneath remains interactive.
 */
@Composable
fun ReaderOverlay(
    viewModel: ReaderViewModel,
    isVisible: Boolean,
    novelName: String,
    onBackPress: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onFontClick: () -> Unit,
    onReadAloudClick: () -> Unit,
    onBrowserClick: () -> Unit,
    onMoreSettingsClick: () -> Unit,
    onCenterTap: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    NovelLibraryTheme(overrideDarkTheme = uiState.isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Semi-transparent scrim when overlay is visible — tap to dismiss
            if (isVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCenterTap() }
                )
            }

            // Top bar
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    novelName = novelName,
                    chapterTitle = uiState.chapterTitle,
                    onBackPress = onBackPress,
                    onMoreSettingsClick = onMoreSettingsClick
                )
            }

            // Bottom bar
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderBottomBar(
                    uiState = uiState,
                    onPreviousChapter = onPreviousChapter,
                    onNextChapter = onNextChapter,
                    onSettingsClick = { viewModel.toggleSettingsPanel() },
                    onFontClick = onFontClick,
                    onReadAloudClick = onReadAloudClick,
                    onBrowserClick = onBrowserClick
                )
            }

            // Settings bottom sheet
            if (uiState.isSettingsPanelVisible) {
                ReaderSettingsPanel(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { viewModel.hideSettingsPanel() },
                    onMoreSettings = onMoreSettingsClick
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    novelName: String,
    chapterTitle: String,
    onBackPress: () -> Unit,
    onMoreSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPress) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = novelName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (chapterTitle.isNotBlank()) {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onMoreSettingsClick) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "More settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
