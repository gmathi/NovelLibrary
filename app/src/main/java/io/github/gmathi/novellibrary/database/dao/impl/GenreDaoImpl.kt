package io.github.gmathi.novellibrary.database.dao.impl

import android.content.ContentValues
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.database.dao.GenreDao
import io.github.gmathi.novellibrary.model.database.Genre
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val LOG = "GenreDaoImpl"

class GenreDaoImpl(private val dbHelper: DBHelper) : GenreDao {
    
    override suspend fun createGenre(genreName: String): Long = withContext(Dispatchers.IO) {
        val genre = getGenre(genreName)
        if (genre != null) return@withContext genre.id
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBKeys.KEY_NAME, genreName)
        }
        db.insert(DBKeys.TABLE_GENRE, null, values)
    }
    
    override suspend fun getGenre(genreName: String): Genre? = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM " + DBKeys.TABLE_GENRE + " WHERE " + DBKeys.KEY_NAME + " = ?"
        getGenreFromQuery(selectQuery, arrayOf(genreName))
    }
    
    override suspend fun getGenre(genreId: Long): Genre? = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM " + DBKeys.TABLE_GENRE + " WHERE " + DBKeys.KEY_ID + " = " + genreId
        getGenreFromQuery(selectQuery)
    }
    
    override suspend fun getGenres(novelId: Long): List<String>? = withContext(Dispatchers.IO) {
        val selectQuery = " SELECT group_concat(g.name) as Genres" +
                " FROM novel_genre ng, genre g" +
                " WHERE ng.genre_id = g.id AND ng.novel_id = $novelId" +
                " GROUP BY ng.novel_id"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, null)
        cursor?.use {
            if (it.moveToFirst()) {
                return@withContext listOf(*it.getString(it.getColumnIndex("Genres")).split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
            }
        }
        null
    }
    
    override fun getAllGenresFlow(): Flow<List<Genre>> = flow {
        emit(getAllGenres())
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllGenres(): List<Genre> = withContext(Dispatchers.IO) {
        val list = ArrayList<Genre>()
        val selectQuery = "SELECT  * FROM " + DBKeys.TABLE_GENRE
        Logs.debug(LOG, selectQuery)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val genre = Genre()
                    genre.id = it.getLong(it.getColumnIndex(DBKeys.KEY_ID))
                    genre.name = it.getString(it.getColumnIndex(DBKeys.KEY_NAME))
                    list.add(genre)
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun updateGenre(genre: Genre): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBKeys.KEY_ID, genre.id)
            put(DBKeys.KEY_NAME, genre.name)
        }

        db.update(
            DBKeys.TABLE_GENRE, values, DBKeys.KEY_ID + " = ?",
            arrayOf(genre.id.toString())
        ).toLong()
    }
    
    override suspend fun deleteGenre(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            DBKeys.TABLE_GENRE, DBKeys.KEY_ID + " = ?",
            arrayOf(id.toString())
        )
        Unit
    }
    
    private suspend fun getGenreFromQuery(selectQuery: String, selectionArgs: Array<String>? = null): Genre? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        Logs.debug(LOG, selectQuery)
        val cursor = db.rawQuery(selectQuery, selectionArgs)
        var genre: Genre? = null
        cursor?.use {
            if (it.moveToFirst()) {
                genre = Genre()
                genre!!.id = it.getLong(it.getColumnIndex(DBKeys.KEY_ID))
                genre!!.name = it.getString(it.getColumnIndex(DBKeys.KEY_NAME))
            }
        }
        genre
    }
}