package io.github.gmathi.novellibrary.compose.search

import androidx.compose.runtime.Composable
import io.github.gmathi.novellibrary.model.database.Novel as DbNovel

@Composable
fun SearchResultsNovelItem(novel: DbNovel) {
    val displayNovel = Novel(
        title = novel.name ?: "Unknown",
        category = novel.genres?.firstOrNull() ?: "",
        rating = try { novel.rating?.toFloat() ?: 0f } catch (e: Exception) { 0f },
        author = novel.metadata["Author"] ?: novel.authors?.firstOrNull() ?: "",
        status = novel.metadata["Status"] ?: "",
        coverUrl = novel.imageUrl ?: ""
    )
    NovelCard(novel = displayNovel)
}
