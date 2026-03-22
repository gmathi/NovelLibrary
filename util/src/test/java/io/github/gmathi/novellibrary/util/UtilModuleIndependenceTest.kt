package io.github.gmathi.novellibrary.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property 3: Util Module Has Zero Project Dependencies
 * 
 * **Validates: Requirements 3.7**
 * 
 * This test verifies that the util module's build.gradle.kts file does not contain
 * any dependencies on other project modules (i.e., no "project(" references).
 */
class UtilModuleIndependenceTest : StringSpec({
    
    "Feature: core-module-extraction, Property 3: Util module has zero project dependencies" {
        // Find the util module's build.gradle.kts file
        val buildFile = findBuildGradleFile()
        
        // Read the build file content
        val buildFileContent = buildFile.readText()
        
        // Extract all dependency declarations
        val dependencyLines = buildFileContent.lines()
            .filter { it.trim().matches(Regex(".*implementation\\(.*\\).*")) || 
                     it.trim().matches(Regex(".*api\\(.*\\).*")) ||
                     it.trim().matches(Regex(".*testImplementation\\(.*\\).*")) ||
                     it.trim().matches(Regex(".*androidTestImplementation\\(.*\\).*")) }
        
        // Check that none of the dependencies reference a project module
        val projectDependencies = dependencyLines.filter { it.contains("project(") }
        
        // Assert that there are no project dependencies
        projectDependencies.isEmpty() shouldBe true
    }
})

/**
 * Helper function to find the build.gradle.kts file for the util module.
 * Navigates up from the test class location to find the module root.
 */
private fun findBuildGradleFile(): File {
    // Start from the current working directory or test class location
    var currentDir = File(System.getProperty("user.dir"))
    
    // If we're in a subdirectory (like util/build/...), navigate up to util/
    while (currentDir.name != "util" && currentDir.parent != null) {
        currentDir = currentDir.parentFile
    }
    
    // Now we should be in the util/ directory
    val buildFile = File(currentDir, "build.gradle.kts")
    
    if (!buildFile.exists()) {
        throw IllegalStateException("Could not find build.gradle.kts in ${currentDir.absolutePath}")
    }
    
    return buildFile
}
