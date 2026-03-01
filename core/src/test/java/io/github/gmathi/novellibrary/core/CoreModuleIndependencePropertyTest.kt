package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for core module independence.
 * **Validates: Requirements 1.7**
 * 
 * Property 1: Core Module Has Zero Project Dependencies
 * For any dependency declared in the core module's build.gradle file,
 * that dependency must not reference another project module.
 */
class CoreModuleIndependencePropertyTest : StringSpec({
    
    "Property 1: Core module has zero project dependencies" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Read the core module's build.gradle.kts file
        val buildFile = File(projectRoot, "core/build.gradle.kts")
        buildFile.exists() shouldBe true
        
        val buildContent = buildFile.readText()
        
        // Extract all dependency declarations
        val dependencyPattern = """(implementation|api|compileOnly|testImplementation|androidTestImplementation)\s*\([^)]+\)""".toRegex()
        val dependencies = dependencyPattern.findAll(buildContent).map { it.value }.toList()
        
        // Check that none of the dependencies reference project modules
        val projectDependencies = dependencies.filter { it.contains("project(") }
        
        // Assert that there are no project dependencies
        projectDependencies.isEmpty() shouldBe true
    }
})
