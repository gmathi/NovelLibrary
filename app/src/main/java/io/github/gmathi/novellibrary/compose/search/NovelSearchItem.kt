package io.github.gmathi.novellibrary.compose.search

// UI model for displaying novel info in search composables
data class NovelSearchItem(
    val title: String,
    val category: String,
    val rating: Float,
    val author: String,
    val status: String,
    val coverUrl: String,
    val rank: String = "",
    val originMarker: String = "",
    val chapters: String = "",
    val frequency: String = "",
    val readers: String = "",
    val reviews: String = "",
    val pages: String = "",
    val views: String = "",
    val lastUpdated: String = "",
    val genres: List<String> = emptyList(),
    val shortDescription: String = ""
)
