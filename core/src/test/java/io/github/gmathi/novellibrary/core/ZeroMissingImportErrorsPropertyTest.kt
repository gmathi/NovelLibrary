package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.io.File

/**
 * Property-based test for zero missing import errors across all modules.
 * **Validates: Requirements 16.7**
 * 
 * Property 11: Build Produces Zero Missing Import Errors
 * For any Kotlin/Java file in the project after migration, the file must not contain
 * unresolved import statements or missing symbol errors.
 */
class ZeroMissingImportErrorsPropertyTest : StringSpec({
    
    "Property 11: Build produces zero missing import errors" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val modules = listOf("util", "common", "core", "app", "settings")
        val allErrors = mutableListOf<String>()
        
        modules.forEach { module ->
            // Run Gradle compile task for each module to check for unresolved references
            val process = ProcessBuilder()
                .directory(projectRoot)
                .command("./gradlew", ":$module:compileNormalDebugKotlin", "--console=plain")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            // Verify the build succeeded
            exitCode shouldBe 0
            
            // Check for unresolved reference errors
            val unresolvedReferencePattern = """Unresolved reference""".toRegex()
            val unresolvedMatches = unresolvedReferencePattern.findAll(output).toList()
            
            if (unresolvedMatches.isNotEmpty()) {
                allErrors.add("Module $module has ${unresolvedMatches.size} unresolved reference errors")
            }
            
            // Check for missing import errors
            val missingImportPattern = """Cannot access class.*Check your module classpath""".toRegex()
            val missingImportMatches = missingImportPattern.findAll(output).toList()
            
            if (missingImportMatches.isNotEmpty()) {
                allErrors.add("Module $module has ${missingImportMatches.size} missing import errors")
            }
        }
        
        // Assert that there are no unresolved references or missing imports
        allErrors.shouldBeEmpty()
    }
})
