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
    
    @Query("DELETE FROM download WHERE name = :novelName")
    fun deleteByNovelName(novelName: String)
    
    @Update
    fun update(download: Download)

    @Query("UPDATE download SET status = :status")
    fun updateStatus(status: Int)
    
    @Query("UPDATE download SET status = :status WHERE name = :novelName")
    fun updateStatusByNovelName(novelName: String, status: Int)
    
    @Query("SELECT * FROM download WHERE web_page_url = :url")
    fun findOneByWebPageUrl(url: String): Download?

    @Query("SELECT * FROM download WHERE status = :status LIMIT 1")
    fun findOneByStatus(status: Int = Download.STATUS_IN_QUEUE): Download?

    @Query("SELECT * FROM download WHERE name = :novelName AND status = :status LIMIT 1")
    fun findOneByNovelNameAndStatus(novelName: String, status: Int = Download.STATUS_IN_QUEUE): Download?

    @Query("SELECT * FROM download WHERE name = :novelName")
    fun findByNovelName(novelName: String): List<Download>

    @Query("SELECT DISTINCT name FROM download")
    fun getAllNovelNames(): List<String>

    @Query("SELECT COUNT(*) FROM download WHERE name = :novelName AND status = :status")
    fun countByNovelNameAndStatus(novelName: String, status: Int = Download.STATUS_IN_QUEUE): Long
    
    @Query("SELECT COUNT(*) FROM download WHERE name = :novelName")
    fun countByNovelName(novelName: String): Long

    @Query("SELECT * FROM download")
    fun getAll(): List<Download>
}