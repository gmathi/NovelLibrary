package io.github.gmathi.novellibrary.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun hashMapNullableToString(value: HashMap<String, String?>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun stringToHashMapNullable(value: String?): HashMap<String, String?> {
        if (value == null) return HashMap()
        return try {
            gson.fromJson(value, object : TypeToken<HashMap<String, String?>>() {}.type)
                ?: HashMap()
        } catch (e: Exception) {
            HashMap()
        }
    }

    @TypeConverter
    fun hashMapToString(value: HashMap<String, String>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun stringToHashMap(value: String?): HashMap<String, String>? {
        if (value == null) return null
        return try {
            gson.fromJson(value, object : TypeToken<HashMap<String, String>>() {}.type)
        } catch (e: Exception) {
            null
        }
    }
}
