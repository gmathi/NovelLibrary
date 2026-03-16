package io.github.gmathi.novellibrary.compose.search

import androidx.compose.runtime.Composable
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

/**
 * Rich card item for browse/URL results (popular, best rated, etc.).
 * Maps DbNovel to the SearchUrlNovelItem composable.
 */
@Composable
fun SearchUrlNovelItemWrapper(novel: DbNovel, onClick: () -> Unit = {}) {
    val displayNovel = NovelSearchItem(
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
    SearchUrlNovelItem(novel = displayNovel, onClick = onClick)
}

/**
 * Rich card item for search-by-term results.
 * Now uses the same rich card as browse results since series-finder returns full data.
 */
@Composable
fun SearchTermResultItem(novel: DbNovel, onClick: () -> Unit = {}) {
    val displayNovel = NovelSearchItem(
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
    SearchUrlNovelItem(novel = displayNovel, onClick = onClick)
}
