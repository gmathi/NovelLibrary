package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.gmathi.novellibrary.model.database.Novel

@Dao
interface NovelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(novel: Novel): Long

    @Update
    fun update(novel: Novel)

    @Query("SELECT * FROM novel WHERE url = :url LIMIT 1")
    fun getByUrl(url: String): Novel?

    @Query("SELECT * FROM novel WHERE id = :id LIMIT 1")
    fun getById(id: Long): Novel?

    @Query("SELECT * FROM novel ORDER BY order_id ASC")
    fun getAll(): List<Novel>

    @Query("SELECT * FROM novel WHERE novel_section_id = :sectionId ORDER BY order_id ASC")
    fun getAllBySection(sectionId: Long): List<Novel>

    @Query("SELECT id FROM novel WHERE url = :url LIMIT 1")
    fun getIdByUrl(url: String): Long?

    @Query("UPDATE novel SET order_id = :orderId WHERE id = :id")
    fun updateOrderId(id: Long, orderId: Long)

    @Query("UPDATE novel SET novel_section_id = :sectionId WHERE id = :id")
    fun updateSectionId(id: Long, sectionId: Long)

    @Query("UPDATE novel SET current_web_page_url = :url WHERE id = :id")
    fun updateCurrentChapterUrl(id: Long, url: String?)

    @Query("UPDATE novel SET new_chapter_count = :count WHERE id = :id")
    fun updateChaptersCount(id: Long, count: Long)

    @Query("UPDATE novel SET chapter_count = :count WHERE id = :id")
    fun updateNewReleasesCount(id: Long, count: Long)

    @Query("UPDATE novel SET new_chapter_count = :chaptersCount, chapter_count = :newReleasesCount WHERE id = :id")
    fun updateChaptersAndReleasesCount(id: Long, chaptersCount: Long, newReleasesCount: Long)

    @Query("UPDATE novel SET metadata = :metadata WHERE id = :id")
    fun updateMetadata(id: Long, metadata: String)

    @Query("DELETE FROM novel WHERE id = :id")
    fun delete(id: Long)
}
