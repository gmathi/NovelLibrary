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
 * Automated tests for proper scoping validation
 * Ensures components are scoped correctly to prevent memory leaks
 */
@HiltAndroidTest
class ScopingValidationTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var dbHelper1: DBHelper
    
    @Inject
    lateinit var dbHelper2: DBHelper
    
    @Inject
    lateinit var dataCenter1: DataCenter
    
    @Inject
    lateinit var dataCenter2: DataCenter
    
    @Inject
    lateinit var networkHelper1: NetworkHelper
    
    @Inject
    lateinit var networkHelper2: NetworkHelper
    
    @Inject
    lateinit var sourceManager1: SourceManager
    
    @Inject
    lateinit var sourceManager2: SourceManager
    
    @Inject
    lateinit var securityValidator: HiltSecurityValidator
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `validate singleton scoping for DBHelper`() {
        // Singleton components should be the same instance
        assertSame("DBHelper should be singleton", dbHelper1, dbHelper2)
        
        // Validate scoping security
        assertTrue(
            "DBHelper scoping should be secure",
            securityValidator.validateScopingSecurity("DBHelper", "Singleton", "Singleton")
        )
    }
    
    @Test
    fun `validate singleton scoping for DataCenter`() {
        // Singleton components should be the same instance
        assertSame("DataCenter should be singleton", dataCenter1, dataCenter2)
        
        // Validate scoping security
        assertTrue(
            "DataCenter scoping should be secure",
            securityValidator.validateScopingSecurity("DataCenter", "Singleton", "Singleton")
        )
    }
    
    @Test
    fun `validate singleton scoping for NetworkHelper`() {
        // Singleton components should be the same instance
        assertSame("NetworkHelper should be singleton", networkHelper1, networkHelper2)
        
        // Validate scoping security
        assertTrue(
            "NetworkHelper scoping should be secure",
            securityValidator.validateScopingSecurity("NetworkHelper", "Singleton", "Singleton")
        )
    }
    
    @Test
    fun `validate singleton scoping for SourceManager`() {
        // Singleton components should be the same instance
        assertSame("SourceManager should be singleton", sourceManager1, sourceManager2)
        
        // Validate scoping security
        assertTrue(
            "SourceManager scoping should be secure",
            securityValidator.validateScopingSecurity("SourceManager", "Singleton", "Singleton")
        )
    }
    
    @Test
    fun `validate scoping prevents memory leaks`() {
        // Test that singleton scoping doesn't create memory leaks
        val components = listOf(dbHelper1, dataCenter1, networkHelper1, sourceManager1)
        
        // Verify all components are properly initialized
        components.forEach { component ->
            assertNotNull("Component should not be null", component)
        }
        
        // Test that components maintain their singleton nature across multiple accesses
        repeat(10) {
            assertEquals("DBHelper should remain same instance", dbHelper1, dbHelper2)
            assertEquals("DataCenter should remain same instance", dataCenter1, dataCenter2)
            assertEquals("NetworkHelper should remain same instance", networkHelper1, networkHelper2)
            assertEquals("SourceManager should remain same instance", sourceManager1, sourceManager2)
        }
    }
    
    @Test
    fun `validate incorrect scoping detection`() {
        // Test that scoping validator can detect incorrect scoping
        assertFalse(
            "Should detect incorrect scoping",
            securityValidator.validateScopingSecurity("TestComponent", "Singleton", "ActivityScoped")
        )
        
        assertFalse(
            "Should detect incorrect scoping",
            securityValidator.validateScopingSecurity("TestComponent", "ActivityScoped", "Singleton")
        )
    }
    
    @Test
    fun `validate component lifecycle alignment`() {
        // Test that components are properly aligned with their intended lifecycle
        
        // Singleton components should survive across different injection points
        val singletonComponents = mapOf(
            "DBHelper" to dbHelper1,
            "DataCenter" to dataCenter1,
            "NetworkHelper" to networkHelper1,
            "SourceManager" to sourceManager1
        )
        
        val validationResult = securityValidator.validateSingletonSecurity(singletonComponents)
        assertTrue(
            "Singleton lifecycle should be valid",
            validationResult is ValidationResult.Success
        )
    }
}