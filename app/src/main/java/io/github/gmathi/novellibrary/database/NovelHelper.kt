package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelGenre
import java.util.*

private val LOG = "NovelHelper"

fun DBHelper.insertNovel(novel: Novel): Long {
    val novelId = createNovel(novel)
    novel.genres?.forEach {
        val genreId = createGenre(it)
        createNovelGenre(NovelGenre(novelId, genreId))
    }
    return novelId
}

fun DBHelper.createNovel(novel: Novel): Long {
    val novelId = getNovelId(novel.name)
    if (novelId != -1L) return novelId

    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    values.put(DBKeys.KEY_METADATA, Gson().toJson(novel.metaData))
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    values.put(DBKeys.KEY_CHAPTER_COUNT, novel.chapterCount)
    values.put(DBKeys.KEY_NEW_CHAPTER_COUNT, novel.newChapterCount)
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, novel.currentWebPageId)

    return db.insert(DBKeys.TABLE_NOVEL, null, values)
}

fun DBHelper.getNovel(novelName: String): Novel? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_NOVEL + " WHERE " + DBKeys.KEY_NAME + " = \"" + novelName + "\""
    return getNovelFromQuery(selectQuery)
}

fun DBHelper.getNovelByUrl(novelUrl: String): Novel? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_NOVEL + " WHERE " + DBKeys.KEY_URL + " = \"" + novelUrl + "\""
    return getNovelFromQuery(selectQuery)
}

fun DBHelper.getNovel(novelId: Long): Novel? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_NOVEL + " WHERE " + DBKeys.KEY_ID + " = " + novelId
    return getNovelFromQuery(selectQuery)
}

fun DBHelper.getNovelFromQuery(selectQuery: String): Novel? {
    Log.d(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)
    var novel: Novel? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            novel = Novel(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)))
            novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
            novel.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
            novel.imageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_URL))
            novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
            novel.shortDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_SHORT_DESCRIPTION))
            novel.longDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_LONG_DESCRIPTION))
            novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
            novel.chapterCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTER_COUNT))
            novel.newChapterCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NEW_CHAPTER_COUNT))
            novel.currentWebPageId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CURRENT_WEB_PAGE_ID))
        }
        cursor.close()
    }
    if (novel != null)
        novel.genres = getGenres(novel.id)
    return novel
}

fun DBHelper.getNovelId(novelName: String): Long {
    val db = this.readableDatabase
    val selectQuery = "SELECT id FROM " + DBKeys.TABLE_NOVEL + " WHERE " + DBKeys.KEY_NAME + " = \"" + novelName + "\""

    Log.d(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    var id: Long = -1
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
        }
        cursor.close()
    }
    return id
}

fun DBHelper.getAllNovels(): List<Novel> {

//    val selectQuery = "select * from novel"
//    " SELECT n.id, n.name, n.url, n.rating, n.image_file_path, n.current_web_page_id," +
//            " group_concat(g.name) as Genres" +
//            " FROM novel_genre ng, genre g, novel n" +
//            " WHERE ng.genre_id = g.id AND ng.novel_id = n.id" +
//            " GROUP BY ng.novel_id"

//    val selectQuery = "SELECT n.id, n.name, n.url, n.rating, n.image_file_path, n.current_web_page_id," +
//        " group_concat(g.name) as Genres" +
//        " FROM novel n" +
//        " LEFT JOIN novel_genre ng" +
//        " ON ng.novel_id = n.id" +
//        " LEFT JOIN genre g" +
//        " ON ng.genre_id = g.id" +
//        " GROUP BY n.id"

    val selectQuery = "select * from novel ORDER BY " + DBKeys.KEY_ORDER_ID + " ASC"

    Log.d(LOG, selectQuery)

    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

    val list = ArrayList<Novel>()
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novel = Novel(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL)))
                novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                novel.metaData = Gson().fromJson(cursor.getString(cursor.getColumnIndex(DBKeys.KEY_METADATA)), object : TypeToken<HashMap<String, String>>() {}.type)
                novel.imageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_URL))
                novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
                novel.shortDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_SHORT_DESCRIPTION))
                novel.longDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_LONG_DESCRIPTION))
                novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
                novel.currentWebPageId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CURRENT_WEB_PAGE_ID))
                novel.chapterCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CHAPTER_COUNT))
                novel.newChapterCount = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_NEW_CHAPTER_COUNT))
                novel.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
                list.add(novel)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    list.forEach {

    }


    return list
}

fun DBHelper.updateNovel(novel: Novel): Long {
    val values = ContentValues()

    //values.put(DBKeys.KEY_ID, novel.id)
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    if (novel.metaData.isNotEmpty())
        values.put(DBKeys.KEY_METADATA, Gson().toJson(novel.metaData))
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    if (novel.chapterCount != 0L)
        values.put(DBKeys.KEY_CHAPTER_COUNT, novel.chapterCount)
    if (novel.newChapterCount != 0L)
    values.put(DBKeys.KEY_NEW_CHAPTER_COUNT, novel.newChapterCount)

    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    if (novel.imageFilePath != null)
        values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    if (novel.currentWebPageId != -1L)
        values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, novel.currentWebPageId)
    if (novel.genres != null) {
        novel.genres?.forEach {
            val genreId = createGenre(it)
            createNovelGenre(NovelGenre(novel.id, genreId))
        }
    }

    // updating row
    return this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString())).toLong()
}

fun DBHelper.updateOrderId(novelId: Long, orderId: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_ORDER_ID, orderId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateBookmarkCurrentWebPageId(novelId: Long, currentWebPageId: Long?) {
    val values = ContentValues()
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, currentWebPageId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.updateNewChapterCount(novelId: Long, newChapterCount: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NEW_CHAPTER_COUNT, newChapterCount)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.deleteNovel(id: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_NOVEL, DBKeys.KEY_ID + " = ?", arrayOf(id.toString()))
}

