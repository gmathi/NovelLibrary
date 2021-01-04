package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_GENRE)
data class Genre(@PrimaryKey(autoGenerate = true)
                 @ColumnInfo(name = DBKeys.KEY_ID)
                 var id: Long = 0,
                 @ColumnInfo(name = DBKeys.KEY_NAME)
                 var name: String = "")