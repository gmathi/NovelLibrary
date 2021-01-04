package io.github.gmathi.novellibrary.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        @TypeConverter
        @JvmStatic fun fromHashMap(map: HashMap<String, String>): String {
            return Gson().toJson(map)
        }
        
        @TypeConverter
        @JvmStatic fun fromString(str: String): HashMap<String, String> {
            return Gson().fromJson(str, object : TypeToken<HashMap<String, String>>() {}.type)
        }
    }
}