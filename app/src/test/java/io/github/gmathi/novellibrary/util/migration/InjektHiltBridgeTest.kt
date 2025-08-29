package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.system.ExtensionManager
import io.github.gmathi.novellibrary.util.system.SourceManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import uy.kohesive.injekt.Injekt

/**
 * Unit tests for InjektHiltBridge to ensure proper hybrid dependency resolution
 * during gradual migration from Injekt to Hilt.
 */
class InjektHiltBridgeTest {
    
    private lateinit var context: Context
    private lateinit var featureFlags: MigrationFeatureFlags
    private lateinit var migrationLogger: MigrationLogger
    private lateinit var hiltDBHelper: DBHelper
    private lateinit var hiltDataCenter: DataCenter
    private lateinit var hiltNetworkHelper: NetworkHelper
    private lateinit var hiltSourceManager: SourceManager
    private lateinit var hiltExtensionManager: ExtensionManager
    private lateinit var injektDBHelper: DBHelper
    private lateinit var bridge: InjektHiltBridge
    
    @Before
    fun setup() {
        context = mockk()
        featureFlags = mockk()
        migrationLogger = mockk(relaxed = true)
        hiltDBHelper = mockk()
        hiltDataCenter = mockk()
        hiltNetworkHelper = mockk()
        hiltSourceManager = mockk()
        hiltExtensionManager = mockk()
        injektDBHelper = mockk()
        
        bridge = InjektHiltBridge(
            context = context,
            featureFlags = featureFlags,
            migrationLogger = migrationLogger,
            hiltDBHelper = hiltDBHelper,
            hiltDataCenter = hiltDataCenter,
            hiltNetworkHelper = hiltNetworkHelper,
            hiltSourceManager = hiltSourceManager,
            hiltExtensionManager = hiltExtensionManager
        )
        
        // Mock Injekt
        mockkObject(Injekt)
    }
    
    @Test
    fun `getDBHelper returns Hilt instance when Hilt is enabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val result = bridge.getDBHelper()
        
        // Then
        assertEquals("Should return Hilt DBHelper", hiltDBHelper, result)
        verify { migrationLogger.logDependencySource("DBHelper", "Hilt") }
    }
    
    @Test
    fun `getDBHelper returns Injekt instance when Hilt is disabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns false
        every { Injekt.get<DBHelper>() } returns injektDBHelper
        
        // When
        val result = bridge.getDBHelper()
        
        // Then
        assertEquals("Should return Injekt DBHelper", injektDBHelper, result)
        verify { migrationLogger.logDependencySource("DBHelper", "Injekt") }
    }
    
    @Test
    fun `getDBHelper falls back to Hilt when Injekt fails`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns false
        every { Injekt.get<DBHelper>() } throws RuntimeException("Injekt failed")
        
        // When
        val result = bridge.getDBHelper()
        
        // Then
        assertEquals("Should fallback to Hilt DBHelper", hiltDBHelper, result)
        verify { migrationLogger.logDependencySource("DBHelper", "Injekt") }
        verify { migrationLogger.logFallbackToHilt("DBHelper", any()) }
    }
    
    @Test
    fun `getDataCenter returns Hilt instance when Hilt is enabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val result = bridge.getDataCenter()
        
        // Then
        assertEquals("Should return Hilt DataCenter", hiltDataCenter, result)
        verify { migrationLogger.logDependencySource("DataCenter", "Hilt") }
    }
    
    @Test
    fun `getNetworkHelper returns Hilt instance when Hilt is enabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val result = bridge.getNetworkHelper()
        
        // Then
        assertEquals("Should return Hilt NetworkHelper", hiltNetworkHelper, result)
        verify { migrationLogger.logDependencySource("NetworkHelper", "Hilt") }
    }
    
    @Test
    fun `getSourceManager returns Hilt instance when Hilt is enabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val result = bridge.getSourceManager()
        
        // Then
        assertEquals("Should return Hilt SourceManager", hiltSourceManager, result)
        verify { migrationLogger.logDependencySource("SourceManager", "Hilt") }
    }
    
    @Test
    fun `getExtensionManager returns Hilt instance when Hilt is enabled`() {
        // Given
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val result = bridge.getExtensionManager()
        
        // Then
        assertEquals("Should return Hilt ExtensionManager", hiltExtensionManager, result)
        verify { migrationLogger.logDependencySource("ExtensionManager", "Hilt") }
    }
    
    @Test
    fun `validateDependencyParity detects database name differences`() {
        // Given
        every { Injekt.get<DBHelper>() } returns injektDBHelper
        every { injektDBHelper.databaseName } returns "injekt_db"
        every { hiltDBHelper.databaseName } returns "hilt_db"
        
        // When
        val result = bridge.validateDependencyParity()
        
        // Then
        assertFalse("Should detect database name difference", result.isValid)
        assertTrue("Should report database name issue", 
            result.issues.any { it.contains("database names differ") })
    }
    
    @Test
    fun `validateDependencyParity passes when dependencies are equivalent`() {
        // Given
        every { Injekt.get<DBHelper>() } returns injektDBHelper
        every { Injekt.get<DataCenter>() } returns hiltDataCenter
        every { injektDBHelper.databaseName } returns "novel_library_db"
        every { hiltDBHelper.databaseName } returns "novel_library_db"
        every { hiltDataCenter.context } returns context
        
        // When
        val result = bridge.validateDependencyParity()
        
        // Then
        assertTrue("Should pass when dependencies are equivalent", result.isValid)
        assertTrue("Should have no issues", result.issues.isEmpty())
    }
    
    @Test
    fun `getMigrationStatus returns current migration state`() {
        // Given
        every { featureFlags.isMigrationEnabled() } returns true
        every { featureFlags.isRollbackMode() } returns false
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS) } returns true
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES) } returns false
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS) } returns true
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES) } returns false
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS) } returns true
        every { featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES) } returns true
        
        // When
        val status = bridge.getMigrationStatus()
        
        // Then
        assertTrue("Migration should be enabled", status.isGlobalMigrationEnabled)
        assertFalse("Rollback should be disabled", status.isRollbackMode)
        assertEquals("Should have 6 component statuses", 6, status.componentStatus.size)
        assertTrue("ViewModels should be enabled", status.componentStatus["ViewModels"] == true)
        assertFalse("Activities should be disabled", status.componentStatus["Activities"] == true)
        assertTrue("Fragments should be enabled", status.componentStatus["Fragments"] == true)
        assertFalse("Services should be disabled", status.componentStatus["Services"] == true)
        assertTrue("Workers should be enabled", status.componentStatus["Workers"] == true)
        assertTrue("Repositories should be enabled", status.componentStatus["Repositories"] == true)
    }
}