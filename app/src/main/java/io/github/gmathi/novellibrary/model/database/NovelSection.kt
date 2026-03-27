package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novel_section")
data class NovelSection(
    @PrimaryKey @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "name") var name: String? = null,
    @ColumnInfo(name = "order_id") var orderId: Long = 999L
)