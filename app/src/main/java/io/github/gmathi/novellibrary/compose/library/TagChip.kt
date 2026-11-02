package io.github.gmathi.novellibrary.compose.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.model.database.NovelTag

/**
 * A tag rendered as a pill with a translucent fill of its color and a solid border of the same
 * color, so tags read as visually distinct from one another regardless of theme. Falls back to a
 * stable name-derived color when the tag has no stored color.
 */
@Composable
fun TagChip(
    tag: NovelTag,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val base = TagColors.resolveTagColor(tag.color, tag.name)
    val fill = if (selected) base.copy(alpha = 0.32f) else base.copy(alpha = 0.16f)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = fill,
        border = BorderStroke(if (selected) 2.dp else 1.dp, base),
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {
        Text(
            text = tag.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A small circular color swatch used in the tag manager and color picker. */
@Composable
fun ColorSwatch(
    color: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color,
        border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
        onClick = onClick ?: {},
        enabled = onClick != null
    ) {}
}
