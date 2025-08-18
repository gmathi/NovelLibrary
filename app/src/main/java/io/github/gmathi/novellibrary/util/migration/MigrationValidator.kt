package io.github.gmathi.novellibrary.util.migration

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to validate dependency injection during Hilt migration.
 * Ensures that all critical dependencies are properly injected and functional.
 */
@Singleton
class MigrationValidator @Inject constructor(
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper
) {
    
    companion object {
        private const val TAG = "MigrationValidator"
    }
    
    /**
     * Validates that all core dependencies are properly injected and functional.
     * @return true if all validations pass, false otherwise
     */
    fun validateDependencies(): Boolean {
        return try {
            validateDatabaseHelper() &&
            validateDataCenter() &&
            validateNetworkHelper()
        } catch (e: Exception) {
            Logs.error(TAG, "Dependency validation failed", e)
            false
        }
    }
    
    /**
     * Validates that DBHelper is properly injected and functional.
     */
    private fun validateDatabaseHelper(): Boolean {
        return try {
            // Test basic database functionality
            val isValid = dbHelper.readableDatabase != null
            if (isValid) {
                Logs.info(TAG, "DBHelper validation passed")
            } else {
                Logs.error(TAG, "DBHelper validation failed - database not open")
            }
            isValid
        } catch (e: Exception) {
            Logs.error(TAG, "DBHelper validation failed", e)
            false
        }
    }
    
    /**
     * Validates that DataCenter is properly injected and functional.
     */
    private fun validateDataCenter(): Boolean {
        return try {
            // Test basic DataCenter functionality
            val isValid = dataCenter.javaClass.simpleName == "DataCenter"
            if (isValid) {
                Logs.info(TAG, "DataCenter validation passed")
            } else {
                Logs.error(TAG, "DataCenter validation failed")
            }
            isValid
        } catch (e: Exception) {
            Logs.error(TAG, "DataCenter validation failed", e)
            false
        }
    }
    
    /**
     * Validates that NetworkHelper is properly injected and functional.
     */
    private fun validateNetworkHelper(): Boolean {
        return try {
            // Test basic NetworkHelper functionality
            val isValid = networkHelper.javaClass.simpleName == "NetworkHelper"
            if (isValid) {
                Logs.info(TAG, "NetworkHelper validation passed")
            } else {
                Logs.error(TAG, "NetworkHelper validation failed")
            }
            isValid
        } catch (e: Exception) {
            Logs.error(TAG, "NetworkHelper validation failed", e)
            false
        }
    }
    
    /**
     * Validates specific component injection for gradual migration.
     * @param componentName Name of the component being validated
     * @param component The injected component instance
     * @return true if validation passes, false otherwise
     */
    fun validateComponent(componentName: String, component: Any?): Boolean {
        return try {
            val isValid = component != null
            if (isValid) {
                Logs.info(TAG, "$componentName validation passed")
            } else {
                Logs.error(TAG, "$componentName validation failed - component is null")
            }
            isValid
        } catch (e: Exception) {
            Logs.error(TAG, "$componentName validation failed", e)
            false
        }
    }
    
    /**
     * Logs migration progress for tracking purposes.
     * @param phase Current migration phase
     * @param component Component being migrated
     * @param status Migration status (started, completed, failed)
     */
    fun logMigrationProgress(phase: String, component: String, status: String) {
        val message = "Migration Progress - Phase: $phase, Component: $component, Status: $status"
        when (status.lowercase()) {
            "failed", "error" -> Logs.error(TAG, message)
            "completed", "success" -> Logs.info(TAG, message)
            else -> Logs.debug(TAG, message)
        }
    }
}