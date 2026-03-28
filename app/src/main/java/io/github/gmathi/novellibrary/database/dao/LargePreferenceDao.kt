package io.github.gmathi.novellibrary.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.gmathi.novellibrary.model.database.LargePreference

@Dao
interface LargePreferenceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(preference: LargePreference)

    /** Inserts or replaces — equivalent to the legacy createOrUpdateLargePreference(). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(preference: LargePreference)

    @Update
    fun update(preference: LargePreference)

    @Query("SELECT * FROM large_preference WHERE name = :name LIMIT 1")
    fun getByName(name: String): LargePreference?

    @Query("DELETE FROM large_preference WHERE name = :name")
    fun delete(name: String)
}
