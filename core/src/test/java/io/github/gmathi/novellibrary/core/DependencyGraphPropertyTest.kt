package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for dependency graph acyclicity.
 * **Validates: Requirements 9.9, 16.8**
 * 
 * Property 5: Dependency Graph Is Acyclic
 * For any module in the project, traversing its dependency graph must never
 * return to the starting module, ensuring no circular dependencies exist.
 */
class DependencyGraphPropertyTest : StringSpec({
    
    "Property 5: Dependency graph is acyclic" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Get all modules from settings.gradle
        val settingsFile = File(projectRoot, "settings.gradle")
        settingsFile.exists() shouldBe true
        
        val settingsContent = settingsFile.readText()
        val modulePattern = """include\s+['"]:([\w-]+)['"]""".toRegex()
        val modules = modulePattern.findAll(settingsContent)
            .map { it.groupValues[1] }
            .toList()
        
        // Build dependency graph
        val dependencyGraph = mutableMapOf<String, Set<String>>()
        
        for (module in modules) {
            val buildFile = File(projectRoot, "$module/build.gradle")
            val buildFileKts = File(projectRoot, "$module/build.gradle.kts")
            
            val actualBuildFile = when {
                buildFile.exists() -> buildFile
                buildFileKts.exists() -> buildFileKts
                else -> continue
            }
            
            val buildContent = actualBuildFile.readText()
            
            // Extract project dependencies
            val projectDepPattern = """project\(['"]:([\w-]+)['"]\)""".toRegex()
            val dependencies = projectDepPattern.findAll(buildContent)
                .map { it.groupValues[1] }
                .toSet()
            
            dependencyGraph[module] = dependencies
        }
        
        // Check for cycles using DFS
        fun hasCycle(module: String, visited: MutableSet<String>, recursionStack: MutableSet<String>): Boolean {
            visited.add(module)
            recursionStack.add(module)
            
            val dependencies = dependencyGraph[module] ?: emptySet()
            for (dependency in dependencies) {
                if (!visited.contains(dependency)) {
                    if (hasCycle(dependency, visited, recursionStack)) {
                        return true
                    }
                } else if (recursionStack.contains(dependency)) {
                    // Found a cycle
                    return true
                }
            }
            
            recursionStack.remove(module)
            return false
        }
        
        // Check each module for cycles
        val visited = mutableSetOf<String>()
        var cycleFound = false
        
        for (module in modules) {
            if (!visited.contains(module)) {
                if (hasCycle(module, visited, mutableSetOf())) {
                    cycleFound = true
                    break
                }
            }
        }
        
        // Assert that no cycles were found
        cycleFound shouldBe false
    }
})
