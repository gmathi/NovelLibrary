package io.github.gmathi.novellibrary.di

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Static analysis tests to validate complete removal of Injekt dependency injection patterns.
 * These tests scan the entire codebase to ensure no Injekt imports, injectLazy() calls,
 * or Injekt.get() usage remains after the cleanup migration.
 * 
 * Requirements: 1.1, 1.2, 1.3, 8.1
 */
class InjektCleanupValidationTest {

    companion object {
        // Source directories to scan
        private val SOURCE_DIRECTORIES = listOf(
            "app/src/main/java",
            "app/src/test/java",
            "app/src/androidTest/java",
            "lib/src/main/java",
            "lib/src/test/java"
        )
        
        // File extensions to scan
        private val KOTLIN_EXTENSIONS = listOf(".kt", ".kts")
        private val JAVA_EXTENSIONS = listOf(".java")
        private val ALL_EXTENSIONS = KOTLIN_EXTENSIONS + JAVA_EXTENSIONS
        
        // Injekt patterns to detect
        private val INJEKT_IMPORT_PATTERNS = listOf(
            "import uy.kohesive.injekt",
            "import uy.kohesive.injekt.*",
            "import uy.kohesive.injekt.Injekt",
            "import uy.kohesive.injekt.injectLazy",
            "import uy.kohesive.injekt.api.*"
        )
        
        private val INJECT_LAZY_PATTERNS = listOf(
            "injectLazy()",
            "by injectLazy()",
            ".injectLazy()",
            "injectLazy<",
            "by injectLazy<"
        )
        
        private val INJEKT_GET_PATTERNS = listOf(
            "Injekt.get(",
            "Injekt.get<",
            "Injekt.getInstance(",
            "Injekt.getInstance<",
            "Injekt.addSingleton",
            "Injekt.addSingletonFactory"
        )
    }

    @Test
    fun `verify no Injekt imports remain in codebase`() {
        // Given
        val sourceFiles = getAllSourceFiles()
        val violatingFiles = mutableListOf<Pair<String, List<String>>>()

        // When
        sourceFiles.forEach { file ->
            val content = file.readText()
            val violations = INJEKT_IMPORT_PATTERNS.filter { pattern ->
                content.contains(pattern, ignoreCase = false)
            }
            
            if (violations.isNotEmpty()) {
                violatingFiles.add(file.relativePath to violations)
            }
        }

        // Then
        if (violatingFiles.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("Found Injekt imports in ${violatingFiles.size} files:")
                violatingFiles.forEach { (filePath, violations) ->
                    appendLine("  $filePath:")
                    violations.forEach { violation ->
                        appendLine("    - $violation")
                    }
                }
                appendLine()
                appendLine("All Injekt imports must be removed for complete migration to Hilt.")
            }
            fail(errorMessage)
        }
    }

    @Test
    fun `verify no injectLazy usage remains in codebase`() {
        // Given
        val sourceFiles = getAllSourceFiles()
        val violatingFiles = mutableListOf<Pair<String, List<ViolationDetail>>>()

        // When
        sourceFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()
            val violations = mutableListOf<ViolationDetail>()
            
            lines.forEachIndexed { lineIndex, line ->
                INJECT_LAZY_PATTERNS.forEach { pattern ->
                    if (line.contains(pattern, ignoreCase = false)) {
                        violations.add(ViolationDetail(
                            pattern = pattern,
                            lineNumber = lineIndex + 1,
                            lineContent = line.trim()
                        ))
                    }
                }
            }
            
            if (violations.isNotEmpty()) {
                violatingFiles.add(file.relativePath to violations)
            }
        }

        // Then
        if (violatingFiles.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("Found injectLazy() usage in ${violatingFiles.size} files:")
                violatingFiles.forEach { (filePath, violations) ->
                    appendLine("  $filePath:")
                    violations.forEach { violation ->
                        appendLine("    Line ${violation.lineNumber}: ${violation.lineContent}")
                        appendLine("      Pattern: ${violation.pattern}")
                    }
                }
                appendLine()
                appendLine("All injectLazy() calls must be replaced with Hilt constructor injection or EntryPoint access.")
            }
            fail(errorMessage)
        }
    }

    @Test
    fun `verify no Injekt get usage remains in codebase`() {
        // Given
        val sourceFiles = getAllSourceFiles()
        val violatingFiles = mutableListOf<Pair<String, List<ViolationDetail>>>()

        // When
        sourceFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()
            val violations = mutableListOf<ViolationDetail>()
            
            lines.forEachIndexed { lineIndex, line ->
                INJEKT_GET_PATTERNS.forEach { pattern ->
                    if (line.contains(pattern, ignoreCase = false)) {
                        violations.add(ViolationDetail(
                            pattern = pattern,
                            lineNumber = lineIndex + 1,
                            lineContent = line.trim()
                        ))
                    }
                }
            }
            
            if (violations.isNotEmpty()) {
                violatingFiles.add(file.relativePath to violations)
            }
        }

        // Then
        if (violatingFiles.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("Found Injekt.get() usage in ${violatingFiles.size} files:")
                violatingFiles.forEach { (filePath, violations) ->
                    appendLine("  $filePath:")
                    violations.forEach { violation ->
                        appendLine("    Line ${violation.lineNumber}: ${violation.lineContent}")
                        appendLine("      Pattern: ${violation.pattern}")
                    }
                }
                appendLine()
                appendLine("All Injekt.get() calls must be replaced with Hilt injection or EntryPoint access.")
            }
            fail(errorMessage)
        }
    }

    @Test
    fun `verify build configuration has no Injekt dependencies`() {
        // Given
        val buildFiles = getBuildFiles()
        val violatingFiles = mutableListOf<Pair<String, List<ViolationDetail>>>()
        
        val injektDependencyPatterns = listOf(
            "uy.kohesive.injekt",
            "injekt-core",
            "injekt-api",
            "implementation.*injekt",
            "api.*injekt",
            "compileOnly.*injekt"
        )

        // When
        buildFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()
            val violations = mutableListOf<ViolationDetail>()
            
            lines.forEachIndexed { lineIndex, line ->
                injektDependencyPatterns.forEach { pattern ->
                    if (line.contains(Regex(pattern), ignoreCase = true)) {
                        violations.add(ViolationDetail(
                            pattern = pattern,
                            lineNumber = lineIndex + 1,
                            lineContent = line.trim()
                        ))
                    }
                }
            }
            
            if (violations.isNotEmpty()) {
                violatingFiles.add(file.relativePath to violations)
            }
        }

        // Then
        if (violatingFiles.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("Found Injekt dependencies in ${violatingFiles.size} build files:")
                violatingFiles.forEach { (filePath, violations) ->
                    appendLine("  $filePath:")
                    violations.forEach { violation ->
                        appendLine("    Line ${violation.lineNumber}: ${violation.lineContent}")
                        appendLine("      Pattern: ${violation.pattern}")
                    }
                }
                appendLine()
                appendLine("All Injekt dependencies must be removed from build configuration.")
            }
            fail(errorMessage)
        }
    }

    @Test
    fun `verify proguard rules have no Injekt configurations`() {
        // Given
        val proguardFiles = getProguardFiles()
        val violatingFiles = mutableListOf<Pair<String, List<ViolationDetail>>>()
        
        val injektProguardPatterns = listOf(
            "uy.kohesive.injekt",
            "-keep.*injekt",
            "-dontwarn.*injekt"
        )

        // When
        proguardFiles.forEach { file ->
            val content = file.readText()
            val lines = content.lines()
            val violations = mutableListOf<ViolationDetail>()
            
            lines.forEachIndexed { lineIndex, line ->
                injektProguardPatterns.forEach { pattern ->
                    if (line.contains(Regex(pattern), ignoreCase = true)) {
                        violations.add(ViolationDetail(
                            pattern = pattern,
                            lineNumber = lineIndex + 1,
                            lineContent = line.trim()
                        ))
                    }
                }
            }
            
            if (violations.isNotEmpty()) {
                violatingFiles.add(file.relativePath to violations)
            }
        }

        // Then
        if (violatingFiles.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("Found Injekt proguard rules in ${violatingFiles.size} files:")
                violatingFiles.forEach { (filePath, violations) ->
                    appendLine("  $filePath:")
                    violations.forEach { violation ->
                        appendLine("    Line ${violation.lineNumber}: ${violation.lineContent}")
                        appendLine("      Pattern: ${violation.pattern}")
                    }
                }
                appendLine()
                appendLine("All Injekt-related proguard rules must be removed.")
            }
            fail(errorMessage)
        }
    }

    @Test
    fun `automated validation fails build if Injekt patterns are found`() {
        // This test combines all validation checks to provide a single point of failure
        // that can be used in CI/CD pipelines to prevent Injekt patterns from being reintroduced
        
        val allViolations = mutableListOf<String>()
        
        try {
            `verify no Injekt imports remain in codebase`()
        } catch (e: AssertionError) {
            allViolations.add("Injekt imports found: ${e.message}")
        }
        
        try {
            `verify no injectLazy usage remains in codebase`()
        } catch (e: AssertionError) {
            allViolations.add("injectLazy usage found: ${e.message}")
        }
        
        try {
            `verify no Injekt get usage remains in codebase`()
        } catch (e: AssertionError) {
            allViolations.add("Injekt.get usage found: ${e.message}")
        }
        
        try {
            `verify build configuration has no Injekt dependencies`()
        } catch (e: AssertionError) {
            allViolations.add("Injekt build dependencies found: ${e.message}")
        }
        
        try {
            `verify proguard rules have no Injekt configurations`()
        } catch (e: AssertionError) {
            allViolations.add("Injekt proguard rules found: ${e.message}")
        }
        
        if (allViolations.isNotEmpty()) {
            val errorMessage = buildString {
                appendLine("=== INJEKT CLEANUP VALIDATION FAILED ===")
                appendLine("Found ${allViolations.size} categories of violations:")
                allViolations.forEachIndexed { index, violation ->
                    appendLine("${index + 1}. $violation")
                }
                appendLine()
                appendLine("Build must fail until all Injekt patterns are removed.")
                appendLine("This ensures complete migration to Hilt dependency injection.")
            }
            fail(errorMessage)
        }
    }

    // Helper methods
    
    private fun getAllSourceFiles(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val projectRoot = getProjectRoot()
        
        SOURCE_DIRECTORIES.forEach { sourceDir ->
            val dirPath = Paths.get(projectRoot, sourceDir)
            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                    .filter { Files.isRegularFile(it) }
                    .filter { path -> ALL_EXTENSIONS.any { ext -> path.toString().endsWith(ext) } }
                    .forEach { path ->
                        val relativePath = Paths.get(projectRoot).relativize(path).toString()
                        files.add(FileInfo(path.toFile(), relativePath))
                    }
            }
        }
        
        return files
    }
    
    private fun getBuildFiles(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val projectRoot = getProjectRoot()
        
        val buildFilePatterns = listOf(
            "build.gradle",
            "build.gradle.kts",
            "app/build.gradle",
            "app/build.gradle.kts",
            "lib/build.gradle",
            "lib/build.gradle.kts"
        )
        
        buildFilePatterns.forEach { pattern ->
            val filePath = Paths.get(projectRoot, pattern)
            if (Files.exists(filePath)) {
                files.add(FileInfo(filePath.toFile(), pattern))
            }
        }
        
        return files
    }
    
    private fun getProguardFiles(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val projectRoot = getProjectRoot()
        
        val proguardFilePatterns = listOf(
            "app/proguard-rules.pro",
            "app/consumer-rules.pro",
            "lib/proguard-rules.pro",
            "lib/consumer-rules.pro"
        )
        
        proguardFilePatterns.forEach { pattern ->
            val filePath = Paths.get(projectRoot, pattern)
            if (Files.exists(filePath)) {
                files.add(FileInfo(filePath.toFile(), pattern))
            }
        }
        
        return files
    }
    
    private fun getProjectRoot(): String {
        // Find project root by looking for settings.gradle or build.gradle
        var currentDir = File(System.getProperty("user.dir"))
        
        while (currentDir != null) {
            if (File(currentDir, "settings.gradle").exists() || 
                File(currentDir, "settings.gradle.kts").exists()) {
                return currentDir.absolutePath
            }
            currentDir = currentDir.parentFile
        }
        
        // Fallback to current directory
        return System.getProperty("user.dir")
    }
    
    private data class FileInfo(
        val file: File,
        val relativePath: String
    )
    
    private data class ViolationDetail(
        val pattern: String,
        val lineNumber: Int,
        val lineContent: String
    )
}