package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * Many-to-many link between a novel and a [NovelTag]. A composite primary key on
 * (novel_id, tag_id) makes a link idempotent — inserting the same pair twice is a no-op.
 */
@Entity(
    tableName = "novel_tag_cross_ref",
    primaryKeys = ["novel_id", "tag_id"],
    indices = [Index(value = ["tag_id"])]
)
data class NovelTagCrossRef(
    @ColumnInfo(name = "novel_id") var novelId: Long,
    @ColumnInfo(name = "tag_id") var tagId: Long
)
