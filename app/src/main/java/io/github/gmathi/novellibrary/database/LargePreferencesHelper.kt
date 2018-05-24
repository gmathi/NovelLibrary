package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import android.util.Log

private const val LOG = "LargePreferenceHelper"

fun DBHelper.createOrUpdateLargePreference(name: String, value: String?) {
    val largePreference = getLargePreference(name)
    if (largePreference == null) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(DBKeys.KEY_NAME, name)
        values.put(DBKeys.KEY_VALUE, value)
        db.insert(DBKeys.TABLE_LARGE_PREFERENCE, null, values)
    } else {
        updateLargePreference(name, value)
    }
}

fun DBHelper.getLargePreference(name: String): String? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_LARGE_PREFERENCE} WHERE ${DBKeys.KEY_NAME} = \"$name\""
    return getLargePreferenceFromQuery(selectQuery)
}

fun DBHelper.getLargePreferenceFromQuery(selectQuery: String): String? {
    val db = this.readableDatabase
    Log.d(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, null)
    var value: String? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            value = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_VALUE))
        }
        cursor.close()
    }
    return value
}

fun DBHelper.updateLargePreference(name: String, value: String?) {
    val values = ContentValues()
    values.put(DBKeys.KEY_VALUE, value)
    this.writableDatabase.update(DBKeys.TABLE_LARGE_PREFERENCE, values, DBKeys.KEY_NAME + " = ?", arrayOf(name)).toLong()
}


fun DBHelper.deleteLargePreference(name: String) {
    this.writableDatabase.delete(DBKeys.TABLE_LARGE_PREFERENCE, DBKeys.KEY_NAME + " = ?", arrayOf(name))
}



