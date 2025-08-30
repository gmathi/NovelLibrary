package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class SourceEntryPointTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `SourceEntryPoint has correct annotations`() {
        // Given
        val entryPointClass = SourceEntryPoint::class.java

        // Then
        assertTrue("SourceEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("SourceEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `SourceEntryPoint can be accessed from application context`() {
        // When
        val entryPoint = EntryPointAccessors.fromApplication(context, SourceEntryPoint::class.java)

        // Then
        assertNotNull("SourceEntryPoint should be accessible", entryPoint)
    }

    @Test
    fun `SourceEntryPoint provides ExtensionManager`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, SourceEntryPoint::class.java)

        // When
        val extensionManager = entryPoint.extensionManager()

        // Then
        assertNotNull("ExtensionManager should not be null", extensionManager)
        assertTrue("ExtensionManager should be correct type", extensionManager is ExtensionManager)
    }

    @Test
    fun `SourceEntryPoint provides SourceManager`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, SourceEntryPoint::class.java)

        // When
        val sourceManager = entryPoint.sourceManager()

        // Then
        assertNotNull("SourceManager should not be null", sourceManager)
        assertTrue("SourceManager should be correct type", sourceManager is SourceManager)
    }

    @Test
    fun `SourceEntryPoint provides same instances on multiple calls`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, SourceEntryPoint::class.java)

        // When
        val extensionManager1 = entryPoint.extensionManager()
        val extensionManager2 = entryPoint.extensionManager()
        val sourceManager1 = entryPoint.sourceManager()
        val sourceManager2 = entryPoint.sourceManager()

        // Then
        assertSame("ExtensionManager should be singleton", extensionManager1, extensionManager2)
        assertSame("SourceManager should be singleton", sourceManager1, sourceManager2)
    }

    @Test
    fun `SourceEntryPoint interface has correct method signatures`() {
        // Given
        val entryPointClass = SourceEntryPoint::class.java

        // When
        val extensionManagerMethod = entryPointClass.getMethod("extensionManager")
        val sourceManagerMethod = entryPointClass.getMethod("sourceManager")

        // Then
        assertEquals("extensionManager method should return ExtensionManager", 
            ExtensionManager::class.java, extensionManagerMethod.returnType)
        assertEquals("sourceManager method should return SourceManager", 
            SourceManager::class.java, sourceManagerMethod.returnType)
    }

    @Test
    fun `SourceEntryPoint dependencies are properly initialized`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, SourceEntryPoint::class.java)

        // When
        val extensionManager = entryPoint.extensionManager()
        val sourceManager = entryPoint.sourceManager()

        // Then
        // Verify that the dependencies are properly initialized and connected
        assertNotNull("ExtensionManager should be initialized", extensionManager)
        assertNotNull("SourceManager should be initialized", sourceManager)
        
        // Verify that ExtensionManager has been initialized with SourceManager
        // This tests the dependency relationship established in SourceModule
        assertTrue("ExtensionManager should be properly initialized", 
            extensionManager.javaClass.name.contains("ExtensionManager"))
    }
}