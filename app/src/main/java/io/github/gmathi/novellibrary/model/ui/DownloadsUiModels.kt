package io.github.gmathi.novellibrary.model.ui

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.model.database.Download

/**
 * Top-level UI state for the downloads screen.
 */
data class DownloadsUiState(
    val novels: List<NovelDownloadUiModel> = emptyList(),
    val selectedNovelId: Long? = null,
    val isLoading: Boolean = true
)

/**
 * Aggregate download info for a single novel in the novel list.
 */
data class NovelDownloadUiModel(
    val novelId: Long,
    val novelName: String,
    val imageUrl: String?,
    val remainingDownloads: Int,
    val status: NovelDownloadStatus
)

enum class NovelDownloadStatus {
    DOWNLOADING,
    PAUSED
}

/**
 * UI state for the chapter-level detail view.
 */
data class ChapterListUiState(
    val novelId: Long = -1L,
    val novelName: String = "",
    val chapters: List<ChapterDownloadUiModel> = emptyList(),
    val isLoading: Boolean = true
)

/**
 * Individual chapter download info.
 */
data class ChapterDownloadUiModel(
    val webPageUrl: String,
    val chapterName: String,
    val orderId: Int,
    val status: ChapterDownloadStatus
)

enum class ChapterDownloadStatus {
    IN_QUEUE,
    RUNNING,
    PAUSED;

    companion object {
        fun fromDownloadStatus(status: Int): ChapterDownloadStatus = when (status) {
            Download.STATUS_IN_QUEUE -> IN_QUEUE
            Download.STATUS_PAUSED -> PAUSED
            Download.STATUS_RUNNING -> RUNNING
            else -> IN_QUEUE
        }
    }
}

// -- Mapping functions --

/**
 * Groups downloads by novelId and maps each group to a [NovelDownloadUiModel].
 * Queries [DBHelper] for novel name and cover image; falls back to the
 * [Download.novelName] field when the novel record is missing.
 */
fun List<Download>.toNovelDownloadUiModels(dbHelper: DBHelper): List<NovelDownloadUiModel> {
    return groupBy { it.novelId }.map { (novelId, downloads) ->
        val novel = dbHelper.getNovel(novelId)
        val hasActive = downloads.any {
            it.status == Download.STATUS_IN_QUEUE || it.status == Download.STATUS_RUNNING
        }
        NovelDownloadUiModel(
            novelId = novelId,
            novelName = novel?.name ?: downloads.first().novelName,
            imageUrl = novel?.imageUrl,
            remainingDownloads = downloads.size,
            status = if (hasActive) NovelDownloadStatus.DOWNLOADING else NovelDownloadStatus.PAUSED
        )
    }
}

/**
 * Maps a list of [Download] entries (for a single novel) to
 * [ChapterDownloadUiModel] instances, sorted ascending by [Download.orderId].
 */
fun List<Download>.toChapterDownloadUiModels(): List<ChapterDownloadUiModel> {
    return map { download ->
        ChapterDownloadUiModel(
            webPageUrl = download.webPageUrl,
            chapterName = download.chapter,
            orderId = download.orderId,
            status = ChapterDownloadStatus.fromDownloadStatus(download.status)
        )
    }.sortedBy { it.orderId }
}
