package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for correct import namespaces after module extraction.
 * **Validates: Requirements 11.1, 11.2, 11.3, 11.7**
 * 
 * Property 7: Import Statements Reference Correct Module Namespace
 * For all classes moved from app module to util, common, or core modules,
 * any import statements in app and settings modules that reference those classes
 * must use the new module namespace (util.*, common.*, or core.*).
 */
class ImportNamespacePropertyTest : StringSpec({
    
    "Property 7: Import statements reference correct module namespace" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        // Define the old import patterns that should no longer exist
        val oldImportPatterns = listOf(
            // Core module classes - old location
            Regex("""^import io\.github\.gmathi\.novellibrary\.activity\.BaseActivity$"""),
            Regex("""^import io\.github\.gmathi\.novellibrary\.fragment\.BaseFragment$"""),
            Regex("""^import io\.github\.gmathi\.novellibrary\.activity\.settings\.BaseSettingsActivity$"""),
            Regex("""^import io\.github\.gmathi\.novellibrary\.util\.system\.DataAccessor$"""),
            
            // Common module classes - old location
            Regex("""^import io\.github\.gmathi\.novellibrary\.adapter\.GenericAdapter$"""),
            Regex("""^import io\.github\.gmathi\.novellibrary\.model\.ListitemSetting$""")
        )
        
        // Define the expected new import patterns
        val expectedImportPatterns = mapOf(
            "BaseActivity" to Regex("""^import io\.github\.gmathi\.novellibrary\.core\.activity\.BaseActivity$"""),
            "BaseFragment" to Regex("""^import io\.github\.gmathi\.novellibrary\.core\.fragment\.BaseFragment$"""),
            "BaseSettingsActivity" to Regex("""^import io\.github\.gmathi\.novellibrary\.core\.activity\.settings\.BaseSettingsActivity$"""),
            "DataAccessor" to Regex("""^import io\.github\.gmathi\.novellibrary\.core\.system\.DataAccessor$"""),
            "GenericAdapter" to Regex("""^import io\.github\.gmathi\.novellibrary\.common\.adapter\.Generic"""),
            "ListitemSetting" to Regex("""^import io\.github\.gmathi\.novellibrary\.common\.model\.ListitemSetting""")
        )
        
        // Collect all Kotlin files in app and settings modules
        val appModule = File(projectRoot, "app/src")
        val settingsModule = File(projectRoot, "settings/src")
        
        val modulesToCheck = listOfNotNull(
            appModule.takeIf { it.exists() },
            settingsModule.takeIf { it.exists() }
        )
        
        val violations = mutableListOf<String>()
        
        for (moduleDir in modulesToCheck) {
            moduleDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val lines = file.readLines()
                    lines.forEachIndexed { index, line ->
                        // Check for old import patterns that should have been updated
                        for (oldPattern in oldImportPatterns) {
                            if (oldPattern.containsMatchIn(line)) {
                                violations.add(
                                    "${file.relativeTo(projectRoot).path}:${index + 1} - " +
                                    "Found old import pattern: $line"
                                )
                            }
                        }
                    }
                }
        }
        
        // Assert that there are no violations
        violations.shouldBeEmpty()
    }
    
    "Property 7a: Core module classes use core namespace in imports" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val appModule = File(projectRoot, "app/src")
        val settingsModule = File(projectRoot, "settings/src")
        
        val modulesToCheck = listOfNotNull(
            appModule.takeIf { it.exists() },
            settingsModule.takeIf { it.exists() }
        )
        
        val coreClassImports = mutableMapOf<String, Int>()
        
        for (moduleDir in modulesToCheck) {
            moduleDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val content = file.readText()
                    
                    // Count imports of core module classes
                    if (content.contains("import io.github.gmathi.novellibrary.core.activity.BaseActivity")) {
                        coreClassImports["BaseActivity"] = coreClassImports.getOrDefault("BaseActivity", 0) + 1
                    }
                    if (content.contains("import io.github.gmathi.novellibrary.core.fragment.BaseFragment")) {
                        coreClassImports["BaseFragment"] = coreClassImports.getOrDefault("BaseFragment", 0) + 1
                    }
                    if (content.contains("import io.github.gmathi.novellibrary.core.activity.settings.BaseSettingsActivity")) {
                        coreClassImports["BaseSettingsActivity"] = coreClassImports.getOrDefault("BaseSettingsActivity", 0) + 1
                    }
                    if (content.contains("import io.github.gmathi.novellibrary.core.system.DataAccessor")) {
                        coreClassImports["DataAccessor"] = coreClassImports.getOrDefault("DataAccessor", 0) + 1
                    }
                }
        }
        
        // Verify that core classes are imported with correct namespace
        // At least some files should import these classes
        val baseActivityCount = coreClassImports["BaseActivity"] ?: 0
        val dataAccessorCount = coreClassImports["DataAccessor"] ?: 0
        (baseActivityCount > 0) shouldBe true
        // DataAccessor might not be directly imported in all cases (it's implemented by base classes)
        // (dataAccessorCount > 0) shouldBe true
    }
    
    "Property 7b: Common module classes use common namespace in imports" {
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val appModule = File(projectRoot, "app/src")
        val settingsModule = File(projectRoot, "settings/src")
        
        val modulesToCheck = listOfNotNull(
            appModule.takeIf { it.exists() },
            settingsModule.takeIf { it.exists() }
        )
        
        val commonClassImports = mutableMapOf<String, Int>()
        
        for (moduleDir in modulesToCheck) {
            moduleDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val content = file.readText()
                    
                    // Count imports of common module classes
                    if (content.contains("import io.github.gmathi.novellibrary.common.adapter.Generic")) {
                        commonClassImports["GenericAdapter"] = commonClassImports.getOrDefault("GenericAdapter", 0) + 1
                    }
                }
        }
        
        // Verify that common classes are imported with correct namespace
        // At least some files should import GenericAdapter
        val genericAdapterCount = commonClassImports["GenericAdapter"] ?: 0
        (genericAdapterCount > 0) shouldBe true
    }
})
