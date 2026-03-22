package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for package declarations matching module namespace.
 * **Validates: Requirements 4.7, 5.6, 12.7**
 * 
 * Property 4: Package Declarations Match Module Namespace
 * For all Kotlin/Java files moved to util, common, or core modules,
 * the package declaration in each file must match the target module's namespace.
 */
class PackageDeclarationPropertyTest : StringSpec({
    
    "Property 4: Package declarations match module namespace" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Define module namespaces
        val moduleNamespaces = mapOf(
            "util" to "io.github.gmathi.novellibrary.util",
            "common" to "io.github.gmathi.novellibrary.common",
            "core" to "io.github.gmathi.novellibrary.core"
        )
        
        val violations = mutableListOf<String>()
        
        // Check each module
        for ((moduleName, expectedNamespace) in moduleNamespaces) {
            val moduleDir = File(projectRoot, "$moduleName/src/main/java")
            
            if (!moduleDir.exists()) {
                violations.add("Module directory does not exist: ${moduleDir.path}")
                continue
            }
            
            // Walk through all Kotlin and Java files
            moduleDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val lines = file.readLines()
                    
                    // Find the package declaration
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine == null) {
                        violations.add(
                            "${file.relativeTo(projectRoot).path} - No package declaration found"
                        )
                    } else {
                        // Extract package name
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        // Verify it starts with the expected namespace
                        if (!packageName.startsWith(expectedNamespace)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with expected namespace '$expectedNamespace'"
                            )
                        }
                    }
                }
        }
        
        // Assert that there are no violations
        violations.shouldBeEmpty()
    }
    
    "Property 4a: Util module files use util namespace" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val utilModule = File(projectRoot, "util/src/main/java")
        val expectedNamespace = "io.github.gmathi.novellibrary.util"
        
        val violations = mutableListOf<String>()
        var fileCount = 0
        
        if (utilModule.exists()) {
            utilModule.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    fileCount++
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        if (!packageName.startsWith(expectedNamespace)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with '$expectedNamespace'"
                            )
                        }
                    }
                }
        }
        
        // Verify we found some files
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
    
    "Property 4b: Common module files use common namespace" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val commonModule = File(projectRoot, "common/src/main/java")
        val expectedNamespace = "io.github.gmathi.novellibrary.common"
        
        val violations = mutableListOf<String>()
        var fileCount = 0
        
        if (commonModule.exists()) {
            commonModule.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    fileCount++
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        if (!packageName.startsWith(expectedNamespace)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with '$expectedNamespace'"
                            )
                        }
                    }
                }
        }
        
        // Verify we found some files
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
    
    "Property 4c: Core module files use core namespace" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val coreModule = File(projectRoot, "core/src/main/java")
        val expectedNamespace = "io.github.gmathi.novellibrary.core"
        
        val violations = mutableListOf<String>()
        var fileCount = 0
        
        if (coreModule.exists()) {
            coreModule.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    fileCount++
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        if (!packageName.startsWith(expectedNamespace)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with '$expectedNamespace'"
                            )
                        }
                    }
                }
        }
        
        // Verify we found some files
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
})
