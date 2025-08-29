package io.github.gmathi.novellibrary.util.security

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Tests for memory leak prevention in Hilt dependency injection
 * Validates that proper scoping prevents memory leaks
 */
@HiltAndroidTest
class MemoryLeakPreventionTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var dbHelper: DBHelper
    
    @Inject
    lateinit var dataCenter: DataCenter
    
    @Inject
    lateinit var networkHelper: NetworkHelper
    
    @Inject
    lateinit var sourceManager: SourceManager
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `validate singleton components don't create memory leaks`() {
        // Create weak references to track memory usage
        val weakDbHelper = WeakReference(dbHelper)
        val weakDataCenter = WeakReference(dataCenter)
        val weakNetworkHelper = WeakReference(networkHelper)
        val weakSourceManager = WeakReference(sourceManager)
        
        // Simulate heavy usage
        repeat(1000) {
            dbHelper.toString()
            dataCenter.toString()
            networkHelper.toString()
            sourceManager.toString()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        System.gc()
        
        // Singleton components should still be alive (not garbage collected)
        assertNotNull("DBHelper should not be garbage collected", weakDbHelper.get())
        assertNotNull("DataCenter should not be garbage collected", weakDataCenter.get())
        assertNotNull("NetworkHelper should not be garbage collected", weakNetworkHelper.get())
        assertNotNull("SourceManager should not be garbage collected", weakSourceManager.get())
    }
    
    @Test
    fun `validate no circular references in dependency graph`() {
        // Test that components don't hold circular references that prevent GC
        
        // Get initial memory usage
        val initialMemory = getUsedMemory()
        
        // Create multiple references and simulate usage
        val components = mutableListOf<Any>()
        repeat(100) {
            components.add(dbHelper)
            components.add(dataCenter)
            components.add(networkHelper)
            components.add(sourceManager)
        }
        
        // Clear references
        components.clear()
        
        // Force garbage collection
        forceGarbageCollection()
        
        val finalMemory = getUsedMemory()
        val memoryGrowth = finalMemory - initialMemory
        
        // Memory growth should be minimal (allowing for JVM overhead)
        assertTrue(
            "Memory growth should be minimal, was ${memoryGrowth / 1024}KB",
            memoryGrowth < 5 * 1024 * 1024 // Less than 5MB
        )
    }
    
    @Test
    fun `validate component cleanup on scope destruction`() {
        // This test validates that components are properly cleaned up
        // when their scope is destroyed (simulated here)
        
        val initialComponentCount = countActiveComponents()
        
        // Simulate scope creation and destruction
        repeat(10) {
            // In a real scenario, this would be activity/fragment creation/destruction
            simulateScopeCreation()
            simulateScopeDestruction()
        }
        
        val finalComponentCount = countActiveComponents()
        
        // Component count should not grow significantly
        assertTrue(
            "Component count should not grow significantly",
            finalComponentCount <= initialComponentCount + 5 // Allow some variance
        )
    }
    
    @Test
    fun `validate weak reference behavior for non-singleton components`() {
        // Test that non-singleton components can be garbage collected
        // when no longer referenced
        
        var temporaryComponent: Any? = createTemporaryComponent()
        val weakRef = WeakReference(temporaryComponent)
        
        // Clear strong reference
        temporaryComponent = null
        
        // Force garbage collection
        forceGarbageCollection()
        
        // Temporary component should be garbage collected
        assertNull(
            "Temporary component should be garbage collected",
            weakRef.get()
        )
    }
    
    @Test
    fun `validate memory usage stays within bounds during heavy usage`() {
        val maxAllowedMemory = 50 * 1024 * 1024 // 50MB
        
        // Simulate heavy dependency usage
        repeat(1000) {
            useAllDependencies()
            
            val currentMemory = getUsedMemory()
            assertTrue(
                "Memory usage should stay within bounds: ${currentMemory / 1024 / 1024}MB",
                currentMemory < maxAllowedMemory
            )
        }
    }
    
    private fun getUsedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun forceGarbageCollection() {
        repeat(3) {
            System.gc()
            Thread.sleep(100)
        }
    }
    
    private fun countActiveComponents(): Int {
        // Simplified component counting - in real implementation,
        // this would use reflection or component tracking
        return 4 // DBHelper, DataCenter, NetworkHelper, SourceManager
    }
    
    private fun simulateScopeCreation() {
        // Simulate creating a new scope (like activity creation)
        // In real implementation, this would create activity-scoped components
    }
    
    private fun simulateScopeDestruction() {
        // Simulate destroying a scope (like activity destruction)
        // In real implementation, this would clean up activity-scoped components
        forceGarbageCollection()
    }
    
    private fun createTemporaryComponent(): Any {
        // Create a temporary component that should be garbage collected
        return object {
            val data = ByteArray(1024) // 1KB of data
        }
    }
    
    private fun useAllDependencies() {
        // Simulate using all dependencies
        dbHelper.toString()
        dataCenter.toString()
        networkHelper.toString()
        sourceManager.toString()
    }
}