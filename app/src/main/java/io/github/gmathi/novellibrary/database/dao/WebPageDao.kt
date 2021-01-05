package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.database.WebPage

@Dao
interface WebPageDao {
    @Insert
    fun insertAll(vararg webPages: WebPage)
    
    @Insert
    fun insert(webPage: WebPage)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(webPage: WebPage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertOrIgnore(webPage: WebPage)

    @Update
    fun update(webPage: WebPage)

    @Delete
    fun deleteAll(webPages: ArrayList<WebPage>)

    @Delete
    fun delete(webPage: WebPage)

    @Query("DELETE FROM web_page WHERE novel_id = :novelId")
    fun deleteByNovelId(novelId: Long)

    @Query("DELETE FROM web_page WHERE url = :url")
    fun deleteByUrl(url: String)

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId ORDER BY order_id ASC")
    fun findByNovelId(novelId: Long): List<WebPage>

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId AND (:sourceId = -1 OR source_id = :sourceId) ORDER BY order_id ASC")
    fun findByNovelAndSourceId(novelId: Long, sourceId: Long): List<WebPage>

    @Query("SELECT * FROM web_page WHERE url = :url")
    fun findOneByUrl(url: String): WebPage?

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId ORDER BY order_id LIMIT :offset, 1")
    fun findOneByNovelIdOffset(novelId: Long, offset: Int): WebPage?

    @Query("SELECT * FROM web_page WHERE novel_id = :novelId AND source_id = :sourceId ORDER BY order_id LIMIT :offset, 1")
    fun findOneByNovelAndSourceIdOffset(novelId: Long, sourceId: Long, offset: Int): WebPage?

    @Query("SELECT * FROM web_page")
    fun getAll(): List<WebPage>
    
    @Transaction
    fun deleteByNovelOrWebPages(novelId: Long, webPages: ArrayList<WebPage>) {
        deleteByNovelId(novelId)
        deleteAll(webPages)
    }
}