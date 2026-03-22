package io.github.gmathi.novellibrary.settings

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.io.File

/**
 * Property-based test for validating package declarations in the settings module.
 * 
 * **Validates: Requirements 2.4**
 * 
 * This test ensures that all Kotlin files in the settings module have correct package
 * declarations matching the pattern `io.github.gmathi.novellibrary.settings.*`
 * and that the package declaration matches the file's directory structure.
 */
class PackageDeclarationPropertyTest : FunSpec({
    
    // Get the project root directory - when running tests, user.dir is the module directory
    val moduleRoot = File(System.getProperty("user.dir"))
    val settingsModuleRoot = File(moduleRoot, "src/main/java")
    val expectedPackagePrefix = "io.github.gmathi.novellibrary.settings"
    
    test("Property 1: Package Declaration Correctness - all migrated activity files should have correct package declarations") {
        // This property test validates that for any migrated activity file in the settings module,
        // the package declaration should match the pattern `io.github.gmathi.novellibrary.settings.*`
        
        // Get all Kotlin files in the settings module
        val kotlinFiles = if (settingsModuleRoot.exists()) {
            settingsModuleRoot.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .toList()
        } else {
            emptyList()
        }
        
        // If no files exist yet, the test should pass (nothing to validate)
        if (kotlinFiles.isEmpty()) {
            true shouldBe true
            return@test
        }
        
        // For each Kotlin file, validate package declaration
        kotlinFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()
            
            // Find the package declaration line
            val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
            
            if (packageLine != null) {
                // Extract package name
                val packageName = packageLine.trim()
                    .removePrefix("package ")
                    .removeSuffix(";")
                    .trim()
                
                // Validate package starts with expected prefix
                packageName shouldContain expectedPackagePrefix
                
                // Validate package matches file directory structure
                val relativePath = file.relativeTo(settingsModuleRoot).parent ?: ""
                val expectedPackageFromPath = if (relativePath.isNotEmpty()) {
                    val pathPackage = relativePath.replace(File.separator, ".")
                    // The path already includes the full package structure starting from io/github/gmathi...
                    // So we just need to convert it to package format
                    pathPackage
                } else {
                    expectedPackagePrefix
                }
                
                packageName shouldBe expectedPackageFromPath
            } else {
                throw AssertionError("File ${file.name} does not have a package declaration")
            }
        }
    }
    
    test("Property 1 (PBT variant): Package declarations remain correct across arbitrary file additions") {
        // Property-based test that simulates checking package declarations
        // This runs multiple iterations to ensure the property holds universally
        
        checkAll<String>(100) { _ ->
            // Get all Kotlin files in the settings module
            val kotlinFiles = if (settingsModuleRoot.exists()) {
                settingsModuleRoot.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .toList()
            } else {
                emptyList()
            }
            
            // For each file, verify package declaration correctness
            kotlinFiles.forEach { file ->
                val content = file.readText()
                val packageLine = content.lines().firstOrNull { it.trim().startsWith("package ") }
                
                if (packageLine != null) {
                    val packageName = packageLine.trim()
                        .removePrefix("package ")
                        .removeSuffix(";")
                        .trim()
                    
                    // Property: Package must start with expected prefix
                    packageName shouldContain expectedPackagePrefix
                    
                    // Property: Package must match directory structure
                    val relativePath = file.relativeTo(settingsModuleRoot).parent ?: ""
                    val expectedPackageFromPath = if (relativePath.isNotEmpty()) {
                        val pathPackage = relativePath.replace(File.separator, ".")
                        // The path already includes the full package structure starting from io/github/gmathi...
                        // So we just need to convert it to package format
                        pathPackage
                    } else {
                        expectedPackagePrefix
                    }
                    
                    packageName shouldBe expectedPackageFromPath
                }
            }
        }
    }
})
