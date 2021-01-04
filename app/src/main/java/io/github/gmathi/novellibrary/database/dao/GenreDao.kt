package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.Genre

@Dao
interface GenreDao {
    @Insert
    fun insertAll(vararg genres: Genre)
    
    @Insert
    fun insert(genre: Genre): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(genre: Genre): Long
    
    @Update
    fun update(genre: Genre)
    
    @Delete
    fun delete(genre: Genre)

    @Query("SELECT * FROM genre WHERE id = :id")
    fun findOneById(id: Long): Genre?
    
    @Query("SELECT * FROM genre WHERE name IS NOT NULL AND name = :name")
    fun findOneByName(name: String): List<Genre>
    
    @Query("SELECT group_concat(g.name) FROM novel_genre ng, genre g WHERE ng.genre_id = g.id AND ng.novel_id = :novelId GROUP BY ng.novel_id")
    fun findGenreNamesByNovel(novelId: Long): List<String?>

    @Query("SELECT * FROM genre")
    fun getAll(): List<Genre>
}