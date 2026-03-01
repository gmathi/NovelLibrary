package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import java.io.File

/**
 * Property-based test for package structure following namespace convention.
 * **Validates: Requirements 12.8**
 * 
 * Property 8: Module Package Structure Follows Namespace Convention
 * For any file moved to a module, if the file was in a subpackage path
 * (e.g., util/system/LocaleManager.kt), it must preserve that relative path
 * under the module's namespace (e.g., io.github.gmathi.novellibrary.util.system.LocaleManager).
 */
class PackageStructurePropertyTest : StringSpec({
    
    "Property 8: Package structure follows namespace convention" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Define module base packages and their source directories
        val modules = mapOf(
            "util" to "io.github.gmathi.novellibrary.util",
            "common" to "io.github.gmathi.novellibrary.common",
            "core" to "io.github.gmathi.novellibrary.core"
        )
        
        val violations = mutableListOf<String>()
        
        // Check each module
        for ((moduleName, basePackage) in modules) {
            val moduleJavaDir = File(projectRoot, "$moduleName/src/main/java")
            
            if (!moduleJavaDir.exists()) {
                violations.add("Module directory does not exist: ${moduleJavaDir.path}")
                continue
            }
            
            // The base package directory structure
            val basePackagePath = basePackage.replace('.', '/')
            val basePackageDir = File(moduleJavaDir, basePackagePath)
            
            if (!basePackageDir.exists()) {
                violations.add(
                    "Base package directory does not exist: ${basePackageDir.path}"
                )
                continue
            }
            
            // Walk through all Kotlin and Java files in the base package directory
            basePackageDir.walkTopDown()
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
                        
                        // Calculate expected package based on file path
                        val relativePath = file.parentFile.relativeTo(moduleJavaDir).path
                        val expectedPackage = relativePath.replace(File.separatorChar, '.')
                        
                        // Verify package matches file path
                        if (packageName != expectedPackage) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not match file path structure. " +
                                "Expected: '$expectedPackage'"
                            )
                        }
                        
                        // Verify package starts with base package
                        if (!packageName.startsWith(basePackage)) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not start with base package '$basePackage'"
                            )
                        }
                    }
                }
        }
        
        // Assert that there are no violations
        violations.shouldBeEmpty()
    }
    
    "Property 8a: Util module preserves relative package paths" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val moduleJavaDir = File(projectRoot, "util/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.util"
        
        val violations = mutableListOf<String>()
        
        if (moduleJavaDir.exists()) {
            moduleJavaDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        // Calculate expected package based on file path
                        val relativePath = file.parentFile.relativeTo(moduleJavaDir).path
                        val expectedPackage = relativePath.replace(File.separatorChar, '.')
                        
                        if (packageName != expectedPackage) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not match file path. Expected: '$expectedPackage'"
                            )
                        }
                    }
                }
        }
        
        violations.shouldBeEmpty()
    }
    
    "Property 8b: Common module preserves relative package paths" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val moduleJavaDir = File(projectRoot, "common/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.common"
        
        val violations = mutableListOf<String>()
        
        if (moduleJavaDir.exists()) {
            moduleJavaDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        // Calculate expected package based on file path
                        val relativePath = file.parentFile.relativeTo(moduleJavaDir).path
                        val expectedPackage = relativePath.replace(File.separatorChar, '.')
                        
                        if (packageName != expectedPackage) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not match file path. Expected: '$expectedPackage'"
                            )
                        }
                    }
                }
        }
        
        violations.shouldBeEmpty()
    }
    
    "Property 8c: Core module preserves relative package paths" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val moduleJavaDir = File(projectRoot, "core/src/main/java")
        val basePackage = "io.github.gmathi.novellibrary.core"
        
        val violations = mutableListOf<String>()
        
        if (moduleJavaDir.exists()) {
            moduleJavaDir.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val lines = file.readLines()
                    val packageLine = lines.firstOrNull { it.trim().startsWith("package ") }
                    
                    if (packageLine != null) {
                        val packageName = packageLine.trim()
                            .removePrefix("package ")
                            .removeSuffix(";")
                            .trim()
                        
                        // Calculate expected package based on file path
                        val relativePath = file.parentFile.relativeTo(moduleJavaDir).path
                        val expectedPackage = relativePath.replace(File.separatorChar, '.')
                        
                        if (packageName != expectedPackage) {
                            violations.add(
                                "${file.relativeTo(projectRoot).path} - " +
                                "Package '$packageName' does not match file path. Expected: '$expectedPackage'"
                            )
                        }
                    }
                }
        }
        
        violations.shouldBeEmpty()
    }
})
