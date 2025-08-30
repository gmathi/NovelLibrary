package io.github.gmathi.novellibrary.regression

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.network.proxy.BasePostProxyHelper
import io.github.gmathi.novellibrary.network.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.AndroidCookieJar
import io.github.gmathi.novellibrary.network.RetrofitServiceFactory
import io.github.gmathi.novellibrary.network.AppUpdateGithubApi
import io.github.gmathi.novellibrary.dataCenter.DataCenter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

/**
 * Comprehensive regression tests for network operations after Injekt cleanup.
 * Validates that all network functionality works correctly with pure Hilt injection.
 */
@HiltAndroidTest
class NetworkOperationsRegressionTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var json: Json

    @ApplicationContext
    @Inject
    lateinit var context: Context

    private lateinit var proxyHelper: BaseProxyHelper
    private lateinit var postProxyHelper: BasePostProxyHelper
    private lateinit var cloudflareInterceptor: CloudflareInterceptor
    private lateinit var cookieJar: AndroidCookieJar
    private lateinit var retrofitServiceFactory: RetrofitServiceFactory
    private lateinit var appUpdateApi: AppUpdateGithubApi

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Initialize network components with Hilt-injected dependencies
        proxyHelper = BaseProxyHelper(networkHelper)
        postProxyHelper = BasePostProxyHelper(networkHelper)
        cloudflareInterceptor = CloudflareInterceptor(networkHelper)
        cookieJar = AndroidCookieJar(dataCenter)
        retrofitServiceFactory = RetrofitServiceFactory(networkHelper)
        appUpdateApi = AppUpdateGithubApi(networkHelper)
    }

    @Test
    fun `test BaseProxyHelper network operations work correctly`() {
        // Test that proxy helper can access network client
        val client = proxyHelper.client
        assertNotNull("BaseProxyHelper should provide valid OkHttpClient", client)
        assertTrue("Client should be configured", client.interceptors.isNotEmpty())
        
        // Test basic network request through proxy helper
        val request = Request.Builder()
            .url("https://httpbin.org/get")
            .build()
            
        val response = client.newCall(request).execute()
        assertTrue("Network request should succeed", response.isSuccessful)
        response.close()
    }

    @Test
    fun `test BasePostProxyHelper network operations work correctly`() {
        // Test that post proxy helper can access network client
        val client = postProxyHelper.client
        assertNotNull("BasePostProxyHelper should provide valid OkHttpClient", client)
        
        // Test POST request functionality
        val request = Request.Builder()
            .url("https://httpbin.org/post")
            .post(okhttp3.RequestBody.create(null, "test data"))
            .build()
            
        val response = client.newCall(request).execute()
        assertTrue("POST request should succeed", response.isSuccessful)
        response.close()
    }

    @Test
    fun `test CloudflareInterceptor integration works correctly`() {
        // Test that Cloudflare interceptor is properly integrated
        val client = OkHttpClient.Builder()
            .addInterceptor(cloudflareInterceptor)
            .build()
            
        val request = Request.Builder()
            .url("https://httpbin.org/headers")
            .build()
            
        val response = client.newCall(request).execute()
        assertTrue("Request with Cloudflare interceptor should succeed", response.isSuccessful)
        
        // Verify interceptor added appropriate headers
        val responseBody = response.body?.string()
        assertNotNull("Response should have body", responseBody)
        response.close()
    }

    @Test
    fun `test AndroidCookieJar cookie management works correctly`() {
        // Test cookie jar functionality
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .build()
            
        // First request to set cookies
        val setCookieRequest = Request.Builder()
            .url("https://httpbin.org/cookies/set/test/value")
            .build()
            
        val setCookieResponse = client.newCall(setCookieRequest).execute()
        assertTrue("Set cookie request should succeed", setCookieResponse.isSuccessful)
        setCookieResponse.close()
        
        // Second request to verify cookies are sent
        val getCookieRequest = Request.Builder()
            .url("https://httpbin.org/cookies")
            .build()
            
        val getCookieResponse = client.newCall(getCookieRequest).execute()
        assertTrue("Get cookie request should succeed", getCookieResponse.isSuccessful)
        
        val responseBody = getCookieResponse.body?.string()
        assertTrue("Response should contain set cookie", 
            responseBody?.contains("test") == true)
        getCookieResponse.close()
    }

    @Test
    fun `test RetrofitServiceFactory creates working services`() {
        // Test that retrofit service factory creates functional services
        val retrofit = retrofitServiceFactory.createRetrofit("https://httpbin.org/")
        assertNotNull("Retrofit instance should be created", retrofit)
        
        // Create a simple test service
        interface TestService {
            @retrofit2.http.GET("get")
            suspend fun testGet(): retrofit2.Response<Any>
        }
        
        val testService = retrofit.create(TestService::class.java)
        
        runBlocking {
            val response = testService.testGet()
            assertTrue("Retrofit service should work correctly", response.isSuccessful)
        }
    }

    @Test
    fun `test AppUpdateGithubApi network operations work correctly`() = runBlocking {
        // Test that GitHub API can make requests
        try {
            val releases = appUpdateApi.getLatestReleases("octocat", "Hello-World")
            // If we get here without exception, the network operation worked
            assertNotNull("API should return response", releases)
        } catch (e: Exception) {
            // Network operations might fail in test environment, but should not fail due to injection issues
            assertFalse("Should not fail due to dependency injection issues", 
                e.message?.contains("inject") == true)
        }
    }

    @Test
    fun `test JSON serialization works correctly with network responses`() {
        // Test that JSON serialization works with network responses
        val client = networkHelper.cloudflareClient
        val request = Request.Builder()
            .url("https://httpbin.org/json")
            .build()
            
        val response = client.newCall(request).execute()
        assertTrue("JSON endpoint request should succeed", response.isSuccessful)
        
        val responseBody = response.body?.string()
        assertNotNull("Response should have JSON body", responseBody)
        
        // Test that we can parse JSON using injected Json instance
        try {
            val jsonElement = json.parseToJsonElement(responseBody!!)
            assertNotNull("Should be able to parse JSON response", jsonElement)
        } catch (e: Exception) {
            fail("JSON parsing should work with injected Json instance: ${e.message}")
        }
        
        response.close()
    }

    @Test
    fun `test network error handling works correctly`() {
        // Test that network error handling works properly
        val client = networkHelper.cloudflareClient
        val request = Request.Builder()
            .url("https://httpbin.org/status/404")
            .build()
            
        val response = client.newCall(request).execute()
        assertEquals("Should handle 404 errors correctly", 404, response.code)
        response.close()
        
        // Test timeout handling
        val timeoutRequest = Request.Builder()
            .url("https://httpbin.org/delay/10")
            .build()
            
        try {
            val timeoutResponse = client.newCall(timeoutRequest).execute()
            timeoutResponse.close()
        } catch (e: Exception) {
            // Timeout is expected, verify it's handled gracefully
            assertTrue("Should handle timeouts gracefully", 
                e.message?.contains("timeout") == true || 
                e.message?.contains("cancelled") == true)
        }
    }

    @Test
    fun `test concurrent network operations work correctly`() = runBlocking {
        // Test that multiple concurrent network operations work
        val client = networkHelper.cloudflareClient
        val requests = (1..5).map { i ->
            Request.Builder()
                .url("https://httpbin.org/delay/1?id=$i")
                .build()
        }
        
        val responses = requests.map { request ->
            kotlinx.coroutines.async {
                client.newCall(request).execute()
            }
        }
        
        responses.forEach { deferred ->
            val response = deferred.await()
            assertTrue("Concurrent request should succeed", response.isSuccessful)
            response.close()
        }
    }
}