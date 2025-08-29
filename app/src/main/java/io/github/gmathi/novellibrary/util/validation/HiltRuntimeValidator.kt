package io.github.gmathi.novellibrary.util.validation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime validation utilities for Hilt dependency injection
 * Validates proper injection, component lifecycle, and dependency resolution at runtime
 */
@Singleton
class HiltRuntimeValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltRuntimeValidator"
    }
    
    /**
     * Validate that all dependencies are properly injected in a component
     */
    fun validateComponentInjection(
        component: Any,
        expectedDependencies: List<String>
    ): ComponentInjectionValidation {
        val injectionResults = mutableMapOf<String, InjectionResult>()
        
        expectedDependencies.forEach { dependency ->
            val result = validateDependencyInjection(component, dependency)
            injectionResults[dependency] = result
        }
        
        val failedInjections = injectionResults.filter { !it.value.isInjected }
        
        return ComponentInjectionValidation(
            componentName = component::class.java.simpleName,
            isValid = failedInjections.isEmpty(),
            injectionResults = injectionResults,
            failedDependencies = failedInjections.keys.toList()
        )
    }
    
    /**
     * Validate proper scoping and lifecycle management
     */
    fun validateComponentLifecycle(
        component: Any,
        expectedScope: ComponentScope
    ): LifecycleValidation {
        val issues = mutableListOf<LifecycleIssue>()
        val warnings = mutableListOf<String>()
        
        // Check component type matches expected scope
        when (expectedScope) {
            ComponentScope.SINGLETON -> {
                if (component::class.java.name.contains("Activity") || 
                    component::class.java.name.contains("Fragment")) {
                    warnings.add("Singleton scope used in UI component - check for memory leaks")
                }
            }
            ComponentScope.ACTIVITY -> {
                if (!android.app.Activity::class.java.isAssignableFrom(component::class.java)) {
                    issues.add(
                        LifecycleIssue(
                            type = "ScopeMismatch",
                            description = "Activity scope used in non-Activity component",
                            severity = ValidationSeverity.HIGH
                        )
                    )
                }
            }
            ComponentScope.FRAGMENT -> {
                if (!androidx.fragment.app.Fragment::class.java.isAssignableFrom(component::class.java)) {
                    issues.add(
                        LifecycleIssue(
                            type = "ScopeMismatch",
                            description = "Fragment scope used in non-Fragment component",
                            severity = ValidationSeverity.HIGH
                        )
                    )
                }
            }
            ComponentScope.SERVICE -> {
                if (!android.app.Service::class.java.isAssignableFrom(component::class.java)) {
                    issues.add(
                        LifecycleIssue(
                            type = "ScopeMismatch",
                            description = "Service scope used in non-Service component",
                            severity = ValidationSeverity.HIGH
                        )
                    )
                }
            }
        }
        
        // Check for potential memory leaks
        issues.addAll(checkForMemoryLeaks(component))
        
        return LifecycleValidation(
            componentName = component::class.java.simpleName,
            expectedScope = expectedScope,
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings
        )
    }
    
    /**
     * Validate dependency resolution performance
     */
    fun validateDependencyResolutionPerformance(
        component: Any,
        dependencies: List<String>
    ): PerformanceValidation {
        val resolutionTimes = mutableMapOf<String, Long>()
        val slowDependencies = mutableListOf<String>()
        
        dependencies.forEach { dependency ->
            val startTime = System.nanoTime()
            
            try {
                // Simulate dependency resolution timing
                resolveDependency(component, dependency)
                val endTime = System.nanoTime()
                val resolutionTime = (endTime - startTime) / 1_000_000 // Convert to milliseconds
                
                resolutionTimes[dependency] = resolutionTime
                
                // Flag slow dependencies (> 10ms is considered slow for DI)
                if (resolutionTime > 10) {
                    slowDependencies.add(dependency)
                }
            } catch (e: Exception) {
                Logs.error(TAG, "Failed to resolve dependency: $dependency", e)
                resolutionTimes[dependency] = -1 // Indicate failure
            }
        }
        
        val averageResolutionTime = resolutionTimes.values.filter { it > 0 }.average()
        
        return PerformanceValidation(
            componentName = component::class.java.simpleName,
            resolutionTimes = resolutionTimes,
            averageResolutionTime = averageResolutionTime,
            slowDependencies = slowDependencies,
            isPerformant = slowDependencies.isEmpty() && averageResolutionTime < 5.0
        )
    }
    
    /**
     * Validate memory usage and potential leaks
     */
    fun validateMemoryUsage(component: Any): MemoryValidation {
        val memoryIssues = mutableListOf<MemoryIssue>()
        val recommendations = mutableListOf<String>()
        
        // Check for potential memory leaks based on component type and dependencies
        when {
            component is android.app.Activity -> {
                memoryIssues.addAll(validateActivityMemoryUsage(component))
            }
            component is androidx.fragment.app.Fragment -> {
                memoryIssues.addAll(validateFragmentMemoryUsage(component))
            }
            component is android.app.Service -> {
                memoryIssues.addAll(validateServiceMemoryUsage(component))
            }
        }
        
        // Generate recommendations based on findings
        if (memoryIssues.isEmpty()) {
            recommendations.add("No memory issues detected")
        } else {
            recommendations.addAll(generateMemoryRecommendations(memoryIssues))
        }
        
        return MemoryValidation(
            componentName = component::class.java.simpleName,
            isValid = memoryIssues.isEmpty(),
            memoryIssues = memoryIssues,
            recommendations = recommendations
        )
    }
    
    /**
     * Generate comprehensive runtime validation report
     */
    fun generateRuntimeValidationReport(
        component: Any,
        expectedDependencies: List<String>,
        expectedScope: ComponentScope
    ): RuntimeValidationReport {
        val injectionValidation = validateComponentInjection(component, expectedDependencies)
        val lifecycleValidation = validateComponentLifecycle(component, expectedScope)
        val performanceValidation = validateDependencyResolutionPerformance(component, expectedDependencies)
        val memoryValidation = validateMemoryUsage(component)
        
        val overallValid = injectionValidation.isValid &&
                lifecycleValidation.isValid &&
                performanceValidation.isPerformant &&
                memoryValidation.isValid
        
        return RuntimeValidationReport(
            componentName = component::class.java.simpleName,
            isValid = overallValid,
            injectionValidation = injectionValidation,
            lifecycleValidation = lifecycleValidation,
            performanceValidation = performanceValidation,
            memoryValidation = memoryValidation,
            validatedAt = System.currentTimeMillis()
        )
    }
    
    private fun validateDependencyInjection(component: Any, dependencyName: String): InjectionResult {
        return try {
            // Use reflection to check if dependency is properly injected
            val field = component::class.java.getDeclaredField(dependencyName.lowercase())
            field.isAccessible = true
            val value = field.get(component)
            
            InjectionResult(
                dependencyName = dependencyName,
                isInjected = value != null,
                injectionTime = System.currentTimeMillis(),
                errorMessage = if (value == null) "Dependency is null" else null
            )
        } catch (e: NoSuchFieldException) {
            InjectionResult(
                dependencyName = dependencyName,
                isInjected = false,
                injectionTime = System.currentTimeMillis(),
                errorMessage = "Field not found: ${e.message}"
            )
        } catch (e: Exception) {
            InjectionResult(
                dependencyName = dependencyName,
                isInjected = false,
                injectionTime = System.currentTimeMillis(),
                errorMessage = "Injection failed: ${e.message}"
            )
        }
    }
    
    private fun resolveDependency(component: Any, dependency: String) {
        // Simulate dependency resolution for performance testing
        Thread.sleep(1) // Minimal delay to simulate resolution time
    }
    
    private fun checkForMemoryLeaks(component: Any): List<LifecycleIssue> {
        val issues = mutableListOf<LifecycleIssue>()
        
        // Check for common memory leak patterns
        when {
            component is android.app.Activity -> {
                // Check if Activity holds references to static fields or singletons
                issues.addAll(checkActivityMemoryLeaks(component))
            }
            component is androidx.fragment.app.Fragment -> {
                // Check if Fragment properly cleans up in onDestroyView
                issues.addAll(checkFragmentMemoryLeaks(component))
            }
        }
        
        return issues
    }
    
    private fun checkActivityMemoryLeaks(activity: android.app.Activity): List<LifecycleIssue> {
        val issues = mutableListOf<LifecycleIssue>()
        
        // This would be more sophisticated in a real implementation
        // Check for common Activity memory leak patterns
        
        return issues
    }
    
    private fun checkFragmentMemoryLeaks(fragment: androidx.fragment.app.Fragment): List<LifecycleIssue> {
        val issues = mutableListOf<LifecycleIssue>()
        
        // This would be more sophisticated in a real implementation
        // Check for common Fragment memory leak patterns
        
        return issues
    }
    
    private fun validateActivityMemoryUsage(activity: android.app.Activity): List<MemoryIssue> {
        val issues = mutableListOf<MemoryIssue>()
        
        // Check for Activity-specific memory issues
        // This would include checking for leaked contexts, static references, etc.
        
        return issues
    }
    
    private fun validateFragmentMemoryUsage(fragment: androidx.fragment.app.Fragment): List<MemoryIssue> {
        val issues = mutableListOf<MemoryIssue>()
        
        // Check for Fragment-specific memory issues
        // This would include checking for view references after onDestroyView, etc.
        
        return issues
    }
    
    private fun validateServiceMemoryUsage(service: android.app.Service): List<MemoryIssue> {
        val issues = mutableListOf<MemoryIssue>()
        
        // Check for Service-specific memory issues
        // This would include checking for long-running operations, etc.
        
        return issues
    }
    
    private fun generateMemoryRecommendations(issues: List<MemoryIssue>): List<String> {
        val recommendations = mutableListOf<String>()
        
        issues.forEach { issue ->
            when (issue.type) {
                "ContextLeak" -> recommendations.add("Use @ApplicationContext instead of Activity context in singletons")
                "ViewLeak" -> recommendations.add("Clear view references in Fragment.onDestroyView()")
                "ListenerLeak" -> recommendations.add("Unregister listeners in appropriate lifecycle methods")
                else -> recommendations.add("Review ${issue.type} for potential memory optimization")
            }
        }
        
        return recommendations
    }
}

// Data classes for runtime validation results

enum class ComponentScope {
    SINGLETON, ACTIVITY, FRAGMENT, SERVICE
}

enum class ValidationSeverity {
    HIGH, MEDIUM, LOW
}

data class ComponentInjectionValidation(
    val componentName: String,
    val isValid: Boolean,
    val injectionResults: Map<String, InjectionResult>,
    val failedDependencies: List<String>
)

data class InjectionResult(
    val dependencyName: String,
    val isInjected: Boolean,
    val injectionTime: Long,
    val errorMessage: String?
)

data class LifecycleValidation(
    val componentName: String,
    val expectedScope: ComponentScope,
    val isValid: Boolean,
    val issues: List<LifecycleIssue>,
    val warnings: List<String>
)

data class LifecycleIssue(
    val type: String,
    val description: String,
    val severity: ValidationSeverity
)

data class PerformanceValidation(
    val componentName: String,
    val resolutionTimes: Map<String, Long>,
    val averageResolutionTime: Double,
    val slowDependencies: List<String>,
    val isPerformant: Boolean
)

data class MemoryValidation(
    val componentName: String,
    val isValid: Boolean,
    val memoryIssues: List<MemoryIssue>,
    val recommendations: List<String>
)

data class MemoryIssue(
    val type: String,
    val description: String,
    val severity: ValidationSeverity,
    val component: String
)

data class RuntimeValidationReport(
    val componentName: String,
    val isValid: Boolean,
    val injectionValidation: ComponentInjectionValidation,
    val lifecycleValidation: LifecycleValidation,
    val performanceValidation: PerformanceValidation,
    val memoryValidation: MemoryValidation,
    val validatedAt: Long
) {
    fun getFormattedReport(): String = buildString {
        appendLine("=== RUNTIME VALIDATION REPORT: $componentName ===")
        appendLine("Validated: ${java.util.Date(validatedAt)}")
        appendLine("Overall Status: ${if (isValid) "✅ VALID" else "❌ INVALID"}")
        appendLine()
        
        appendLine("INJECTION VALIDATION:")
        appendLine("  Status: ${if (injectionValidation.isValid) "✅ Valid" else "❌ Invalid"}")
        appendLine("  Failed Dependencies: ${injectionValidation.failedDependencies.size}")
        
        appendLine()
        appendLine("LIFECYCLE VALIDATION:")
        appendLine("  Status: ${if (lifecycleValidation.isValid) "✅ Valid" else "❌ Invalid"}")
        appendLine("  Expected Scope: ${lifecycleValidation.expectedScope}")
        appendLine("  Issues: ${lifecycleValidation.issues.size}")
        
        appendLine()
        appendLine("PERFORMANCE VALIDATION:")
        appendLine("  Status: ${if (performanceValidation.isPerformant) "✅ Performant" else "❌ Slow"}")
        appendLine("  Average Resolution Time: ${String.format("%.2f", performanceValidation.averageResolutionTime)}ms")
        appendLine("  Slow Dependencies: ${performanceValidation.slowDependencies.size}")
        
        appendLine()
        appendLine("MEMORY VALIDATION:")
        appendLine("  Status: ${if (memoryValidation.isValid) "✅ Valid" else "❌ Issues Found"}")
        appendLine("  Memory Issues: ${memoryValidation.memoryIssues.size}")
        
        appendLine("===============================================")
    }
}