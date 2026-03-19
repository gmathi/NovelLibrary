package io.github.gmathi.novellibrary.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.common.URLImage
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants

@Composable
fun NovelItem(
    novel: Novel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image loading with URLImage
        URLImage(
            imageUrl = novel.imageUrl,
            contentDescription = novel.name,
            modifier = Modifier.clip(CircleShape),
            size = 56.dp,
            shape = CircleShape,
            contentScale = ContentScale.Crop
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = novel.name ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Display star rating
            novel.rating?.let { rating ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val ratingValue = try {
                        rating.toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                    
                    // Display 5 stars
                    repeat(5) { index ->
                        Text(
                            text = if (index < ratingValue.toInt()) "★" else "☆",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index < ratingValue.toInt()) 
                                Color(0xFFFFC107) // Yellow color for filled stars
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = "(${String.format("%.1f", ratingValue)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Preview Parameter Provider
private class NovelPreviewProvider : PreviewParameterProvider<Novel> {
    override val values = sequenceOf(
        // Novel with all data
        Novel("The Beginning After The End", "https://example.com/novel1", Constants.SourceId.NOVEL_UPDATES).apply {
            imageUrl = "https://example.com/image1.jpg"
            rating = "4.8"
            metadata["OriginMarker"] = "Korean"
        },
        // Novel with long title
        Novel(
            "A Very Long Novel Title That Should Be Truncated With Ellipsis After Two Lines Maximum",
            "https://example.com/novel2",
            Constants.SourceId.NOVEL_UPDATES
        ).apply {
            imageUrl = "https://example.com/image2.jpg"
            rating = "4.9"
            metadata["OriginMarker"] = "Chinese"
        },
        // Novel without rating
        Novel("Solo Leveling", "https://example.com/novel3", Constants.SourceId.NOVEL_UPDATES).apply {
            imageUrl = "https://example.com/image3.jpg"
            metadata["OriginMarker"] = "Korean"
        },
        // Novel without origin marker
        Novel("Omniscient Reader's Viewpoint", "https://example.com/novel4", Constants.SourceId.NOVEL_UPDATES).apply {
            imageUrl = "https://example.com/image4.jpg"
            rating = "4.7"
        },
        // Minimal novel (no image, rating, or origin)
        Novel("Unknown Novel", "https://example.com/novel5", Constants.SourceId.NOVEL_UPDATES)
    )
}

@Preview(name = "Novel Item - Complete", showBackground = true)
@Composable
private fun NovelItemPreview(
    @PreviewParameter(NovelPreviewProvider::class) novel: Novel
) {
    MaterialTheme {
        NovelItem(
            novel = novel,
            onClick = {}
        )
    }
}

@Preview(name = "Novel Item - With All Data", showBackground = true)
@Composable
private fun NovelItemCompletePreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel("The Beginning After The End", "https://example.com/novel", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image.jpg"
                rating = "4.8"
                metadata["OriginMarker"] = "Korean"
            },
            onClick = {}
        )
    }
}

@Preview(name = "Novel Item - No Rating", showBackground = true)
@Composable
private fun NovelItemNoRatingPreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel("Solo Leveling", "https://example.com/novel", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image.jpg"
                metadata["OriginMarker"] = "Korean"
            },
            onClick = {}
        )
    }
}

@Preview(name = "Novel Item - No Origin", showBackground = true)
@Composable
private fun NovelItemNoOriginPreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel("Omniscient Reader's Viewpoint", "https://example.com/novel", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image.jpg"
                rating = "4.7"
            },
            onClick = {}
        )
    }
}

@Preview(name = "Novel Item - Minimal Data", showBackground = true)
@Composable
private fun NovelItemMinimalPreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel("Unknown Novel", "https://example.com/novel", Constants.SourceId.NOVEL_UPDATES),
            onClick = {}
        )
    }
}

@Preview(name = "Novel Item - Long Title", showBackground = true)
@Composable
private fun NovelItemLongTitlePreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel(
                "A Very Long Novel Title That Should Be Truncated With Ellipsis After Two Lines Maximum To Prevent Layout Issues",
                "https://example.com/novel",
                Constants.SourceId.NOVEL_UPDATES
            ).apply {
                imageUrl = "https://example.com/image.jpg"
                rating = "4.5"
                metadata["OriginMarker"] = "Chinese"
            },
            onClick = {}
        )
    }
}

@Preview(
    name = "Novel Item - Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NovelItemDarkPreview() {
    MaterialTheme {
        NovelItem(
            novel = Novel("Reverend Insanity", "https://example.com/novel", Constants.SourceId.NOVEL_UPDATES).apply {
                imageUrl = "https://example.com/image.jpg"
                rating = "4.9"
                metadata["OriginMarker"] = "Chinese"
            },
            onClick = {}
        )
    }
}

@Preview(name = "Novel List", showBackground = true, heightDp = 500)
@Composable
private fun NovelListPreview() {
    MaterialTheme {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    Novel("The Beginning After The End", "https://example.com/1", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.8"
                        metadata["OriginMarker"] = "Korean"
                    },
                    Novel("Solo Leveling", "https://example.com/2", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.9"
                        metadata["OriginMarker"] = "Korean"
                    },
                    Novel("Omniscient Reader's Viewpoint", "https://example.com/3", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.7"
                        metadata["OriginMarker"] = "Korean"
                    },
                    Novel("Reverend Insanity", "https://example.com/4", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.9"
                        metadata["OriginMarker"] = "Chinese"
                    },
                    Novel("Lord of the Mysteries", "https://example.com/5", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.8"
                        metadata["OriginMarker"] = "Chinese"
                    }
                )
            ) { novel ->
                NovelItem(
                    novel = novel,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "Novel List - Dark Mode",
    showBackground = true,
    heightDp = 500,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun NovelListDarkPreview() {
    MaterialTheme {
        LazyColumn(
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    Novel("The Beginning After The End", "https://example.com/1", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.8"
                        metadata["OriginMarker"] = "Korean"
                    },
                    Novel("Solo Leveling", "https://example.com/2", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.9"
                        metadata["OriginMarker"] = "Korean"
                    },
                    Novel("Omniscient Reader's Viewpoint", "https://example.com/3", Constants.SourceId.NOVEL_UPDATES).apply {
                        rating = "4.7"
                        metadata["OriginMarker"] = "Korean"
                    }
                )
            ) { novel ->
                NovelItem(
                    novel = novel,
                    onClick = {}
                )
            }
        }
    }
}
