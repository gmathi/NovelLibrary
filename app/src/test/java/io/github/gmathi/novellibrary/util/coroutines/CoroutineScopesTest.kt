package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CoroutineScopes.
 */
class CoroutineScopesTest {
    
    private lateinit var coroutineScopes: CoroutineScopes
    
    @Before
    fun setUp() {
        coroutineScopes = CoroutineScopes()
    }
    
    @Test
    fun `applicationScope should be initialized`() {
        val scope = coroutineScopes.applicationScope
        assertNotNull("Application scope should not be null", scope)
        assertNotNull("Application scope should have a job", scope.coroutineContext[Job])
    }
    
    @Test
    fun `ioScope should be initialized`() {
        val scope = coroutineScopes.ioScope
        assertNotNull("IO scope should not be null", scope)
        assertNotNull("IO scope should have a job", scope.coroutineContext[Job])
    }
    
    @Test
    fun `backgroundScope should be initialized`() {
        val scope = coroutineScopes.backgroundScope
        assertNotNull("Background scope should not be null", scope)
        assertNotNull("Background scope should have a job", scope.coroutineContext[Job])
    }
    
    @Test
    fun `serviceScope should be initialized`() {
        val scope = coroutineScopes.serviceScope
        assertNotNull("Service scope should not be null", scope)
        assertNotNull("Service scope should have a job", scope.coroutineContext[Job])
    }
    
    @Test
    fun `all scopes should be different instances`() {
        assertNotSame("Application and IO scopes should be different", 
            coroutineScopes.applicationScope, coroutineScopes.ioScope)
        assertNotSame("Application and background scopes should be different", 
            coroutineScopes.applicationScope, coroutineScopes.backgroundScope)
        assertNotSame("Application and service scopes should be different", 
            coroutineScopes.applicationScope, coroutineScopes.serviceScope)
        assertNotSame("IO and background scopes should be different", 
            coroutineScopes.ioScope, coroutineScopes.backgroundScope)
    }
}