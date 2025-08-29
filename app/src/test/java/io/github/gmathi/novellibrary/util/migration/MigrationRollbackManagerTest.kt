package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import android.content.SharedPreferences
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MigrationRollbackManager to ensure proper rollback functionality
 * during Hilt migration.
 */
class MigrationRollbackManagerTest {
    
    private lateinit var context: Context
    private lateinit var featureFlags: MigrationFeatureFlags
    private lateinit var migrationLogger: MigrationLogger
    private lateinit var migrationValidator: MigrationValidator
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var rollbackManager: MigrationRollbackManager
    
    @Before
    fun setup() {
        context = mockk()
        featureFlags = mockk(relaxed = true)
        migrationLogger = mockk(relaxed = true)
        migrationValidator = mockk(relaxed = true)
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        
        every { context.getSharedPreferences("migration_rollback_state", Context.MODE_PRIVATE) } returns sharedPreferences
        every { sharedPreferences.edit() } returns editor
        every { sharedPreferences.all } returns emptyMap()
        
        rollbackManager = MigrationRollbackManager(
            context = context,
            featureFlags = featureFlags,
            migrationLogger = migrationLogger,
            migrationValidator = migrationValidator
        )
    }
    
    @Test
    fun `performEmergencyRollback activates rollback mode and validates state`() = runTest {
        // Given
        every { featureFlags.getAllFlags() } returns mapOf(
            MigrationFeatureFlags.FLAG_HILT_VIEWMODELS to true,
            MigrationFeatureFlags.FLAG_HILT_ACTIVITIES to true,
            MigrationFeatureFlags.FLAG_MIGRATION_ENABLED to true,
            MigrationFeatureFlags.FLAG_ROLLBACK_MODE to false
        )
        every { featureFlags.isRollbackMode() } returns true
        every { migrationValidator.validateInjektDependencies() } returns io.github.gmathi.novellibrary.util.migration.ValidationResult(true, emptyList())
        every { sharedPreferences.getInt("rollback_count", 0) } returns 0
        
        // When
        val result = rollbackManager.performEmergencyRollback("Critical error detected")
        
        // Then
        assertTrue("Emergency rollback should succeed", result is RollbackResult.Success)
        verify { featureFlags.emergencyRollback() }
        verify { editor.putString("rollback_reason", "Critical error detected") }
        verify { editor.putLong("rollback_timestamp", any()) }
        verify { editor.putInt("rollback_count", 1) }
        verify { migrationLogger.logPhaseStart("Emergency Rollback") }
        verify { migrationLogger.logPhaseComplete("Emergency Rollback") }
    }
    
    @Test
    fun `performEmergencyRollback fails when validation fails`() = runTest {
        // Given
        every { featureFlags.getAllFlags() } returns mapOf(
            MigrationFeatureFlags.FLAG_HILT_VIEWMODELS to true,
            MigrationFeatureFlags.FLAG_MIGRATION_ENABLED to true,
            MigrationFeatureFlags.FLAG_ROLLBACK_MODE to false
        )
        every { featureFlags.isRollbackMode() } returns false // Rollback mode not activated
        every { migrationValidator.validateInjektDependencies() } returns io.github.gmathi.novellibrary.util.migration.ValidationResult(false, listOf("Injekt not available"))
        
        // When
        val result = rollbackManager.performEmergencyRollback("Test failure")
        
        // Then
        assertTrue("Emergency rollback should fail", result is RollbackResult.Failure)
        val failure = result as RollbackResult.Failure
        assertTrue("Should contain validation error", failure.error.contains("validation failed"))
    }
    
    @Test
    fun `performSelectiveRollback disables specific components`() = runTest {
        // Given
        val componentsToRollback = listOf("ViewModels", "Activities")
        every { featureFlags.getAllFlags() } returns mapOf(
            MigrationFeatureFlags.FLAG_HILT_VIEWMODELS to false,
            MigrationFeatureFlags.FLAG_HILT_ACTIVITIES to false,
            MigrationFeatureFlags.FLAG_HILT_FRAGMENTS to true,
            MigrationFeatureFlags.FLAG_MIGRATION_ENABLED to true,
            MigrationFeatureFlags.FLAG_ROLLBACK_MODE to false
        )
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS) } returns false
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES) } returns false
        
        // When
        val result = rollbackManager.performSelectiveRollback(componentsToRollback, "Selective rollback test")
        
        // Then
        assertTrue("Selective rollback should succeed", result is RollbackResult.Success)
        verify { featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, false) }
        verify { featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false) }
        verify(exactly = 0) { featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false) }
        verify { migrationLogger.logComponentMigration("ViewModels", "Hilt", "Injekt", "rolled_back") }
        verify { migrationLogger.logComponentMigration("Activities", "Hilt", "Injekt", "rolled_back") }
    }
    
    @Test
    fun `createMigrationCheckpoint saves current state`() {
        // Given
        every { featureFlags.isMigrationEnabled() } returns true
        every { featureFlags.isRollbackMode() } returns false
        every { featureFlags.getAllFlags() } returns mapOf(
            MigrationFeatureFlags.FLAG_HILT_VIEWMODELS to true,
            MigrationFeatureFlags.FLAG_MIGRATION_ENABLED to true,
            MigrationFeatureFlags.FLAG_ROLLBACK_MODE to false
        )
        
        // When
        rollbackManager.createMigrationCheckpoint("before_services_migration")
        
        // Then
        verify { editor.putString("checkpoint_before_services_migration", any()) }
        verify { editor.putLong("checkpoint_before_services_migration_timestamp", any()) }
        verify { editor.apply() }
        verify { migrationLogger.logValidationResult("Checkpoint", true, "Created checkpoint: before_services_migration") }
    }
    
    @Test
    fun `restoreFromCheckpoint applies saved state`() = runTest {
        // Given
        val checkpointJson = "{\"globalEnabled\":true,\"rollbackMode\":false,\"flags\":{\"hilt_viewmodels_enabled\":true,\"hilt_activities_enabled\":false}}"
        every { sharedPreferences.getString("checkpoint_test_checkpoint", null) } returns checkpointJson
        every { migrationValidator.validateCurrentMigrationState() } returns io.github.gmathi.novellibrary.util.migration.ValidationResult(true, emptyList())
        
        // When
        val result = rollbackManager.restoreFromCheckpoint("test_checkpoint")
        
        // Then
        assertTrue("Checkpoint restoration should succeed", result is RollbackResult.Success)
        verify { featureFlags.setMigrationEnabled(true) }
        verify { featureFlags.setRollbackMode(false) }
        verify { featureFlags.setHiltEnabled("hilt_viewmodels_enabled", true) }
        verify { featureFlags.setHiltEnabled("hilt_activities_enabled", false) }
        verify { migrationLogger.logPhaseStart("Restore from Checkpoint: test_checkpoint") }
        verify { migrationLogger.logPhaseComplete("Restore from Checkpoint: test_checkpoint") }
    }
    
    @Test
    fun `restoreFromCheckpoint fails when checkpoint not found`() = runTest {
        // Given
        every { sharedPreferences.getString("checkpoint_nonexistent", null) } returns null
        
        // When
        val result = rollbackManager.restoreFromCheckpoint("nonexistent")
        
        // Then
        assertTrue("Should fail when checkpoint not found", result is RollbackResult.Failure)
        val failure = result as RollbackResult.Failure
        assertTrue("Should indicate checkpoint not found", failure.error.contains("not found"))
    }
    
    @Test
    fun `restoreToLastSuccessfulState applies last successful state`() = runTest {
        // Given
        val successfulStateJson = "{\"globalEnabled\":true,\"rollbackMode\":false,\"flags\":{\"hilt_repositories_enabled\":true}}"
        every { sharedPreferences.getString("last_successful_state", null) } returns successfulStateJson
        every { migrationValidator.validateCurrentMigrationState() } returns io.github.gmathi.novellibrary.util.migration.ValidationResult(true, emptyList())
        
        // When
        val result = rollbackManager.restoreToLastSuccessfulState()
        
        // Then
        assertTrue("Should succeed when restoring last successful state", result is RollbackResult.Success)
        verify { featureFlags.setMigrationEnabled(true) }
        verify { featureFlags.setRollbackMode(false) }
        verify { featureFlags.setHiltEnabled("hilt_repositories_enabled", true) }
    }
    
    @Test
    fun `restoreToLastSuccessfulState fails when no successful state exists`() = runTest {
        // Given
        every { sharedPreferences.getString("last_successful_state", null) } returns null
        
        // When
        val result = rollbackManager.restoreToLastSuccessfulState()
        
        // Then
        assertTrue("Should fail when no successful state exists", result is RollbackResult.Failure)
        val failure = result as RollbackResult.Failure
        assertTrue("Should indicate no successful state", failure.error.contains("No successful state"))
    }
    
    @Test
    fun `getRollbackHistory returns complete history`() {
        // Given
        every { sharedPreferences.getInt("rollback_count", 0) } returns 3
        every { sharedPreferences.getString("rollback_reason", null) } returns "Performance issues"
        every { sharedPreferences.getLong("rollback_timestamp", 0) } returns 1234567890L
        every { sharedPreferences.getString("last_successful_state", null) } returns "{\"test\":\"state\"}"
        every { sharedPreferences.all } returns mapOf(
            "checkpoint_test1" to "state1",
            "checkpoint_test1_timestamp" to 123L,
            "checkpoint_test2" to "state2",
            "checkpoint_test2_timestamp" to 456L,
            "other_key" to "value"
        )
        
        // When
        val history = rollbackManager.getRollbackHistory()
        
        // Then
        assertEquals("Should have correct rollback count", 3, history.rollbackCount)
        assertEquals("Should have correct last reason", "Performance issues", history.lastRollbackReason)
        assertEquals("Should have correct timestamp", 1234567890L, history.lastRollbackTimestamp)
        assertTrue("Should have last successful state", history.hasLastSuccessfulState)
        assertEquals("Should have correct checkpoints", listOf("test1", "test2"), history.availableCheckpoints)
    }
    
    @Test
    fun `markCurrentStateAsSuccessful saves current state`() {
        // Given
        every { featureFlags.isMigrationEnabled() } returns true
        every { featureFlags.isRollbackMode() } returns false
        every { featureFlags.getAllFlags() } returns mapOf(
            MigrationFeatureFlags.FLAG_HILT_VIEWMODELS to true,
            MigrationFeatureFlags.FLAG_MIGRATION_ENABLED to true,
            MigrationFeatureFlags.FLAG_ROLLBACK_MODE to false
        )
        
        // When
        rollbackManager.markCurrentStateAsSuccessful()
        
        // Then
        verify { editor.putString("last_successful_state", any()) }
        verify { editor.apply() }
        verify { migrationLogger.logValidationResult("Migration State", true, "Marked current state as successful") }
    }
}