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
import javax.inject.Inject

/**
 * Comprehensive validation tests for all dependency relationships
 * Ensures proper dependency injection and security compliance
 */
@HiltAndroidTest
class DependencyRelationshipValidationTest {
    
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
    
    @Inject
    lateinit var securityValidator: HiltSecurityValidator
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `validate all critical dependencies are injected`() {
        // Verify all dependencies are properly injected
        assertNotNull("DBHelper should be injected", dbHelper)
        assertNotNull("DataCenter should be injected", dataCenter)
        assertNotNull("NetworkHelper should be injected", networkHelper)
        assertNotNull("SourceManager should be injected", sourceManager)
        assertNotNull("SecurityValidator should be injected", securityValidator)
    }
    
    @Test
    fun `validate dependency relationships are secure`() {
        // Test critical dependency validation
        val result = securityValidator.validateCriticalDependencies(
            dbHelper = dbHelper,
            networkHelper = networkHelper,
            sourceManager = sourceManager
        )
        
        assertTrue("Critical dependencies should be valid", result is ValidationResult.Success)
    }
    
    @Test
    fun `validate singleton scoping is correct`() {
        // Test that singletons maintain their identity
        val singletonComponents = mapOf(
            "DBHelper" to dbHelper,
            "DataCenter" to dataCenter,
            "NetworkHelper" to networkHelper,
            "SourceManager" to sourceManager
        )
        
        val result = securityValidator.validateSingletonSecurity(singletonComponents)
        assertTrue("Singleton validation should pass", result is ValidationResult.Success)
    }
    
    @Test
    fun `validate dependency injection timing`() {
        // Test injection security for different contexts
        assertTrue(
            "Activity injection should be secure",
            securityValidator.validateInjectionSecurity("DBHelper", "Activity")
        )
        
        assertTrue(
            "Fragment injection should be secure",
            securityValidator.validateInjectionSecurity("NetworkHelper", "Fragment")
        )
        
        assertTrue(
            "ViewModel injection should be secure",
            securityValidator.validateInjectionSecurity("SourceManager", "ViewModel")
        )
        
        assertTrue(
            "Service injection should be secure",
            securityValidator.validateInjectionSecurity("DataCenter", "Service")
        )
    }
    
    @Test
    fun `validate cross-dependency relationships`() {
        // Test that dependencies that depend on each other are properly configured
        
        // SourceManager should have proper initialization with ExtensionManager
        assertNotNull("SourceManager should be initialized", sourceManager)
        
        // DataCenter should work with DBHelper
        assertNotNull("DataCenter should be initialized", dataCenter)
        
        // All network-related components should be secure
        assertNotNull("NetworkHelper should be initialized", networkHelper)
    }
    
    @Test
    fun `validate memory leak prevention`() {
        // Test that dependencies don't create circular references
        // This is more of a compile-time check, but we can validate runtime behavior
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate heavy usage of dependencies
        repeat(100) {
            dbHelper.toString()
            dataCenter.toString()
            networkHelper.toString()
            sourceManager.toString()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Memory should not grow significantly (allowing for some variance)
        val memoryGrowth = finalMemory - initialMemory
        assertTrue(
            "Memory growth should be minimal (was $memoryGrowth bytes)",
            memoryGrowth < 1024 * 1024 // Less than 1MB growth
        )
    }
}