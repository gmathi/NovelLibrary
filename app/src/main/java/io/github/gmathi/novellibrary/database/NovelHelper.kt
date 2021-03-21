package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelGenre
import io.github.gmathi.novellibrary.util.Logs
import java.util.*

private const val LOG = "NovelHelper"

fun DBHelper.insertNovel(novel: Novel): Long {
    val novelId = createNovel(novel)
    novel.genres?.forEach {
        val genreId = createGenre(it)
        createNovelGenre(NovelGenre(novelId, genreId))
    }
    return novelId
}

fun DBHelper.createNovel(novel: Novel): Long {
    val novelId = getNovelId(novel.url)
    if (novelId != -1L) return novelId

    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    values.put(DBKeys.KEY_SOURCE_ID, novel.sourceId)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    values.put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
    values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    values.put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
    values.put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
    values.put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)

    return db.insert(DBKeys.TABLE_NOVEL, null, values)
}

fun DBHelper.getNovelByUrl(novelUrl: String): Novel? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_URL} = ?"
    return getNovelFromQuery(selectQuery, arrayOf(novelUrl))
}

fun DBHelper.getNovel(novelId: Long): Novel? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_ID} = ?"
    return getNovelFromQuery(selectQuery, arrayOf(novelId.toString()))
}

fun DBHelper.getNovelFromQuery(selectQuery: String, selectionArgs: Array<String>? = null): Novel? {
    Logs.debug(LOG, selectQuery)
    var novel: Novel? = null
    val cursor = this.readableDatabase.rawQuery(selectQuery, selectionArgs)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            novel = getNovelFromCursor(cursor)
        }
        cursor.close()
    }
    if (novel != null)
        novel.genres = getGenres(novel.id)
    return novel
}

fun getNovelFromCursor(cursor: Cursor): Novel {
    val novel = Novel(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)), cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_SOURCE_ID)))
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


fun DBHelper.getAllNovels(): List<Novel> {
    val selectQuery = "SELECT * FROM novel ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)
    val list = ArrayList<Novel>()
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novel = getNovelFromCursor(cursor)
                list.add(novel)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getAllNovels(novelSectionId: Long): List<Novel> {
    val selectQuery = "SELECT * FROM novel WHERE ${DBKeys.KEY_NOVEL_SECTION_ID} = $novelSectionId ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)
    val list = ArrayList<Novel>()
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novel = getNovelFromCursor(cursor)
                list.add(novel)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

fun DBHelper.getNovelId(novelUrl: String): Long {
    var id = -1L
    val selectQuery = "SELECT id FROM ${DBKeys.TABLE_NOVEL} WHERE ${DBKeys.KEY_URL} = ?"
    val cursor = this.readableDatabase.rawQuery(selectQuery, arrayOf(novelUrl))
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
        }
        cursor.close()
    }
    return id
}


fun DBHelper.updateNovel(novel: Novel): Long {
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    if (novel.metadata.isNotEmpty())
        values.put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    if (novel.newReleasesCount != 0L)
        values.put(DBKeys.KEY_NEW_RELEASES_COUNT, novel.newReleasesCount)
    if (novel.chaptersCount != 0L)
        values.put(DBKeys.KEY_CHAPTERS_COUNT, novel.chaptersCount)

    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    values.put(DBKeys.KEY_EXTERNAL_NOVEL_ID, novel.externalNovelId)
    if (novel.imageFilePath != null)
        values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    if (novel.currentChapterUrl != null)
        values.put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, novel.currentChapterUrl)
    if (novel.novelSectionId != -1L)
        values.put(DBKeys.KEY_NOVEL_SECTION_ID, novel.novelSectionId)
    if (novel.genres != null) {
        novel.genres?.forEach {
            val genreId = createGenre(it)
            createNovelGenre(NovelGenre(novel.id, genreId))
        }
    }

    // updating row
    return this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString())).toLong()
}

fun DBHelper.updateNovelOrderId(novelId: Long, orderId: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_ORDER_ID, orderId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateNovelSectionId(novelId: Long, novelSectionId: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NOVEL_SECTION_ID, novelSectionId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateBookmarkCurrentWebPageUrl(novelId: Long, currentChapterUrl: String?) {
    val values = ContentValues()
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_URL, currentChapterUrl)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateTotalChapterCount(novelId: Long, totalChaptersCount: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_CHAPTERS_COUNT, totalChaptersCount)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateNewReleasesCount(novelId: Long, newReleasesCount: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NEW_RELEASES_COUNT, newReleasesCount)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateNovelMetaData(novel: Novel, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_METADATA, Gson().toJson(novel.metadata))
    writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString())).toLong()
}

fun DBHelper.updateChaptersAndReleasesCount(novelId: Long, totalChaptersCount: Long, newReleasesCount: Long, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_CHAPTERS_COUNT, totalChaptersCount)
    values.put(DBKeys.KEY_NEW_RELEASES_COUNT, newReleasesCount)
    writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.deleteNovel(id: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_NOVEL, DBKeys.KEY_ID + " = ?", arrayOf(id.toString()))
}

fun DBHelper.updateChaptersCount(novelId: Long, chaptersCount: Long, db: SQLiteDatabase? = null) {
    val writableDatabase = db ?: this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_CHAPTERS_COUNT, chaptersCount)
    writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}


//suspend fun DBHelper.resetNovel(novel: Novel) = coroutineScope {
//    // Completely delete all novel data and start fresh. Hard reset mode ;p
//    cleanupNovelData(novel)
//
//    // Notice: Cannot run getNovelDetails on MainThread
//    val newNovel = NovelApi.getNovelDetails(novel.url)
//    newNovel?.novelSectionId = novel.novelSectionId
//    newNovel?.orderId = novel.orderId
//    if (newNovel != null) insertNovel(newNovel)
//}
