package io.github.gmathi.novellibrary.compose.reader

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    isMenuIconVisible: Boolean,
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

            // Floating menu icon — shown only when the overlay is hidden.
            // The "reader mode button" preference enables the feature; when enabled
            // the icon auto-hides on scroll down and reappears on scroll up / tap.
            AnimatedVisibility(
                visible = !isVisible && uiState.isReaderModeButtonVisible && isMenuIconVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = onCenterTap,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Open reader menu",
                        modifier = Modifier.size(28.dp)
                    )
                }
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
                    onBackPress = onBackPress
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
    onBackPress: () -> Unit
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

                // Balances the back button so the title stays centred. The old "More settings"
                // action lived here; the full settings list is reachable from the settings sheet
                // ("More options") and from the app's Settings > Reader.
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}
