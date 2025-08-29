package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Integration tests for rollback scenarios during Hilt migration.
 * Tests various rollback situations and recovery mechanisms.
 */
@HiltAndroidTest
class RollbackScenariosIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var rollbackManager: MigrationRollbackManager
    
    @Inject
    lateinit var featureFlags: MigrationFeatureFlags
    
    @Inject
    lateinit var migrationLogger: MigrationLogger
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        
        // Reset to clean state
        featureFlags.resetToDefaults()
    }
    
    @Test
    fun `emergency rollback scenario - critical error during migration`() = runTest {
        // Given - Migration is partially complete
        featureFlags.setMigrationEnabled(true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, false)
        
        // Verify initial state
        assertTrue("ViewModels should be using Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertTrue("Activities should be using Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        
        // When - Emergency rollback is triggered
        val result = rollbackManager.performEmergencyRollback("Critical dependency injection failure")
        
        // Then - All components should be reverted to Injekt
        assertTrue("Emergency rollback should succeed", result is RollbackResult.Success)
        assertTrue("Rollback mode should be active", featureFlags.isRollbackMode())
        assertFalse("ViewModels should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertFalse("Activities should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertFalse("Fragments should remain with Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS))
        assertFalse("Services should remain with Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        
        // Verify rollback history
        val history = rollbackManager.getRollbackHistory()
        assertEquals("Should have one rollback", 1, history.rollbackCount)
        assertEquals("Should record correct reason", "Critical dependency injection failure", history.lastRollbackReason)
        assertTrue("Should have timestamp", history.lastRollbackTimestamp > 0)
    }
    
    @Test
    fun `selective rollback scenario - specific component issues`() = runTest {
        // Given - Full migration is complete
        featureFlags.enableProgressiveRollout()
        
        // Verify all components are using Hilt
        assertTrue("All components should be using Hilt initially", 
            featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS) &&
            featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES) &&
            featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS) &&
            featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        
        // When - Selective rollback for problematic components
        val componentsToRollback = listOf("Services", "Workers")
        val result = rollbackManager.performSelectiveRollback(componentsToRollback, "Service injection causing crashes")
        
        // Then - Only specified components should be reverted
        assertTrue("Selective rollback should succeed", result is RollbackResult.Success)
        assertFalse("Rollback mode should not be active", featureFlags.isRollbackMode())
        assertTrue("ViewModels should still use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertTrue("Activities should still use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertTrue("Fragments should still use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS))
        assertFalse("Services should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        assertFalse("Workers should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS))
    }
    
    @Test
    fun `checkpoint and restore scenario - safe migration points`() = runTest {
        // Given - Create checkpoint at stable state
        featureFlags.setMigrationEnabled(true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        
        rollbackManager.createMigrationCheckpoint("after_viewmodels_migration")
        
        // When - Continue migration and encounter issues
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, true)
        
        // Verify problematic state
        assertTrue("Activities should be using Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertTrue("Services should be using Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        
        // Then - Restore from checkpoint
        val result = rollbackManager.restoreFromCheckpoint("after_viewmodels_migration")
        
        // Verify restoration
        assertTrue("Checkpoint restoration should succeed", result is RollbackResult.Success)
        assertTrue("ViewModels should still use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertTrue("Repositories should still use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES))
        assertFalse("Activities should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertFalse("Fragments should be reverted to Injekt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS))
        
        // Verify checkpoint is available in history
        val history = rollbackManager.getRollbackHistory()
        assertTrue("Checkpoint should be available", history.availableCheckpoints.contains("after_viewmodels_migration"))
    }
    
    @Test
    fun `successful state tracking scenario - mark and restore`() = runTest {
        // Given - Achieve stable migration state
        featureFlags.setMigrationEnabled(true)
        featureFlags.setRollbackMode(false)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true)
        
        // Mark as successful
        rollbackManager.markCurrentStateAsSuccessful()
        
        // When - Migration continues and fails
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, true)
        featureFlags.emergencyRollback() // Simulate failure
        
        // Verify failure state
        assertTrue("Should be in rollback mode", featureFlags.isRollbackMode())
        
        // Then - Restore to last successful state
        val result = rollbackManager.restoreToLastSuccessfulState()
        
        // Verify restoration to successful state
        assertTrue("Restoration should succeed", result is RollbackResult.Success)
        assertTrue("Migration should be enabled", featureFlags.isMigrationEnabled())
        assertFalse("Rollback mode should be disabled", featureFlags.isRollbackMode())
        assertTrue("ViewModels should use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertTrue("Repositories should use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES))
        assertTrue("Activities should use Hilt", featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        
        // Verify history shows successful state is available
        val history = rollbackManager.getRollbackHistory()
        assertTrue("Should have last successful state", history.hasLastSuccessfulState)
    }
    
    @Test
    fun `multiple rollback scenario - rollback count tracking`() = runTest {
        // Given - Initial state
        featureFlags.enableProgressiveRollout()
        
        // When - Perform multiple rollbacks
        rollbackManager.performEmergencyRollback("First issue")
        
        // Reset and try again
        featureFlags.setRollbackMode(false)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        
        rollbackManager.performSelectiveRollback(listOf("ViewModels"), "Second issue")
        
        // Reset and try emergency again
        featureFlags.setMigrationEnabled(true)
        rollbackManager.performEmergencyRollback("Third issue")
        
        // Then - Verify rollback count
        val history = rollbackManager.getRollbackHistory()
        assertEquals("Should track multiple rollbacks", 2, history.rollbackCount) // Emergency rollbacks are counted
        assertEquals("Should have latest reason", "Third issue", history.lastRollbackReason)
        assertTrue("Should have recent timestamp", 
            System.currentTimeMillis() - history.lastRollbackTimestamp < 10000) // Within 10 seconds
    }
    
    @Test
    fun `rollback validation scenario - ensure proper state after rollback`() = runTest {
        // Given - Migration state with some components enabled
        featureFlags.setMigrationEnabled(true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, true)
        featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        
        // When - Perform emergency rollback
        val result = rollbackManager.performEmergencyRollback("Validation test")
        
        // Then - Verify rollback was successful and properly validated
        assertTrue("Rollback should succeed", result is RollbackResult.Success)
        
        // Verify all Hilt flags are disabled
        val allFlags = featureFlags.getAllFlags()
        allFlags.forEach { (flag, enabled) ->
            if (flag != MigrationFeatureFlags.FLAG_MIGRATION_ENABLED && 
                flag != MigrationFeatureFlags.FLAG_ROLLBACK_MODE) {
                assertFalse("Flag $flag should be disabled after rollback", enabled)
            }
        }
        
        // Verify rollback mode is active
        assertTrue("Rollback mode should be active", featureFlags.isRollbackMode())
        
        // Verify migration is still enabled (rollback mode overrides component flags)
        assertTrue("Migration should remain enabled", featureFlags.isMigrationEnabled())
    }
}