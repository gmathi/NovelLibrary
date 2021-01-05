package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.Novel

@Dao
interface NovelDao {
    @Insert
    fun insertAll(vararg novels: Novel)

    @Insert
    fun insert(novel: Novel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(novel: Novel): Long
    
    @Update
    fun update(novel: Novel)
    
    @Delete
    fun delete(novel: Novel)
    
    @Query("SELECT * FROM novel WHERE id = :id")
    fun findOneById(id: Long): Novel?

    @Query("SELECT * FROM novel WHERE url = :url")
    fun findOneByUrl(url: String): Novel?

    @Query("SELECT * FROM novel WHERE name = :name")
    fun findOneByName(name: String): Novel?

    @Query("SELECT * FROM novel WHERE novel_section_id = :novelSectionId ORDER BY order_id ASC")
    fun findByNovelSection(novelSectionId: Long): List<Novel>

    @Query("SELECT id FROM novel WHERE novel_section_id = :novelSectionId ORDER BY order_id ASC")
    fun findIdsByNovelSection(novelSectionId: Long): List<Long>

    @Query("SELECT id FROM novel WHERE name = :name ORDER BY order_id ASC")
    fun findIdByName(name: String): Long?

    @Query("SELECT * FROM novel ORDER BY order_id ASC")
    fun getAll(): List<Novel>
}