package io.github.gmathi.novellibrary.di

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SourceModuleTest {

    @Test
    fun `SourceModule has correct annotations`() {
        // Given
        val moduleClass = SourceModule::class.java

        // Then
        assertTrue("SourceModule should have @Module annotation", 
            moduleClass.isAnnotationPresent(dagger.Module::class.java))
        assertTrue("SourceModule should have @InstallIn annotation", 
            moduleClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `SourceModule follows clean architecture principles`() {
        // Given
        val moduleClass = SourceModule::class.java

        // Then
        // Verify that the module doesn't have any @Provides methods since all dependencies
        // should use @Inject constructors according to the architecture guide
        val providesMethods = moduleClass.declaredMethods.filter { method ->
            method.isAnnotationPresent(dagger.Provides::class.java)
        }
        
        assertTrue("SourceModule should not have @Provides methods - dependencies should use @Inject constructors", 
            providesMethods.isEmpty())
    }

    @Test
    fun `SourceModule is properly configured for Hilt`() {
        // Given
        val moduleClass = SourceModule::class.java

        // Then
        assertTrue("SourceModule should be an object", moduleClass.kotlin.isObject)
        
        val installInAnnotation = moduleClass.getAnnotation(dagger.hilt.InstallIn::class.java)
        assertNotNull("SourceModule should have @InstallIn annotation", installInAnnotation)
        
        val components = installInAnnotation.value
        assertEquals("SourceModule should be installed in SingletonComponent", 
            1, components.size)
        assertEquals("SourceModule should target SingletonComponent", 
            dagger.hilt.components.SingletonComponent::class.java, components[0].java)
    }
}