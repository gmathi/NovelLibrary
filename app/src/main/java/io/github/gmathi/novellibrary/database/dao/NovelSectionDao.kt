package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gmathi.novellibrary.model.database.NovelSection

@Dao
interface NovelSectionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(section: NovelSection): Long

    @Query("SELECT * FROM novel_section WHERE name = :name LIMIT 1")
    fun getByName(name: String): NovelSection?

    @Query("SELECT * FROM novel_section WHERE id = :id LIMIT 1")
    fun getById(id: Long): NovelSection?

    @Query("SELECT * FROM novel_section ORDER BY order_id ASC")
    fun getAll(): List<NovelSection>

    @Query("UPDATE novel_section SET order_id = :orderId WHERE id = :id")
    fun updateOrderId(id: Long, orderId: Long)

    @Query("UPDATE novel_section SET name = :name WHERE id = :id")
    fun updateName(id: Long, name: String)

    @Query("DELETE FROM novel_section WHERE id = :id")
    fun delete(id: Long)
}
