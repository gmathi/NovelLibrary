package io.github.gmathi.novellibrary.compose.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.common.URLImage

// Data class for display Novel
data class Novel(
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
    val lastUpdated: String = "",
    val genres: List<String> = emptyList(),
    val shortDescription: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchUrlNovelItem(novel: Novel, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Cover image + rating + origin
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (novel.coverUrl.isNotBlank()) {
                    URLImage(
                        imageUrl = novel.coverUrl,
                        contentDescription = novel.title,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        size = 90.dp,
                        shape = RoundedCornerShape(6.dp),
                        contentScale = ContentScale.Crop,
                        errorContent = {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(90.dp, 120.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Star rating row
                if (novel.rating > 0f) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val fullStars = novel.rating.toInt()
                        val hasHalf = (novel.rating - fullStars) >= 0.3f
                        repeat(5) { i ->
                            val starColor = if (i < fullStars || (i == fullStars && hasHalf))
                                Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            Text(
                                text = if (i < fullStars) "★" else if (i == fullStars && hasHalf) "★" else "☆",
                                style = MaterialTheme.typography.labelSmall,
                                color = starColor
                            )
                        }
                    }
                }

                // Origin + rating text (e.g. "KR (4.5)")
                if (novel.originMarker.isNotBlank() || novel.rating > 0f) {
                    val originColor = when (novel.originMarker) {
                        "KR" -> Color(0xFFE53935)
                        "CN" -> Color(0xFFFF8F00)
                        "JP" -> Color(0xFF1E88E5)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Text(
                        text = buildString {
                            if (novel.originMarker.isNotBlank()) append(novel.originMarker)
                            if (novel.rating > 0f) {
                                if (isNotEmpty()) append(" ")
                                append("(${String.format("%.1f", novel.rating)})")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = originColor
                    )
                }
            }

            // Right: Title, stats, genres, description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Rank + Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (novel.rank.isNotBlank()) {
                        Text(
                            text = novel.rank,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = novel.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row 2: Stats line
                val stats = listOfNotNull(
                    novel.chapters.takeIf { it.isNotBlank() }?.let { "📖 $it" },
                    novel.frequency.takeIf { it.isNotBlank() }?.let { "⚡ $it" },
                    novel.readers.takeIf { it.isNotBlank() }?.let { "👤 $it" },
                    novel.reviews.takeIf { it.isNotBlank() }?.let { "✏ $it" },
                    novel.lastUpdated.takeIf { it.isNotBlank() }?.let { "📅 $it" }
                )
                if (stats.isNotEmpty()) {
                    Text(
                        text = stats.joinToString("  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Row 3: Genre tags
                if (novel.genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        novel.genres.forEach { genre ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = genre,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // Row 4: Short description
                if (novel.shortDescription.isNotBlank()) {
                    Text(
                        text = novel.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Ongoing" -> Color(0xFF4CAF50)
        "Completed" -> Color(0xFF2196F3)
        "Hiatus" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
fun ResultsHeader(count: Int, isSearching: Boolean) {
    if (isSearching || count > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count ${if (count == 1) "novel" else "novels"} found",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(message: String, icon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WelcomeContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Search for Novels",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Try searching for your favorite novels\nor browse by category",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Simpler list item for search results — shows cover, title, rating, and origin marker.
 * Used when displaying search-by-term results (as opposed to the richer browse items).
 */
@Composable
fun SearchResultsNovelItem(novel: Novel, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover image (circular)
            if (novel.coverUrl.isNotBlank()) {
                URLImage(
                    imageUrl = novel.coverUrl,
                    contentDescription = novel.title,
                    modifier = Modifier.clip(RoundedCornerShape(50)),
                    size = 56.dp,
                    shape = RoundedCornerShape(50),
                    contentScale = ContentScale.Crop,
                    errorContent = {
                        Icon(
                            imageVector = Icons.Filled.Book,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Title + origin + rating
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (novel.originMarker.isNotBlank() || novel.rating > 0f) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (novel.originMarker.isNotBlank()) {
                            val originColor = when (novel.originMarker) {
                                "KR" -> Color(0xFFE53935)
                                "CN" -> Color(0xFFFF8F00)
                                "JP" -> Color(0xFF1E88E5)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Text(
                                text = novel.originMarker,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = originColor
                            )
                        }
                        if (novel.rating > 0f) {
                            Text(
                                text = "★ ${String.format("%.1f", novel.rating)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }
        }
    }
}
