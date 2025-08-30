package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RetrofitServiceFactoryTest {

    private lateinit var networkHelper: NetworkHelper
    private lateinit var retrofitServiceFactory: RetrofitServiceFactory

    @Before
    fun setUp() {
        networkHelper = mockk(relaxed = true) {
            every { client } returns OkHttpClient()
            every { cloudflareClient } returns OkHttpClient()
        }
        retrofitServiceFactory = RetrofitServiceFactory(networkHelper)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val factory = RetrofitServiceFactory(networkHelper)
        
        // Then
        assertNotNull(factory)
    }

    @Test
    fun `createGitHubApiService returns valid service`() {
        // When
        val service = retrofitServiceFactory.createGitHubApiService()
        
        // Then
        assertNotNull(service)
        assertTrue(service is GitHubApiService)
    }

    @Test
    fun `createCloudflareGitHubApiService returns valid service`() {
        // When
        val service = retrofitServiceFactory.createCloudflareGitHubApiService()
        
        // Then
        assertNotNull(service)
        assertTrue(service is GitHubApiService)
    }

    @Test
    fun `createRetrofit returns valid retrofit instance`() {
        // Given
        val baseUrl = "https://api.example.com/"
        
        // When
        val retrofit = retrofitServiceFactory.createRetrofit(baseUrl)
        
        // Then
        assertNotNull(retrofit)
        assertEquals(baseUrl, retrofit.baseUrl().toString())
    }

    @Test
    fun `createCloudflareRetrofit returns valid retrofit instance`() {
        // Given
        val baseUrl = "https://api.example.com/"
        
        // When
        val retrofit = retrofitServiceFactory.createCloudflareRetrofit(baseUrl)
        
        // Then
        assertNotNull(retrofit)
        assertEquals(baseUrl, retrofit.baseUrl().toString())
    }
}