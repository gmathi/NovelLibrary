package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.gmathi.novellibrary.model.database.NovelTag
import io.github.gmathi.novellibrary.model.database.NovelTagCrossRef

@Dao
interface NovelTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tag: NovelTag): Long

    @Query("SELECT * FROM novel_tag WHERE name = :name LIMIT 1")
    fun getByName(name: String): NovelTag?

    @Query("SELECT * FROM novel_tag WHERE id = :id LIMIT 1")
    fun getById(id: Long): NovelTag?

    @Query("SELECT * FROM novel_tag ORDER BY order_id ASC, name ASC")
    fun getAll(): List<NovelTag>

    @Query("UPDATE novel_tag SET name = :name WHERE id = :id")
    fun updateName(id: Long, name: String)

    @Query("UPDATE novel_tag SET order_id = :orderId WHERE id = :id")
    fun updateOrderId(id: Long, orderId: Long)

    @Query("DELETE FROM novel_tag WHERE id = :id")
    fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun link(crossRef: NovelTagCrossRef)

    @Query("DELETE FROM novel_tag_cross_ref WHERE novel_id = :novelId AND tag_id = :tagId")
    fun unlink(novelId: Long, tagId: Long)

    @Query("DELETE FROM novel_tag_cross_ref WHERE novel_id = :novelId")
    fun unlinkAllForNovel(novelId: Long)

    @Query("DELETE FROM novel_tag_cross_ref WHERE tag_id = :tagId")
    fun unlinkAllForTag(tagId: Long)

    @Query("SELECT tag_id FROM novel_tag_cross_ref WHERE novel_id = :novelId")
    fun getTagIdsForNovel(novelId: Long): List<Long>

    @Query("SELECT novel_id FROM novel_tag_cross_ref WHERE tag_id = :tagId")
    fun getNovelIdsForTag(tagId: Long): List<Long>
}
