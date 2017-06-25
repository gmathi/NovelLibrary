package com.mgn.bingenovelreader.database

import android.content.ContentValues
import android.util.Log
import com.mgn.bingenovelreader.models.NovelGenre
import java.util.*

private val LOG = "NovelGenreHelper"

fun DBHelper.createNovelGenre(arg: NovelGenre): Long {
    if (!hasNovelGenreEntry(arg)) {
        val values = ContentValues()
        values.put(DBKeys.KEY_NOVEL_ID, arg.novelId)
        values.put(DBKeys.KEY_GENRE_ID, arg.genreId)
        return this.writableDatabase.insert(DBKeys.TABLE_NOVEL_GENRE, null, values)
    }
    return 0
}

fun DBHelper.hasNovelGenreEntry(novelGenre: NovelGenre): Boolean {
    val db = this.readableDatabase
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_NOVEL_GENRE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelGenre.novelId + " AND " + DBKeys.KEY_GENRE_ID + " = " + novelGenre.genreId
    Log.d(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    var exists = false
    if (cursor != null) {
        exists = cursor.moveToFirst()
        cursor.close()
    }
    return exists
}

fun DBHelper.getAllNovelGenre(novelId: Long): List<NovelGenre> {
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_NOVEL_GENRE + " WHERE " + DBKeys.KEY_NOVEL_ID + " = " + novelId
    Log.d(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)
    val list = ArrayList<NovelGenre>()
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novelGenre = NovelGenre()
                novelGenre.novelId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_ID))
                novelGenre.genreId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_GENRE_ID))

                list.add(novelGenre)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.updateNovelGenre(arg: NovelGenre): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_ID, arg.novelId)
    values.put(DBKeys.KEY_GENRE_ID, arg.genreId)

    return this.writableDatabase.update(DBKeys.TABLE_NOVEL_GENRE, values, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(arg.novelId.toString())).toLong()
}

fun DBHelper.deleteNovelGenre(novelId: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_NOVEL_GENRE, DBKeys.KEY_NOVEL_ID + " = ?", arrayOf(novelId.toString()))
}
