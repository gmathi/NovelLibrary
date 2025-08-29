package io.github.gmathi.novellibrary.util.security

import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates security aspects of Hilt dependency injection
 * Ensures proper security practices in DI configuration
 */
@Singleton
class HiltSecurityValidator @Inject constructor(
    private val dependencyValidator: DependencySecurityValidator
) {
    
    companion object {
        private const val TAG = "HiltSecurityValidator"
    }
    
    /**
     * Validates that all critical dependencies are properly secured
     */
    fun validateCriticalDependencies(
        dbHelper: Any?,
        networkHelper: Any?,
        sourceManager: Any?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        try {
            dependencyValidator.validateDependency(dbHelper, "DBHelper")
        } catch (e: Exception) {
            errors.add("DBHelper validation failed: ${e.message}")
        }
        
        try {
            dependencyValidator.validateDependency(networkHelper, "NetworkHelper")
        } catch (e: Exception) {
            errors.add("NetworkHelper validation failed: ${e.message}")
        }
        
        try {
            dependencyValidator.validateDependency(sourceManager, "SourceManager")
        } catch (e: Exception) {
            errors.add("SourceManager validation failed: ${e.message}")
        }
        
        return if (errors.isEmpty()) {
            Logs.info(TAG, "All critical dependencies validated successfully")
            ValidationResult.Success
        } else {
            Logs.error(TAG, "Critical dependency validation failed: ${errors.joinToString(", ")}")
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates scoping security to prevent memory leaks
     */
    fun validateScopingSecurity(
        componentName: String,
        expectedScope: String,
        actualScope: String
    ): Boolean {
        val isValid = expectedScope == actualScope
        
        if (!isValid) {
            Logs.warning(TAG, "Scoping security violation: $componentName expected $expectedScope but got $actualScope")
        } else {
            Logs.debug(TAG, "Scoping validation passed for $componentName with scope $actualScope")
        }
        
        return isValid
    }
    
    /**
     * Validates that singleton components are properly secured
     */
    fun validateSingletonSecurity(singletonComponents: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        singletonComponents.forEach { (name, component) ->
            try {
                dependencyValidator.validateDependency(component, name)
                
                // Additional singleton-specific validations
                if (name.contains("Network", ignoreCase = true)) {
                    dependencyValidator.validateNetworkConfiguration()
                }
                
                if (name.contains("Database", ignoreCase = true)) {
                    dependencyValidator.validateDatabaseSecurity()
                }
                
            } catch (e: Exception) {
                errors.add("Singleton $name validation failed: ${e.message}")
            }
        }
        
        return if (errors.isEmpty()) {
            Logs.info(TAG, "All singleton components validated successfully")
            ValidationResult.Success
        } else {
            Logs.error(TAG, "Singleton validation failed: ${errors.joinToString(", ")}")
            ValidationResult.Failure(errors)
        }
    }
    
    /**
     * Validates injection security at runtime
     */
    fun validateInjectionSecurity(
        componentName: String,
        injectionContext: String
    ): Boolean {
        return try {
            // Validate injection context is appropriate
            require(injectionContext in listOf("Activity", "Fragment", "Service", "ViewModel")) {
                "Invalid injection context: $injectionContext"
            }
            
            dependencyValidator.validateInjectionTiming(componentName, true)
            
            Logs.debug(TAG, "Injection security validation passed for $componentName in $injectionContext")
            true
        } catch (e: Exception) {
            Logs.error(TAG, "Injection security validation failed for $componentName: ${e.message}")
            false
        }
    }
}

/**
 * Result of security validation
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}