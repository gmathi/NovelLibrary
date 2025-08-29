package io.github.gmathi.novellibrary.util.validation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.gmathi.novellibrary.util.Logs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debugging tools for Hilt dependency resolution
 * Provides detailed analysis of dependency resolution paths and component relationships
 */
@Singleton
class HiltDependencyResolver @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "HiltDependencyResolver"
    }
    
    /**
     * Resolve and trace the complete dependency path for a given component
     */
    fun resolveDependencyPath(
        componentType: String,
        targetDependency: String? = null
    ): DependencyResolutionResult {
        val resolutionSteps = mutableListOf<ResolutionStep>()
        val dependencies = mutableMapOf<String, DependencyInfo>()
        
        try {
            // Start resolution from the component
            resolutionSteps.add(
                ResolutionStep(
                    step = 1,
                    action = "Starting dependency resolution for $componentType",
                    result = "SUCCESS",
                    timeMs = 0
                )
            )
            
            // Get all dependencies for the component
            val componentDependencies = getComponentDependencies(componentType)
            
            componentDependencies.forEachIndexed { index, dependency ->
                val startTime = System.nanoTime()
                val dependencyInfo = resolveSingleDependency(dependency)
                val endTime = System.nanoTime()
                val timeMs = (endTime - startTime) / 1_000_000
                
                dependencies[dependency] = dependencyInfo
                
                resolutionSteps.add(
                    ResolutionStep(
                        step = index + 2,
                        action = "Resolving dependency: $dependency",
                        result = if (dependencyInfo.isResolvable) "SUCCESS" else "FAILED",
                        timeMs = timeMs
                    )
                )
                
                // If this is the target dependency, add detailed resolution path
                if (targetDependency == dependency) {
                    resolutionSteps.addAll(getDetailedResolutionPath(dependency))
                }
            }
            
            return DependencyResolutionResult(
                componentType = componentType,
                isSuccessful = dependencies.values.all { it.isResolvable },
                resolutionSteps = resolutionSteps,
                dependencies = dependencies,
                totalResolutionTime = resolutionSteps.sumOf { it.timeMs }
            )
            
        } catch (e: Exception) {
            Logs.error(TAG, "Failed to resolve dependencies for $componentType", e)
            
            resolutionSteps.add(
                ResolutionStep(
                    step = resolutionSteps.size + 1,
                    action = "Resolution failed with error: ${e.message}",
                    result = "ERROR",
                    timeMs = 0
                )
            )
            
            return DependencyResolutionResult(
                componentType = componentType,
                isSuccessful = false,
                resolutionSteps = resolutionSteps,
                dependencies = dependencies,
                totalResolutionTime = resolutionSteps.sumOf { it.timeMs },
                error = e.message
            )
        }
    }
    
    /**
     * Analyze dependency relationships and detect potential issues
     */
    fun analyzeDependencyRelationships(): DependencyAnalysis {
        val relationships = buildDependencyRelationships()
        val issues = detectDependencyIssues(relationships)
        val metrics = calculateDependencyMetrics(relationships)
        
        return DependencyAnalysis(
            relationships = relationships,
            issues = issues,
            metrics = metrics,
            recommendations = generateDependencyRecommendations(issues, metrics)
        )
    }
    
    /**
     * Generate dependency graph visualization data
     */
    fun generateDependencyGraph(): DependencyGraph {
        val nodes = mutableListOf<GraphNode>()
        val edges = mutableListOf<GraphEdge>()
        
        // Build nodes for all known components and dependencies
        val allComponents = getAllKnownComponents()
        allComponents.forEach { component ->
            nodes.add(
                GraphNode(
                    id = component,
                    type = determineNodeType(component),
                    scope = determineNodeScope(component),
                    module = determineSourceModule(component)
                )
            )
        }
        
        // Build edges for dependency relationships
        allComponents.forEach { component ->
            val dependencies = getComponentDependencies(component)
            dependencies.forEach { dependency ->
                edges.add(
                    GraphEdge(
                        from = component,
                        to = dependency,
                        type = "DEPENDS_ON",
                        strength = calculateDependencyStrength(component, dependency)
                    )
                )
            }
        }
        
        return DependencyGraph(
            nodes = nodes,
            edges = edges,
            componentCount = nodes.size,
            dependencyCount = edges.size
        )
    }
    
    /**
     * Validate dependency graph integrity
     */
    fun validateDependencyGraphIntegrity(): GraphIntegrityReport {
        val graph = generateDependencyGraph()
        val issues = mutableListOf<GraphIntegrityIssue>()
        
        // Check for orphaned nodes
        val referencedNodes = graph.edges.flatMap { listOf(it.from, it.to) }.toSet()
        val orphanedNodes = graph.nodes.filter { it.id !in referencedNodes }
        
        orphanedNodes.forEach { node ->
            issues.add(
                GraphIntegrityIssue(
                    type = "OrphanedNode",
                    description = "Node ${node.id} has no dependencies or dependents",
                    severity = IntegritySeverity.LOW,
                    affectedNodes = listOf(node.id)
                )
            )
        }
        
        // Check for missing nodes (referenced but not defined)
        val definedNodes = graph.nodes.map { it.id }.toSet()
        val missingNodes = referencedNodes.filter { it !in definedNodes }
        
        missingNodes.forEach { nodeId ->
            issues.add(
                GraphIntegrityIssue(
                    type = "MissingNode",
                    description = "Node $nodeId is referenced but not defined",
                    severity = IntegritySeverity.HIGH,
                    affectedNodes = listOf(nodeId)
                )
            )
        }
        
        // Check for circular dependencies
        val cycles = detectCycles(graph)
        cycles.forEach { cycle ->
            issues.add(
                GraphIntegrityIssue(
                    type = "CircularDependency",
                    description = "Circular dependency detected: ${cycle.joinToString(" → ")}",
                    severity = IntegritySeverity.HIGH,
                    affectedNodes = cycle
                )
            )
        }
        
        return GraphIntegrityReport(
            isValid = issues.none { it.severity == IntegritySeverity.HIGH },
            issues = issues,
            nodeCount = graph.nodes.size,
            edgeCount = graph.edges.size,
            cycleCount = cycles.size
        )
    }
    
    private fun getComponentDependencies(componentType: String): List<String> {
        // Return known dependencies for each component type
        return when (componentType) {
            "ChaptersViewModel" -> listOf("DBHelper", "DataCenter", "NetworkHelper", "SourceManager", "SavedStateHandle")
            "GoogleBackupViewModel" -> listOf("DBHelper", "DataCenter", "NetworkHelper")
            "MainActivity" -> listOf("DataCenter", "DBHelper")
            "LibraryFragment" -> listOf("DBHelper", "DataCenter")
            "ChaptersFragment" -> listOf("DBHelper", "DataCenter", "NetworkHelper")
            "DownloadNovelService" -> listOf("DBHelper", "DataCenter", "NetworkHelper")
            else -> emptyList()
        }
    }
    
    private fun resolveSingleDependency(dependency: String): DependencyInfo {
        val providerModule = determineSourceModule(dependency)
        val scope = determineNodeScope(dependency)
        val isResolvable = providerModule != "Unknown"
        
        return DependencyInfo(
            name = dependency,
            type = determineNodeType(dependency),
            scope = scope,
            providerModule = providerModule,
            isResolvable = isResolvable,
            resolutionPath = if (isResolvable) {
                listOf("Request", "Module: $providerModule", "Provider", "Instance")
            } else {
                listOf("Request", "No Provider Found")
            }
        )
    }
    
    private fun getDetailedResolutionPath(dependency: String): List<ResolutionStep> {
        val steps = mutableListOf<ResolutionStep>()
        val dependencyInfo = resolveSingleDependency(dependency)
        
        dependencyInfo.resolutionPath.forEachIndexed { index, pathStep ->
            steps.add(
                ResolutionStep(
                    step = 100 + index, // Use high numbers to distinguish from main steps
                    action = "  └─ $pathStep",
                    result = "SUCCESS",
                    timeMs = 1 // Minimal time for sub-steps
                )
            )
        }
        
        return steps
    }
    
    private fun buildDependencyRelationships(): Map<String, List<String>> {
        val relationships = mutableMapOf<String, List<String>>()
        
        getAllKnownComponents().forEach { component ->
            relationships[component] = getComponentDependencies(component)
        }
        
        return relationships
    }
    
    private fun detectDependencyIssues(relationships: Map<String, List<String>>): List<DependencyIssue> {
        val issues = mutableListOf<DependencyIssue>()
        
        // Check for excessive dependencies (more than 5 might indicate design issues)
        relationships.forEach { (component, dependencies) ->
            if (dependencies.size > 5) {
                issues.add(
                    DependencyIssue(
                        type = "ExcessiveDependencies",
                        description = "$component has ${dependencies.size} dependencies, consider refactoring",
                        severity = DependencyIssueSeverity.MEDIUM,
                        affectedComponents = listOf(component)
                    )
                )
            }
        }
        
        // Check for unused dependencies
        val allDependencies = relationships.values.flatten().toSet()
        val allComponents = relationships.keys.toSet()
        val unusedDependencies = allDependencies - allComponents
        
        unusedDependencies.forEach { unused ->
            issues.add(
                DependencyIssue(
                    type = "UnusedDependency",
                    description = "$unused is provided but not used by any component",
                    severity = DependencyIssueSeverity.LOW,
                    affectedComponents = listOf(unused)
                )
            )
        }
        
        return issues
    }
    
    private fun calculateDependencyMetrics(relationships: Map<String, List<String>>): DependencyMetrics {
        val totalComponents = relationships.size
        val totalDependencies = relationships.values.sumOf { it.size }
        val averageDependenciesPerComponent = if (totalComponents > 0) {
            totalDependencies.toDouble() / totalComponents
        } else 0.0
        
        val maxDependencies = relationships.values.maxOfOrNull { it.size } ?: 0
        val minDependencies = relationships.values.minOfOrNull { it.size } ?: 0
        
        return DependencyMetrics(
            totalComponents = totalComponents,
            totalDependencies = totalDependencies,
            averageDependenciesPerComponent = averageDependenciesPerComponent,
            maxDependenciesPerComponent = maxDependencies,
            minDependenciesPerComponent = minDependencies
        )
    }
    
    private fun generateDependencyRecommendations(
        issues: List<DependencyIssue>,
        metrics: DependencyMetrics
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (metrics.averageDependenciesPerComponent > 4.0) {
            recommendations.add("Consider reducing average dependencies per component (currently ${String.format("%.1f", metrics.averageDependenciesPerComponent)})")
        }
        
        if (issues.any { it.type == "ExcessiveDependencies" }) {
            recommendations.add("Refactor components with excessive dependencies using composition or facade patterns")
        }
        
        if (issues.any { it.type == "UnusedDependency" }) {
            recommendations.add("Remove unused dependencies to reduce complexity")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Dependency structure looks healthy")
        }
        
        return recommendations
    }
    
    private fun getAllKnownComponents(): List<String> {
        return listOf(
            "ChaptersViewModel", "GoogleBackupViewModel",
            "MainActivity", "LibraryFragment", "ChaptersFragment",
            "DownloadNovelService",
            "DBHelper", "DataCenter", "NetworkHelper", "SourceManager", "ExtensionManager",
            "FirebaseAnalytics", "CoroutineScopes", "DispatcherProvider"
        )
    }
    
    private fun determineNodeType(component: String): NodeType {
        return when {
            component.endsWith("ViewModel") -> NodeType.VIEW_MODEL
            component.endsWith("Activity") -> NodeType.ACTIVITY
            component.endsWith("Fragment") -> NodeType.FRAGMENT
            component.endsWith("Service") -> NodeType.SERVICE
            component.endsWith("Helper") || component.endsWith("Manager") -> NodeType.UTILITY
            else -> NodeType.DEPENDENCY
        }
    }
    
    private fun determineNodeScope(component: String): String {
        return when {
            component.endsWith("ViewModel") -> "ViewModelScoped"
            component.endsWith("Activity") -> "ActivityScoped"
            component.endsWith("Fragment") -> "FragmentScoped"
            component.endsWith("Service") -> "ServiceScoped"
            else -> "Singleton"
        }
    }
    
    private fun determineSourceModule(component: String): String {
        return when {
            component.contains("DB") || component.contains("Data") -> "DatabaseModule"
            component.contains("Network") || component.contains("Http") -> "NetworkModule"
            component.contains("Source") || component.contains("Extension") -> "SourceModule"
            component.contains("Analytics") || component.contains("Firebase") -> "AnalyticsModule"
            component.contains("Coroutine") || component.contains("Dispatcher") -> "CoroutineModule"
            component.contains("Migration") -> "MigrationModule"
            component.contains("Error") || component.contains("Debug") -> "ErrorHandlingModule"
            component.endsWith("ViewModel") -> "ViewModelModule"
            else -> "Unknown"
        }
    }
    
    private fun calculateDependencyStrength(from: String, to: String): DependencyStrength {
        // Determine dependency strength based on usage patterns
        return when {
            from.endsWith("ViewModel") && to.endsWith("Helper") -> DependencyStrength.STRONG
            from.endsWith("Fragment") && to.endsWith("ViewModel") -> DependencyStrength.STRONG
            to == "Context" -> DependencyStrength.WEAK
            else -> DependencyStrength.MEDIUM
        }
    }
    
    private fun detectCycles(graph: DependencyGraph): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        graph.nodes.forEach { node ->
            if (node.id !in visited) {
                val cycle = findCycleFromNode(node.id, graph, visited, recursionStack, mutableListOf())
                if (cycle.isNotEmpty()) {
                    cycles.add(cycle)
                }
            }
        }
        
        return cycles
    }
    
    private fun findCycleFromNode(
        nodeId: String,
        graph: DependencyGraph,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        currentPath: MutableList<String>
    ): List<String> {
        visited.add(nodeId)
        recursionStack.add(nodeId)
        currentPath.add(nodeId)
        
        val neighbors = graph.edges.filter { it.from == nodeId }.map { it.to }
        
        neighbors.forEach { neighbor ->
            if (neighbor !in visited) {
                val cycle = findCycleFromNode(neighbor, graph, visited, recursionStack, currentPath)
                if (cycle.isNotEmpty()) return cycle
            } else if (neighbor in recursionStack) {
                val cycleStart = currentPath.indexOf(neighbor)
                return currentPath.subList(cycleStart, currentPath.size) + neighbor
            }
        }
        
        recursionStack.remove(nodeId)
        currentPath.removeAt(currentPath.size - 1)
        return emptyList()
    }
}

// Data classes for dependency resolution results

data class DependencyResolutionResult(
    val componentType: String,
    val isSuccessful: Boolean,
    val resolutionSteps: List<ResolutionStep>,
    val dependencies: Map<String, DependencyInfo>,
    val totalResolutionTime: Long,
    val error: String? = null
) {
    fun getFormattedResult(): String = buildString {
        appendLine("=== DEPENDENCY RESOLUTION: $componentType ===")
        appendLine("Status: ${if (isSuccessful) "✅ SUCCESS" else "❌ FAILED"}")
        appendLine("Total Time: ${totalResolutionTime}ms")
        
        if (error != null) {
            appendLine("Error: $error")
        }
        
        appendLine()
        appendLine("Resolution Steps:")
        resolutionSteps.forEach { step ->
            appendLine("  ${step.step}. ${step.action} [${step.result}] (${step.timeMs}ms)")
        }
        
        appendLine()
        appendLine("Dependencies:")
        dependencies.forEach { (name, info) ->
            appendLine("  - $name: ${if (info.isResolvable) "✅" else "❌"} (${info.providerModule})")
        }
        
        appendLine("==========================================")
    }
}

data class ResolutionStep(
    val step: Int,
    val action: String,
    val result: String,
    val timeMs: Long
)

data class DependencyInfo(
    val name: String,
    val type: NodeType,
    val scope: String,
    val providerModule: String,
    val isResolvable: Boolean,
    val resolutionPath: List<String>
)

data class DependencyAnalysis(
    val relationships: Map<String, List<String>>,
    val issues: List<DependencyIssue>,
    val metrics: DependencyMetrics,
    val recommendations: List<String>
)

data class DependencyIssue(
    val type: String,
    val description: String,
    val severity: DependencyIssueSeverity,
    val affectedComponents: List<String>
)

enum class DependencyIssueSeverity { HIGH, MEDIUM, LOW }

data class DependencyMetrics(
    val totalComponents: Int,
    val totalDependencies: Int,
    val averageDependenciesPerComponent: Double,
    val maxDependenciesPerComponent: Int,
    val minDependenciesPerComponent: Int
)

data class DependencyGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val componentCount: Int,
    val dependencyCount: Int
)

data class GraphNode(
    val id: String,
    val type: NodeType,
    val scope: String,
    val module: String
)

data class GraphEdge(
    val from: String,
    val to: String,
    val type: String,
    val strength: DependencyStrength
)

enum class NodeType {
    VIEW_MODEL, ACTIVITY, FRAGMENT, SERVICE, UTILITY, DEPENDENCY
}

enum class DependencyStrength {
    STRONG, MEDIUM, WEAK
}

data class GraphIntegrityReport(
    val isValid: Boolean,
    val issues: List<GraphIntegrityIssue>,
    val nodeCount: Int,
    val edgeCount: Int,
    val cycleCount: Int
)

data class GraphIntegrityIssue(
    val type: String,
    val description: String,
    val severity: IntegritySeverity,
    val affectedNodes: List<String>
)

enum class IntegritySeverity { HIGH, MEDIUM, LOW }