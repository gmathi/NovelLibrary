package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for consistent base packages across all modules.
 * **Validates: Requirements 12.1, 12.3, 12.5**
 * 
 * Property 9: All Modules Use Consistent Base Package
 * For all files in util module, the package declaration must start with
 * "io.github.gmathi.novellibrary.util"; for all files in common module,
 * must start with "io.github.gmathi.novellibrary.common"; for all files
 * in core module, must start with "io.github.gmathi.novellibrary.core".
 */
class ConsistentBasePackagePropertyTest : StringSpec({
    
    "Property 9: All modules use consistent base package" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Define module base packages
        val moduleBasePackages = mapOf(
            "util" to "io.github.gmathi.novellibrary.util",
            "common" to "io.github.gmathi.novellibrary.common",
            "core" to "io.github.gmathi.novellibrary.core"
        )
        
        val violations = mutableListOf<String>()
        val moduleFileCounts = mutableMapOf<String, Int>()
        
        // Check each module
        for ((moduleName, basePackage) in moduleBasePackages) {
            val moduleDir = File(projectRoot, "$moduleName/src/main/java")
            
            if (!moduleDir.exists()) {
                violations.add("Module directory does not exist: ${moduleDir.path}")
                continue
            }
            
            var fileCount = 0
            
            // Walk through all Kotlin and Java files
            moduleDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    fileCount++
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
                        
                        // Verify it starts with the base package
                        if (!packageName.startsWith(basePackage)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with base package '$basePackage'"
                            )
                        }
                    }
                }
            
            moduleFileCounts[moduleName] = fileCount
        }
        
        // Verify we found files in each module
        for ((moduleName, count) in moduleFileCounts) {
            if (count == 0) {
                violations.add("No Kotlin/Java files found in $moduleName module")
            }
        }
        
        // Assert that there are no violations
        violations.shouldBeEmpty()
    }
    
    "Property 9a: Util module consistently uses util base package" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val utilModule = File(projectRoot, "util/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.util"
        
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
                        
                        if (!packageName.startsWith(basePackage)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with base package '$basePackage'"
                            )
                        }
                    }
                }
        }
        
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
    
    "Property 9b: Common module consistently uses common base package" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val commonModule = File(projectRoot, "common/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.common"
        
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
                        
                        if (!packageName.startsWith(basePackage)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with base package '$basePackage'"
                            )
                        }
                    }
                }
        }
        
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
    
    "Property 9c: Core module consistently uses core base package" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val coreModule = File(projectRoot, "core/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.core"
        
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
                        
                        if (!packageName.startsWith(basePackage)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with base package '$basePackage'"
                            )
                        }
                    }
                }
        }
        
        (fileCount > 0) shouldBe true
        violations.shouldBeEmpty()
    }
})
