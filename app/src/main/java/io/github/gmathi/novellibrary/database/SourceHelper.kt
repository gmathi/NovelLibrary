package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.util.Log
import io.github.gmathi.novellibrary.util.Logs


private const val LOG = "SourceHelper"

fun DBHelper.createSource(sourceName: String): Long {
    val source = getSource(sourceName)
    if (source != null) return source.first
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, sourceName)
    return db.insert(DBKeys.TABLE_SOURCE, null, values)
}

fun DBHelper.getSource(sourceName: String): Pair<Long, String>? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_SOURCE} WHERE ${DBKeys.KEY_NAME} = \"$sourceName\""
    return getSourceFromQuery(selectQuery)
}

fun DBHelper.getSource(sourceId: Long): Pair<Long, String>? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_SOURCE} WHERE ${DBKeys.KEY_ID} = $sourceId"
    return getSourceFromQuery(selectQuery)
}

fun DBHelper.getSourceFromQuery(selectQuery: String): Pair<Long, String>? {
    val db = this.readableDatabase
    Logs.debug(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    var source: Pair<Long, String>? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            source = Pair<Long, String>(cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)))
        }
        cursor.close()
    }
    return source
}

fun DBHelper.getSourcesForNovel(novelId: Long): ArrayList<Pair<Long, String>> {
    val sources: ArrayList<Pair<Long, String>> = ArrayList()
    val selectQuery = "SELECT DISTINCT w.${DBKeys.KEY_SOURCE_ID} AS ${DBKeys.KEY_ID}, s.${DBKeys.KEY_NAME} as ${DBKeys.KEY_NAME} " +
            "FROM ${DBKeys.TABLE_NOVEL} n, ${DBKeys.TABLE_WEB_PAGE} w, ${DBKeys.TABLE_SOURCE} s " +
            "WHERE n.${DBKeys.KEY_ID} == $novelId AND n.${DBKeys.KEY_ID} == w.${DBKeys.KEY_NOVEL_ID} AND s.${DBKeys.KEY_ID} == w.${DBKeys.KEY_SOURCE_ID} " +
            "ORDER BY name ASC"
    val db = this.readableDatabase
    Logs.debug(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                sources.add(Pair<Long, String>(cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return sources
}

fun DBHelper.deleteSource(id: Long) {
    val db = this.writableDatabase
    db.delete(DBKeys.TABLE_SOURCE, DBKeys.KEY_ID + " = ?",
            arrayOf(id.toString()))
}



