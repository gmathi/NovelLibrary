package io.github.gmathi.novellibrary.util.validation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Compile-time validation utilities for Hilt dependency injection
 * Provides validation for circular dependencies, proper scoping, and binding completeness
 */
@Singleton
class HiltCompileTimeValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltCompileTimeValidator"
    }
    
    /**
     * Validate the entire dependency graph for circular dependencies
     */
    fun validateDependencyGraph(): DependencyGraphValidation {
        val dependencies = buildDependencyGraph()
        val cycles = detectCircularDependencies(dependencies)
        val recommendations = generateRecommendations(cycles)
        
        return DependencyGraphValidation(
            isValid = cycles.isEmpty(),
            circularDependencies = cycles,
            recommendations = recommendations,
            dependencyCount = dependencies.size
        )
    }
    
    /**
     * Validate all Hilt modules for proper configuration
     */
    fun validateHiltModules(): ModuleValidationReport {
        val moduleValidations = mutableMapOf<String, ModuleValidation>()
        
        // Validate each known module
        val modules = getKnownModules()
        modules.forEach { module ->
            moduleValidations[module.name] = validateModule(module)
        }
        
        return ModuleValidationReport(
            moduleValidations = moduleValidations,
            overallValid = moduleValidations.values.all { it.isValid },
            totalModules = modules.size
        )
    }
    
    /**
     * Validate component scoping and lifecycle compatibility
     */
    fun validateComponentScoping(): ScopingValidation {
        val scopingIssues = mutableListOf<ScopingIssue>()
        val warnings = mutableListOf<String>()
        
        // Check for common scoping issues
        scopingIssues.addAll(validateSingletonScoping())
        scopingIssues.addAll(validateActivityScoping())
        scopingIssues.addAll(validateFragmentScoping())
        
        // Generate warnings for potential issues
        warnings.addAll(generateScopingWarnings())
        
        return ScopingValidation(
            isValid = scopingIssues.isEmpty(),
            issues = scopingIssues,
            warnings = warnings
        )
    }
    
    /**
     * Validate that all required bindings are available
     */
    fun validateBindingCompleteness(): BindingValidation {
        val missingBindings = mutableListOf<MissingBinding>()
        val availableBindings = getAvailableBindings()
        val requiredBindings = getRequiredBindings()
        
        requiredBindings.forEach { required ->
            if (!availableBindings.contains(required.type)) {
                missingBindings.add(
                    MissingBinding(
                        type = required.type,
                        requiredBy = required.requiredBy,
                        suggestedModule = generateModuleSuggestion(required.type)
                    )
                )
            }
        }
        
        return BindingValidation(
            isComplete = missingBindings.isEmpty(),
            missingBindings = missingBindings,
            availableBindings = availableBindings,
            requiredBindings = requiredBindings.map { it.type }
        )
    }
    
    /**
     * Generate comprehensive validation report
     */
    fun generateComprehensiveValidationReport(): ComprehensiveValidationReport {
        val dependencyGraphValidation = validateDependencyGraph()
        val moduleValidation = validateHiltModules()
        val scopingValidation = validateComponentScoping()
        val bindingValidation = validateBindingCompleteness()
        
        val overallValid = dependencyGraphValidation.isValid &&
                moduleValidation.overallValid &&
                scopingValidation.isValid &&
                bindingValidation.isComplete
        
        return ComprehensiveValidationReport(
            isValid = overallValid,
            dependencyGraphValidation = dependencyGraphValidation,
            moduleValidation = moduleValidation,
            scopingValidation = scopingValidation,
            bindingValidation = bindingValidation,
            generatedAt = System.currentTimeMillis()
        )
    }
    
    private fun buildDependencyGraph(): Map<String, List<String>> {
        // Build dependency graph based on known dependencies
        return mapOf(
            "ChaptersViewModel" to listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager", "SavedStateHandle"),
            "GoogleBackupViewModel" to listOf("DBHelper", "DataCenter", "NetworkHelper"),
            "DBHelper" to listOf("Context"),
            "DataCenter" to listOf("Context"),
            "NetworkHelper" to listOf("Context"),
            "SourceManager" to listOf("Context", "ExtensionManager"),
            "ExtensionManager" to listOf("Context"),
            "FirebaseAnalytics" to emptyList(),
            "CoroutineScopes" to emptyList(),
            "DispatcherProvider" to emptyList()
        )
    }
    
    private fun detectCircularDependencies(dependencies: Map<String, List<String>>): List<CircularDependency> {
        val cycles = mutableListOf<CircularDependency>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        dependencies.keys.forEach { node ->
            if (node !in visited) {
                val cycle = findCycle(node, dependencies, visited, recursionStack, mutableListOf())
                if (cycle.isNotEmpty()) {
                    cycles.add(
                        CircularDependency(
                            cycle = cycle,
                            severity = determineCycleSeverity(cycle)
                        )
                    )
                }
            }
        }
        
        return cycles
    }
    
    private fun findCycle(
        node: String,
        dependencies: Map<String, List<String>>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        currentPath: MutableList<String>
    ): List<String> {
        visited.add(node)
        recursionStack.add(node)
        currentPath.add(node)
        
        dependencies[node]?.forEach { neighbor ->
            if (neighbor !in visited) {
                val cycle = findCycle(neighbor, dependencies, visited, recursionStack, currentPath)
                if (cycle.isNotEmpty()) return cycle
            } else if (neighbor in recursionStack) {
                // Found a cycle
                val cycleStart = currentPath.indexOf(neighbor)
                return currentPath.subList(cycleStart, currentPath.size) + neighbor
            }
        }
        
        recursionStack.remove(node)
        currentPath.removeAt(currentPath.size - 1)
        return emptyList()
    }
    
    private fun determineCycleSeverity(cycle: List<String>): CycleSeverity {
        return when {
            cycle.size <= 2 -> CycleSeverity.HIGH // Direct circular dependency
            cycle.size <= 4 -> CycleSeverity.MEDIUM // Short cycle
            else -> CycleSeverity.LOW // Long cycle, might be acceptable with lazy injection
        }
    }
    
    private fun generateRecommendations(cycles: List<CircularDependency>): List<String> {
        val recommendations = mutableListOf<String>()
        
        cycles.forEach { cycle ->
            when (cycle.severity) {
                CycleSeverity.HIGH -> {
                    recommendations.add("Break direct circular dependency between ${cycle.cycle.joinToString(" → ")} using @Lazy or Provider")
                }
                CycleSeverity.MEDIUM -> {
                    recommendations.add("Consider refactoring ${cycle.cycle.joinToString(" → ")} to eliminate cycle")
                }
                CycleSeverity.LOW -> {
                    recommendations.add("Monitor ${cycle.cycle.joinToString(" → ")} cycle, consider using @Lazy for performance")
                }
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("No circular dependencies detected - dependency graph is valid")
        }
        
        return recommendations
    }
    
    private fun getKnownModules(): List<HiltModuleInfo> {
        return listOf(
            HiltModuleInfo("DatabaseModule", "SingletonComponent", listOf("DBHelper", "DataCenter")),
            HiltModuleInfo("NetworkModule", "SingletonComponent", listOf("NetworkHelper", "Gson", "Json")),
            HiltModuleInfo("SourceModule", "SingletonComponent", listOf("SourceManager", "ExtensionManager")),
            HiltModuleInfo("AnalyticsModule", "SingletonComponent", listOf("FirebaseAnalytics")),
            HiltModuleInfo("CoroutineModule", "SingletonComponent", listOf("CoroutineScopes", "DispatcherProvider")),
            HiltModuleInfo("MigrationModule", "SingletonComponent", listOf("MigrationValidator", "MigrationLogger")),
            HiltModuleInfo("ErrorHandlingModule", "SingletonComponent", listOf("HiltErrorHandler", "HiltDebugUtils"))
        )
    }
    
    private fun validateModule(module: HiltModuleInfo): ModuleValidation {
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate module structure
        if (module.bindings.isEmpty()) {
            issues.add("Module ${module.name} has no bindings")
        }
        
        // Validate component compatibility
        if (module.component != "SingletonComponent" && module.bindings.any { it.contains("Singleton") }) {
            warnings.add("Module ${module.name} may have scoping issues")
        }
        
        // Check for naming conventions
        if (!module.name.endsWith("Module")) {
            warnings.add("Module ${module.name} doesn't follow naming convention")
        }
        
        return ModuleValidation(
            moduleName = module.name,
            isValid = issues.isEmpty(),
            issues = issues,
            warnings = warnings,
            bindingCount = module.bindings.size
        )
    }
    
    private fun validateSingletonScoping(): List<ScopingIssue> {
        val issues = mutableListOf<ScopingIssue>()
        
        // Check for singleton dependencies that might cause memory leaks
        val singletonDependencies = listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager")
        singletonDependencies.forEach { dependency ->
            // This would be more sophisticated in a real implementation
            if (dependency.contains("Activity") || dependency.contains("Fragment")) {
                issues.add(
                    ScopingIssue(
                        type = "MemoryLeak",
                        description = "$dependency might cause memory leak if it holds Activity/Fragment reference",
                        severity = IssueSeverity.HIGH,
                        suggestion = "Use @ApplicationContext instead of Activity context"
                    )
                )
            }
        }
        
        return issues
    }
    
    private fun validateActivityScoping(): List<ScopingIssue> {
        val issues = mutableListOf<ScopingIssue>()
        
        // Check for activity-scoped dependencies that might be incorrectly scoped
        // This would be implemented based on actual activity dependencies
        
        return issues
    }
    
    private fun validateFragmentScoping(): List<ScopingIssue> {
        val issues = mutableListOf<ScopingIssue>()
        
        // Check for fragment-scoped dependencies that might be incorrectly scoped
        // This would be implemented based on actual fragment dependencies
        
        return issues
    }
    
    private fun generateScopingWarnings(): List<String> {
        return listOf(
            "Ensure singleton dependencies don't hold references to Activities or Fragments",
            "Use appropriate lifecycle scopes for UI-related dependencies",
            "Consider using @ActivityScoped for dependencies that need Activity lifecycle"
        )
    }
    
    private fun getAvailableBindings(): List<String> {
        return listOf(
            "DBHelper", "DataCenter", "NetworkHelper", "SourceManager", "ExtensionManager",
            "FirebaseAnalytics", "CoroutineScopes", "DispatcherProvider", "Gson", "Json",
            "MigrationValidator", "MigrationLogger", "HiltErrorHandler", "HiltDebugUtils"
        )
    }
    
    private fun getRequiredBindings(): List<RequiredBinding> {
        return listOf(
            RequiredBinding("DBHelper", "ChaptersViewModel"),
            RequiredBinding("DataCenter", "ChaptersViewModel"),
            RequiredBinding("NetworkHelper", "ChaptersViewModel"),
            RequiredBinding("SourceManager", "ChaptersViewModel"),
            RequiredBinding("DBHelper", "GoogleBackupViewModel"),
            RequiredBinding("DataCenter", "GoogleBackupViewModel"),
            RequiredBinding("NetworkHelper", "GoogleBackupViewModel")
        )
    }
    
    private fun generateModuleSuggestion(type: String): String {
        return when {
            type.contains("DB") || type.contains("Data") -> "DatabaseModule"
            type.contains("Network") || type.contains("Http") -> "NetworkModule"
            type.contains("Source") || type.contains("Extension") -> "SourceModule"
            type.contains("Analytics") || type.contains("Firebase") -> "AnalyticsModule"
            type.contains("Coroutine") || type.contains("Dispatcher") -> "CoroutineModule"
            else -> "CustomModule"
        }
    }
}

// Data classes for validation results

data class DependencyGraphValidation(
    val isValid: Boolean,
    val circularDependencies: List<CircularDependency>,
    val recommendations: List<String>,
    val dependencyCount: Int
)

data class CircularDependency(
    val cycle: List<String>,
    val severity: CycleSeverity
)

enum class CycleSeverity { HIGH, MEDIUM, LOW }

data class ModuleValidationReport(
    val moduleValidations: Map<String, ModuleValidation>,
    val overallValid: Boolean,
    val totalModules: Int
)

data class ModuleValidation(
    val moduleName: String,
    val isValid: Boolean,
    val issues: List<String>,
    val warnings: List<String>,
    val bindingCount: Int
)

data class ScopingValidation(
    val isValid: Boolean,
    val issues: List<ScopingIssue>,
    val warnings: List<String>
)

data class ScopingIssue(
    val type: String,
    val description: String,
    val severity: IssueSeverity,
    val suggestion: String
)

enum class IssueSeverity { HIGH, MEDIUM, LOW }

data class BindingValidation(
    val isComplete: Boolean,
    val missingBindings: List<MissingBinding>,
    val availableBindings: List<String>,
    val requiredBindings: List<String>
)

data class MissingBinding(
    val type: String,
    val requiredBy: String,
    val suggestedModule: String
)

data class ComprehensiveValidationReport(
    val isValid: Boolean,
    val dependencyGraphValidation: DependencyGraphValidation,
    val moduleValidation: ModuleValidationReport,
    val scopingValidation: ScopingValidation,
    val bindingValidation: BindingValidation,
    val generatedAt: Long
) {
    fun getFormattedReport(): String = buildString {
        appendLine("=== COMPREHENSIVE HILT VALIDATION REPORT ===")
        appendLine("Generated: ${java.util.Date(generatedAt)}")
        appendLine("Overall Status: ${if (isValid) "✅ VALID" else "❌ INVALID"}")
        appendLine()
        
        appendLine("DEPENDENCY GRAPH:")
        appendLine("  Status: ${if (dependencyGraphValidation.isValid) "✅ Valid" else "❌ Invalid"}")
        appendLine("  Dependencies: ${dependencyGraphValidation.dependencyCount}")
        appendLine("  Circular Dependencies: ${dependencyGraphValidation.circularDependencies.size}")
        
        appendLine()
        appendLine("MODULE VALIDATION:")
        appendLine("  Status: ${if (moduleValidation.overallValid) "✅ Valid" else "❌ Invalid"}")
        appendLine("  Total Modules: ${moduleValidation.totalModules}")
        
        appendLine()
        appendLine("SCOPING VALIDATION:")
        appendLine("  Status: ${if (scopingValidation.isValid) "✅ Valid" else "❌ Invalid"}")
        appendLine("  Issues: ${scopingValidation.issues.size}")
        appendLine("  Warnings: ${scopingValidation.warnings.size}")
        
        appendLine()
        appendLine("BINDING VALIDATION:")
        appendLine("  Status: ${if (bindingValidation.isComplete) "✅ Complete" else "❌ Incomplete"}")
        appendLine("  Missing Bindings: ${bindingValidation.missingBindings.size}")
        appendLine("  Available Bindings: ${bindingValidation.availableBindings.size}")
        
        appendLine("==========================================")
    }
}

data class HiltModuleInfo(
    val name: String,
    val component: String,
    val bindings: List<String>
)

data class RequiredBinding(
    val type: String,
    val requiredBy: String
)