package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.TranslatorSource

@Dao
interface TranslatorSourceDao {
    @Insert
    fun insertAll(vararg translatorSources: TranslatorSource)
    
    @Insert
    fun insert(translatorSource: TranslatorSource): Long
    
    @Update
    fun update(translatorSource: TranslatorSource)
    
    @Delete
    fun delete(translatorSource: TranslatorSource)

    @Query("SELECT * FROM source WHERE id = :id")
    fun findOneById(id: Long): TranslatorSource?
    
    @Query("SELECT * FROM source WHERE name = :name")
    fun findOneByName(name: String): TranslatorSource?
    
    @Query("SELECT DISTINCT w.source_id AS id, s.name AS name FROM novel n, web_page w, source s WHERE n.id = :id AND n.id = w.novel_id AND s.id = w.source_id")
    fun findByNovelId(id: Long): List<TranslatorSource>

    @Query("SELECT * FROM source")
    fun getAll(): List<TranslatorSource>
}