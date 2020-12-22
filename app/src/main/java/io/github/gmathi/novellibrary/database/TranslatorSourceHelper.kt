package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.util.Logs


private const val LOG = "SourceHelper"

fun DBHelper.createTranslatorSource(sourceName: String): Long {
    val source = getTranslatorSource(sourceName)
    if (source != null) return source.id
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, sourceName)
    return db.insert(DBKeys.TABLE_SOURCE, null, values)
}

fun DBHelper.getTranslatorSource(sourceName: String): TranslatorSource? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_SOURCE} WHERE ${DBKeys.KEY_NAME} = ?"
    return getTranslatorSourceFromQuery(selectQuery, arrayOf(sourceName))
}

fun DBHelper.getTranslatorSource(sourceId: Long): TranslatorSource? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_SOURCE} WHERE ${DBKeys.KEY_ID} = $sourceId"
    return getTranslatorSourceFromQuery(selectQuery)
}

private fun DBHelper.getTranslatorSourceFromQuery(selectQuery: String, selectionArgs: Array<String>? = null): TranslatorSource? {
    val db = this.readableDatabase
    Logs.debug(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, selectionArgs)
    var translatorSource: TranslatorSource? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            translatorSource = TranslatorSource(cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME)))
        }
        cursor.close()
    }
    return translatorSource
}

fun DBHelper.getTranslatorSourcesForNovel(novelId: Long): ArrayList<TranslatorSource> {
    val translatorSources: ArrayList<TranslatorSource> = ArrayList()
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
                translatorSources.add(TranslatorSource(cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID)), cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))))
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return translatorSources
}

fun DBHelper.deleteTranslatorSource(id: Long) {
    val db = this.writableDatabase
    db.delete(DBKeys.TABLE_SOURCE, DBKeys.KEY_ID + " = ?", arrayOf(id.toString()))
}



