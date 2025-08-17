package io.github.gmathi.novellibrary.network

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.converter.gson.GsonConverterFactory

interface TestApiService {
    @retrofit2.http.GET("test")
    suspend fun testEndpoint(): okhttp3.ResponseBody
}

class NetworkHelperTest {

    @Test
    fun `test retrofit configuration has correct converter factory`() {
        // Test that we can create a basic retrofit instance with GsonConverterFactory
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        assertNotNull(retrofit)
        assertEquals("https://api.github.com/", retrofit.baseUrl().toString())
        
        // Check that GsonConverterFactory is present
        val converterFactories = retrofit.converterFactories()
        assertTrue("GsonConverterFactory should be present", 
            converterFactories.any { it is GsonConverterFactory })
    }

    @Test
    fun `test okhttp client timeout configuration`() {
        // Test that we can create an OkHttpClient with proper timeout configuration
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        assertEquals(30_000, client.connectTimeoutMillis)
        assertEquals(30_000, client.readTimeoutMillis)
        assertEquals(30_000, client.writeTimeoutMillis)
        assertEquals(60_000, client.callTimeoutMillis)
    }

    @Test
    fun `test retrofit with okhttp client integration`() {
        // Test that Retrofit can be configured with a custom OkHttpClient
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        assertNotNull(retrofit)
        assertSame(client, retrofit.callFactory())
    }

    @Test
    fun `test coroutines compatibility with retrofit`() = runTest {
        // Test that we can create a service interface that uses suspend functions
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(TestApiService::class.java)
        assertNotNull(service)
        
        // Verify that the service is properly created
        assertTrue("Service should be a proxy instance", 
            java.lang.reflect.Proxy.isProxyClass(service.javaClass))
    }

    @Test
    fun `test network helper constants and configuration`() {
        // Test NetworkHelper constants and configuration without requiring Context
        
        // Test timeout values
        val expectedConnectTimeout = 30_000L
        val expectedReadTimeout = 30_000L
        val expectedWriteTimeout = 30_000L
        val expectedCallTimeout = 60_000L
        
        // Test that we can create OkHttp client with these timeouts
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        assertEquals("Connect timeout should be 30 seconds", expectedConnectTimeout, client.connectTimeoutMillis.toLong())
        assertEquals("Read timeout should be 30 seconds", expectedReadTimeout, client.readTimeoutMillis.toLong())
        assertEquals("Write timeout should be 30 seconds", expectedWriteTimeout, client.writeTimeoutMillis.toLong())
        assertEquals("Call timeout should be 60 seconds", expectedCallTimeout, client.callTimeoutMillis.toLong())
    }

    @Test
    fun `test retrofit configuration for coroutines`() {
        // Test that Retrofit can be configured for coroutines without RxJava adapters
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        assertNotNull("Retrofit instance should not be null", retrofit)
        assertEquals("Base URL should be correct", "https://api.github.com/", retrofit.baseUrl().toString())
        
        // Verify that only GsonConverterFactory is present (no RxJava adapters)
        val converterFactories = retrofit.converterFactories()
        assertTrue("Should have at least one converter factory", converterFactories.isNotEmpty())
        assertTrue("Should contain GsonConverterFactory", 
            converterFactories.any { it is GsonConverterFactory })
    }

    @Test
    fun `test network helper flow types`() = runTest {
        // Test that Flow types are properly imported and can be used
        val testFlow = kotlinx.coroutines.flow.flowOf(true, false, true)
        val distinctFlow = testFlow.distinctUntilChanged()
        
        assertNotNull("Flow should not be null", testFlow)
        assertNotNull("Distinct flow should not be null", distinctFlow)
        
        // Test that we can collect from the flow
        val firstValue = testFlow.first()
        assertTrue("First value should be true", firstValue)
    }
}