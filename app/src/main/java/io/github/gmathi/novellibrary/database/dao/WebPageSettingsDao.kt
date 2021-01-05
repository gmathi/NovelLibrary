package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.WebPageSettings

@Dao
interface WebPageSettingsDao {
    @Insert
    fun insertAll(vararg webPageSettings: WebPageSettings)
    
    @Insert
    fun insert(webPageSettings: WebPageSettings)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(webPageSettings: WebPageSettings)
    
    @Update
    fun update(webPageSettings: WebPageSettings)
    
    @Delete
    fun delete(webPageSettings: WebPageSettings)

    @Query("DELETE FROM web_page_settings WHERE novel_id = :novelId")
    fun deleteByNovelId(novelId: Long)

    @Query("DELETE FROM web_page_settings WHERE url = :url")
    fun deleteByUrl(url: String)

    @Query("SELECT * FROM web_page_settings WHERE url = :url")
    fun findOneByUrl(url: String): WebPageSettings?
    
    @Query("SELECT * FROM web_page_settings WHERE redirect_url = :redirectUrl")
    fun findOneByRedirectUrl(redirectUrl: String): WebPageSettings?
    
    @Query("SELECT * FROM web_page_settings WHERE novel_id = :novelId")
    fun findByNovelId(novelId: Long): List<WebPageSettings>
    
    @Query("SELECT * FROM web_page_settings")
    fun getAll(): List<WebPageSettings>
}