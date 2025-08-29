package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MigrationFeatureFlags to ensure proper feature flag management
 * during gradual migration from Injekt to Hilt.
 */
class MigrationFeatureFlagsTest {
    
    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var migrationLogger: MigrationLogger
    private lateinit var featureFlags: MigrationFeatureFlags
    
    @Before
    fun setup() {
        context = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        migrationLogger = mockk(relaxed = true)
        
        every { context.getSharedPreferences("migration_feature_flags", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        
        featureFlags = MigrationFeatureFlags(context, migrationLogger)
    }
    
    @Test
    fun `isHiltEnabled returns false when rollback mode is active`() {
        // Given
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) } returns true
        
        // When
        val result = featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS)
        
        // Then
        assertFalse("Should return false when rollback mode is active", result)
    }
    
    @Test
    fun `isHiltEnabled returns false when migration is disabled globally`() {
        // Given
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) } returns true
        
        // When
        val result = featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS)
        
        // Then
        assertFalse("Should return false when migration is disabled globally", result)
    }
    
    @Test
    fun `isHiltEnabled returns component-specific flag when migration is enabled`() {
        // Given
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) } returns true
        
        // When
        val result = featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS)
        
        // Then
        assertTrue("Should return component-specific flag value", result)
    }
    
    @Test
    fun `setHiltEnabled updates preference and logs change`() {
        // When
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false) }
        verify { editor.apply() }
        verify { migrationLogger.logFlagChange(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false) }
    }
    
    @Test
    fun `setMigrationEnabled updates global flag and logs change`() {
        // When
        featureFlags.setMigrationEnabled(false)
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, false) }
        verify { editor.apply() }
        verify { migrationLogger.logGlobalMigrationChange(false) }
    }
    
    @Test
    fun `setRollbackMode updates rollback flag and logs change`() {
        // When
        featureFlags.setRollbackMode(true)
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, true) }
        verify { editor.apply() }
        verify { migrationLogger.logRollbackModeChange(true) }
    }
    
    @Test
    fun `getAllFlags returns current state of all flags`() {
        // Given
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_SERVICES, true) } returns false
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_WORKERS, true) } returns true
        every { sharedPreferences.getBoolean(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true) } returns true
        
        // When
        val flags = featureFlags.getAllFlags()
        
        // Then
        assertEquals(8, flags.size)
        assertTrue("ViewModels should be enabled", flags[MigrationFeatureFlags.FLAG_HILT_VIEWMODELS] == true)
        assertFalse("Activities should be disabled", flags[MigrationFeatureFlags.FLAG_HILT_ACTIVITIES] == true)
        assertTrue("Migration should be enabled", flags[MigrationFeatureFlags.FLAG_MIGRATION_ENABLED] == true)
        assertFalse("Rollback should be disabled", flags[MigrationFeatureFlags.FLAG_ROLLBACK_MODE] == true)
    }
    
    @Test
    fun `resetToDefaults sets all flags to default values and logs reset`() {
        // When
        featureFlags.resetToDefaults()
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_SERVICES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_WORKERS, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) }
        verify { editor.apply() }
        verify { migrationLogger.logFlagsReset() }
    }
    
    @Test
    fun `enableProgressiveRollout enables all components in correct order`() {
        // When
        featureFlags.enableProgressiveRollout()
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_MIGRATION_ENABLED, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, false) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_SERVICES, true) }
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_HILT_WORKERS, true) }
        verify { migrationLogger.logProgressiveRollout() }
    }
    
    @Test
    fun `emergencyRollback activates rollback mode and logs emergency`() {
        // When
        featureFlags.emergencyRollback()
        
        // Then
        verify { editor.putBoolean(MigrationFeatureFlags.FLAG_ROLLBACK_MODE, true) }
        verify { editor.apply() }
        verify { migrationLogger.logRollbackModeChange(true) }
        verify { migrationLogger.logEmergencyRollback() }
    }
}