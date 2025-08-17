package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for the coroutine infrastructure components working together.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineInfrastructureIntegrationTest {
    
    private lateinit var coroutineScopes: CoroutineScopes
    private lateinit var dispatcherProvider: DispatcherProvider
    
    @Before
    fun setUp() {
        coroutineScopes = CoroutineScopes()
        dispatcherProvider = TestDispatcherProvider()
    }
    
    @Test
    fun `coroutine infrastructure should work together`() = runTest {
        var taskCompleted = false
        
        // Test that scopes and error handling work together
        val job = coroutineScopes.applicationScope.launch {
            val result = CoroutineErrorHandler.safeCall {
                delay(10) // Simulate some work
                "Task completed successfully"
            }
            
            if (result.isSuccess) {
                taskCompleted = true
            }
        }
        
        job.join()
        assertTrue("Task should complete successfully", taskCompleted)
    }
    
    @Test
    fun `dispatcher provider should provide different dispatchers`() {
        assertNotNull("Main dispatcher should not be null", dispatcherProvider.main)
        assertNotNull("IO dispatcher should not be null", dispatcherProvider.io)
        assertNotNull("Default dispatcher should not be null", dispatcherProvider.default)
        assertNotNull("Unconfined dispatcher should not be null", dispatcherProvider.unconfined)
    }
    
    @Test
    fun `coroutine scopes should be properly initialized`() {
        assertNotNull("Application scope should not be null", coroutineScopes.applicationScope)
        assertNotNull("IO scope should not be null", coroutineScopes.ioScope)
        assertNotNull("Background scope should not be null", coroutineScopes.backgroundScope)
        assertNotNull("Service scope should not be null", coroutineScopes.serviceScope)
    }
    
    @Test
    fun `error handling should work with retry utilities`() = runTest {
        var attempts = 0
        
        val result = CoroutineUtils.withRetry(maxRetries = 3) {
            attempts++
            if (attempts < 2) throw RuntimeException("Temporary failure")
            "Success after retry"
        }
        
        assertEquals("Should return success result", "Success after retry", result)
        assertEquals("Should attempt twice", 2, attempts)
    }
}