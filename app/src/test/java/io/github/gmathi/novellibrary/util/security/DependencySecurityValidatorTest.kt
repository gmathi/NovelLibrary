package io.github.gmathi.novellibrary.util.security

import android.app.Application
import android.content.Context
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DependencySecurityValidator
 * Tests security validation logic for dependency injection
 */
class DependencySecurityValidatorTest {
    
    private lateinit var validator: DependencySecurityValidator
    
    @Before
    fun setup() {
        validator = DependencySecurityValidator()
    }
    
    @Test
    fun `validateApplicationContext should pass with Application context`() {
        // Given
        val applicationContext = mockk<Application>()
        
        // When & Then - should not throw
        validator.validateApplicationContext(applicationContext)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `validateApplicationContext should fail with non-Application context`() {
        // Given
        val context = mockk<Context>()
        
        // When & Then - should throw
        validator.validateApplicationContext(context)
    }
    
    @Test
    fun `validateNetworkConfiguration should pass with secure settings`() {
        // When & Then - should not throw
        validator.validateNetworkConfiguration(
            allowHttps = true,
            allowHttp = false,
            requireCertificatePinning = true
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `validateNetworkConfiguration should fail when HTTPS is disabled`() {
        // When & Then - should throw
        validator.validateNetworkConfiguration(
            allowHttps = false,
            allowHttp = true
        )
    }
    
    @Test
    fun `validateDatabaseSecurity should pass with secure settings`() {
        // When & Then - should not throw
        validator.validateDatabaseSecurity(
            encryptionEnabled = true,
            allowExternalAccess = false
        )
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `validateDatabaseSecurity should fail with external access enabled`() {
        // When & Then - should throw
        validator.validateDatabaseSecurity(
            encryptionEnabled = true,
            allowExternalAccess = true
        )
    }
    
    @Test
    fun `validateDependency should return dependency when not null`() {
        // Given
        val dependency = "test-dependency"
        
        // When
        val result = validator.validateDependency(dependency, "TestDependency")
        
        // Then
        assertEquals(dependency, result)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `validateDependency should fail when dependency is null`() {
        // When & Then - should throw
        validator.validateDependency(null, "TestDependency")
    }
    
    @Test
    fun `validateInjectionTiming should pass when component is initialized`() {
        // When & Then - should not throw
        validator.validateInjectionTiming("TestComponent", true)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `validateInjectionTiming should fail when component is not initialized`() {
        // When & Then - should throw
        validator.validateInjectionTiming("TestComponent", false)
    }
}