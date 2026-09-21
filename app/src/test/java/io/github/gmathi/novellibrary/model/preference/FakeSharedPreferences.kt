package io.github.gmathi.novellibrary.model.preference

import android.content.SharedPreferences

/**
 * Minimal in-memory fake of [SharedPreferences] for plain-JVM unit tests.
 *
 * Only implements the subset of the API exercised by [DataCenterDownloadStorageLocationTest]:
 * `getString` and an `Editor` supporting `putString`/`apply`. All other members throw
 * [NotImplementedError] since this fake is not intended for general-purpose use.
 */
class FakeSharedPreferences : SharedPreferences {

    private val values = mutableMapOf<String, String?>()

    override fun getString(key: String?, defValue: String?): String? = values[key] ?: defValue

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun getAll(): MutableMap<String, *> = values
    override fun getInt(key: String?, defValue: Int): Int = throw NotImplementedError()
    override fun getLong(key: String?, defValue: Long): Long = throw NotImplementedError()
    override fun getFloat(key: String?, defValue: Float): Float = throw NotImplementedError()
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = throw NotImplementedError()
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = throw NotImplementedError()
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private inner class FakeEditor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, String?>()
        private val toRemove = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = throw NotImplementedError()
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = throw NotImplementedError()
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = throw NotImplementedError()
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = throw NotImplementedError()
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = throw NotImplementedError()

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) toRemove.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            if (clearAll) values.clear()
            toRemove.forEach { values.remove(it) }
            values.putAll(pending)
        }
    }
}
