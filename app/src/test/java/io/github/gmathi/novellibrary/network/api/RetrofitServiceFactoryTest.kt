package io.github.gmathi.novellibrary.network.api

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitServiceFactoryTest {

    @Test
    fun `test retrofit factory creates retrofit with correct base URL`() {
        val baseUrl = "https://example.com/api/"
        val client = okhttp3.OkHttpClient()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        assertEquals(baseUrl, retrofit.baseUrl().toString())
        assertSame(client, retrofit.callFactory())
        
        // Check that GsonConverterFactory is present
        val converterFactories = retrofit.converterFactories()
        assertTrue("GsonConverterFactory should be present", 
            converterFactories.any { it is GsonConverterFactory })
    }

    @Test
    fun `test service creation with retrofit`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GitHubApiService::class.java)

        assertNotNull(service)
        assertTrue("Service should be a GitHubApiService instance", 
            service is GitHubApiService)
        assertTrue("Service should be a proxy instance", 
            java.lang.reflect.Proxy.isProxyClass(service.javaClass))
    }

    @Test
    fun `test multiple retrofit instances are independent`() {
        val baseUrl = "https://example.com/api/"
        val client1 = okhttp3.OkHttpClient()
        val client2 = okhttp3.OkHttpClient()
        
        val retrofit1 = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client1)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        val retrofit2 = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client2)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Retrofit instances should be different objects
        assertNotSame(retrofit1, retrofit2)
        
        // But they should have the same base URL
        assertEquals(retrofit1.baseUrl(), retrofit2.baseUrl())
        
        // And different clients
        assertNotSame(retrofit1.callFactory(), retrofit2.callFactory())
    }

    @Test
    fun `test service instances are different objects`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service1 = retrofit.create(GitHubApiService::class.java)
        val service2 = retrofit.create(GitHubApiService::class.java)

        // Services should be different instances (Retrofit creates new proxies)
        assertNotSame(service1, service2)
        
        // But both should be GitHubApiService instances
        assertTrue(service1 is GitHubApiService)
        assertTrue(service2 is GitHubApiService)
    }
}