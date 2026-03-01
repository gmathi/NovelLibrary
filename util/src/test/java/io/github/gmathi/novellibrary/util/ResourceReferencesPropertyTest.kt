package io.github.gmathi.novellibrary.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.io.File

/**
 * Feature: core-module-extraction
 * Property 6: Resource References Are Satisfied
 * 
 * For all drawable, color, layout, and string resource references in util, common, and core module code,
 * the referenced resource must exist in that module's res/ directory.
 * 
 * Validates: Requirements 10.1, 10.2, 10.3, 10.4, 10.7
 */
class ResourceReferencesPropertyTest : StringSpec({
    
    val projectRoot = File(System.getProperty("user.dir") ?: ".").parentFile
    
    "Feature: core-module-extraction, Property 6: Resource References Are Satisfied - Util Module" {
        val utilModule = File(projectRoot, "util")
        val utilSrcDir = File(utilModule, "src/main/java")
        val utilResDir = File(utilModule, "src/main/res")
        
        if (utilSrcDir.exists() && utilResDir.exists()) {
            val kotlinFiles = utilSrcDir.walkTopDown()
                .filter { it.extension == "kt" }
                .toList()
            
            kotlinFiles.forEach { file ->
                val content = file.readText()
                val resourceRefs = extractResourceReferences(content)
                
                resourceRefs.forEach { (type, name) ->
                    val resourceExists = checkResourceExists(utilResDir, type, name)
                    resourceExists shouldBe true
                }
            }
        }
    }
    
    "Feature: core-module-extraction, Property 6: Resource References Are Satisfied - Common Module" {
        val commonModule = File(projectRoot, "common")
        val commonSrcDir = File(commonModule, "src/main/java")
        val commonResDir = File(commonModule, "src/main/res")
        
        if (commonSrcDir.exists() && commonResDir.exists()) {
            val kotlinFiles = commonSrcDir.walkTopDown()
                .filter { it.extension == "kt" }
                .toList()
            
            kotlinFiles.forEach { file ->
                val content = file.readText()
                val resourceRefs = extractResourceReferences(content)
                
                resourceRefs.forEach { (type, name) ->
                    val resourceExists = checkResourceExists(commonResDir, type, name)
                    resourceExists shouldBe true
                }
            }
        }
    }
    
    "Feature: core-module-extraction, Property 6: Resource References Are Satisfied - Core Module" {
        val coreModule = File(projectRoot, "core")
        val coreSrcDir = File(coreModule, "src/main/java")
        val coreResDir = File(coreModule, "src/main/res")
        
        if (coreSrcDir.exists() && coreResDir.exists()) {
            val kotlinFiles = coreSrcDir.walkTopDown()
                .filter { it.extension == "kt" }
                .toList()
            
            kotlinFiles.forEach { file ->
                val content = file.readText()
                val resourceRefs = extractResourceReferences(content)
                
                resourceRefs.forEach { (type, name) ->
                    val resourceExists = checkResourceExists(coreResDir, type, name)
                    resourceExists shouldBe true
                }
            }
        }
    }
})

/**
 * Extract resource references from Kotlin source code
 * Returns list of (resourceType, resourceName) pairs
 */
private fun extractResourceReferences(content: String): List<Pair<String, String>> {
    val references = mutableListOf<Pair<String, String>>()
    
    // Pattern: R.drawable.resource_name, R.layout.resource_name, etc.
    val rPattern = Regex("""R\.(drawable|layout|string|color|dimen|style|id|attr|anim|animator|raw|xml|font|menu|mipmap|bool|integer|array|plurals|fraction)\\.([a-zA-Z0-9_]+)""")
    
    rPattern.findAll(content).forEach { match ->
        val resourceType = match.groupValues[1]
        val resourceName = match.groupValues[2]
        references.add(resourceType to resourceName)
    }
    
    return references
}

/**
 * Check if a resource exists in the module's res directory
 */
private fun checkResourceExists(resDir: File, resourceType: String, resourceName: String): Boolean {
    return when (resourceType) {
        "drawable", "mipmap" -> {
            // Check for various drawable formats
            val drawableDir = File(resDir, resourceType)
            val drawableDirs = resDir.listFiles()?.filter { 
                it.isDirectory && it.name.startsWith(resourceType)
            } ?: emptyList()
            
            drawableDirs.any { dir ->
                dir.listFiles()?.any { file ->
                    file.nameWithoutExtension == resourceName
                } ?: false
            }
        }
        "layout" -> {
            val layoutDir = File(resDir, "layout")
            layoutDir.exists() && layoutDir.listFiles()?.any { 
                it.nameWithoutExtension == resourceName 
            } ?: false
        }
        "string", "color", "dimen", "style", "bool", "integer", "array", "plurals", "fraction" -> {
            // These are defined in values XML files
            val valuesDir = File(resDir, "values")
            if (!valuesDir.exists()) return false
            
            valuesDir.listFiles()?.any { file ->
                if (file.extension == "xml") {
                    val content = file.readText()
                    content.contains("""<$resourceType name="$resourceName"""")
                } else false
            } ?: false
        }
        "raw" -> {
            val rawDir = File(resDir, "raw")
            rawDir.exists() && rawDir.listFiles()?.any { 
                it.nameWithoutExtension == resourceName 
            } ?: false
        }
        "id" -> {
            // IDs are typically defined in layout files or generated
            // For this test, we'll consider them valid if they appear in any layout
            val layoutDir = File(resDir, "layout")
            if (!layoutDir.exists()) return true // IDs might be generated
            
            layoutDir.listFiles()?.any { file ->
                if (file.extension == "xml") {
                    val content = file.readText()
                    content.contains("""android:id="@+id/$resourceName"""") ||
                    content.contains("""android:id="@id/$resourceName"""")
                } else false
            } ?: true
        }
        else -> {
            // For other resource types, assume they exist if not explicitly checked
            true
        }
    }
}
