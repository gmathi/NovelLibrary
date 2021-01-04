package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.Download

@Dao
interface DownloadDao {
    @Insert
    fun insertAll(vararg downloads: Download)

    @Insert
    fun insert(download: Download)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(download: Download): Long
    
    @Delete
    fun delete(download: Download)
    
    @Update
    fun update(download: Download)
    
    @Query("SELECT * FROM download WHERE web_page_url = :url")
    fun findOneByWebPageUrl(url: String): Download?

    @Query("SELECT * FROM download WHERE name = :novelName")
    fun findByNovelName(novelName: String): List<Download>

    @Query("SELECT * FROM download")
    fun getAll(): List<Download>
}