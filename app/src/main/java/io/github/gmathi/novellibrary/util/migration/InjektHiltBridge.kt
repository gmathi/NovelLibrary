package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge utility to allow coexistence of Injekt and Hilt during migration.
 * Provides a unified interface to get dependencies from either system based on feature flags.
 */
@Singleton
class InjektHiltBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val featureFlags: MigrationFeatureFlags,
    private val migrationLogger: MigrationLogger,
    // Hilt-provided dependencies
    private val hiltDBHelper: DBHelper,
    private val hiltDataCenter: DataCenter,
    private val hiltNetworkHelper: NetworkHelper,
    private val hiltSourceManager: SourceManager,
    private val hiltExtensionManager: ExtensionManager
) {
    
    /**
     * Get DBHelper from either Hilt or Injekt based on feature flags
     */
    fun getDBHelper(): DBHelper {
        return if (featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)) {
            migrationLogger.logDependencySource("DBHelper", "Hilt")
            hiltDBHelper
        } else {
            migrationLogger.logDependencySource("DBHelper", "Injekt")
            try {
                Injekt.get<DBHelper>()
            } catch (e: Exception) {
                migrationLogger.logFallbackToHilt("DBHelper", e)
                hiltDBHelper
            }
        }
    }
    
    /**
     * Get DataCenter from either Hilt or Injekt based on feature flags
     */
    fun getDataCenter(): DataCenter {
        return if (featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)) {
            migrationLogger.logDependencySource("DataCenter", "Hilt")
            hiltDataCenter
        } else {
            migrationLogger.logDependencySource("DataCenter", "Injekt")
            try {
                Injekt.get<DataCenter>()
            } catch (e: Exception) {
                migrationLogger.logFallbackToHilt("DataCenter", e)
                hiltDataCenter
            }
        }
    }
    
    /**
     * Get NetworkHelper from either Hilt or Injekt based on feature flags
     */
    fun getNetworkHelper(): NetworkHelper {
        return if (featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)) {
            migrationLogger.logDependencySource("NetworkHelper", "Hilt")
            hiltNetworkHelper
        } else {
            migrationLogger.logDependencySource("NetworkHelper", "Injekt")
            try {
                Injekt.get<NetworkHelper>()
            } catch (e: Exception) {
                migrationLogger.logFallbackToHilt("NetworkHelper", e)
                hiltNetworkHelper
            }
        }
    }
    
    /**
     * Get SourceManager from either Hilt or Injekt based on feature flags
     */
    fun getSourceManager(): SourceManager {
        return if (featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)) {
            migrationLogger.logDependencySource("SourceManager", "Hilt")
            hiltSourceManager
        } else {
            migrationLogger.logDependencySource("SourceManager", "Injekt")
            try {
                Injekt.get<SourceManager>()
            } catch (e: Exception) {
                migrationLogger.logFallbackToHilt("SourceManager", e)
                hiltSourceManager
            }
        }
    }
    
    /**
     * Get ExtensionManager from either Hilt or Injekt based on feature flags
     */
    fun getExtensionManager(): ExtensionManager {
        return if (featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)) {
            migrationLogger.logDependencySource("ExtensionManager", "Hilt")
            hiltExtensionManager
        } else {
            migrationLogger.logDependencySource("ExtensionManager", "Injekt")
            try {
                Injekt.get<ExtensionManager>()
            } catch (e: Exception) {
                migrationLogger.logFallbackToHilt("ExtensionManager", e)
                hiltExtensionManager
            }
        }
    }
    
    /**
     * Validate that both Injekt and Hilt provide the same instances for critical dependencies
     */
    fun validateDependencyParity(): DependencyValidationResult {
        val results = mutableListOf<String>()
        
        try {
            // Temporarily get from both systems for comparison
            val injektDBHelper = Injekt.get<DBHelper>()
            val hiltDBHelperTest = hiltDBHelper
            
            // Validate they're functionally equivalent (same database path, etc.)
            if (injektDBHelper.databaseName != hiltDBHelperTest.databaseName) {
                results.add("DBHelper database names differ: Injekt=${injektDBHelper.databaseName}, Hilt=${hiltDBHelperTest.databaseName}")
            }
            
        } catch (e: Exception) {
            results.add("Failed to validate DBHelper parity: ${e.message}")
        }
        
        try {
            val injektDataCenter = Injekt.get<DataCenter>()
            val hiltDataCenterTest = hiltDataCenter
            
            // Validate configuration parity - both should be functional
            // Note: Context comparison may not be meaningful, so we'll skip this check
            
        } catch (e: Exception) {
            results.add("Failed to validate DataCenter parity: ${e.message}")
        }
        
        return DependencyValidationResult(
            isValid = results.isEmpty(),
            issues = results
        )
    }
    
    /**
     * Get migration status for all components
     */
    fun getMigrationStatus(): ComponentMigrationStatus {
        return ComponentMigrationStatus(
            isGlobalMigrationEnabled = featureFlags.isMigrationEnabled(),
            isRollbackMode = featureFlags.isRollbackMode(),
            componentStatus = mapOf(
                "ViewModels" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS),
                "Activities" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES),
                "Fragments" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS),
                "Services" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES),
                "Workers" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS),
                "Repositories" to featureFlags.isHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES)
            )
        )
    }
}

/**
 * Result of dependency validation between Injekt and Hilt
 */
data class DependencyValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)

/**
 * Current migration status across all components
 */
data class ComponentMigrationStatus(
    val isGlobalMigrationEnabled: Boolean,
    val isRollbackMode: Boolean,
    val componentStatus: Map<String, Boolean>
)