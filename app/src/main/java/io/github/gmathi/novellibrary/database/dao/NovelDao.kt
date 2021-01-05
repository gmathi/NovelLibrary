package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.db
import io.github.gmathi.novellibrary.model.database.Genre
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelGenre
import io.github.gmathi.novellibrary.network.NovelApi
import io.github.gmathi.novellibrary.network.getNovelDetails

@Dao
interface NovelDao {
    @Insert
    fun insertAll(vararg novels: Novel)

    @Insert
    fun insert(novel: Novel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(novel: Novel): Long
    
    @Update
    fun updateAll(vararg novels: Novel)

    @Update
    fun updateAll(novels: ArrayList<Novel>)

    @Update
    fun update(novel: Novel)
    
    @Delete
    fun delete(novel: Novel)
    
    @Query("SELECT * FROM novel WHERE id = :id")
    fun findOneById(id: Long): Novel?

    @Query("SELECT * FROM novel WHERE url = :url")
    fun findOneByUrl(url: String): Novel?

    @Query("SELECT * FROM novel WHERE name = :name")
    fun findOneByName(name: String): Novel?

    @Query("SELECT * FROM novel WHERE novel_section_id = :novelSectionId ORDER BY order_id ASC")
    fun findByNovelSection(novelSectionId: Long): List<Novel>

    @Query("SELECT id FROM novel WHERE novel_section_id = :novelSectionId ORDER BY order_id ASC")
    fun findIdsByNovelSection(novelSectionId: Long): List<Long>
    
    @Query("SELECT id FROM novel WHERE novel_section_id = :novelSectionId")
    fun countByNovelSection(novelSectionId: Long): Int

    @Query("SELECT id FROM novel WHERE name = :name ORDER BY order_id ASC")
    fun findIdByName(name: String): Long?

    @Query("SELECT * FROM novel ORDER BY order_id ASC")
    fun getAll(): List<Novel>

    @RawQuery
    fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int

    /**
     * Helper for inserting novels. Sets novel.id to 0 so the ID can be automatically generated
     * @param novel Novel to insert into database
     */
    @Transaction
    fun insertNovel(novel: Novel): Long {
        if(novel.id == -1L)
            novel.id = 0

        val id = insert(novel)
        novel.genres?.forEach {
            val genreId = db.genreDao().findOneByName(it)?.id ?: db.genreDao().insert(Genre(0, it))
            db.novelGenreDao().insertOrIgnore(NovelGenre(id, genreId))
        }
        return id
    }

    @Transaction
    fun cleanupNovel(novel: Novel) {
        delete(novel) // Deletes web_page, web_page_settings, novel_genre
        // because cascading foreign key property
        db.downloadDao().deleteByNovelName(novel.name)
    }

    @Transaction
    fun resetNovel(novel: Novel) {
        cleanupNovel(novel)

        val newNovel = NovelApi.getNovelDetails(novel.url)
        newNovel?.novelSectionId = novel.novelSectionId
        newNovel?.orderId = novel.orderId
        if (newNovel != null) insertNovel(novel)
    }
}