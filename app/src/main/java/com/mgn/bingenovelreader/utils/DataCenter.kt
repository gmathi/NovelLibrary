package com.mgn.bookmark.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mgn.bingenovelreader.models.WebPage


class DataCenter(context: Context) {

    val PREFS_FILENAME = "com.mgn.bookmark.preferences"
    val BOOKMARKS_LIST = "bookmarksList"
    val CACHE_LIST = "cacheList"

    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    var bookmarksJson: String
        get() = prefs.getString(BOOKMARKS_LIST, "[]")
        set(value) = prefs.edit().putString(BOOKMARKS_LIST, value).apply()

    var cacheJson: String
        get() = prefs.getString(CACHE_LIST, "{}")
        set(value) = prefs.edit().putString(CACHE_LIST, value).apply()

    var cacheMap: HashMap<String, ArrayList<WebPage>> = HashMap()

    fun loadCacheMap() {
       cacheMap = Gson().fromJson(cacheJson, object : TypeToken<HashMap<String, ArrayList<WebPage>>>() {}.type)
    }

    fun saveCacheMap() {
        cacheJson = Gson().toJson(cacheMap)
    }


}