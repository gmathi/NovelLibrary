package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.NovelSection

@Dao
interface NovelSectionDao {
    @Insert
    fun insertAll(vararg novelSections: NovelSection)
    
    @Insert
    fun insert(novelSection: NovelSection): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(novelSection: NovelSection): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(novelSection: NovelSection)
    
    @Update
    fun update(novelSection: NovelSection)
    
    @Delete
    fun delete(novelSection: NovelSection)

    @Query("SELECT * FROM novel_section WHERE id = :id")
    fun findOneById(id: Long): NovelSection?
    
    @Query("SELECT * FROM novel_section WHERE name = :name")
    fun findOneByName(name: String): NovelSection?

    @Query("SELECT * FROM novel_section WHERE id <> - 1 ORDER BY order_id ASC")
    fun getAll(): List<NovelSection>
}