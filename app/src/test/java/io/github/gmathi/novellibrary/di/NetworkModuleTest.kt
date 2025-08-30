package io.github.gmathi.novellibrary.di

import android.content.Context
import com.google.gson.Gson
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.AndroidCookieJar
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.api.RetrofitServiceFactory
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import kotlinx.serialization.json.Json
import org.mockito.Mockito.mock
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NetworkModuleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `provideCloudflareInterceptor returns valid instance`() {
        // Given
        val module = NetworkModule

        // When
        val interceptor = module.provideCloudflareInterceptor(context)

        // Then
        assertNotNull(interceptor)
        assertTrue("CloudflareInterceptor should be properly initialized", 
            interceptor is io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor)
    }

    @Test
    fun `provideGson returns valid instance`() {
        // Given
        val module = NetworkModule

        // When
        val gson = module.provideGson()

        // Then
        assertNotNull(gson)
        assertTrue("Gson should be properly initialized", gson is Gson)
    }

    @Test
    fun `provideJson returns valid instance with correct configuration`() {
        // Given
        val module = NetworkModule

        // When
        val json = module.provideJson()

        // Then
        assertNotNull(json)
        assertTrue("Json should be properly initialized", json is Json)
        // Test that ignoreUnknownKeys is configured correctly
        assertTrue("Json should ignore unknown keys", json.configuration.ignoreUnknownKeys)
    }

    @Test
    fun `NetworkModule has correct annotations`() {
        // Given
        val moduleClass = NetworkModule::class.java

        // Then
        assertTrue("NetworkModule should have @Module annotation", 
            moduleClass.isAnnotationPresent(dagger.Module::class.java))
        assertTrue("NetworkModule should have @InstallIn annotation", 
            moduleClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `provideCloudflareInterceptor method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideCloudflareInterceptor", Context::class.java)

        // Then
        assertTrue("provideCloudflareInterceptor should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideCloudflareInterceptor should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideGson method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideGson")

        // Then
        assertTrue("provideGson should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideGson should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideJson method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideJson")

        // Then
        assertTrue("provideJson should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideJson should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideAndroidCookieJar returns valid instance`() {
        // Given
        val module = NetworkModule
        val dataCenter = mock(DataCenter::class.java)

        // When
        val cookieJar = module.provideAndroidCookieJar(dataCenter)

        // Then
        assertNotNull(cookieJar)
        assertTrue("AndroidCookieJar should be properly initialized", cookieJar is AndroidCookieJar)
    }

    @Test
    fun `provideRetrofitServiceFactory returns valid instance`() {
        // Given
        val module = NetworkModule
        val networkHelper = mock(NetworkHelper::class.java)

        // When
        val factory = module.provideRetrofitServiceFactory(networkHelper)

        // Then
        assertNotNull(factory)
        assertTrue("RetrofitServiceFactory should be properly initialized", factory is RetrofitServiceFactory)
    }

    @Test
    fun `provideAndroidCookieJar method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideAndroidCookieJar", 
            io.github.gmathi.novellibrary.model.preference.DataCenter::class.java)

        // Then
        assertTrue("provideAndroidCookieJar should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideAndroidCookieJar should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideRetrofitServiceFactory method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideRetrofitServiceFactory", 
            NetworkHelper::class.java)

        // Then
        assertTrue("provideRetrofitServiceFactory should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideRetrofitServiceFactory should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }
}