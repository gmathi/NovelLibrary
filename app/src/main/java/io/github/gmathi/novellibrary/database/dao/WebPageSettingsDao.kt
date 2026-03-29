package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.gmathi.novellibrary.model.database.WebPageSettings

@Dao
interface WebPageSettingsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(settings: WebPageSettings): Long

    @Update
    fun update(settings: WebPageSettings)

    @Query("SELECT * FROM web_page_settings WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): WebPageSettings?

    @Query("SELECT * FROM web_page_settings WHERE redirect_url = :redirectUrl LIMIT 1")
    fun getByRedirectUrl(redirectUrl: String): WebPageSettings?

    @Query("SELECT * FROM web_page_settings WHERE novel_id = :novelId")
    fun getAllForNovel(novelId: Long): List<WebPageSettings>

    @Query("UPDATE web_page_settings SET is_read = :isRead WHERE url = :url")
    fun updateReadStatus(url: String, isRead: Boolean)

    @Query("DELETE FROM web_page_settings WHERE novel_id = :novelId")
    fun deleteAllForNovel(novelId: Long)

    @Query("DELETE FROM web_page_settings WHERE url = :url")
    fun deleteByUrl(url: String)
}
