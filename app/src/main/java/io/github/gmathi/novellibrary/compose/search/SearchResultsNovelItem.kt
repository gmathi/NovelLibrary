package io.github.gmathi.novellibrary.compose.search

import androidx.compose.runtime.Composable
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

@Composable
fun SearchResultsNovelItem(novel: DbNovel, onClick: () -> Unit = {}) {
    val displayNovel = Novel(
        title = novel.name ?: "Unknown",
        category = novel.genres?.firstOrNull() ?: "",
        rating = try { novel.rating?.toFloat() ?: 0f } catch (e: Exception) { 0f },
        author = novel.metadata["Author"] ?: novel.authors?.firstOrNull() ?: "",
        status = novel.metadata["Status"] ?: "",
        coverUrl = novel.imageUrl ?: "",
        rank = novel.metadata["Rank"] ?: "",
        originMarker = novel.metadata["OriginMarker"] ?: "",
        chapters = novel.metadata["Chapters"] ?: "",
        frequency = novel.metadata["Frequency"] ?: "",
        readers = novel.metadata["Readers"] ?: "",
        reviews = novel.metadata["Reviews"] ?: "",
        lastUpdated = novel.metadata["LastUpdated"] ?: "",
        genres = novel.genres ?: emptyList(),
        shortDescription = novel.shortDescription ?: ""
    )
    NovelSearchItem(novel = displayNovel, onClick = onClick)
}
