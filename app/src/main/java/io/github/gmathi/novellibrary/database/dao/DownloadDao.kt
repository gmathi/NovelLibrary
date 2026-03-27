package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.gmathi.novellibrary.model.database.Download

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(download: Download): Long

    @Update
    fun update(download: Download)

    @Query("SELECT * FROM download WHERE web_page_url = :webPageUrl LIMIT 1")
    fun getByUrl(webPageUrl: String): Download?

    @Query("SELECT * FROM download")
    fun getAll(): List<Download>

    @Query("SELECT * FROM download WHERE novel_id = :novelId")
    fun getAllForNovel(novelId: Long): List<Download>

    @Query("SELECT * FROM download WHERE status = ${Download.STATUS_IN_QUEUE} LIMIT 1")
    fun getNextInQueue(): Download?

    @Query("SELECT * FROM download WHERE status = ${Download.STATUS_IN_QUEUE} AND novel_id = :novelId LIMIT 1")
    fun getNextInQueueForNovel(novelId: Long): Download?

    @Query("SELECT * FROM download WHERE novel_id = :novelId AND web_page_url IN (:urls)")
    fun getAllForNovelByUrls(novelId: Long, urls: List<String>): List<Download>

    @Query("UPDATE download SET status = :status WHERE web_page_url = :webPageUrl")
    fun updateStatusByUrl(webPageUrl: String, status: Int)

    @Query("UPDATE download SET status = :status WHERE novel_id = :novelId")
    fun updateStatusByNovelId(novelId: Long, status: Int)

    @Query("UPDATE download SET status = :status")
    fun updateAllStatuses(status: Int)

    @Query("SELECT DISTINCT novel_id FROM download")
    fun getDistinctNovelIds(): List<Long>

    @Query("SELECT COUNT(*) FROM download WHERE novel_id = :novelId AND status = ${Download.STATUS_IN_QUEUE}")
    fun hasDownloadsInQueue(novelId: Long): Int

    @Query("SELECT COUNT(*) FROM download WHERE novel_id = :novelId")
    fun getRemainingCountForNovel(novelId: Long): Int

    @Query("DELETE FROM download WHERE web_page_url = :webPageUrl")
    fun deleteByUrl(webPageUrl: String)

    @Query("DELETE FROM download WHERE novel_id = :novelId")
    fun deleteAllForNovel(novelId: Long)
}
