package io.github.gmathi.novellibrary.compose.downloads

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gmathi.novellibrary.viewmodel.DownloadsViewModel

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chapterListState by viewModel.chapterListState.collectAsStateWithLifecycle()

    // Handle system back button
    BackHandler(enabled = uiState.selectedNovelId != null) {
        viewModel.clearSelection()
    }

    if (uiState.selectedNovelId != null) {
        ChapterListView(
            chapterListState = chapterListState,
            onCancelChapter = { webPageUrl -> viewModel.deleteChapter(webPageUrl) },
            onBackClick = { viewModel.clearSelection() }
        )
    } else {
        NovelListView(
            uiState = uiState,
            onNovelClick = { novelId -> viewModel.selectNovel(novelId) },
            onPause = { novelId -> viewModel.pauseNovel(novelId) },
            onResume = { novelId -> viewModel.resumeNovel(novelId) },
            onDelete = { novelId -> viewModel.deleteNovel(novelId) },
            onBackClick = onBackClick
        )
    }
}
