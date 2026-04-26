package io.github.gmathi.novellibrary.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.database.*
import io.github.gmathi.novellibrary.model.database.Download
import io.github.gmathi.novellibrary.model.other.DownloadNovelEvent
import io.github.gmathi.novellibrary.model.other.DownloadWebPageEvent
import io.github.gmathi.novellibrary.model.other.EventType
import io.github.gmathi.novellibrary.model.ui.*
import io.github.gmathi.novellibrary.service.download.DownloadNovelService
import io.github.gmathi.novellibrary.service.download.ServiceActionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

class DownloadsViewModel : ViewModel() {

    private val dbHelper: DBHelper by injectLazy()

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val _chapterListState = MutableStateFlow(ChapterListUiState())
    val chapterListState: StateFlow<ChapterListUiState> = _chapterListState.asStateFlow()

    /** Set by the Activity so the ViewModel can trigger service actions. */
    var serviceActionHandler: ServiceActionHandler? = null

    // ── Data loading ────────────────────────────────────────────────────

    fun loadDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val novels = withContext(Dispatchers.IO) {
                val allDownloads = dbHelper.getAllDownloads()
                allDownloads.toNovelDownloadUiModels(dbHelper)
            }
            _uiState.update { it.copy(novels = novels, isLoading = false) }
        }
    }

    fun selectNovel(novelId: Long) {
        _uiState.update { it.copy(selectedNovelId = novelId) }
        viewModelScope.launch {
            _chapterListState.update { it.copy(isLoading = true) }
            val (novelName, chapters) = withContext(Dispatchers.IO) {
                val novel = dbHelper.getNovel(novelId)
                val downloads = dbHelper.getAllDownloadsForNovel(novelId)
                val name = novel?.name ?: downloads.firstOrNull()?.novelName ?: ""
                name to downloads.toChapterDownloadUiModels()
            }
            _chapterListState.update {
                ChapterListUiState(
                    novelId = novelId,
                    novelName = novelName,
                    chapters = chapters,
                    isLoading = false
                )
            }
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedNovelId = null) }
        _chapterListState.value = ChapterListUiState()
    }

    // ── Novel-level actions ─────────────────────────────────────────────

    fun pauseNovel(novelId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.updateDownloadStatusNovelId(Download.STATUS_PAUSED, novelId)
            }
            serviceActionHandler?.handleAction(novelId, DownloadNovelService.ACTION_PAUSE)
            refreshNovelInList(novelId)
        }
    }

    fun resumeNovel(novelId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.updateDownloadStatusNovelId(Download.STATUS_IN_QUEUE, novelId)
            }
            val handler = serviceActionHandler
            if (handler != null && handler.isServiceRunning()) {
                handler.handleAction(novelId, DownloadNovelService.ACTION_START)
            } else {
                handler?.startService(novelId)
            }
            refreshNovelInList(novelId)
        }
    }

    fun deleteNovel(novelId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                dbHelper.deleteDownloads(novelId)
            }
            serviceActionHandler?.handleAction(novelId, DownloadNovelService.ACTION_REMOVE)
            _uiState.update { state ->
                state.copy(
                    novels = state.novels.filter { it.novelId != novelId },
                    selectedNovelId = if (state.selectedNovelId == novelId) null else state.selectedNovelId
                )
            }
            if (_uiState.value.selectedNovelId == null && _chapterListState.value.novelId == novelId) {
                _chapterListState.value = ChapterListUiState()
            }
        }
    }

    // ── Chapter-level actions ───────────────────────────────────────────

    fun deleteChapter(webPageUrl: String) {
        viewModelScope.launch {
            val currentNovelId = _chapterListState.value.novelId
            withContext(Dispatchers.IO) {
                dbHelper.deleteDownload(webPageUrl)
            }
            // Refresh chapter list from DB
            val chapters = withContext(Dispatchers.IO) {
                dbHelper.getAllDownloadsForNovel(currentNovelId).toChapterDownloadUiModels()
            }
            if (chapters.isEmpty()) {
                // Last chapter removed — navigate back and remove novel from list
                clearSelection()
                _uiState.update { state ->
                    state.copy(novels = state.novels.filter { it.novelId != currentNovelId })
                }
            } else {
                _chapterListState.update { it.copy(chapters = chapters) }
                refreshNovelInList(currentNovelId)
            }
        }
    }

    // ── Event handlers (called from Activity's DownloadListener) ────────

    fun onDownloadWebPageEvent(event: DownloadWebPageEvent) {
        val download = event.download

        // If the event is for a novel not currently in our list (e.g. a new download
        // added from ChaptersPagerActivity), refresh the full novel list to pick it up.
        val novelInList = _uiState.value.novels.any { it.novelId == download.novelId }
        if (!novelInList) {
            loadDownloads()
            return
        }

        if (event.type == EventType.COMPLETE) {
            // Chapter finished downloading — remove it from the chapter list
            if (_chapterListState.value.novelId == download.novelId) {
                val updatedChapters = _chapterListState.value.chapters
                    .filter { it.webPageUrl != download.webPageUrl }
                if (updatedChapters.isEmpty()) {
                    // All chapters for this novel are done — go back to novel list
                    clearSelection()
                    _uiState.update { state ->
                        state.copy(novels = state.novels.filter { it.novelId != download.novelId })
                    }
                } else {
                    _chapterListState.update { it.copy(chapters = updatedChapters) }
                    // Optimistically decrement remaining count in-memory instead of querying DB
                    decrementRemainingDownloads(download.novelId)
                }
            } else {
                // Not viewing this novel's chapters — optimistically decrement
                decrementRemainingDownloads(download.novelId)
            }
        } else {
            val newChapterStatus = ChapterDownloadStatus.fromDownloadStatus(download.status)

            // Update chapter list if we're viewing this novel
            if (_chapterListState.value.novelId == download.novelId) {
                _chapterListState.update { state ->
                    state.copy(
                        chapters = state.chapters.map { chapter ->
                            if (chapter.webPageUrl == download.webPageUrl) {
                                chapter.copy(status = newChapterStatus)
                            } else {
                                chapter
                            }
                        }
                    )
                }
            }

            // Update novel-level status
            _uiState.update { state ->
                state.copy(
                    novels = state.novels.map {
                        if (it.novelId == download.novelId) {
                            it.copy(status = NovelDownloadStatus.DOWNLOADING)
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    fun onDownloadNovelEvent(event: DownloadNovelEvent) {
        when (event.type) {
            EventType.INSERT -> {
                // A new novel was added to the download queue — refresh the full list
                // to pick up novels added from other screens (e.g. ChaptersPagerActivity).
                loadDownloads()
            }
            EventType.RUNNING -> {
                val novelInList = _uiState.value.novels.any { it.novelId == event.novelId }
                if (!novelInList) {
                    // Novel not in our list yet — full refresh to pick it up.
                    loadDownloads()
                } else {
                    _uiState.update { state ->
                        state.copy(
                            novels = state.novels.map {
                                if (it.novelId == event.novelId) it.copy(status = NovelDownloadStatus.DOWNLOADING)
                                else it
                            }
                        )
                    }
                }
            }
            EventType.PAUSED -> {
                val novelInList = _uiState.value.novels.any { it.novelId == event.novelId }
                if (!novelInList) {
                    // Novel not in our list yet — full refresh to pick it up.
                    loadDownloads()
                } else {
                    _uiState.update { state ->
                        state.copy(
                            novels = state.novels.map {
                                if (it.novelId == event.novelId) it.copy(status = NovelDownloadStatus.PAUSED)
                                else it
                            }
                        )
                    }
                }
            }
            EventType.DELETE, EventType.COMPLETE -> {
                _uiState.update { state ->
                    state.copy(novels = state.novels.filter { it.novelId != event.novelId })
                }
                if (_chapterListState.value.novelId == event.novelId) {
                    clearSelection()
                }
            }
            else -> { /* ignore */ }
        }
    }

    fun onServiceDisconnected() {
        _uiState.update { state ->
            state.copy(
                novels = state.novels.map { novel ->
                    if (novel.status == NovelDownloadStatus.DOWNLOADING) {
                        novel.copy(status = NovelDownloadStatus.PAUSED)
                    } else {
                        novel
                    }
                }
            )
        }
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Re-reads a single novel's download entries from the DB and updates
     * its row in the novel list without reloading everything.
     */
    private fun refreshNovelInList(novelId: Long) {
        viewModelScope.launch {
            val updated = withContext(Dispatchers.IO) {
                val downloads = dbHelper.getAllDownloadsForNovel(novelId)
                if (downloads.isEmpty()) return@withContext null
                downloads.toNovelDownloadUiModels(dbHelper).firstOrNull()
            }
            _uiState.update { state ->
                if (updated == null) {
                    state.copy(novels = state.novels.filter { it.novelId != novelId })
                } else {
                    state.copy(
                        novels = state.novels.map { if (it.novelId == novelId) updated else it }
                    )
                }
            }
        }
    }

    /**
     * Derives the novel-level status from the current chapter list state
     * and updates the remaining download count.
     */
    private fun updateNovelStatusFromChapters(novelId: Long) {
        viewModelScope.launch {
            val (remaining, hasActive) = withContext(Dispatchers.IO) {
                val downloads = dbHelper.getAllDownloadsForNovel(novelId)
                val active = downloads.any {
                    it.status == Download.STATUS_IN_QUEUE || it.status == Download.STATUS_RUNNING
                }
                downloads.size to active
            }
            _uiState.update { state ->
                state.copy(
                    novels = state.novels.map {
                        if (it.novelId == novelId) {
                            it.copy(
                                remainingDownloads = remaining,
                                status = if (hasActive) NovelDownloadStatus.DOWNLOADING else NovelDownloadStatus.PAUSED
                            )
                        } else {
                            it
                        }
                    }
                )
            }
        }
    }

    /**
     * Optimistically decrements the remaining download count for a novel
     * without querying the DB. This keeps the UI responsive when chapters
     * complete in rapid succession.
     */
    private fun decrementRemainingDownloads(novelId: Long) {
        _uiState.update { state ->
            state.copy(
                novels = state.novels.map {
                    if (it.novelId == novelId) {
                        val newRemaining = (it.remainingDownloads - 1).coerceAtLeast(0)
                        it.copy(remainingDownloads = newRemaining)
                    } else {
                        it
                    }
                }
            )
        }
    }
}
