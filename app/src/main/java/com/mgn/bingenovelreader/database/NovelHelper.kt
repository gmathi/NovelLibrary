package com.mgn.bingenovelreader.database

import android.content.ContentValues
import android.util.Log
import com.mgn.bingenovelreader.database.DBKeys.KEY_CURRENT_WEB_PAGE_ID
import com.mgn.bingenovelreader.models.Novel
import org.jsoup.helper.StringUtil
import java.util.*

private val LOG = "NovelHelper"

fun DBHelper.createNovel(novel: Novel): Long {
    val novelId = getNovelId(novel.name!!)
    if (novelId != -1L) return novelId

    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    values.put(DBKeys.KEY_AUTHOR, novel.author)
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, novel.currentWebPageId)

    return db.insert(DBKeys.TABLE_NOVEL, null, values)
}

fun DBHelper.getNovel(novelName: String): Novel? {
    val selectQuery = "SELECT * FROM " + DBKeys.TABLE_NOVEL + " WHERE " + DBKeys.KEY_NAME + " = \"" + novelName + "\""
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
            novel = Novel()
            novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
            novel.name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
            novel.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
            novel.author = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_AUTHOR))
            novel.imageUrl = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_URL))
            novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
            novel.shortDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_SHORT_DESCRIPTION))
            novel.longDescription = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_LONG_DESCRIPTION))
            novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
            novel.currentWebPageId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_CURRENT_WEB_PAGE_ID))
        }
        cursor.close()
    }
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

    val selectQuery = "SELECT n.id, n.name, n.url, n.rating, n.image_file_path, n.current_web_page_id," +
            " group_concat(g.name) as Genres" +
            " FROM novel n" +
            " LEFT JOIN novel_genre ng" +
            " ON ng.novel_id = n.id" +
            " LEFT JOIN genre g" +
            " ON ng.genre_id = g.id" +
            " GROUP BY n.id"

    Log.d(LOG, selectQuery)

    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)

    val list = ArrayList<Novel>()
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novel = Novel()
                novel.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                novel.name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
                novel.url = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_URL))
                novel.rating = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_RATING))
                novel.imageFilePath = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_IMAGE_FILE_PATH))
//                novel.author = cursor.getString(cursor.getColumnIndex(KEY_AUTHOR))
//                novel.imageUrl = cursor.getString(cursor.getColumnIndex(KEY_IMAGE_URL))
//                novel.shortDescription = cursor.getString(cursor.getColumnIndex(KEY_SHORT_DESCRIPTION))
//                novel.longDescription = cursor.getString(cursor.getColumnIndex(KEY_LONG_DESCRIPTION))
                novel.currentWebPageId = cursor.getLong(cursor.getColumnIndex(KEY_CURRENT_WEB_PAGE_ID))
                val genres = cursor.getString(cursor.getColumnIndex("Genres"))
                if (!StringUtil.isBlank(genres))
                    novel.genres = Arrays.asList<String>(*genres.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
                //getGenres(novel.id)


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

    values.put(DBKeys.KEY_ID, novel.id)
    values.put(DBKeys.KEY_NAME, novel.name)
    values.put(DBKeys.KEY_URL, novel.url)
    values.put(DBKeys.KEY_AUTHOR, novel.author)
    values.put(DBKeys.KEY_IMAGE_URL, novel.imageUrl)
    values.put(DBKeys.KEY_RATING, novel.rating)
    values.put(DBKeys.KEY_SHORT_DESCRIPTION, novel.shortDescription)
    values.put(DBKeys.KEY_LONG_DESCRIPTION, novel.longDescription)
    values.put(DBKeys.KEY_IMAGE_FILE_PATH, novel.imageFilePath)
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, novel.currentWebPageId)

    // updating row
    return this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novel.id.toString())).toLong()
}

fun DBHelper.updateCurrentWebPageId(novelId: Long, currentWebPageId: Long?) {
    val values = ContentValues()
    values.put(DBKeys.KEY_CURRENT_WEB_PAGE_ID, currentWebPageId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL, values, DBKeys.KEY_ID + " = ?", arrayOf(novelId.toString())).toLong()
}

fun DBHelper.deleteNovel(id: Long) {
    this.writableDatabase.delete(DBKeys.TABLE_NOVEL, DBKeys.KEY_ID + " = ?", arrayOf(id.toString()))
}
