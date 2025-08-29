package io.github.gmathi.novellibrary.util.migration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages rollback mechanisms for Hilt migration.
 * Provides quick reversion to Injekt if issues are discovered during migration.
 */
@Singleton
class MigrationRollbackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val featureFlags: MigrationFeatureFlags,
    private val migrationLogger: MigrationLogger,
    private val migrationValidator: MigrationValidator
) {
    
    companion object {
        private const val TAG = "MigrationRollback"
        private const val ROLLBACK_PREFS = "migration_rollback_state"
        private const val KEY_ROLLBACK_REASON = "rollback_reason"
        private const val KEY_ROLLBACK_TIMESTAMP = "rollback_timestamp"
        private const val KEY_PREVIOUS_STATE = "previous_migration_state"
        private const val KEY_ROLLBACK_COUNT = "rollback_count"
        private const val KEY_LAST_SUCCESSFUL_STATE = "last_successful_state"
    }
    
    private val rollbackPrefs by lazy {
        context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
    }
    
    /**
     * Performs immediate rollback to Injekt for all components
     */
    suspend fun performEmergencyRollback(reason: String): RollbackResult {
        return withContext(Dispatchers.IO) {
            try {
                migrationLogger.logPhaseStart("Emergency Rollback")
                
                // Save current state before rollback
                saveCurrentStateAsRollback(reason)
                
                // Activate rollback mode
                featureFlags.emergencyRollback()
                
                // Validate rollback was successful
                val validationResult = validateRollbackState()
                
                if (validationResult.isSuccessful) {
                    incrementRollbackCount()
                    migrationLogger.logPhaseComplete("Emergency Rollback")
                    RollbackResult.Success("Emergency rollback completed successfully")
                } else {
                    migrationLogger.logError("Emergency rollback validation failed", 
                        RuntimeException(validationResult.issues.joinToString("; ")))
                    RollbackResult.Failure("Rollback validation failed: ${validationResult.issues.joinToString("; ")}")
                }
                
            } catch (e: Exception) {
                migrationLogger.logError("Emergency rollback failed", e)
                RollbackResult.Failure("Emergency rollback failed: ${e.message}")
            }
        }
    }
    
    /**
     * Performs selective rollback for specific components
     */
    suspend fun performSelectiveRollback(components: List<String>, reason: String): RollbackResult {
        return withContext(Dispatchers.IO) {
            try {
                migrationLogger.logPhaseStart("Selective Rollback")
                
                // Save current state
                saveCurrentStateAsRollback(reason)
                
                // Disable Hilt for specified components
                components.forEach { component ->
                    when (component) {
                        "ViewModels" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_VIEWMODELS, false)
                        "Activities" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_ACTIVITIES, false)
                        "Fragments" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_FRAGMENTS, false)
                        "Services" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_SERVICES, false)
                        "Workers" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_WORKERS, false)
                        "Repositories" -> featureFlags.setHiltEnabled(MigrationFeatureFlags.FLAG_HILT_REPOSITORIES, false)
                    }
                    migrationLogger.logComponentMigration(component, "Hilt", "Injekt", "rolled_back")
                }
                
                // Validate selective rollback
                val validationResult = validateSelectiveRollback(components)
                
                if (validationResult.isSuccessful) {
                    migrationLogger.logPhaseComplete("Selective Rollback")
                    RollbackResult.Success("Selective rollback completed for: ${components.joinToString(", ")}")
                } else {
                    RollbackResult.Failure("Selective rollback validation failed: ${validationResult.issues.joinToString("; ")}")
                }
                
            } catch (e: Exception) {
                migrationLogger.logError("Selective rollback failed", e)
                RollbackResult.Failure("Selective rollback failed: ${e.message}")
            }
        }
    }
    
    /**
     * Restores migration to a previously successful state
     */
    suspend fun restoreToLastSuccessfulState(): RollbackResult {
        return withContext(Dispatchers.IO) {
            try {
                val lastSuccessfulState = getLastSuccessfulState()
                if (lastSuccessfulState == null) {
                    return@withContext RollbackResult.Failure("No successful state found to restore")
                }
                
                migrationLogger.logPhaseStart("Restore to Last Successful State")
                
                // Apply the last successful state
                applyMigrationState(lastSuccessfulState)
                
                // Validate restoration
                val isValid = migrationValidator.validateDependencies()
                
                if (isValid) {
                    migrationLogger.logPhaseComplete("Restore to Last Successful State")
                    RollbackResult.Success("Successfully restored to last successful state")
                } else {
                    RollbackResult.Failure("Restoration validation failed")
                }
                
            } catch (e: Exception) {
                migrationLogger.logError("Restoration failed", e)
                RollbackResult.Failure("Restoration failed: ${e.message}")
            }
        }
    }
    
    /**
     * Creates a checkpoint of current migration state for potential rollback
     */
    fun createMigrationCheckpoint(checkpointName: String) {
        try {
            val currentState = getCurrentMigrationState()
            saveCheckpoint(checkpointName, currentState)
            migrationLogger.logValidationResult("Checkpoint", true, "Created checkpoint: $checkpointName")
        } catch (e: Exception) {
            migrationLogger.logError("Failed to create checkpoint", e)
        }
    }
    
    /**
     * Restores migration state from a specific checkpoint
     */
    suspend fun restoreFromCheckpoint(checkpointName: String): RollbackResult {
        return withContext(Dispatchers.IO) {
            try {
                val checkpointState = getCheckpoint(checkpointName)
                if (checkpointState == null) {
                    return@withContext RollbackResult.Failure("Checkpoint '$checkpointName' not found")
                }
                
                migrationLogger.logPhaseStart("Restore from Checkpoint: $checkpointName")
                
                // Apply checkpoint state
                applyMigrationState(checkpointState)
                
                // Validate restoration
                val isValid = migrationValidator.validateDependencies()
                
                if (isValid) {
                    migrationLogger.logPhaseComplete("Restore from Checkpoint: $checkpointName")
                    RollbackResult.Success("Successfully restored from checkpoint: $checkpointName")
                } else {
                    RollbackResult.Failure("Checkpoint restoration validation failed")
                }
                
            } catch (e: Exception) {
                migrationLogger.logError("Checkpoint restoration failed", e)
                RollbackResult.Failure("Checkpoint restoration failed: ${e.message}")
            }
        }
    }
    
    /**
     * Gets rollback history and statistics
     */
    fun getRollbackHistory(): RollbackHistory {
        return RollbackHistory(
            rollbackCount = rollbackPrefs.getInt(KEY_ROLLBACK_COUNT, 0),
            lastRollbackReason = rollbackPrefs.getString(KEY_ROLLBACK_REASON, null),
            lastRollbackTimestamp = rollbackPrefs.getLong(KEY_ROLLBACK_TIMESTAMP, 0),
            hasLastSuccessfulState = getLastSuccessfulState() != null,
            availableCheckpoints = getAvailableCheckpoints()
        )
    }
    
    /**
     * Validates that rollback was successful
     */
    private suspend fun validateRollbackState(): RollbackValidationResult {
        return withContext(Dispatchers.IO) {
            val issues = mutableListOf<String>()
            
            try {
                // Verify rollback mode is active
                if (!featureFlags.isRollbackMode()) {
                    issues.add("Rollback mode is not active")
                }
                
                // Verify all Hilt flags are disabled
                val allFlags = featureFlags.getAllFlags()
                allFlags.forEach { (flag, enabled) ->
                    if (flag != MigrationFeatureFlags.FLAG_MIGRATION_ENABLED && 
                        flag != MigrationFeatureFlags.FLAG_ROLLBACK_MODE && enabled) {
                        issues.add("Flag $flag is still enabled after rollback")
                    }
                }
                
                // Validate that dependencies are accessible
                val dependenciesValid = migrationValidator.validateDependencies()
                if (!dependenciesValid) {
                    issues.add("Dependency validation failed")
                }
                
            } catch (e: Exception) {
                issues.add("Rollback validation error: ${e.message}")
            }
            
            RollbackValidationResult(
                isSuccessful = issues.isEmpty(),
                issues = issues
            )
        }
    }
    
    /**
     * Validates selective rollback for specific components
     */
    private suspend fun validateSelectiveRollback(components: List<String>): RollbackValidationResult {
        return withContext(Dispatchers.IO) {
            val issues = mutableListOf<String>()
            
            try {
                components.forEach { component ->
                    val flag = when (component) {
                        "ViewModels" -> MigrationFeatureFlags.FLAG_HILT_VIEWMODELS
                        "Activities" -> MigrationFeatureFlags.FLAG_HILT_ACTIVITIES
                        "Fragments" -> MigrationFeatureFlags.FLAG_HILT_FRAGMENTS
                        "Services" -> MigrationFeatureFlags.FLAG_HILT_SERVICES
                        "Workers" -> MigrationFeatureFlags.FLAG_HILT_WORKERS
                        "Repositories" -> MigrationFeatureFlags.FLAG_HILT_REPOSITORIES
                        else -> null
                    }
                    
                    if (flag != null && featureFlags.isHiltEnabled(flag)) {
                        issues.add("Component $component is still using Hilt after rollback")
                    }
                }
                
            } catch (e: Exception) {
                issues.add("Selective rollback validation error: ${e.message}")
            }
            
            RollbackValidationResult(
                isSuccessful = issues.isEmpty(),
                issues = issues
            )
        }
    }
    
    private fun saveCurrentStateAsRollback(reason: String) {
        val currentState = getCurrentMigrationState()
        rollbackPrefs.edit()
            .putString(KEY_PREVIOUS_STATE, currentState.toJson())
            .putString(KEY_ROLLBACK_REASON, reason)
            .putLong(KEY_ROLLBACK_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    
    private fun getCurrentMigrationState(): MigrationState {
        return MigrationState(
            isGlobalMigrationEnabled = featureFlags.isMigrationEnabled(),
            isRollbackMode = featureFlags.isRollbackMode(),
            componentFlags = featureFlags.getAllFlags()
        )
    }
    
    private fun applyMigrationState(state: MigrationState) {
        featureFlags.setMigrationEnabled(state.isGlobalMigrationEnabled)
        featureFlags.setRollbackMode(state.isRollbackMode)
        
        state.componentFlags.forEach { (flag, enabled) ->
            if (flag != MigrationFeatureFlags.FLAG_MIGRATION_ENABLED && 
                flag != MigrationFeatureFlags.FLAG_ROLLBACK_MODE) {
                featureFlags.setHiltEnabled(flag, enabled)
            }
        }
    }
    
    private fun getLastSuccessfulState(): MigrationState? {
        val stateJson = rollbackPrefs.getString(KEY_LAST_SUCCESSFUL_STATE, null)
        return stateJson?.let { MigrationState.fromJson(it) }
    }
    
    private fun saveCheckpoint(name: String, state: MigrationState) {
        rollbackPrefs.edit()
            .putString("checkpoint_$name", state.toJson())
            .putLong("checkpoint_${name}_timestamp", System.currentTimeMillis())
            .apply()
    }
    
    private fun getCheckpoint(name: String): MigrationState? {
        val stateJson = rollbackPrefs.getString("checkpoint_$name", null)
        return stateJson?.let { MigrationState.fromJson(it) }
    }
    
    private fun getAvailableCheckpoints(): List<String> {
        return rollbackPrefs.all.keys
            .filter { it.startsWith("checkpoint_") && !it.endsWith("_timestamp") }
            .map { it.removePrefix("checkpoint_") }
    }
    
    private fun incrementRollbackCount() {
        val currentCount = rollbackPrefs.getInt(KEY_ROLLBACK_COUNT, 0)
        rollbackPrefs.edit().putInt(KEY_ROLLBACK_COUNT, currentCount + 1).apply()
    }
    
    /**
     * Marks current state as successful for future rollback reference
     */
    fun markCurrentStateAsSuccessful() {
        val currentState = getCurrentMigrationState()
        rollbackPrefs.edit()
            .putString(KEY_LAST_SUCCESSFUL_STATE, currentState.toJson())
            .apply()
        migrationLogger.logValidationResult("Migration State", true, "Marked current state as successful")
    }
}

/**
 * Result of a rollback operation
 */
sealed class RollbackResult {
    data class Success(val message: String) : RollbackResult()
    data class Failure(val error: String) : RollbackResult()
}

/**
 * Rollback history and statistics
 */
data class RollbackHistory(
    val rollbackCount: Int,
    val lastRollbackReason: String?,
    val lastRollbackTimestamp: Long,
    val hasLastSuccessfulState: Boolean,
    val availableCheckpoints: List<String>
)

/**
 * Validation result for rollback operations
 */
data class RollbackValidationResult(
    val isSuccessful: Boolean,
    val issues: List<String>
)

/**
 * Represents a migration state that can be saved and restored
 */
data class MigrationState(
    val isGlobalMigrationEnabled: Boolean,
    val isRollbackMode: Boolean,
    val componentFlags: Map<String, Boolean>
) {
    fun toJson(): String {
        // Simple JSON serialization for SharedPreferences
        val flagsJson = componentFlags.entries.joinToString(",") { "\"${it.key}\":${it.value}" }
        return "{\"globalEnabled\":$isGlobalMigrationEnabled,\"rollbackMode\":$isRollbackMode,\"flags\":{$flagsJson}}"
    }
    
    companion object {
        fun fromJson(json: String): MigrationState? {
            return try {
                // Simple JSON parsing - in production, use a proper JSON library
                val globalEnabled = json.contains("\"globalEnabled\":true")
                val rollbackMode = json.contains("\"rollbackMode\":true")
                
                // Extract flags (simplified parsing)
                val flagsSection = json.substringAfter("\"flags\":{").substringBefore("}")
                val flags = mutableMapOf<String, Boolean>()
                
                if (flagsSection.isNotEmpty()) {
                    flagsSection.split(",").forEach { pair ->
                        val key = pair.substringAfter("\"").substringBefore("\"")
                        val value = pair.substringAfter(":").toBoolean()
                        flags[key] = value
                    }
                }
                
                MigrationState(globalEnabled, rollbackMode, flags)
            } catch (e: Exception) {
                Logs.error("MigrationState", "Failed to parse JSON: $json", e)
                null
            }
        }
    }
}