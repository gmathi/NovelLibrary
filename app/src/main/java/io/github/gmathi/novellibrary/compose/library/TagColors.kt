package io.github.gmathi.novellibrary.compose.library

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/**
 * Fixed palette for tag chips. Uses the same `Color(0xFF…)` Material-ish hues the search screens
 * already use, so tag colors sit visually alongside the rest of the app.
 *
 * A tag's color is persisted as an ARGB [Long] in `novel_tag.color`. When a tag has no stored
 * color (null — e.g. tags seeded by the section→tag migration), [resolveTagColor] derives a stable
 * color from the tag's name so every tag still looks distinct without the user having to pick one.
 */
object TagColors {

    /** Selectable swatches, in display order, for the tag color picker. Shared with the data layer. */
    val palette: List<Long> = io.github.gmathi.novellibrary.database.TagPalette.argb

    /** ARGB long of the color a tag should render with, falling back to a name-derived palette hue. */
    fun resolveTagColor(storedColor: Long?, tagName: String): Color {
        val argb = storedColor ?: palette[abs(tagName.hashCode()) % palette.size]
        return Color(argb)
    }

    /** A readable on-color (black/white) for text/border drawn over [fill]. */
    fun contentColorFor(fill: Color): Color {
        // Perceived luminance (Rec. 601); dark text on light fills, light text on dark fills.
        val luminance = 0.299f * fill.red + 0.587f * fill.green + 0.114f * fill.blue
        return if (luminance > 0.6f) Color(0xFF1A1A1A) else Color.White
    }
}
