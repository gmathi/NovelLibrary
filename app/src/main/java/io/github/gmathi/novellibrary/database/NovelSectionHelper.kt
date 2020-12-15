package io.github.gmathi.novellibrary.database

import android.content.ContentValues
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.util.Logs


private const val LOG = "NovelSectionHelper"

fun DBHelper.createNovelSection(novelSectionName: String): Long {
    val novelSection = getNovelSection(novelSectionName)
    if (novelSection != null) return novelSection.id
    val db = this.writableDatabase
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, novelSectionName)
    return db.insert(DBKeys.TABLE_NOVEL_SECTION, null, values)
}

fun DBHelper.getNovelSection(novelSectionName: String): NovelSection? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL_SECTION} WHERE ${DBKeys.KEY_NAME} = ?"
    return getNovelSectionFromQuery(selectQuery, arrayOf(novelSectionName))
}

fun DBHelper.getNovelSection(novelSectionId: Long): NovelSection? {
    val selectQuery = "SELECT * FROM ${DBKeys.TABLE_NOVEL_SECTION} WHERE ${DBKeys.KEY_ID} = $novelSectionId"
    return getNovelSectionFromQuery(selectQuery)
}

fun DBHelper.getNovelSectionFromQuery(selectQuery: String, selectionArgs: Array<String>? = null): NovelSection? {
    val db = this.readableDatabase
    Logs.debug(LOG, selectQuery)
    val cursor = db.rawQuery(selectQuery, selectionArgs)
    var novelSection: NovelSection? = null
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            novelSection = NovelSection()
            novelSection.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
            novelSection.name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
            novelSection.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))
        }
        cursor.close()
    }
    return novelSection
}

fun DBHelper.getAllNovelSections(): List<NovelSection> {
    val list = ArrayList<NovelSection>()
    val selectQuery = "SELECT  * FROM ${DBKeys.TABLE_NOVEL_SECTION} ORDER BY ${DBKeys.KEY_ORDER_ID} ASC"
    Logs.debug(LOG, selectQuery)
    val db = this.readableDatabase
    val cursor = db.rawQuery(selectQuery, null)
    if (cursor != null) {
        if (cursor.moveToFirst()) {
            do {
                val novelSection = NovelSection()
                novelSection.id = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ID))
                novelSection.name = cursor.getString(cursor.getColumnIndex(DBKeys.KEY_NAME))
                novelSection.orderId = cursor.getLong(cursor.getColumnIndex(DBKeys.KEY_ORDER_ID))

                list.add(novelSection)
            } while (cursor.moveToNext())
        }
        cursor.close()
    }
    return list
}

//fun DBHelper.updateNovelSection(novelSection: NovelSection): Long {
//    val db = this.writableDatabase
//    val values = ContentValues()
//    values.put(DBKeys.KEY_ID, novelSection.id)
//    values.put(DBKeys.KEY_NAME, novelSection.name)
//
//    return db.update(DBKeys.TABLE_NOVEL_SECTION, values, DBKeys.KEY_ID + " = ?",
//            arrayOf(novelSection.id.toString())).toLong()
//}

fun DBHelper.updateNovelSectionOrderId(novelSectionId: Long, orderId: Long) {
    val values = ContentValues()
    values.put(DBKeys.KEY_ORDER_ID, orderId)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL_SECTION, values, DBKeys.KEY_ID + " = ?", arrayOf(novelSectionId.toString())).toLong()
}

fun DBHelper.updateNovelSectionName(novelSectionId: Long, name: String) {
    val values = ContentValues()
    values.put(DBKeys.KEY_NAME, name)
    this.writableDatabase.update(DBKeys.TABLE_NOVEL_SECTION, values, DBKeys.KEY_ID + " = ?", arrayOf(novelSectionId.toString())).toLong()
}


fun DBHelper.deleteNovelSection(id: Long) {
    val db = this.writableDatabase
    db.delete(
        DBKeys.TABLE_NOVEL_SECTION, DBKeys.KEY_ID + " = ?",
        arrayOf(id.toString())
    )
}



