package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "genre")
data class Genre(
    @PrimaryKey @ColumnInfo(name = "id") var id: Long = 0,
    @ColumnInfo(name = "name") var name: String? = null
)