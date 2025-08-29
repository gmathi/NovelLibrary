package io.github.gmathi.novellibrary.util.migration

import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized logging utility for tracking Hilt migration progress.
 * Provides structured logging for migration events and debugging.
 */
@Singleton
class MigrationLogger @Inject constructor() {
    
    companion object {
        private const val TAG = "HiltMigration"
        private const val SEPARATOR = "=================================================="
    }
    
    /**
     * Logs the start of a migration phase.
     */
    fun logPhaseStart(phase: String) {
        Logs.info(TAG, SEPARATOR)
        Logs.info(TAG, "Starting Migration Phase: $phase")
        Logs.info(TAG, SEPARATOR)
    }
    
    /**
     * Logs the completion of a migration phase.
     */
    fun logPhaseComplete(phase: String) {
        Logs.info(TAG, "Migration Phase Completed: $phase")
        Logs.info(TAG, SEPARATOR)
    }
    
    /**
     * Logs component migration events.
     */
    fun logComponentMigration(componentName: String, fromFramework: String, toFramework: String, status: String) {
        val message = "Component Migration - $componentName: $fromFramework -> $toFramework ($status)"
        when (status.lowercase()) {
            "failed", "error" -> Logs.error(TAG, message)
            "completed", "success" -> Logs.info(TAG, message)
            "started", "in_progress" -> Logs.debug(TAG, message)
            else -> Logs.debug(TAG, message)
        }
    }
    
    /**
     * Logs dependency injection validation results.
     */
    fun logValidationResult(componentName: String, isValid: Boolean, details: String? = null) {
        val status = if (isValid) "PASSED" else "FAILED"
        val message = "Validation $status for $componentName" + (details?.let { " - $it" } ?: "")
        
        if (isValid) {
            Logs.info(TAG, message)
        } else {
            Logs.error(TAG, message)
        }
    }
    
    /**
     * Logs fallback mechanism activation.
     */
    fun logFallbackActivation(componentName: String, reason: String) {
        Logs.warning(TAG, "Fallback activated for $componentName: $reason")
    }
    
    /**
     * Logs performance metrics during migration.
     */
    fun logPerformanceMetric(operation: String, durationMs: Long) {
        Logs.debug(TAG, "Performance - $operation: ${durationMs}ms")
    }
    
    /**
     * Logs error details with context for debugging.
     */
    fun logError(context: String, error: Throwable) {
        Logs.error(TAG, "Migration Error in $context", error)
    }
    
    /**
     * Logs migration summary statistics.
     */
    fun logMigrationSummary(
        totalComponents: Int,
        migratedComponents: Int,
        failedComponents: Int,
        durationMs: Long
    ) {
        Logs.info(TAG, SEPARATOR)
        Logs.info(TAG, "Migration Summary:")
        Logs.info(TAG, "  Total Components: $totalComponents")
        Logs.info(TAG, "  Successfully Migrated: $migratedComponents")
        Logs.info(TAG, "  Failed: $failedComponents")
        Logs.info(TAG, "  Success Rate: ${(migratedComponents * 100) / totalComponents}%")
        Logs.info(TAG, "  Total Duration: ${durationMs}ms")
        Logs.info(TAG, SEPARATOR)
    }
    
    /**
     * Log dependency injection source (Hilt vs Injekt)
     */
    fun logDependencySource(dependency: String, source: String) {
        Logs.debug(TAG, "Dependency $dependency resolved from $source")
    }
    
    /**
     * Log fallback to Hilt when Injekt fails
     */
    fun logFallbackToHilt(dependency: String, error: Exception) {
        Logs.warning(TAG, "Falling back to Hilt for $dependency due to Injekt failure: ${error.message}")
    }
    
    /**
     * Log feature flag changes
     */
    fun logFlagChange(flag: String, enabled: Boolean) {
        Logs.info(TAG, "Feature flag changed: $flag = $enabled")
    }
    
    /**
     * Log global migration state changes
     */
    fun logGlobalMigrationChange(enabled: Boolean) {
        Logs.info(TAG, "Global migration ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Log rollback mode changes
     */
    fun logRollbackModeChange(enabled: Boolean) {
        Logs.warning(TAG, "Rollback mode ${if (enabled) "ACTIVATED" else "deactivated"}")
    }
    
    /**
     * Log when flags are reset to defaults
     */
    fun logFlagsReset() {
        Logs.info(TAG, "All feature flags reset to default values")
    }
    
    /**
     * Log progressive rollout activation
     */
    fun logProgressiveRollout() {
        Logs.info(TAG, "Progressive rollout activated - all Hilt components enabled")
    }
    
    /**
     * Log emergency rollback
     */
    fun logEmergencyRollback() {
        Logs.error(TAG, "EMERGENCY ROLLBACK ACTIVATED - All components reverted to Injekt")
    }
}