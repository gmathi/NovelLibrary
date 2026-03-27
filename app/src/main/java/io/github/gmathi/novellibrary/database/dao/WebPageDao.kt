package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gmathi.novellibrary.model.database.WebPage

@Dao
interface WebPageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(webPage: WebPage): Long

    @Query("SELECT * FROM web_page WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): WebPage?

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId ORDER BY order_id ASC")
    fun getAllForNovel(novelId: Long): List<WebPage>

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId AND translator_source_name = :sourceName ORDER BY order_id ASC")
    fun getAllForNovelBySource(novelId: Long, sourceName: String): List<WebPage>

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId ORDER BY order_id ASC LIMIT 1 OFFSET :offset")
    fun getAtOffset(novelId: Long, offset: Long): WebPage?

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId AND translator_source_name = :sourceName ORDER BY order_id ASC LIMIT 1 OFFSET :offset")
    fun getAtOffsetBySource(novelId: Long, sourceName: String, offset: Long): WebPage?

    @Query("DELETE FROM web_page WHERE novel_id = :novelId")
    fun deleteAllForNovel(novelId: Long)

    @Query("DELETE FROM web_page WHERE url = :url")
    fun deleteByUrl(url: String)
}
