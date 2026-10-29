package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelTag
import io.github.gmathi.novellibrary.util.logging.Logs

private const val LOG = "NovelTagHelper"

/**
 * DBHelper extension layer for the tag model. Mirrors the style of [NovelSectionHelper]/[NovelHelper]
 * (raw-SQL over the shared `bnr_db`) so the new Compose library can read/write tags through the
 * same singleton the rest of the app already injects.
 */

fun DBHelper.createNovelTag(name: String, color: Long? = null): Long {
    val existing = getNovelTagByName(name)
    if (existing != null) return existing.id
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, name)
    if (color != null) values.put(DBKeys.KEY_COLOR, color)
    values.put(DBKeys.KEY_ORDER_ID, getAllNovelTags().size.toLong())
    return this.writableDatabase.insert(DBKeys.TABLE_NOVEL_TAG, null, values)
}

fun DBHelper.getNovelTagByName(name: String): NovelTag? =
    getNovelTagFromQuery("SELECT * FROM ${DBKeys.TABLE_NOVEL_TAG} WHERE ${DBKeys.KEY_NAME} = ? LIMIT 1", arrayOf(name))

fun DBHelper.getNovelTag(id: Long): NovelTag? =
    getNovelTagFromQuery("SELECT * FROM ${DBKeys.TABLE_NOVEL_TAG} WHERE ${DBKeys.KEY_ID} = ? LIMIT 1", arrayOf(id.toString()))

private fun DBHelper.getNovelTagFromQuery(query: String, args: Array<String>?): NovelTag? {
    val cursor = this.readableDatabase.rawQuery(query, args)
    var tag: NovelTag? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) tag = cursorToTag(cursor)
        cursor.close()
    }
    return tag
}

fun DBHelper.getAllNovelTags(): List<NovelTag> {
    val list = ArrayList<NovelTag>()
    val query = "SELECT * FROM ${DBKeys.TABLE_NOVEL_TAG} ORDER BY ${DBKeys.KEY_ORDER_ID} ASC, ${DBKeys.KEY_NAME} ASC"
    val cursor = this.readableDatabase.rawQuery(query, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToTag(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

private fun cursorToTag(cursor: android.database.Cursor): NovelTag {
    val colorIndex = cursor.getColumnIndex(DBKeys.KEY_COLOR)
    return NovelTag(
        id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID)),
        name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)) ?: "",
        color = if (colorIndex >= 0 && !cursor.isNull(colorIndex)) cursor.getLong(colorIndex) else null,
        orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
    )
}

fun DBHelper.updateNovelTagName(id: Long, name: String) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, name)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL_TAG, values, "${DBKeys.KEY_ID} = ?", arrayOf(id.toString()))
}

fun DBHelper.updateNovelTagColor(id: Long, color: Long?) {
    val values = ContentValues()
    if (color == null) values.putNull(DBKeys.KEY_COLOR) else values.put(DBKeys.KEY_COLOR, color)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL_TAG, values, "${DBKeys.KEY_ID} = ?", arrayOf(id.toString()))
}

fun DBHelper.updateNovelTagOrderIds(tags: List<NovelTag>) {
    val db = this.writableDatabase
    db.beginTransaction()
    try {
        tags.forEachIndexed { index, tag ->
            val values = ContentValues()
            values.put(DBKeys.KEY_ORDER_ID, index.toLong())
            db.update(DBKeys.TABLE_NOVEL_TAG, values, "${DBKeys.KEY_ID} = ?", arrayOf(tag.id.toString()))
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

/** Deletes a tag and all its novel links. */
fun DBHelper.deleteNovelTag(id: Long) {
    val db = this.writableDatabase
    db.delete(DBKeys.TABLE_NOVEL_TAG_CROSS_REF, "${DBKeys.KEY_TAG_ID} = ?", arrayOf(id.toString()))
    db.delete(DBKeys.TABLE_NOVEL_TAG, "${DBKeys.KEY_ID} = ?", arrayOf(id.toString()))
}

//region Links

fun DBHelper.addTagToNovel(novelId: Long, tagId: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_ID, novelId)
    values.put(DBKeys.KEY_TAG_ID, tagId)
    this.writableDatabase.insertWithOnConflict(
        DBKeys.TABLE_NOVEL_TAG_CROSS_REF, null, values,
        android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
    )
}

fun DBHelper.removeTagFromNovel(novelId: Long, tagId: Long) {
    this.writableDatabase.delete(
        DBKeys.TABLE_NOVEL_TAG_CROSS_REF,
        "${DBKeys.KEY_NOVEL_ID} = ? AND ${DBKeys.KEY_TAG_ID} = ?",
        arrayOf(novelId.toString(), tagId.toString())
    )
}

/** Replaces the full set of tags on a novel with [tagIds]. */
fun DBHelper.setNovelTags(novelId: Long, tagIds: Collection<Long>) {
    val db = this.writableDatabase
    db.beginTransaction()
    try {
        db.delete(DBKeys.TABLE_NOVEL_TAG_CROSS_REF, "${DBKeys.KEY_NOVEL_ID} = ?", arrayOf(novelId.toString()))
        tagIds.forEach { tagId ->
            val values = ContentValues()
            values.put(DBKeys.KEY_NOVEL_ID, novelId)
            values.put(DBKeys.KEY_TAG_ID, tagId)
            db.insertWithOnConflict(
                DBKeys.TABLE_NOVEL_TAG_CROSS_REF, null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
            )
        }
        db.setTransactionSuccessful()
    } finally {
        db.endTransaction()
    }
}

fun DBHelper.getTagIdsForNovel(novelId: Long): List<Long> {
    val list = ArrayList<Long>()
    val cursor = this.readableDatabase.rawQuery(
        "SELECT ${DBKeys.KEY_TAG_ID} FROM ${DBKeys.TABLE_NOVEL_TAG_CROSS_REF} WHERE ${DBKeys.KEY_NOVEL_ID} = ?",
        arrayOf(novelId.toString())
    )
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do { list.add(cursor.getLong(0)) } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

/** Returns a map of novelId -> list of tag ids, for every linked novel (one query). */
fun DBHelper.getAllNovelTagLinks(): Map<Long, List<Long>> {
    val map = HashMap<Long, MutableList<Long>>()
    val cursor = this.readableDatabase.rawQuery(
        "SELECT ${DBKeys.KEY_NOVEL_ID}, ${DBKeys.KEY_TAG_ID} FROM ${DBKeys.TABLE_NOVEL_TAG_CROSS_REF}", null
    )
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novelId = cursor.getLong(0)
                val tagId = cursor.getLong(1)
                map.getOrPut(novelId) { ArrayList() }.add(tagId)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return map
}

/** Count of novels carrying each tag (tagId -> count). */
fun DBHelper.getNovelCountPerTag(): Map<Long, Int> {
    val map = HashMap<Long, Int>()
    val cursor = this.readableDatabase.rawQuery(
        "SELECT ${DBKeys.KEY_TAG_ID}, COUNT(*) FROM ${DBKeys.TABLE_NOVEL_TAG_CROSS_REF} GROUP BY ${DBKeys.KEY_TAG_ID}", null
    )
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do { map[cursor.getLong(0)] = cursor.getInt(1) } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return map
}

//endregion
