package io.github.gmathi.novellibrary.util.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler for Hilt dependency injection failures
 * Provides clear error messages and debugging information for DI issues
 */
@Singleton
class HiltErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltErrorHandler"
        
        // Common error patterns and their solutions
        private val ERROR_SOLUTIONS = mapOf(
            "MissingBinding" to "Check if the dependency is provided in a Hilt module with @Provides or @Binds",
            "CircularDependency" to "Review dependency graph for circular references and use @Lazy or Provider<T>",
            "WrongScope" to "Verify that component scopes match - use @Singleton for app-level dependencies",
            "MissingEntryPoint" to "Add @AndroidEntryPoint annotation to Activity, Fragment, or Service",
            "ModuleNotInstalled" to "Ensure module is annotated with @InstallIn and correct component",
            "QualifierMismatch" to "Check that @Named or custom qualifiers match between provider and injection site"
        )
    }
    
    /**
     * Handle dependency injection failures with detailed error information
     */
    fun handleDependencyInjectionFailure(
        componentName: String,
        dependencyType: String,
        error: Throwable,
        injectionSite: String? = null
    ) {
        val errorType = categorizeError(error)
        val solution = ERROR_SOLUTIONS[errorType] ?: "Check Hilt configuration and module setup"
        
        val errorMessage = buildString {
            appendLine("=== HILT DEPENDENCY INJECTION FAILURE ===")
            appendLine("Component: $componentName")
            appendLine("Dependency: $dependencyType")
            appendLine("Error Type: $errorType")
            if (injectionSite != null) {
                appendLine("Injection Site: $injectionSite")
            }
            appendLine("Original Error: ${error.message}")
            appendLine("Suggested Solution: $solution")
            appendLine("Stack Trace:")
            appendLine(error.stackTraceToString())
            appendLine("==========================================")
        }
        
        Logs.error(TAG, errorMessage)
        
        // Log to crash reporting if available
        logToCrashReporting(componentName, dependencyType, errorType, error)
    }
    
    /**
     * Handle missing binding errors with specific guidance
     */
    fun handleMissingBinding(
        dependencyClass: Class<*>,
        injectionSite: String,
        availableBindings: List<String> = emptyList()
    ) {
        val errorMessage = buildString {
            appendLine("=== MISSING HILT BINDING ===")
            appendLine("Missing Dependency: ${dependencyClass.simpleName}")
            appendLine("Injection Site: $injectionSite")
            appendLine("Required Actions:")
            appendLine("1. Create a @Module with @InstallIn annotation")
            appendLine("2. Add @Provides method for ${dependencyClass.simpleName}")
            appendLine("3. Ensure module is in correct component scope")
            
            if (availableBindings.isNotEmpty()) {
                appendLine("Available Bindings:")
                availableBindings.forEach { binding ->
                    appendLine("  - $binding")
                }
            }
            
            appendLine("Example Module:")
            appendLine(generateExampleModule(dependencyClass))
            appendLine("===========================")
        }
        
        Logs.error(TAG, errorMessage)
    }
    
    /**
     * Handle circular dependency errors with resolution guidance
     */
    fun handleCircularDependency(
        dependencyChain: List<String>,
        error: Throwable
    ) {
        val errorMessage = buildString {
            appendLine("=== CIRCULAR DEPENDENCY DETECTED ===")
            appendLine("Dependency Chain:")
            dependencyChain.forEachIndexed { index, dependency ->
                appendLine("  ${index + 1}. $dependency")
                if (index < dependencyChain.size - 1) {
                    appendLine("     ↓ depends on")
                }
            }
            appendLine("Resolution Options:")
            appendLine("1. Use @Lazy<T> for one of the dependencies")
            appendLine("2. Use Provider<T> to break the cycle")
            appendLine("3. Refactor to remove circular dependency")
            appendLine("4. Use @Singleton scope if appropriate")
            appendLine("Original Error: ${error.message}")
            appendLine("===================================")
        }
        
        Logs.error(TAG, errorMessage)
    }
    
    /**
     * Validate component setup and provide diagnostic information
     */
    fun validateComponentSetup(componentClass: Class<*>): ComponentValidationResult {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check for @AndroidEntryPoint annotation
        if (!componentClass.isAnnotationPresent(dagger.hilt.android.AndroidEntryPoint::class.java)) {
            issues.add("Missing @AndroidEntryPoint annotation on ${componentClass.simpleName}")
        }
        
        // Check for proper inheritance
        when {
            android.app.Activity::class.java.isAssignableFrom(componentClass) -> {
                if (!androidx.appcompat.app.AppCompatActivity::class.java.isAssignableFrom(componentClass)) {
                    warnings.add("Consider extending AppCompatActivity for better compatibility")
                }
            }
            androidx.fragment.app.Fragment::class.java.isAssignableFrom(componentClass) -> {
                // Fragment-specific validations
            }
            android.app.Service::class.java.isAssignableFrom(componentClass) -> {
                // Service-specific validations
            }
        }
        
        return ComponentValidationResult(
            componentName = componentClass.simpleName,
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
    }
    
    private fun categorizeError(error: Throwable): String {
        val message = error.message?.lowercase() ?: ""
        val stackTrace = error.stackTraceToString().lowercase()
        
        return when {
            message.contains("missing binding") || stackTrace.contains("missing binding") -> "MissingBinding"
            message.contains("circular") || stackTrace.contains("circular") -> "CircularDependency"
            message.contains("scope") || stackTrace.contains("scope") -> "WrongScope"
            message.contains("entrypoint") || stackTrace.contains("entrypoint") -> "MissingEntryPoint"
            message.contains("module") || stackTrace.contains("module") -> "ModuleNotInstalled"
            message.contains("qualifier") || stackTrace.contains("qualifier") -> "QualifierMismatch"
            else -> "UnknownError"
        }
    }
    
    private fun generateExampleModule(dependencyClass: Class<*>): String {
        val className = dependencyClass.simpleName
        return """
        @Module
        @InstallIn(SingletonComponent::class)
        object ${className}Module {
            
            @Provides
            @Singleton
            fun provide$className(
                @ApplicationContext context: Context
                // Add other dependencies as needed
            ): $className {
                return $className(context)
            }
        }
        """.trimIndent()
    }
    
    private fun logToCrashReporting(
        componentName: String,
        dependencyType: String,
        errorType: String,
        error: Throwable
    ) {
        try {
            // Log to Firebase Crashlytics or other crash reporting service
            // This would be implemented based on the app's crash reporting setup
            Logs.debug(TAG, "Logging DI error to crash reporting: $componentName -> $dependencyType ($errorType)")
        } catch (e: Exception) {
            Logs.error(TAG, "Failed to log to crash reporting", e)
        }
    }
}

/**
 * Result of component validation
 */
data class ComponentValidationResult(
    val componentName: String,
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>
) {
    fun hasIssues(): Boolean = issues.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()
    
    fun getFormattedReport(): String = buildString {
        appendLine("=== COMPONENT VALIDATION: $componentName ===")
        appendLine("Status: ${if (isValid) "VALID" else "INVALID"}")
        
        if (issues.isNotEmpty()) {
            appendLine("Issues:")
            issues.forEach { issue ->
                appendLine("  ❌ $issue")
            }
        }
        
        if (warnings.isNotEmpty()) {
            appendLine("Warnings:")
            warnings.forEach { warning ->
                appendLine("  ⚠️ $warning")
            }
        }
        
        appendLine("=======================================")
    }
}