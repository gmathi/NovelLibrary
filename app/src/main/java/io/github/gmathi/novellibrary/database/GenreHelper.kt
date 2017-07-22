package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.util.Log
import io.github.gmathi.novellibrary.model.Genre
import java.util.*
import kotlin.collections.ArrayList


private val LOG = "GenreHelper"

fun DBHelper.createGenre(genreName: String): Long {
    val genre = getGenre(genreName)
    if (genre != null) return genre.id
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, genreName)
    return db.insert(DBKeys.TABLE_GENRE, null, values)
}

fun DBHelper.getGenre(genreName: String): Genre? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_GENRE + " WHERE " + DBKeys.KEY_NAME + " = \"" + genreName + "\""
    return getGenreFromQuery(selectQuery)
}

fun DBHelper.getGenre(genreId: Long): Genre? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_GENRE + " WHERE " + DBKeys.KEY_ID + " = " + genreId
    return getGenreFromQuery(selectQuery)
}

//fun DBHelper.getGenres(novelId: Long): List<String> {
//    val novelGenres = getAllNovelGenre(novelId)
//    val genres = ArrayList<String>()
//    novelGenres.forEach {
//        val genre = getGenre(it.genreId)
//        if (genre != null)
//            genres.add(genre.name)
//    }
//    return genres
//}

fun DBHelper.getGenres(novelId: Long): List<String>? {
    val selectQuery =  " SELECT group_concat(g.name) as Genres" +
            " FROM novel_genre ng, genre g" +
            " WHERE ng.genre_id = g.id AND ng.novel_id = $novelId" +
            " GROUP BY ng.novel_id"
    val cursor = this.readableDatabase.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
           return Arrays.asList<String>(*cursor.getString(cursor.getColumnIndex("Genres")).split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())

        }
        cursor.close()
    }
    return null
}


fun DBHelper.getGenreFromQuery(selectQuery: String): Genre? {
    val db = this.readableDatabase
    Log.d(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    var genre: Genre? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            genre = Genre()
            genre.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
            genre.name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
        }
        cursor.close()
    }
    return genre
}

fun DBHelper.getAllGenre(): List<Genre> {
    val list = ArrayList<Genre>()
    val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_GENRE
    Log.d(LOG, selectQuery)
    val db = this.readableDatabase
    val c = db.rawQuery(selectQuery, null)
    if (c != null) {
        if (c.moveToFirst()) {
            do {
                val genre = Genre()
                genre.id = c.getLong(c.getColumnIndex(DBKeys.KEY_ID))
                genre.name = c.getString(c.getColumnIndex(DBKeys.KEY_NAME))

                list.add(genre)
            } while (c.moveToNext())
        }
        c.close()
    }
    return list
}

fun DBHelper.updateGenre(genre: Genre): Long {
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_ID, genre.id)
    values.put(DBKeys.KEY_NAME, genre.name)

    return db.update(DBKeys.TABLE_GENRE, values, DBKeys.KEY_ID + " = ?",
            arrayOf(genre.id.toString())).toLong()
}

fun DBHelper.deleteGenre(id: Long) {
    val db = this.writableDatabase
    db.delete(DBKeys.TABLE_GENRE, DBKeys.KEY_ID + " = ?",
            arrayOf(id.toString()))
}



