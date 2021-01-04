package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelGenre

@Dao
interface NovelGenreDao {
    @Insert
    fun insertAll(vararg novelGenres: NovelGenre)
    
    @Insert
    fun insert(novelGenre: NovelGenre)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(novelGenre: NovelGenre)
    
    @Update
    fun update(novelGenre: NovelGenre)
    
    @Delete
    fun delete(novelGenre: NovelGenre)

    @Query("SELECT * FROM novel_genre WHERE novel_id = :novelId")
    fun findByNovelId(novelId: Long): List<NovelGenre>

    @Query("SELECT * FROM novel_genre WHERE genre_id = :genreId")
    fun findByGenreId(genreId: Long): List<NovelGenre>
    
    @Query("SELECT * FROM novel_genre")
    fun getAll(): List<NovelGenre>
}