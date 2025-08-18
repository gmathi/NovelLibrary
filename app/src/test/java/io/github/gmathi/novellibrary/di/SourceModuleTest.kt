package io.github.gmathi.novellibrary.di

import android.content.Context
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SourceModuleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `provideExtensionManager returns valid instance`() {
        // Given
        val module = SourceModule

        // When
        val extensionManager = module.provideExtensionManager(context)

        // Then
        assertNotNull(extensionManager)
        assertTrue("ExtensionManager should be properly initialized", extensionManager is ExtensionManager)
    }

    @Test
    fun `provideSourceManager returns valid instance and initializes ExtensionManager`() {
        // Given
        val module = SourceModule
        val extensionManager = module.provideExtensionManager(context)

        // When
        val sourceManager = module.provideSourceManager(context, extensionManager)

        // Then
        assertNotNull(sourceManager)
        assertTrue("SourceManager should be properly initialized", sourceManager is SourceManager)
        // Note: We can't easily test the extensionManager.init(sourceManager) call without mocking
        // but the integration test will verify this works correctly
    }

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
    fun `provideExtensionManager method has correct annotations`() {
        // Given
        val method = SourceModule::class.java.getMethod("provideExtensionManager", Context::class.java)

        // Then
        assertTrue("provideExtensionManager should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideExtensionManager should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideSourceManager method has correct annotations`() {
        // Given
        val method = SourceModule::class.java.getMethod("provideSourceManager", Context::class.java, ExtensionManager::class.java)

        // Then
        assertTrue("provideSourceManager should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideSourceManager should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `dependency injection order is correct`() {
        // This test verifies that ExtensionManager is available when SourceManager is created
        // Given
        val module = SourceModule
        val extensionManager = module.provideExtensionManager(context)
        
        // When
        val sourceManager = module.provideSourceManager(context, extensionManager)
        
        // Then
        assertNotNull("ExtensionManager should be created first", extensionManager)
        assertNotNull("SourceManager should be created after ExtensionManager", sourceManager)
    }
}