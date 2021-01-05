package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_LARGE_PREFERENCE)
data class LargePreference(@PrimaryKey
                           @ColumnInfo(name = DBKeys.KEY_NAME)
                           var name: String,
                           @ColumnInfo(name = DBKeys.KEY_VALUE)
                           var value: String) {
}