package io.github.gmathi.novellibrary.database.dao.impl

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.database.createGenre
import io.github.gmathi.novellibrary.database.createNovelGenre
import io.github.gmathi.novellibrary.database.getGenres
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelGenre
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.util.*

private const val LOG = "NovelDaoImpl"

class NovelDaoImpl(private val dbHelper: DBHelper) : NovelDao {
    
    override suspend fun insertNovel(novel: Novel): Long = withContext(Dispatchers.IO) {
        val novelId = createNovel(novel)
        novel.genres?.forEach {
            val genreId = dbHelper.createGenre(it)
            dbHelper.createNovelGenre(NovelGenre(novelId, genreId))
        }
        novelId
    }
    
    override suspend fun createNovel(novel: Novel): Long = withContext(Dispatchers.IO) {
        val novelId = getNovelId(novel.url)
        if (novelId != -1L) return@withContext novelId

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DBKeys.KEY_NAME, novel.name)
            put(DBKeys.KEY_URL, novel.url)
            put(DBKeys.KEY_SOURCE_ID, novel.sourceId)
            put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
            put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
            put(DBKeys.KEY_RATING, novel.rating)
            put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
            put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
            put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
            put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
            put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
            put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)
            put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
            put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)
        }

        db.insert(DBKeys.TABLE_NOVEL, null, values)
    }
    
    override suspend fun getNovelByUrl(novelUrl: String): Novel? = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_URL} = ?"
        getNovelFromQuery(selectQuery, arrayOf(novelUrl))
    }
    
    override suspend fun getNovel(novelId: Long): Novel? = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_ID} = ?"
        getNovelFromQuery(selectQuery, arrayOf(novelId.toString()))
    }
    
    override fun getAllNovelsFlow(): Flow<List<Novel>> = flow {
        emit(getAllNovels())
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllNovels(): List<Novel> = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM novel ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
        Logs.debug(LOG, selectQuery)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        val list = ArrayList<Novel>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val novel = getNovelFromCursor(it)
                    list.add(novel)
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override fun getAllNovelsFlow(novelSectionId: Long): Flow<List<Novel>> = flow {
        emit(getAllNovels(novelSectionId))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun getAllNovels(novelSectionId: Long): List<Novel> = withContext(Dispatchers.IO) {
        val selectQuery = "SELECT * FROM novel WHERE ${DBKeys.KEY_NOVEL_SECTION_ID} = $novelSectionId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        val list = ArrayList<Novel>()
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val novel = getNovelFromCursor(it)
                    list.add(novel)
                } while (it.moveToNext())
            }
        }
        list
    }
    
    override suspend fun getNovelId(novelUrl: String): Long = withContext(Dispatchers.IO) {
        var id = -1L
        val selectQuery = "SELECT id FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_URL} = ?"
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, arrayOf(novelUrl))
        cursor?.use {
            if (it.moveToFirst()) {
                id = it.getLong(it.getColumnIndex(DBKeys.KEY_ID))
            }
        }
        id
    }
    
    override suspend fun updateNovel(novel: Novel): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_NAME, novel.name)
            put(DBKeys.KEY_URL, novel.url)
            if (novel.metadata.isNotEmpty())
                put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
            put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
            put(DBKeys.KEY_RATING, novel.rating)
            put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
            if (novel.newReleasesCount != 0L)
                put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
            if (novel.chaptersCount != 0L)
                put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)
            put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
            put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
            if (novel.imageFilePath != null)
                put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
            if (novel.currentChapterUrl != null)
                put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
            if (novel.novelSectionId != -1L)
                put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)
        }
        
        if (novel.genres != null) {
            novel.genres?.forEach {
                val genreId = dbHelper.createGenre(it)
                dbHelper.createNovelGenre(NovelGenre(novel.id, genreId))
            }
        }

        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString())).toLong()
    }
    
    override suspend fun updateNovelOrderId(novelId: Long, orderId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_ORDER_ID, orderId)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateNovelSectionId(novelId: Long, novelSectionId: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_NOVEL_SECTION_ID, novelSectionId)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateBookmarkCurrentWebPageUrl(novelId: Long, currentChapterUrl: String?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, currentChapterUrl)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateTotalChapterCount(novelId: Long, totalChaptersCount: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_CHAPTERS_COUNT, totalChaptersCount)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateNewReleasesCount(novelId: Long, newReleasesCount: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_NEW_RELEASES_COUNT, newReleasesCount)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateNovelMetaData(novel: Novel) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString()))
        Unit
    }
    
    override suspend fun updateChaptersAndReleasesCount(novelId: Long, totalChaptersCount: Long, newReleasesCount: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_CHAPTERS_COUNT, totalChaptersCount)
            put(DBKeys.KEY_NEW_RELEASES_COUNT, newReleasesCount)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun updateChaptersCount(novelId: Long, chaptersCount: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(DBKeys.KEY_CHAPTERS_COUNT, chaptersCount)
        }
        dbHelper.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString()))
        Unit
    }
    
    override suspend fun deleteNovel(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete(DBKeys.TABLE_NOVEL, DBKeys.KEY_ID + " = ?", arrayOf(id.toString()))
        Unit
    }
    
    override suspend fun resetNovel(novel: Novel) = withContext(Dispatchers.IO) {
        // Completely delete all novel data and start fresh. Hard reset mode ;p
        dbHelper.cleanupNovelData(novel)

        // Notice: Cannot run getNovelDetails on MainThread
        val sourceManager: SourceManager by injectLazy()
        val newNovel = sourceManager.get(novel.sourceId)?.getNovelDetails(novel)
        newNovel?.novelSectionId = novel.novelSectionId
        newNovel?.orderId = novel.orderId
        if (newNovel != null) insertNovel(newNovel)
    }
    
    private suspend fun getNovelFromQuery(selectQuery: String, selectionArgs: Array<String>? = null): Novel? = withContext(Dispatchers.IO) {
        Logs.debug(LOG, selectQuery)
        var novel: Novel? = null
        val cursor = dbHelper.readableDatabase.rawQuery(selectQuery, selectionArgs)
        cursor?.use {
            if (it.moveToFirst()) {
                novel = getNovelFromCursor(it)
            }
        }
        if (novel != null)
            novel!!.genres = dbHelper.getGenres(novel!!.id)
        novel
    }
    
    private fun getNovelFromCursor(cursor: Cursor): Novel {
        val novel = Novel(
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)), 
            cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), 
            cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_SOURCE_ID))
        )
        novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
        novel.metadata = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
        novel.imageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_URL))
        novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
        novel.shortDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_SHORT_DESCRIPTION))
        novel.longDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_LONG_DESCRIPTION))
        novel.externalNovelId = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_EXTERNAL_NOVEL_ID))
        novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
        novel.newReleasesCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NEW_RELEASES_COUNT))
        novel.chaptersCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTERS_COUNT))
        novel.currentChapterUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_CURRENT_WEB_PAGE_URL))
        novel.novelSectionId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NOVEL_SECTION_ID))
        return novel
    }
}