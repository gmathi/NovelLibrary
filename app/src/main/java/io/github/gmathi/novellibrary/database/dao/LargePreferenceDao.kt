package io.github.gmathi.novellibrary.database.dao

import androidx.room.*
import io.github.gmathi.novellibrary.model.database.LargePreference

@Dao
interface LargePreferenceDao {
    @Insert
    fun insertAll(vararg largePreferences: LargePreference)

    @Insert
    fun insert(largePreference: LargePreference)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(largePreference: LargePreference)
    
    @Update
    fun update(largePreference: LargePreference)
    
    @Delete
    fun delete(largePreference: LargePreference)

    @Query("DELETE FROM large_preference WHERE name = :name")
    fun deleteByName(name: String)
    
    @Query("SELECT * FROM large_preference WHERE name = :name")
    fun findOneByName(name: String): LargePreference?

    @Query("SELECT * FROM large_preference")
    fun getAll(): List<LargePreference>
}