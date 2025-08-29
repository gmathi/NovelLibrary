package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flags system to control gradual migration from Injekt to Hilt.
 * Allows enabling/disabling Hilt for specific components during migration.
 */
@Singleton
class MigrationFeatureFlags @Inject constructor(
    private val context: Context,
    private val migrationLogger: MigrationLogger
) {
    
    companion object {
        private const val PREFS_NAME = "migration_feature_flags"
        
        // Component-specific flags
        const val FLAG_HILT_VIEWMODELS = "hilt_viewmodels_enabled"
        const val FLAG_HILT_ACTIVITIES = "hilt_activities_enabled"
        const val FLAG_HILT_FRAGMENTS = "hilt_fragments_enabled"
        const val FLAG_HILT_SERVICES = "hilt_services_enabled"
        const val FLAG_HILT_WORKERS = "hilt_workers_enabled"
        const val FLAG_HILT_REPOSITORIES = "hilt_repositories_enabled"
        
        // Global migration flag
        const val FLAG_MIGRATION_ENABLED = "migration_enabled"
        const val FLAG_ROLLBACK_MODE = "rollback_mode"
        
        // Default values (start with Hilt disabled for gradual rollout)
        private val DEFAULT_FLAGS = mapOf(
            FLAG_HILT_VIEWMODELS to true,  // ViewModels already migrated
            FLAG_HILT_ACTIVITIES to true,  // Activities already migrated
            FLAG_HILT_FRAGMENTS to true,   // Fragments already migrated
            FLAG_HILT_SERVICES to true,    // Services already migrated
            FLAG_HILT_WORKERS to true,     // Workers already migrated
            FLAG_HILT_REPOSITORIES to true, // Repositories already migrated
            FLAG_MIGRATION_ENABLED to true,
            FLAG_ROLLBACK_MODE to false
        )
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Check if Hilt is enabled for a specific component type
     */
    fun isHiltEnabled(componentFlag: String): Boolean {
        if (isRollbackMode()) return false
        if (!isMigrationEnabled()) return false
        
        return prefs.getBoolean(componentFlag, DEFAULT_FLAGS[componentFlag] ?: false)
    }
    
    /**
     * Enable/disable Hilt for a specific component type
     */
    fun setHiltEnabled(componentFlag: String, enabled: Boolean) {
        prefs.edit().putBoolean(componentFlag, enabled).apply()
        migrationLogger.logFlagChange(componentFlag, enabled)
    }
    
    /**
     * Check if migration is globally enabled
     */
    fun isMigrationEnabled(): Boolean {
        return prefs.getBoolean(FLAG_MIGRATION_ENABLED, DEFAULT_FLAGS[FLAG_MIGRATION_ENABLED] ?: true)
    }
    
    /**
     * Enable/disable migration globally
     */
    fun setMigrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(FLAG_MIGRATION_ENABLED, enabled).apply()
        migrationLogger.logGlobalMigrationChange(enabled)
    }
    
    /**
     * Check if rollback mode is active (forces all components to use Injekt)
     */
    fun isRollbackMode(): Boolean {
        return prefs.getBoolean(FLAG_ROLLBACK_MODE, DEFAULT_FLAGS[FLAG_ROLLBACK_MODE] ?: false)
    }
    
    /**
     * Enable/disable rollback mode
     */
    fun setRollbackMode(enabled: Boolean) {
        prefs.edit().putBoolean(FLAG_ROLLBACK_MODE, enabled).apply()
        migrationLogger.logRollbackModeChange(enabled)
    }
    
    /**
     * Get all current flag states for debugging
     */
    fun getAllFlags(): Map<String, Boolean> {
        return mapOf(
            FLAG_HILT_VIEWMODELS to isHiltEnabled(FLAG_HILT_VIEWMODELS),
            FLAG_HILT_ACTIVITIES to isHiltEnabled(FLAG_HILT_ACTIVITIES),
            FLAG_HILT_FRAGMENTS to isHiltEnabled(FLAG_HILT_FRAGMENTS),
            FLAG_HILT_SERVICES to isHiltEnabled(FLAG_HILT_SERVICES),
            FLAG_HILT_WORKERS to isHiltEnabled(FLAG_HILT_WORKERS),
            FLAG_HILT_REPOSITORIES to isHiltEnabled(FLAG_HILT_REPOSITORIES),
            FLAG_MIGRATION_ENABLED to isMigrationEnabled(),
            FLAG_ROLLBACK_MODE to isRollbackMode()
        )
    }
    
    /**
     * Reset all flags to default values
     */
    fun resetToDefaults() {
        val editor = prefs.edit()
        DEFAULT_FLAGS.forEach { (flag, value) ->
            editor.putBoolean(flag, value)
        }
        editor.apply()
        migrationLogger.logFlagsReset()
    }
    
    /**
     * Enable progressive rollout - gradually enable Hilt components
     */
    fun enableProgressiveRollout() {
        setMigrationEnabled(true)
        setRollbackMode(false)
        
        // Enable components in order of migration completion
        setHiltEnabled(FLAG_HILT_REPOSITORIES, true)
        setHiltEnabled(FLAG_HILT_VIEWMODELS, true)
        setHiltEnabled(FLAG_HILT_ACTIVITIES, true)
        setHiltEnabled(FLAG_HILT_FRAGMENTS, true)
        setHiltEnabled(FLAG_HILT_SERVICES, true)
        setHiltEnabled(FLAG_HILT_WORKERS, true)
        
        migrationLogger.logProgressiveRollout()
    }
    
    /**
     * Emergency rollback - disable all Hilt components
     */
    fun emergencyRollback() {
        setRollbackMode(true)
        migrationLogger.logEmergencyRollback()
    }
}