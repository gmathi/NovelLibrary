package io.github.gmathi.novellibrary.compose.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.common.URLImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchUrlNovelItem(novel: NovelSearchItem, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left: Cover image + compact rating overlay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(70.dp)
            ) {
                if (novel.coverUrl.isNotBlank()) {
                    URLImage(
                        imageUrl = novel.coverUrl,
                        contentDescription = novel.title,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
                        width = 70.dp,
                        height = 100.dp,
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
                            .size(70.dp, 100.dp)
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

                if (novel.originMarker.isNotBlank() || novel.rating > 0f) {
                    val originColor = when (novel.originMarker) {
                        "KR" -> Color(0xFFE53935)
                        "CN" -> Color(0xFFFF8F00)
                        "JP" -> Color(0xFF1E88E5)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (novel.originMarker.isNotBlank()) {
                            Text(
                                text = novel.originMarker,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = originColor
                            )
                        }
                        if (novel.rating > 0f) {
                            Text(
                                text = " ★ ${String.format("%.1f", novel.rating)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFB300)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusColor = when (novel.status.lowercase()) {
                    "completed" -> Color(0xFF4CAF50)
                    "ongoing" -> Color(0xFFFF9800)
                    else -> Color(0xFF9E9E9E)
                }
                Text(
                    text = buildAnnotatedString {
                        if (novel.rank.isNotBlank()) {
                            withStyle(SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.labelMedium.fontSize
                            )) {
                                append(novel.rank.trim())
                                append(" ")
                            }
                        }
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )) {
                            append(novel.title.trim())
                        }
                    },
                    style = MaterialTheme.typography.titleSmall
                )
                if (novel.status.isNotBlank()) {
                    Text(
                        text = novel.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier
                            .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                val stats = listOfNotNull(
                    novel.chapters.takeIf { it.isNotBlank() }?.let { "📖\u00A0$it" },
                    novel.frequency.takeIf { it.isNotBlank() }?.let { "⚡\u00A0$it" },
                    novel.readers.takeIf { it.isNotBlank() }?.let { "👤\u00A0$it" },
                    novel.reviews.takeIf { it.isNotBlank() }?.let { "✏\u00A0$it" },
                    novel.pages.takeIf { it.isNotBlank() }?.let { "📄\u00A0$it" },
                    novel.views.takeIf { it.isNotBlank() }?.let { "👁\u00A0$it" },
                    novel.lastUpdated.takeIf { it.isNotBlank() }?.let { "📅\u00A0$it" }
                )
                if (stats.isNotEmpty()) {
                    Text(
                        text = stats.joinToString(" \u00A0"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
