package io.github.gmathi.novellibrary.util.error

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debugging utilities for Hilt component tree visualization and dependency resolution
 */
@Singleton
class HiltDebugUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltDebugUtils"
    }
    
    /**
     * Generate a visual representation of the Hilt component tree
     */
    fun generateComponentTree(): String = buildString {
        appendLine("=== HILT COMPONENT TREE ===")
        appendLine("SingletonComponent (Application Scope)")
        appendLine("├── DatabaseModule")
        appendLine("│   ├── DBHelper (@Singleton)")
        appendLine("│   └── DataCenter (@Singleton)")
        appendLine("├── NetworkModule")
        appendLine("│   ├── NetworkHelper (@Singleton)")
        appendLine("│   ├── Gson (@Singleton)")
        appendLine("│   └── Json (@Singleton)")
        appendLine("├── SourceModule")
        appendLine("│   ├── SourceManager (@Singleton)")
        appendLine("│   └── ExtensionManager (@Singleton)")
        appendLine("├── AnalyticsModule")
        appendLine("│   └── FirebaseAnalytics (@Singleton)")
        appendLine("├── CoroutineModule")
        appendLine("│   ├── CoroutineScopes (@Singleton)")
        appendLine("│   └── DispatcherProvider (@Singleton)")
        appendLine("└── MigrationModule")
        appendLine("    ├── MigrationValidator (@Singleton)")
        appendLine("    ├── MigrationLogger (@Singleton)")
        appendLine("    └── MigrationFeatureFlags (@Singleton)")
        appendLine()
        appendLine("ActivityComponent (Activity Scope)")
        appendLine("└── [Activity-scoped dependencies]")
        appendLine()
        appendLine("FragmentComponent (Fragment Scope)")
        appendLine("└── [Fragment-scoped dependencies]")
        appendLine()
        appendLine("ServiceComponent (Service Scope)")
        appendLine("└── [Service-scoped dependencies]")
        appendLine("===========================")
    }
    
    /**
     * Trace dependency resolution path for a given type
     */
    fun traceDependencyResolution(dependencyType: String): DependencyTrace {
        val trace = mutableListOf<String>()
        val providers = mutableListOf<String>()
        
        // Simulate dependency resolution tracing
        when (dependencyType) {
            "DBHelper" -> {
                trace.add("1. Request for DBHelper")
                trace.add("2. Found in DatabaseModule")
                trace.add("3. Singleton scope - checking existing instance")
                trace.add("4. Creating new instance with Application context")
                providers.add("DatabaseModule.provideDBHelper()")
            }
            "ChaptersViewModel" -> {
                trace.add("1. Request for ChaptersViewModel")
                trace.add("2. Found @HiltViewModel annotation")
                trace.add("3. Resolving constructor dependencies:")
                trace.add("   - SavedStateHandle (provided by Hilt)")
                trace.add("   - DBHelper (from DatabaseModule)")
                trace.add("   - DataCenter (from DatabaseModule)")
                trace.add("   - NetworkHelper (from NetworkModule)")
                trace.add("   - SourceManager (from SourceModule)")
                trace.add("4. All dependencies resolved successfully")
                providers.add("@HiltViewModel constructor injection")
            }
            else -> {
                trace.add("1. Request for $dependencyType")
                trace.add("2. Searching in all installed modules...")
                trace.add("3. No provider found - this will cause a compilation error")
            }
        }
        
        return DependencyTrace(
            dependencyType = dependencyType,
            resolutionSteps = trace,
            providers = providers,
            isResolvable = providers.isNotEmpty()
        )
    }
    
    /**
     * Validate all Hilt modules and their bindings
     */
    fun validateAllModules(): ModuleValidationReport {
        val moduleResults = mutableMapOf<String, ModuleValidationResult>()
        
        // Validate each module
        val modules = listOf(
            "DatabaseModule",
            "NetworkModule", 
            "SourceModule",
            "AnalyticsModule",
            "CoroutineModule",
            "MigrationModule"
        )
        
        modules.forEach { moduleName ->
            moduleResults[moduleName] = validateModule(moduleName)
        }
        
        return ModuleValidationReport(
            moduleResults = moduleResults,
            overallValid = moduleResults.values.all { it.isValid }
        )
    }
    
    /**
     * Check for potential circular dependencies
     */
    fun checkCircularDependencies(): CircularDependencyReport {
        val potentialCycles = mutableListOf<List<String>>()
        
        // Define known dependency relationships
        val dependencies = mapOf(
            "SourceManager" to listOf("ExtensionManager", "Context"),
            "ExtensionManager" to listOf("Context"),
            "ChaptersViewModel" to listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager"),
            "DBHelper" to listOf("Context"),
            "DataCenter" to listOf("Context"),
            "NetworkHelper" to listOf("Context")
        )
        
        // Simple cycle detection (in a real implementation, this would be more sophisticated)
        dependencies.forEach { (component, deps) ->
            deps.forEach { dep ->
                val depDeps = dependencies[dep] ?: emptyList()
                if (depDeps.contains(component)) {
                    potentialCycles.add(listOf(component, dep, component))
                }
            }
        }
        
        return CircularDependencyReport(
            hasCycles = potentialCycles.isNotEmpty(),
            cycles = potentialCycles,
            recommendations = if (potentialCycles.isNotEmpty()) {
                listOf(
                    "Use @Lazy<T> to break circular dependencies",
                    "Consider using Provider<T> for optional dependencies",
                    "Refactor architecture to eliminate cycles"
                )
            } else {
                listOf("No circular dependencies detected")
            }
        )
    }
    
    /**
     * Generate troubleshooting guide for common issues
     */
    fun generateTroubleshootingGuide(): String = buildString {
        appendLine("=== HILT TROUBLESHOOTING GUIDE ===")
        appendLine()
        appendLine("1. MISSING BINDING ERROR")
        appendLine("   Problem: 'Missing binding for [Type]'")
        appendLine("   Solution:")
        appendLine("   - Create @Module with @InstallIn annotation")
        appendLine("   - Add @Provides method for the missing type")
        appendLine("   - Ensure module is in correct component scope")
        appendLine()
        appendLine("2. CIRCULAR DEPENDENCY ERROR")
        appendLine("   Problem: 'Circular dependency detected'")
        appendLine("   Solution:")
        appendLine("   - Use @Lazy<Type> for one dependency")
        appendLine("   - Use Provider<Type> to break the cycle")
        appendLine("   - Refactor to eliminate circular reference")
        appendLine()
        appendLine("3. WRONG SCOPE ERROR")
        appendLine("   Problem: 'Component scope mismatch'")
        appendLine("   Solution:")
        appendLine("   - Use @Singleton for app-level dependencies")
        appendLine("   - Use @ActivityScoped for activity dependencies")
        appendLine("   - Match provider and injection scopes")
        appendLine()
        appendLine("4. MISSING @AndroidEntryPoint")
        appendLine("   Problem: 'Injection failed in Activity/Fragment'")
        appendLine("   Solution:")
        appendLine("   - Add @AndroidEntryPoint to class")
        appendLine("   - Ensure class extends proper base class")
        appendLine("   - Check that Hilt is properly configured")
        appendLine()
        appendLine("5. MODULE NOT INSTALLED")
        appendLine("   Problem: 'Module bindings not available'")
        appendLine("   Solution:")
        appendLine("   - Add @InstallIn(Component::class) to module")
        appendLine("   - Verify component type is correct")
        appendLine("   - Check module is in classpath")
        appendLine()
        appendLine("6. QUALIFIER MISMATCH")
        appendLine("   Problem: 'Multiple bindings for same type'")
        appendLine("   Solution:")
        appendLine("   - Use @Named or custom qualifiers")
        appendLine("   - Ensure qualifiers match exactly")
        appendLine("   - Check for typos in qualifier names")
        appendLine("=================================")
    }
    
    private fun validateModule(moduleName: String): ModuleValidationResult {
        val issues = mutableListOf<String>()
        val bindings = mutableListOf<String>()
        
        // Simulate module validation based on known modules
        when (moduleName) {
            "DatabaseModule" -> {
                bindings.addAll(listOf("DBHelper", "DataCenter"))
            }
            "NetworkModule" -> {
                bindings.addAll(listOf("NetworkHelper", "Gson", "Json"))
            }
            "SourceModule" -> {
                bindings.addAll(listOf("SourceManager", "ExtensionManager"))
            }
            "AnalyticsModule" -> {
                bindings.addAll(listOf("FirebaseAnalytics"))
            }
            "CoroutineModule" -> {
                bindings.addAll(listOf("CoroutineScopes", "DispatcherProvider"))
            }
            "MigrationModule" -> {
                bindings.addAll(listOf("MigrationValidator", "MigrationLogger", "MigrationFeatureFlags"))
            }
            else -> {
                issues.add("Unknown module: $moduleName")
            }
        }
        
        return ModuleValidationResult(
            moduleName = moduleName,
            isValid = issues.isEmpty(),
            issues = issues,
            bindings = bindings
        )
    }
}

/**
 * Result of dependency resolution tracing
 */
data class DependencyTrace(
    val dependencyType: String,
    val resolutionSteps: List<String>,
    val providers: List<String>,
    val isResolvable: Boolean
) {
    fun getFormattedTrace(): String = buildString {
        appendLine("=== DEPENDENCY TRACE: $dependencyType ===")
        resolutionSteps.forEach { step ->
            appendLine(step)
        }
        if (providers.isNotEmpty()) {
            appendLine("Providers:")
            providers.forEach { provider ->
                appendLine("  - $provider")
            }
        }
        appendLine("Resolvable: ${if (isResolvable) "YES" else "NO"}")
        appendLine("=====================================")
    }
}

/**
 * Result of module validation
 */
data class ModuleValidationResult(
    val moduleName: String,
    val isValid: Boolean,
    val issues: List<String>,
    val bindings: List<String>
)

/**
 * Report of all module validations
 */
data class ModuleValidationReport(
    val moduleResults: Map<String, ModuleValidationResult>,
    val overallValid: Boolean
) {
    fun getFormattedReport(): String = buildString {
        appendLine("=== MODULE VALIDATION REPORT ===")
        appendLine("Overall Status: ${if (overallValid) "VALID" else "INVALID"}")
        appendLine()
        
        moduleResults.forEach { (moduleName, result) ->
            appendLine("Module: $moduleName")
            appendLine("  Status: ${if (result.isValid) "✅ VALID" else "❌ INVALID"}")
            
            if (result.issues.isNotEmpty()) {
                appendLine("  Issues:")
                result.issues.forEach { issue ->
                    appendLine("    - $issue")
                }
            }
            
            if (result.bindings.isNotEmpty()) {
                appendLine("  Bindings:")
                result.bindings.forEach { binding ->
                    appendLine("    - $binding")
                }
            }
            appendLine()
        }
        appendLine("===============================")
    }
}

/**
 * Report of circular dependency analysis
 */
data class CircularDependencyReport(
    val hasCycles: Boolean,
    val cycles: List<List<String>>,
    val recommendations: List<String>
) {
    fun getFormattedReport(): String = buildString {
        appendLine("=== CIRCULAR DEPENDENCY ANALYSIS ===")
        appendLine("Cycles Detected: ${if (hasCycles) "YES" else "NO"}")
        
        if (hasCycles) {
            appendLine("Detected Cycles:")
            cycles.forEachIndexed { index, cycle ->
                appendLine("  Cycle ${index + 1}: ${cycle.joinToString(" → ")}")
            }
        }
        
        appendLine("Recommendations:")
        recommendations.forEach { recommendation ->
            appendLine("  - $recommendation")
        }
        appendLine("===================================")
    }
}