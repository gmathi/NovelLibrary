package io.github.gmathi.novellibrary.network.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineApiExtensionsTest {

    @Test
    fun `asFlow should emit successful response`() = runTest {
        val expectedData = "test data"
        val response = Response.success(expectedData)
        val apiCall: suspend () -> Response<String> = { response }
        
        val result = apiCall.asFlow().first()
        
        assertEquals("Should emit expected data", expectedData, result)
    }

    @Test
    fun `asFlow should throw exception on error response`() = runTest {
        val response = Response.error<String>(404, "Not Found".toResponseBody())
        val apiCall: suspend () -> Response<String> = { response }
        
        try {
            apiCall.asFlow().first()
            fail("Should have thrown exception")
        } catch (e: NotFoundException) {
            assertEquals("Should have correct code", 404, e.code)
        }
    }

    @Test
    fun `asFlowWithErrorHandling should emit successful response`() = runTest {
        val expectedData = "test data"
        val response = Response.success(expectedData)
        val apiCall: suspend () -> Response<String> = { response }
        
        val result = apiCall.asFlowWithErrorHandling().first()
        
        assertEquals("Should emit expected data", expectedData, result)
    }

    @Test
    fun `asFlowWithErrorHandling should handle exceptions`() = runTest {
        val apiCall: suspend () -> Response<String> = { 
            throw RuntimeException("Network error")
        }
        
        try {
            apiCall.asFlowWithErrorHandling().first()
            fail("Should have thrown exception")
        } catch (e: Exception) {
            // Should be mapped to NetworkException
            assertTrue("Should be mapped exception", e.message!!.contains("Unknown network error"))
        }
    }

    @Test
    fun `asPollingFlow should emit multiple values`() = runTest {
        var callCount = 0
        val apiCall: suspend () -> Response<String> = { 
            callCount++
            Response.success("data $callCount")
        }
        
        val flow = apiCall.asPollingFlow(100L)
        val results = flow.take(3).toList()
        
        assertEquals("Should emit 3 values", 3, results.size)
        assertEquals("First value should be correct", "data 1", results[0])
        assertEquals("Second value should be correct", "data 2", results[1])
        assertEquals("Third value should be correct", "data 3", results[2])
    }

    @Test
    fun `asRetryFlow should succeed after retries`() = runTest {
        var attempts = 0
        val apiCall: suspend () -> Response<String> = { 
            attempts++
            if (attempts < 3) {
                Response.error<String>(500, "Server Error".toResponseBody())
            } else {
                Response.success("success")
            }
        }
        
        val result = apiCall.asRetryFlow(maxRetries = 3).first()
        
        assertEquals("Should return success", "success", result)
        assertEquals("Should have made 3 attempts", 3, attempts)
    }

    @Test
    fun `asRetryFlow should throw exception after max retries`() = runTest {
        var attempts = 0
        val apiCall: suspend () -> Response<String> = { 
            attempts++
            Response.error<String>(500, "Server Error".toResponseBody())
        }
        
        try {
            apiCall.asRetryFlow(maxRetries = 2).first()
            fail("Should have thrown exception")
        } catch (e: ServerException) {
            assertEquals("Should have made 3 attempts (initial + 2 retries)", 3, attempts)
            assertEquals("Should have correct code", 500, e.code)
        }
    }

    @Test
    fun `asStringFlow should convert ResponseBody to string`() = runTest {
        val expectedString = "response content"
        val responseBody = expectedString.toResponseBody()
        
        val result = responseBody.asStringFlow().first()
        
        assertEquals("Should return expected string", expectedString, result)
    }

    @Test
    fun `createPaginatedFlow should emit multiple pages`() = runTest {
        val pages = mapOf(
            1 to listOf("item1", "item2"),
            2 to listOf("item3", "item4"),
            3 to emptyList<String>()
        )
        
        val apiCall: suspend (Int) -> Response<List<String>> = { page ->
            Response.success(pages[page] ?: emptyList())
        }
        
        val results = createPaginatedFlow(apiCall).toList()
        
        assertEquals("Should emit 2 pages", 2, results.size)
        assertEquals("First page should be correct", listOf("item1", "item2"), results[0])
        assertEquals("Second page should be correct", listOf("item3", "item4"), results[1])
    }

    @Test
    fun `createPaginatedFlow should stop on empty page`() = runTest {
        val apiCall: suspend (Int) -> Response<List<String>> = { page ->
            if (page == 1) {
                Response.success(listOf("item1"))
            } else {
                Response.success(emptyList())
            }
        }
        
        val results = createPaginatedFlow(apiCall).toList()
        
        assertEquals("Should emit only 1 page", 1, results.size)
        assertEquals("Page should be correct", listOf("item1"), results[0])
    }

    @Test
    fun `createPaginatedFlow should throw exception on error`() = runTest {
        val apiCall: suspend (Int) -> Response<List<String>> = { _ ->
            Response.error<List<String>>(404, "Not Found".toResponseBody())
        }
        
        try {
            createPaginatedFlow(apiCall).first()
            fail("Should have thrown exception")
        } catch (e: ApiException) {
            assertEquals("Should have correct message", "Pagination failed", e.message)
            assertEquals("Should have correct code", 404, e.code)
        }
    }

    @Test
    fun `createCacheFirstFlow should emit cached data first`() = runTest {
        val cachedData = "cached"
        val freshData = "fresh"
        
        val getCachedData: suspend () -> String? = { cachedData }
        val getFreshData: suspend () -> Response<String> = { Response.success(freshData) }
        
        val results = createCacheFirstFlow(getCachedData, getFreshData).toList()
        
        assertEquals("Should emit 2 values", 2, results.size)
        assertEquals("First value should be cached", cachedData, results[0])
        assertEquals("Second value should be fresh", freshData, results[1])
    }

    @Test
    fun `createCacheFirstFlow should emit only fresh data when no cache`() = runTest {
        val freshData = "fresh"
        
        val getCachedData: suspend () -> String? = { null }
        val getFreshData: suspend () -> Response<String> = { Response.success(freshData) }
        
        val results = createCacheFirstFlow(getCachedData, getFreshData).toList()
        
        assertEquals("Should emit 1 value", 1, results.size)
        assertEquals("Value should be fresh", freshData, results[0])
    }

    @Test
    fun `createCacheFirstFlow should ignore fresh data error when cache exists`() = runTest {
        val cachedData = "cached"
        
        val getCachedData: suspend () -> String? = { cachedData }
        val getFreshData: suspend () -> Response<String> = { 
            Response.error<String>(500, "Server Error".toResponseBody())
        }
        
        val results = createCacheFirstFlow(getCachedData, getFreshData).toList()
        
        assertEquals("Should emit 1 value", 1, results.size)
        assertEquals("Value should be cached", cachedData, results[0])
    }

    @Test
    fun `createCacheFirstFlow should throw error when no cache and fresh data fails`() = runTest {
        val getCachedData: suspend () -> String? = { null }
        val getFreshData: suspend () -> Response<String> = { 
            Response.error<String>(500, "Server Error".toResponseBody())
        }
        
        try {
            createCacheFirstFlow(getCachedData, getFreshData).first()
            fail("Should have thrown exception")
        } catch (e: ServerException) {
            assertEquals("Should have correct code", 500, e.code)
        }
    }

    @Test
    fun `asFlowWithTimeout should succeed within timeout`() = runTest {
        val expectedData = "success"
        val response = Response.success(expectedData)
        
        val result = suspend { 
            kotlinx.coroutines.delay(50L)
            response 
        }.asFlowWithTimeout(1000L).first()
        
        assertEquals("Should return expected data", expectedData, result)
    }

    @Test
    fun `asFlowWithTimeout should throw timeout exception`() = runTest {
        try {
            suspend { 
                kotlinx.coroutines.delay(200L)
                Response.success("should not reach")
            }.asFlowWithTimeout(100L).first()
            
            fail("Should have thrown timeout exception")
        } catch (e: Exception) {
            assertTrue("Should be timeout error", e.message!!.contains("timeout"))
        }
    }

    @Test
    fun `asRetryFlow should use custom retry strategy`() = runTest {
        var attempts = 0
        val delays = mutableListOf<Long>()
        
        try {
            suspend { 
                attempts++
                Response.error<String>(500, "Server Error".toResponseBody())
            }.asRetryFlow(
                maxRetries = 2,
                retryStrategy = { attempt ->
                    val delay = 100L * attempt
                    delays.add(delay)
                    delay
                }
            ).first()
            
            fail("Should have thrown exception")
        } catch (e: ServerException) {
            assertEquals("Should have made 3 attempts (initial + 2 retries)", 3, attempts)
            assertEquals("Should have used custom delays", listOf(100L, 200L), delays)
        }
    }

    @Test
    fun `asFlowWithRetryAndTimeout should combine both strategies`() = runTest {
        var attempts = 0
        
        try {
            suspend { 
                attempts++
                kotlinx.coroutines.delay(200L) // Always timeout
                Response.success("should not reach")
            }.asFlowWithRetryAndTimeout(
                maxRetries = 2,
                timeoutMs = 100L,
                retryStrategy = { 50L }
            ).first()
            
            fail("Should have thrown exception")
        } catch (e: Exception) {
            assertEquals("Should have made 3 attempts (initial + 2 retries)", 3, attempts)
            assertTrue("Should be timeout error", e.message!!.contains("timeout"))
        }
    }
}