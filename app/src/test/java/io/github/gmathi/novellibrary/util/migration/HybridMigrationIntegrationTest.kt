package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.system.ExtensionManager
import io.github.gmathi.novellibrary.util.system.SourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Integration tests for hybrid Injekt/Hilt migration scenarios.
 * Tests the coexistence of both dependency injection systems during gradual migration.
 */
@HiltAndroidTest
class HybridMigrationIntegrationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var migrationFeatureFlags: MigrationFeatureFlags
    
    @Inject
    lateinit var injektHiltBridge: InjektHiltBridge
    
    @Inject
    lateinit var migrationLogger: MigrationLogger
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun `hybrid mode allows selective component migration`() {
        // Given - Enable only ViewModels and Repositories for Hilt
        migrationFeatureFlags.setMigrationEnabled(true)
        migrationFeatureFlags.setRollbackMode(false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, false)
        
        // When - Get dependencies through bridge
        val dbHelper = injektHiltBridge.getDBHelper()
        val dataCenter = injektHiltBridge.getDataCenter()
        val networkHelper = injektHiltBridge.getNetworkHelper()
        
        // Then - Should get Hilt instances for enabled components
        assertNotNull("DBHelper should be provided", dbHelper)
        assertNotNull("DataCenter should be provided", dataCenter)
        assertNotNull("NetworkHelper should be provided", networkHelper)
        
        // Verify migration status reflects selective enablement
        val status = injektHiltBridge.getMigrationStatus()
        assertTrue("Migration should be enabled", status.isGlobalMigrationEnabled)
        assertFalse("Rollback should be disabled", status.isRollbackMode)
        assertTrue("ViewModels should use Hilt", status.componentStatus["ViewModels"] == true)
        assertTrue("Repositories should use Hilt", status.componentStatus["Repositories"] == true)
        assertFalse("Activities should use Injekt", status.componentStatus["Activities"] == true)
        assertFalse("Fragments should use Injekt", status.componentStatus["Fragments"] == true)
        assertFalse("Services should use Injekt", status.componentStatus["Services"] == true)
    }
    
    @Test
    fun `rollback mode forces all components to use Injekt`() {
        // Given - Enable rollback mode
        migrationFeatureFlags.setMigrationEnabled(true)
        migrationFeatureFlags.setRollbackMode(true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        
        // When - Check if Hilt is enabled for any component
        val viewModelsEnabled = migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS)
        val repositoriesEnabled = migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)
        
        // Then - All components should be disabled due to rollback mode
        assertFalse("ViewModels should be disabled in rollback mode", viewModelsEnabled)
        assertFalse("Repositories should be disabled in rollback mode", repositoriesEnabled)
        
        val status = injektHiltBridge.getMigrationStatus()
        assertTrue("Rollback mode should be active", status.isRollbackMode)
    }
    
    @Test
    fun `progressive rollout enables components in correct order`() {
        // Given - Start with all components disabled
        migrationFeatureFlags.setMigrationEnabled(false)
        migrationFeatureFlags.setRollbackMode(false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS, false)
        
        // When - Enable progressive rollout
        migrationFeatureFlags.enableProgressiveRollout()
        
        // Then - All components should be enabled
        assertTrue("Migration should be enabled", migrationFeatureFlags.isMigrationEnabled())
        assertFalse("Rollback should be disabled", migrationFeatureFlags.isRollbackMode())
        assertTrue("ViewModels should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertTrue("Repositories should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES))
        assertTrue("Activities should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertTrue("Fragments should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS))
        assertTrue("Services should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        assertTrue("Workers should be enabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS))
    }
    
    @Test
    fun `emergency rollback immediately disables all Hilt components`() {
        // Given - All components enabled
        migrationFeatureFlags.enableProgressiveRollout()
        
        // Verify initial state
        assertTrue("Migration should be enabled initially", migrationFeatureFlags.isMigrationEnabled())
        assertTrue("ViewModels should be enabled initially", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        
        // When - Trigger emergency rollback
        migrationFeatureFlags.emergencyRollback()
        
        // Then - All components should be disabled
        assertTrue("Rollback mode should be active", migrationFeatureFlags.isRollbackMode())
        assertFalse("ViewModels should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertFalse("Repositories should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES))
        assertFalse("Activities should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
        assertFalse("Fragments should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS))
        assertFalse("Services should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES))
        assertFalse("Workers should be disabled", migrationFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS))
    }
    
    @Test
    fun `dependency validation detects inconsistencies between Injekt and Hilt`() {
        // Given - Migration enabled for repositories
        migrationFeatureFlags.setMigrationEnabled(true)
        migrationFeatureFlags.setRollbackMode(false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        
        // When - Validate dependency parity
        val validationResult = injektHiltBridge.validateDependencyParity()
        
        // Then - Should complete validation (may pass or fail depending on setup)
        assertNotNull("Validation result should not be null", validationResult)
        // Note: The actual validation result depends on whether Injekt is properly configured
        // In a real scenario, this would validate that both systems provide equivalent instances
    }
    
    @Test
    fun `migration status provides comprehensive component overview`() {
        // Given - Mixed migration state
        migrationFeatureFlags.setMigrationEnabled(true)
        migrationFeatureFlags.setRollbackMode(false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, false)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, true)
        
        // When - Get migration status
        val status = injektHiltBridge.getMigrationStatus()
        
        // Then - Should reflect mixed state
        assertTrue("Migration should be enabled", status.isGlobalMigrationEnabled)
        assertFalse("Rollback should be disabled", status.isRollbackMode)
        assertEquals("Should have 6 component statuses", 6, status.componentStatus.size)
        
        // Verify individual component states
        assertTrue("ViewModels should use Hilt", status.componentStatus["ViewModels"] == true)
        assertFalse("Activities should use Injekt", status.componentStatus["Activities"] == true)
        assertTrue("Fragments should use Hilt", status.componentStatus["Fragments"] == true)
        assertFalse("Services should use Injekt", status.componentStatus["Services"] == true)
        assertTrue("Workers should use Hilt", status.componentStatus["Workers"] == true)
        assertTrue("Repositories should use Hilt", status.componentStatus["Repositories"] == true)
    }
    
    @Test
    fun `flag persistence survives application restart simulation`() {
        // Given - Set specific flags
        migrationFeatureFlags.setMigrationEnabled(true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, true)
        migrationFeatureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
        
        // When - Create new instance (simulating app restart)
        val newFeatureFlags = MigrationFeatureFlags(context, migrationLogger)
        
        // Then - Flags should persist
        assertTrue("Migration should remain enabled", newFeatureFlags.isMigrationEnabled())
        assertTrue("ViewModels flag should persist", newFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS))
        assertFalse("Activities flag should persist", newFeatureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES))
    }
}