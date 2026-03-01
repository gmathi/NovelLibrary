package io.github.gmathi.novellibrary.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import java.io.File

/**
 * Property-based test for zero compilation errors across all modules.
 * **Validates: Requirements 16.6**
 * 
 * Property 10: Build Produces Zero Compilation Errors
 * For any module (util, common, core, app, settings), running the Gradle compile task
 * must complete successfully with zero compilation errors.
 */
class ZeroCompilationErrorsPropertyTest : StringSpec({
    
    "Property 10: Build produces zero compilation errors for all modules" {
        // Find the project root by looking for settings.gradle
        var projectRoot = File(System.getProperty("user.dir"))
        while (!File(projectRoot, "settings.gradle").exists() && projectRoot.parentFile != null) {
            projectRoot = projectRoot.parentFile
        }
        
        val modules = listOf("util", "common", "core", "app", "settings")
        
        modules.forEach { module ->
            // Run Gradle compile task for each module
            val process = ProcessBuilder()
                .directory(projectRoot)
                .command("./gradlew", ":$module:compileNormalDebugKotlin", "--console=plain")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            // Verify the build succeeded (exit code 0)
            exitCode shouldBe 0
            
            // Verify no compilation errors in output
            output shouldNotContain "e: file:///"
            output shouldNotContain "Compilation error"
            output shouldNotContain "BUILD FAILED"
        }
    }
})
