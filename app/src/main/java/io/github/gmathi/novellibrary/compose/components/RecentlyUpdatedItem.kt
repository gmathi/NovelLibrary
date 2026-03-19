package io.github.gmathi.novellibrary.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem

@Composable
fun RecentlyUpdatedItemView(
    item: RecentlyUpdatedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEvenPosition: Boolean = false
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.novelName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Show chapter and publisher if available
            val subtitle = buildString {
                item.chapterName?.takeIf { it.isNotBlank() }?.let { append(it) }
                if (item.chapterName?.isNotBlank() == true && item.publisherName?.isNotBlank() == true) {
                    append(" • ")
                }
                item.publisherName?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
            
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Preview Parameter Provider
private class RecentlyUpdatedItemPreviewProvider : PreviewParameterProvider<RecentlyUpdatedItem> {
    override val values = sequenceOf(
        RecentlyUpdatedItem().apply {
            novelName = "The Beginning After The End"
            chapterName = "Chapter 456: The Final Battle"
            publisherName = "Tapas"
            novelUrl = "https://example.com/novel"
        },
        RecentlyUpdatedItem().apply {
            novelName = "Solo Leveling"
            chapterName = "Chapter 270: Epilogue"
            publisherName = "Webnovel"
            novelUrl = "https://example.com/novel2"
        },
        RecentlyUpdatedItem().apply {
            novelName = "A Very Long Novel Title That Should Be Truncated With Ellipsis"
            chapterName = "Chapter 999: A Very Long Chapter Name That Also Needs Truncation"
            publisherName = "LongPublisherName"
            novelUrl = "https://example.com/novel3"
        }
    )
}

@Preview(name = "Recently Updated Item - Light", showBackground = true)
@Composable
private fun RecentlyUpdatedItemPreview(
    @PreviewParameter(RecentlyUpdatedItemPreviewProvider::class) item: RecentlyUpdatedItem
) {
    MaterialTheme {
        RecentlyUpdatedItemView(
            item = item,
            onClick = {},
            isEvenPosition = false
        )
    }
}

@Preview(name = "Recently Updated Item - Even Position", showBackground = true)
@Composable
private fun RecentlyUpdatedItemEvenPreview() {
    MaterialTheme {
        RecentlyUpdatedItemView(
            RecentlyUpdatedItem().apply {
                novelName = "Omniscient Reader's Viewpoint"
                chapterName = "Chapter 551: Epilogue (End)"
                publisherName = "Munpia"
                novelUrl = "https://example.com/novel"
            }, {}, isEvenPosition = true
        )
    }
}

@Preview(name = "Recently Updated Item - Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecentlyUpdatedItemDarkPreview() {
    MaterialTheme {
        RecentlyUpdatedItemView(
            item = RecentlyUpdatedItem().apply {
                novelName = "Reverend Insanity"
                chapterName = "Chapter 2334: Fang Yuan's Victory"
                publisherName = "Qidian"
                novelUrl = "https://example.com/novel"
            },
            onClick = {},
            isEvenPosition = false
        )
    }
}

@Preview(name = "Recently Updated List", showBackground = true, heightDp = 400)
@Composable
private fun RecentlyUpdatedListPreview() {
    MaterialTheme {
        LazyColumn {
            itemsIndexed(
                listOf(
                    RecentlyUpdatedItem().apply {
                        novelName = "The Beginning After The End"
                        chapterName = "Chapter 456"
                        publisherName = "Tapas"
                    },
                    RecentlyUpdatedItem().apply {
                        novelName = "Solo Leveling"
                        chapterName = "Chapter 270"
                        publisherName = "Webnovel"
                    },
                    RecentlyUpdatedItem().apply {
                        novelName = "Omniscient Reader"
                        chapterName = "Chapter 551"
                        publisherName = "Munpia"
                    }
                )
            ) { index, item ->
                RecentlyUpdatedItemView(
                    item = item,
                    onClick = {},
                    isEvenPosition = index % 2 == 0
                )
            }
        }
    }
}
