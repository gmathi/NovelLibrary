package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.network.NetworkHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uy.kohesive.injekt.injectLazy

/**
 * Factory for creating Retrofit service instances with proper coroutines configuration
 */
object RetrofitServiceFactory {

    private val networkHelper: NetworkHelper by injectLazy()

    /**
     * Create a GitHub API service instance
     */
    fun createGitHubApiService(): GitHubApiService {
        return createRetrofit("https://api.github.com/")
            .create(GitHubApiService::class.java)
    }

    /**
     * Create a GitHub API service instance with Cloudflare client
     */
    fun createCloudflareGitHubApiService(): GitHubApiService {
        return createCloudflareRetrofit("https://api.github.com/")
            .create(GitHubApiService::class.java)
    }

    /**
     * Create a generic Retrofit instance for any base URL
     */
    fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(networkHelper.client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create a generic Retrofit instance with Cloudflare client for any base URL
     */
    fun createCloudflareRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(networkHelper.cloudflareClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create a service instance for any API interface
     */
    inline fun <reified T> createService(baseUrl: String): T {
        return createRetrofit(baseUrl).create(T::class.java)
    }

    /**
     * Create a service instance with Cloudflare client for any API interface
     */
    inline fun <reified T> createCloudflareService(baseUrl: String): T {
        return createCloudflareRetrofit(baseUrl).create(T::class.java)
    }
}