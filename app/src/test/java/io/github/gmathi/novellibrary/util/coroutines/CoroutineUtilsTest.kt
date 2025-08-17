package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Unit tests for CoroutineUtils.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineUtilsTest {
    
    @Test
    fun `withRetry should succeed on first attempt`() = runTest {
        var attempts = 0
        
        val result = CoroutineUtils.withRetry {
            attempts++
            "success"
        }
        
        assertEquals("Should return correct result", "success", result)
        assertEquals("Should only attempt once", 1, attempts)
    }
    
    @Test
    fun `withRetry should retry on failure and eventually succeed`() = runTest {
        var attempts = 0
        
        val result = CoroutineUtils.withRetry(maxRetries = 3, initialDelay = 10.milliseconds) {
            attempts++
            if (attempts < 3) throw RuntimeException("Fail")
            "success"
        }
        
        assertEquals("Should return correct result", "success", result)
        assertEquals("Should attempt 3 times", 3, attempts)
    }
    
    @Test
    fun `withRetry should throw exception after max retries`() = runTest {
        var attempts = 0
        val exception = RuntimeException("Always fail")
        
        try {
            CoroutineUtils.withRetry(maxRetries = 2, initialDelay = 10.milliseconds) {
                attempts++
                throw exception
            }
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals("Should throw original exception", exception, e)
            assertEquals("Should attempt 3 times (initial + 2 retries)", 3, attempts)
        }
    }
    
    @Test
    fun `flowWithRetry should emit value on success`() = runTest {
        val flow = CoroutineUtils.flowWithRetry {
            "success"
        }
        
        val result = flow.first()
        assertEquals("Should emit correct value", "success", result)
    }
    
    @Test
    fun `CoroutineUtils should provide utility functions`() {
        // Test that the object exists and has the expected functions
        assertNotNull("CoroutineUtils should not be null", CoroutineUtils)
    }
    
    @Test
    fun `CoroutineDebouncer should debounce rapid calls`() = runTest {
        var executionCount = 0
        val debouncer = CoroutineUtils.CoroutineDebouncer(this, 100L)
        
        // Make rapid calls
        debouncer.debounce { executionCount++ }
        debouncer.debounce { executionCount++ }
        debouncer.debounce { executionCount++ }
        
        // Advance time to trigger debounced execution
        advanceTimeBy(150L)
        
        assertEquals("Should execute only once", 1, executionCount)
    }
    
    @Test
    fun `CoroutineDebouncer cancel should prevent execution`() = runTest {
        var executionCount = 0
        val debouncer = CoroutineUtils.CoroutineDebouncer(this, 100L)
        
        debouncer.debounce { executionCount++ }
        debouncer.cancel()
        
        advanceTimeBy(150L)
        
        assertEquals("Should not execute after cancel", 0, executionCount)
    }
}