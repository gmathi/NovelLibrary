package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.gmathi.novellibrary.model.database.Genre

@Dao
interface GenreDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(genre: Genre): Long

    @Update
    fun update(genre: Genre)

    @Query("SELECT * FROM genre WHERE name = :name LIMIT 1")
    fun getByName(name: String): Genre?

    @Query("SELECT * FROM genre WHERE id = :id LIMIT 1")
    fun getById(id: Long): Genre?

    @Query("SELECT * FROM genre")
    fun getAll(): List<Genre>

    @Query("""
        SELECT g.* FROM genre g
        INNER JOIN novel_genre ng ON g.id = ng.genre_id
        WHERE ng.novel_id = :novelId
    """)
    fun getForNovel(novelId: Long): List<Genre>

    @Query("DELETE FROM genre WHERE id = :id")
    fun delete(id: Long)
}
