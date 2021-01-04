package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_SOURCE)
data class TranslatorSource(@PrimaryKey(autoGenerate = true)
                            @ColumnInfo(name = DBKeys.KEY_ID)
                            val id: Long,
                            @ColumnInfo(name = DBKeys.KEY_NAME)
                            val name: String)