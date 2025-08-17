package io.github.gmathi.novellibrary.util.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class RetryUtilsTest {

    @Test
    fun `withRetry should succeed on first attempt`() = runTest {
        val expectedResult = "success"
        var attempts = 0
        
        val result = RetryUtils.withRetry(maxRetries = 3) {
            attempts++
            expectedResult
        }
        
        assertEquals("Should return expected result", expectedResult, result)
        assertEquals("Should make only 1 attempt", 1, attempts)
    }

    @Test
    fun `withRetry should succeed after retries`() = runTest {
        val expectedResult = "success"
        var attempts = 0
        
        val result = RetryUtils.withRetry(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                throw IOException("Network error")
            }
            expectedResult
        }
        
        assertEquals("Should return expected result", expectedResult, result)
        assertEquals("Should make 3 attempts", 3, attempts)
    }

    @Test
    fun `withRetry should throw exception after max retries`() = runTest {
        var attempts = 0
        
        try {
            RetryUtils.withRetry(maxRetries = 2) {
                attempts++
                throw IOException("Network error")
            }
            fail("Should have thrown exception")
        } catch (e: IOException) {
            assertEquals("Should make 3 attempts (initial + 2 retries)", 3, attempts)
            assertEquals("Should have correct message", "Network error", e.message)
        }
    }

    @Test
    fun `withRetry should respect shouldRetry condition`() = runTest {
        var attempts = 0
        
        try {
            RetryUtils.withRetry(
                maxRetries = 3,
                shouldRetry = { false } // Never retry
            ) {
                attempts++
                throw IOException("Network error")
            }
            fail("Should have thrown exception")
        } catch (e: IOException) {
            assertEquals("Should make only 1 attempt", 1, attempts)
        }
    }

    @Test
    fun `withRetry should use custom retry strategy`() = runTest {
        var attempts = 0
        val delays = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()
        
        try {
            RetryUtils.withRetry(
                maxRetries = 2,
                retryStrategy = { attempt ->
                    val delay = 100L * attempt
                    delays.add(delay)
                    delay
                }
            ) {
                attempts++
                throw IOException("Network error")
            }
            fail("Should have thrown exception")
        } catch (e: IOException) {
            assertEquals("Should make 3 attempts (initial + 2 retries)", 3, attempts)
            assertEquals("Should use custom delays", listOf(100L, 200L), delays)
        }
    }

    @Test
    fun `flowWithRetry should succeed on first attempt`() = runTest {
        val expectedResult = "success"
        var attempts = 0
        
        val result = RetryUtils.flowWithRetry(maxRetries = 3) {
            attempts++
            expectedResult
        }.first()
        
        assertEquals("Should return expected result", expectedResult, result)
        assertEquals("Should make only 1 attempt", 1, attempts)
    }

    @Test
    fun `flowWithRetry should succeed after retries`() = runTest {
        val expectedResult = "success"
        var attempts = 0
        
        val result = RetryUtils.flowWithRetry(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                throw NetworkException("Network error", IOException())
            }
            expectedResult
        }.first()
        
        assertEquals("Should return expected result", expectedResult, result)
        assertEquals("Should make 3 attempts", 3, attempts)
    }

    @Test
    fun `RetryStrategies fixedDelay should return constant delay`() {
        val strategy = RetryUtils.RetryStrategies.fixedDelay(1000L)
        
        assertEquals("Should return fixed delay", 1000L, strategy(1))
        assertEquals("Should return fixed delay", 1000L, strategy(5))
        assertEquals("Should return fixed delay", 1000L, strategy(10))
    }

    @Test
    fun `RetryStrategies linearBackoff should increase linearly`() {
        val strategy = RetryUtils.RetryStrategies.linearBackoff(1000L)
        
        assertEquals("Should return linear delay", 1000L, strategy(1))
        assertEquals("Should return linear delay", 2000L, strategy(2))
        assertEquals("Should return linear delay", 3000L, strategy(3))
    }

    @Test
    fun `RetryStrategies exponentialBackoff should double each time`() {
        val strategy = RetryUtils.RetryStrategies.exponentialBackoff(1000L, 10000L)
        
        assertEquals("Should return exponential delay", 1000L, strategy(1))
        assertEquals("Should return exponential delay", 2000L, strategy(2))
        assertEquals("Should return exponential delay", 4000L, strategy(3))
        assertEquals("Should return exponential delay", 8000L, strategy(4))
        assertEquals("Should respect max delay", 10000L, strategy(5))
    }

    @Test
    fun `RetryStrategies fibonacciBackoff should follow Fibonacci sequence`() {
        val strategy = RetryUtils.RetryStrategies.fibonacciBackoff(1000L)
        
        assertEquals("Should return Fibonacci delay", 1000L, strategy(1))
        assertEquals("Should return Fibonacci delay", 1000L, strategy(2))
        assertEquals("Should return Fibonacci delay", 2000L, strategy(3))
        assertEquals("Should return Fibonacci delay", 3000L, strategy(4))
        assertEquals("Should return Fibonacci delay", 5000L, strategy(5))
        assertEquals("Should return Fibonacci delay", 8000L, strategy(6))
    }

    @Test
    fun `RetryConditions networkErrors should identify network exceptions`() {
        val condition = RetryUtils.RetryConditions.networkErrors
        
        assertTrue("Should retry on NetworkException", condition(NetworkException("error", IOException())))
        assertTrue("Should retry on SocketTimeoutException", condition(SocketTimeoutException("timeout")))
        assertTrue("Should retry on UnknownHostException", condition(UnknownHostException("unknown host")))
        assertTrue("Should retry on IOException", condition(IOException("io error")))
        assertFalse("Should not retry on RuntimeException", condition(RuntimeException("runtime error")))
    }

    @Test
    fun `RetryConditions serverErrors should identify server errors`() {
        val condition = RetryUtils.RetryConditions.serverErrors
        
        assertTrue("Should retry on ServerException", 
            condition(io.github.gmathi.novellibrary.network.api.ServerException("server error", 500)))
        assertTrue("Should retry on 5xx ApiException", 
            condition(io.github.gmathi.novellibrary.network.api.ApiException("server error", 500)))
        assertFalse("Should not retry on 4xx ApiException", 
            condition(io.github.gmathi.novellibrary.network.api.ApiException("client error", 400)))
        assertTrue("Should retry on network errors", condition(NetworkException("error", IOException())))
    }

    @Test
    fun `RetryConditions transientErrors should identify transient errors`() {
        val condition = RetryUtils.RetryConditions.transientErrors
        
        assertTrue("Should retry on RateLimitException", 
            condition(io.github.gmathi.novellibrary.network.api.RateLimitException("rate limit", 429)))
        assertTrue("Should retry on SocketTimeoutException", condition(SocketTimeoutException("timeout")))
        assertFalse("Should not retry on UnknownHostException", condition(UnknownHostException("unknown host")))
        assertFalse("Should not retry on RuntimeException", condition(RuntimeException("runtime error")))
    }
}