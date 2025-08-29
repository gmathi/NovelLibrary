package io.github.gmathi.novellibrary.util.security

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SecureNetworkHelper
 * Tests security features of network helper wrapper
 */
class SecureNetworkHelperTest {
    
    private lateinit var secureNetworkHelper: SecureNetworkHelper
    private lateinit var mockContext: Application
    private lateinit var mockValidator: DependencySecurityValidator
    
    @Before
    fun setup() {
        mockContext = mockk<Application>()
        mockValidator = mockk<DependencySecurityValidator>(relaxed = true)
        secureNetworkHelper = SecureNetworkHelper(mockContext, mockValidator)
    }
    
    @Test
    fun `validateUrl should pass for valid HTTPS URL`() {
        // Given
        val validUrl = "https://example.com/api/novels"
        
        // When
        val result = secureNetworkHelper.validateUrl(validUrl)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `validateUrl should fail for HTTP URL`() {
        // Given
        val httpUrl = "http://example.com/api/novels"
        
        // When
        val result = secureNetworkHelper.validateUrl(httpUrl)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `validateUrl should fail for localhost URL`() {
        // Given
        val localhostUrl = "https://localhost:8080/api"
        
        // When
        val result = secureNetworkHelper.validateUrl(localhostUrl)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `validateUrl should fail for 127_0_0_1 URL`() {
        // Given
        val localUrl = "https://127.0.0.1:8080/api"
        
        // When
        val result = secureNetworkHelper.validateUrl(localUrl)
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `createSecureHttpClient should create client with security interceptors`() {
        // When
        val client = secureNetworkHelper.createSecureHttpClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.interceptors.isNotEmpty())
        assertNotNull(client.certificatePinner)
    }
    
    @Test
    fun `getNetworkHelper should validate injection timing`() {
        // When
        secureNetworkHelper.getNetworkHelper()
        
        // Then
        verify { mockValidator.validateInjectionTiming("NetworkHelper", any()) }
    }
}