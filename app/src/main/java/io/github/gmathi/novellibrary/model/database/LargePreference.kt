package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "large_preference")
data class LargePreference(
    @PrimaryKey @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "value") var value: String? = null
)
