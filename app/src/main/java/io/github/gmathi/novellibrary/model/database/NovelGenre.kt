package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "novel_genre", primaryKeys = ["novel_id", "genre_id"])
data class NovelGenre(
    @ColumnInfo(name = "novel_id") var novelId: Long = 0,
    @ColumnInfo(name = "genre_id") var genreId: Long = 0
)
