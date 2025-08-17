package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.util.coroutines.NetworkException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorHandlerTest {

    @Test
    fun `handleResponse should return body for successful response`() {
        val responseBody = "test data"
        val response = Response.success(responseBody)
        
        val result = ApiErrorHandler.handleResponse(response)
        
        assertEquals("Should return response body", responseBody, result)
    }

    @Test
    fun `handleResponse should throw ApiException for empty body`() {
        val response = Response.success<String>(null)
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown ApiException")
        } catch (e: ApiException) {
            assertEquals("Should have correct message", "Empty response body", e.message)
            assertEquals("Should have correct code", 200, e.code)
        }
    }

    @Test
    fun `handleResponse should throw NotFoundException for 404`() {
        val response = Response.error<String>(404, "Not Found".toResponseBody())
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown NotFoundException")
        } catch (e: NotFoundException) {
            assertEquals("Should have correct message", "Resource not found", e.message)
            assertEquals("Should have correct code", 404, e.code)
        }
    }

    @Test
    fun `handleResponse should throw UnauthorizedException for 401`() {
        val response = Response.error<String>(401, "Unauthorized".toResponseBody())
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown UnauthorizedException")
        } catch (e: UnauthorizedException) {
            assertEquals("Should have correct message", "Unauthorized access", e.message)
            assertEquals("Should have correct code", 401, e.code)
        }
    }

    @Test
    fun `handleResponse should throw RateLimitException for 429`() {
        val response = Response.error<String>(429, "Too Many Requests".toResponseBody())
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown RateLimitException")
        } catch (e: RateLimitException) {
            assertEquals("Should have correct message", "Rate limit exceeded", e.message)
            assertEquals("Should have correct code", 429, e.code)
        }
    }

    @Test
    fun `handleResponse should throw ServerException for 5xx errors`() {
        val response = Response.error<String>(500, "Internal Server Error".toResponseBody())
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown ServerException")
        } catch (e: ServerException) {
            assertTrue("Should contain server error message", e.message!!.contains("Server error"))
            assertEquals("Should have correct code", 500, e.code)
        }
    }

    @Test
    fun `handleResponse should throw ApiException for other errors`() {
        val response = Response.error<String>(400, "Bad Request".toResponseBody())
        
        try {
            ApiErrorHandler.handleResponse(response)
            fail("Should have thrown ApiException")
        } catch (e: ApiException) {
            assertTrue("Should contain API error message", e.message!!.contains("API error"))
            assertEquals("Should have correct code", 400, e.code)
        }
    }

    @Test
    fun `mapException should map SocketTimeoutException to NetworkException`() {
        val originalException = SocketTimeoutException("Timeout")
        
        val mappedException = ApiErrorHandler.mapException(originalException)
        
        assertTrue("Should be NetworkException", mappedException is NetworkException)
        assertEquals("Should have correct message", "Request timeout", mappedException.message)
        assertEquals("Should have correct cause", originalException, mappedException.cause)
    }

    @Test
    fun `mapException should map UnknownHostException to NetworkException`() {
        val originalException = UnknownHostException("Unknown host")
        
        val mappedException = ApiErrorHandler.mapException(originalException)
        
        assertTrue("Should be NetworkException", mappedException is NetworkException)
        assertEquals("Should have correct message", "No internet connection", mappedException.message)
        assertEquals("Should have correct cause", originalException, mappedException.cause)
    }

    @Test
    fun `mapException should map IOException to NetworkException`() {
        val originalException = IOException("IO error")
        
        val mappedException = ApiErrorHandler.mapException(originalException)
        
        assertTrue("Should be NetworkException", mappedException is NetworkException)
        assertEquals("Should have correct message", "Network error", mappedException.message)
        assertEquals("Should have correct cause", originalException, mappedException.cause)
    }

    @Test
    fun `mapException should preserve ApiException`() {
        val originalException = ApiException("API error", 400)
        
        val mappedException = ApiErrorHandler.mapException(originalException)
        
        assertEquals("Should be same exception", originalException, mappedException)
    }

    @Test
    fun `mapException should map unknown exceptions to NetworkException`() {
        val originalException = RuntimeException("Unknown error")
        
        val mappedException = ApiErrorHandler.mapException(originalException)
        
        assertTrue("Should be NetworkException", mappedException is NetworkException)
        assertEquals("Should have correct message", "Unknown network error", mappedException.message)
        assertEquals("Should have correct cause", originalException, mappedException.cause)
    }

    @Test
    fun `safeApiCall should return result on success`() = runTest {
        val expectedResult = "success"
        val response = Response.success(expectedResult)
        
        val result = ApiErrorHandler.safeApiCall { response }
        
        assertEquals("Should return expected result", expectedResult, result)
    }

    @Test
    fun `safeApiCall should throw mapped exception on error`() = runTest {
        val response = Response.error<String>(404, "Not Found".toResponseBody())
        
        try {
            ApiErrorHandler.safeApiCall { response }
            fail("Should have thrown exception")
        } catch (e: NotFoundException) {
            assertEquals("Should have correct code", 404, e.code)
        }
    }

    @Test
    fun `safeApiCallWithFallback should return result on success`() = runTest {
        val expectedResult = "success"
        val fallback = "fallback"
        val response = Response.success(expectedResult)
        
        val result = ApiErrorHandler.safeApiCallWithFallback({ response }, fallback)
        
        assertEquals("Should return expected result", expectedResult, result)
    }

    @Test
    fun `safeApiCallWithFallback should return fallback on error`() = runTest {
        val fallback = "fallback"
        val response = Response.error<String>(404, "Not Found".toResponseBody())
        
        val result = ApiErrorHandler.safeApiCallWithFallback({ response }, fallback)
        
        assertEquals("Should return fallback", fallback, result)
    }

    @Test
    fun `safeApiCallWithRetry should succeed after retries`() = runTest {
        var attempts = 0
        val expectedResult = "success"
        
        val result = ApiErrorHandler.safeApiCallWithRetry(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                Response.error<String>(500, "Server Error".toResponseBody())
            } else {
                Response.success(expectedResult)
            }
        }
        
        assertEquals("Should return expected result", expectedResult, result)
        assertEquals("Should have made 3 attempts", 3, attempts)
    }

    @Test
    fun `safeApiCallWithRetry should throw exception after max retries`() = runTest {
        var attempts = 0
        
        try {
            ApiErrorHandler.safeApiCallWithRetry(maxRetries = 2) {
                attempts++
                Response.error<String>(500, "Server Error".toResponseBody())
            }
            fail("Should have thrown exception")
        } catch (e: ServerException) {
            assertEquals("Should have made 3 attempts (initial + 2 retries)", 3, attempts)
            assertEquals("Should have correct code", 500, e.code)
        }
    }

    @Test
    fun `safeApiCallWithRetry should use custom retry strategy`() = runTest {
        var attempts = 0
        val customDelays = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()
        
        try {
            ApiErrorHandler.safeApiCallWithRetry(
                maxRetries = 3,
                retryStrategy = { attempt -> 
                    val delay = 500L * attempt
                    customDelays.add(delay)
                    delay
                }
            ) {
                attempts++
                Response.error<String>(500, "Server Error".toResponseBody())
            }
            fail("Should have thrown exception")
        } catch (e: ServerException) {
            assertEquals("Should have made 4 attempts (initial + 3 retries)", 4, attempts)
            assertEquals("Should have used custom delays", listOf(500L, 1000L, 1500L), customDelays)
        }
    }

    @Test
    fun `safeApiCallWithTimeout should succeed within timeout`() = runTest {
        val expectedResult = "success"
        val response = Response.success(expectedResult)
        
        val result = ApiErrorHandler.safeApiCallWithTimeout(timeoutMs = 5000L) {
            kotlinx.coroutines.delay(100L) // Short delay
            response
        }
        
        assertEquals("Should return expected result", expectedResult, result)
    }

    @Test
    fun `safeApiCallWithTimeout should throw timeout exception`() = runTest {
        try {
            ApiErrorHandler.safeApiCallWithTimeout(timeoutMs = 100L) {
                kotlinx.coroutines.delay(200L) // Longer than timeout
                Response.success("should not reach")
            }
            fail("Should have thrown timeout exception")
        } catch (e: NetworkException) {
            assertTrue("Should be timeout error", e.message!!.contains("timeout"))
        }
    }

    @Test
    fun `safeApiCallWithRetryAndTimeout should combine both strategies`() = runTest {
        var attempts = 0
        
        try {
            ApiErrorHandler.safeApiCallWithRetryAndTimeout(
                maxRetries = 2,
                timeoutMs = 100L,
                retryStrategy = { 50L }
            ) {
                attempts++
                kotlinx.coroutines.delay(200L) // Always timeout
                Response.success("should not reach")
            }
            fail("Should have thrown exception")
        } catch (e: Exception) {
            // The combined strategy should attempt retries and handle timeouts
            assertTrue("Should have made at least 1 attempt", attempts >= 1)
            // Should throw some kind of exception (timeout or network error)
            assertNotNull("Should have thrown an exception", e)
        }
    }
}