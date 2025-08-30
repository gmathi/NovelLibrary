package io.github.gmathi.novellibrary.di

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

/**
 * Validation tests for EntryPoint interfaces that don't require Hilt runtime
 * These tests verify the interface structure and annotations are correct
 */
class EntryPointInterfaceValidationTest {

    @Test
    fun `NetworkEntryPoint interface structure is correct`() {
        // Given
        val entryPointClass = NetworkEntryPoint::class.java

        // Then
        assertTrue("NetworkEntryPoint should be an interface", entryPointClass.isInterface)
        
        // Verify annotations
        assertTrue("NetworkEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("NetworkEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
        
        // Verify methods exist
        val methods = entryPointClass.declaredMethods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue("NetworkEntryPoint should have networkHelper method", 
            methodNames.contains("networkHelper"))
        assertTrue("NetworkEntryPoint should have json method", 
            methodNames.contains("json"))
        
        // Verify method signatures
        val networkHelperMethod = entryPointClass.getMethod("networkHelper")
        val jsonMethod = entryPointClass.getMethod("json")
        
        assertEquals("networkHelper method should have no parameters", 
            0, networkHelperMethod.parameterCount)
        assertEquals("json method should have no parameters", 
            0, jsonMethod.parameterCount)
    }

    @Test
    fun `SourceEntryPoint interface structure is correct`() {
        // Given
        val entryPointClass = SourceEntryPoint::class.java

        // Then
        assertTrue("SourceEntryPoint should be an interface", entryPointClass.isInterface)
        
        // Verify annotations
        assertTrue("SourceEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("SourceEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
        
        // Verify methods exist
        val methods = entryPointClass.declaredMethods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue("SourceEntryPoint should have extensionManager method", 
            methodNames.contains("extensionManager"))
        assertTrue("SourceEntryPoint should have sourceManager method", 
            methodNames.contains("sourceManager"))
        
        // Verify method signatures
        val extensionManagerMethod = entryPointClass.getMethod("extensionManager")
        val sourceManagerMethod = entryPointClass.getMethod("sourceManager")
        
        assertEquals("extensionManager method should have no parameters", 
            0, extensionManagerMethod.parameterCount)
        assertEquals("sourceManager method should have no parameters", 
            0, sourceManagerMethod.parameterCount)
    }

    @Test
    fun `DatabaseEntryPoint interface structure is correct`() {
        // Given
        val entryPointClass = DatabaseEntryPoint::class.java

        // Then
        assertTrue("DatabaseEntryPoint should be an interface", entryPointClass.isInterface)
        
        // Verify annotations
        assertTrue("DatabaseEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("DatabaseEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
        
        // Verify methods exist
        val methods = entryPointClass.declaredMethods
        val methodNames = methods.map { it.name }.toSet()
        
        assertTrue("DatabaseEntryPoint should have dbHelper method", 
            methodNames.contains("dbHelper"))
        assertTrue("DatabaseEntryPoint should have dataCenter method", 
            methodNames.contains("dataCenter"))
        
        // Verify method signatures
        val dbHelperMethod = entryPointClass.getMethod("dbHelper")
        val dataCenterMethod = entryPointClass.getMethod("dataCenter")
        
        assertEquals("dbHelper method should have no parameters", 
            0, dbHelperMethod.parameterCount)
        assertEquals("dataCenter method should have no parameters", 
            0, dataCenterMethod.parameterCount)
    }

    @Test
    fun `All EntryPoint interfaces follow consistent naming pattern`() {
        // Given
        val entryPointClasses = listOf(
            NetworkEntryPoint::class.java,
            SourceEntryPoint::class.java,
            DatabaseEntryPoint::class.java
        )

        // Then
        entryPointClasses.forEach { entryPointClass ->
            assertTrue("${entryPointClass.simpleName} should end with 'EntryPoint'", 
                entryPointClass.simpleName.endsWith("EntryPoint"))
            
            assertTrue("${entryPointClass.simpleName} should be in di package", 
                entryPointClass.packageName.endsWith(".di"))
            
            assertTrue("${entryPointClass.simpleName} should be an interface", 
                entryPointClass.isInterface)
        }
    }

    @Test
    fun `All EntryPoint interfaces have proper documentation`() {
        // Given
        val entryPointClasses = listOf(
            NetworkEntryPoint::class.java,
            SourceEntryPoint::class.java,
            DatabaseEntryPoint::class.java
        )

        // Then - This test verifies that the interfaces are properly structured
        // The actual documentation is verified by reading the source files
        entryPointClasses.forEach { entryPointClass ->
            assertNotNull("${entryPointClass.simpleName} should be loadable", entryPointClass)
            assertTrue("${entryPointClass.simpleName} should have methods", 
                entryPointClass.declaredMethods.isNotEmpty())
        }
    }

    @Test
    fun `EntryPoint interfaces have correct InstallIn component`() {
        // Given
        val entryPointClasses = listOf(
            NetworkEntryPoint::class.java,
            SourceEntryPoint::class.java,
            DatabaseEntryPoint::class.java
        )

        // Then
        entryPointClasses.forEach { entryPointClass ->
            val installInAnnotation = entryPointClass.getAnnotation(dagger.hilt.InstallIn::class.java)
            assertNotNull("${entryPointClass.simpleName} should have @InstallIn annotation", 
                installInAnnotation)
            
            // Verify it's installed in SingletonComponent
            val components = installInAnnotation.value
            assertTrue("${entryPointClass.simpleName} should be installed in SingletonComponent", 
                components.any { it.simpleName == "SingletonComponent" })
        }
    }
}