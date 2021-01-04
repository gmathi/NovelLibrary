package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_NOVEL_SECTION)
data class NovelSection(@PrimaryKey(autoGenerate = true)
                        @ColumnInfo(name = DBKeys.KEY_ID)
                        var id: Long = 0,
                        @ColumnInfo(name = DBKeys.KEY_NAME)
                        var name: String? = null,
                        @ColumnInfo(name = DBKeys.KEY_ORDER_ID)
                        var orderId: Long = 999L)