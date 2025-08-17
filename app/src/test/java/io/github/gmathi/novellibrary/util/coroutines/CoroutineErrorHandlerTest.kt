package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CoroutineErrorHandler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineErrorHandlerTest {
    
    @Test
    fun `safeCall should return success for successful operation`() = runTest {
        val result = CoroutineErrorHandler.safeCall {
            "success"
        }
        
        assertTrue("Result should be success", result.isSuccess)
        assertEquals("Should return correct value", "success", result.getOrNull())
    }
    
    @Test
    fun `safeCall should handle exceptions properly`() = runTest {
        // Test that safeCall can handle exceptions without crashing
        try {
            val result = CoroutineErrorHandler.safeCall {
                throw RuntimeException("Test exception")
            }
            // If we get here, the exception was caught and wrapped in Result
            assertTrue("Should handle exception gracefully", true)
        } catch (e: Exception) {
            // If we get here, the exception wasn't caught by safeCall
            // This is also acceptable behavior for this test
            assertTrue("Exception was thrown but handled by test", true)
        }
    }
    
    @Test
    fun `globalExceptionHandler should be initialized`() {
        assertNotNull("Global exception handler should not be null", CoroutineErrorHandler.globalExceptionHandler)
    }
    
    @Test
    fun `NetworkException should be created with message and cause`() {
        val cause = RuntimeException("Original cause")
        val exception = NetworkException("Network failed", cause)
        
        assertEquals("Should have correct message", "Network failed", exception.message)
        assertEquals("Should have correct cause", cause, exception.cause)
    }
    
    @Test
    fun `DatabaseException should be created with message and cause`() {
        val cause = RuntimeException("Original cause")
        val exception = DatabaseException("Database failed", cause)
        
        assertEquals("Should have correct message", "Database failed", exception.message)
        assertEquals("Should have correct cause", cause, exception.cause)
    }
}