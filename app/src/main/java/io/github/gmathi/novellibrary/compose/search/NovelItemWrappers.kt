package io.github.gmathi.novellibrary.compose.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

/**
 * Maps a DbNovel to a NovelSearchItem UI model.
 * Extracted so both wrappers share the same logic and the result can be memoized.
 */
private fun DbNovel.toSearchItem() = NovelSearchItem(
    title = name ?: "Unknown",
    category = genres?.firstOrNull() ?: "",
    rating = rating?.toFloatOrNull() ?: 0f,
    author = metadata["Author"] ?: authors?.firstOrNull() ?: "",
    status = metadata["Status"] ?: "",
    coverUrl = imageUrl ?: "",
    rank = metadata["Rank"] ?: "",
    originMarker = metadata["OriginMarker"] ?: "",
    chapters = metadata["Chapters"] ?: "",
    frequency = metadata["Frequency"] ?: "",
    readers = metadata["Readers"] ?: "",
    reviews = metadata["Reviews"] ?: "",
    pages = metadata["Pages"] ?: "",
    views = metadata["Views"] ?: "",
    lastUpdated = metadata["LastUpdated"] ?: "",
    genres = genres ?: emptyList(),
    shortDescription = shortDescription ?: ""
)

/**
 * Rich card item for browse/URL results (popular, best rated, etc.).
 * Maps DbNovel to the SearchUrlNovelItem composable.
 */
@Composable
fun SearchUrlNovelItemWrapper(novel: DbNovel, onClick: () -> Unit = {}) {
    val displayNovel = remember(novel.id, novel.url) { novel.toSearchItem() }
    SearchUrlNovelItem(novel = displayNovel, onClick = onClick)
}

/**
 * Rich card item for search-by-term results.
 * Now uses the same rich card as browse results since series-finder returns full data.
 */
@Composable
fun SearchTermResultItem(novel: DbNovel, onClick: () -> Unit = {}) {
    val displayNovel = remember(novel.id, novel.url) { novel.toSearchItem() }
    SearchUrlNovelItem(novel = displayNovel, onClick = onClick)
}
