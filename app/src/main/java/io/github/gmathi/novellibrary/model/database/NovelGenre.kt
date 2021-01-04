package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_NOVEL_GENRE,
    primaryKeys = [DBKeys.KEY_NOVEL_ID, DBKeys.KEY_GENRE_ID],
    foreignKeys = [ForeignKey(entity = Novel::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_NOVEL_ID],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Genre::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_GENRE_ID],
            onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = [DBKeys.KEY_NOVEL_ID]),
        Index(value = [DBKeys.KEY_GENRE_ID])])
data class NovelGenre(@ColumnInfo(name = DBKeys.KEY_NOVEL_ID)
                      var novelId: Long = 0,
                      @ColumnInfo(name = DBKeys.KEY_GENRE_ID)
                      var genreId: Long = 0)
