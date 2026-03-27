package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gmathi.novellibrary.model.database.NovelGenre

@Dao
interface NovelGenreDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(novelGenre: NovelGenre)

    @Query("SELECT * FROM novel_genre WHERE novel_id = :novelId AND genre_id = :genreId LIMIT 1")
    fun get(novelId: Long, genreId: Long): NovelGenre?

    @Query("SELECT * FROM novel_genre WHERE novel_id = :novelId")
    fun getAllForNovel(novelId: Long): List<NovelGenre>

    @Query("DELETE FROM novel_genre WHERE novel_id = :novelId")
    fun deleteAllForNovel(novelId: Long)

    @Query("DELETE FROM novel_genre WHERE novel_id = :novelId AND genre_id = :genreId")
    fun delete(novelId: Long, genreId: Long)
}
