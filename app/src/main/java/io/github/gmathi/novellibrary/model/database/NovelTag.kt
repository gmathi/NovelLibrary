package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-defined label that can be applied to any number of novels. Tags replace the
 * single-section model used by the legacy library: a novel may carry many tags (via
 * [NovelTagCrossRef]) and the new library can filter by them.
 *
 * On the first run after the tag migration, one tag is seeded per existing novel-section name
 * (including the implicit default "Currently Reading"), so a user's existing organization carries
 * over. Sections themselves are left untouched so the old library keeps working.
 */
@Entity(
    tableName = "novel_tag",
    indices = [Index(value = ["name"], unique = true)]
)
data class NovelTag(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "name") var name: String = "",
    /** Optional ARGB color for the chip, or null to use the theme default. */
    @ColumnInfo(name = "color") var color: Long? = null,
    @ColumnInfo(name = "order_id") var orderId: Long = 999L
)
