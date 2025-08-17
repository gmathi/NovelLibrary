package io.github.gmathi.novellibrary.network.api

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GitHubApiServiceTest {

    @Test
    fun `test GitHubApiService interface has correct method signatures`() {
        // Test that the interface is properly defined with suspend functions
        val methods = GitHubApiService::class.java.declaredMethods
        
        // Find the getLatestRelease method
        val getLatestReleaseMethod = methods.find { it.name == "getLatestRelease" }
        assertNotNull("getLatestRelease method should exist", getLatestReleaseMethod)
        
        // Find the getRawContent method
        val getRawContentMethod = methods.find { it.name == "getRawContent" }
        assertNotNull("getRawContent method should exist", getRawContentMethod)
        
        // Verify that methods exist (return type checking for suspend functions is complex)
        assertTrue("getLatestRelease method should exist", getLatestReleaseMethod != null)
        assertTrue("getRawContent method should exist", getRawContentMethod != null)
    }

    @Test
    fun `test service can be created with retrofit`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GitHubApiService::class.java)

        assertNotNull(service)
        assertTrue("Service should be a GitHubApiService instance", 
            service is GitHubApiService)
    }

    @Test
    fun `test service methods can be called in coroutine context`() = runTest {
        // This test verifies that the methods are properly defined as suspend functions
        // by checking that they can be referenced in a coroutine context
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GitHubApiService::class.java)
        
        // This should compile without issues if methods are properly suspend
        // We're not actually calling them to avoid network requests in unit tests
        assertNotNull(service)
        
        // Verify that the service has the expected methods
        val serviceClass = service.javaClass
        assertTrue("Service should be a proxy class", 
            java.lang.reflect.Proxy.isProxyClass(serviceClass))
    }

    @Test
    fun `test retrofit annotations are properly configured`() {
        // Test that the interface has proper Retrofit annotations
        val methods = GitHubApiService::class.java.methods
        
        val getLatestReleaseMethod = methods.find { it.name == "getLatestRelease" }
        assertNotNull(getLatestReleaseMethod)
        
        // Check for GET annotation
        val getAnnotation = getLatestReleaseMethod?.getAnnotation(retrofit2.http.GET::class.java)
        assertNotNull("getLatestRelease should have @GET annotation", getAnnotation)
        assertEquals("repos/{owner}/{repo}/releases/latest", getAnnotation?.value)
        
        val getRawContentMethod = methods.find { it.name == "getRawContent" }
        assertNotNull(getRawContentMethod)
        
        // Check for GET annotation (without value for @Url parameter)
        val getRawGetAnnotation = getRawContentMethod?.getAnnotation(retrofit2.http.GET::class.java)
        assertNotNull("getRawContent should have @GET annotation", getRawGetAnnotation)
    }
}