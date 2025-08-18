package io.github.gmathi.novellibrary.di

import android.content.Context
import com.google.gson.Gson
import io.github.gmathi.novellibrary.network.NetworkHelper
import kotlinx.serialization.json.Json
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
    fun `provideNetworkHelper returns valid instance`() {
        // Given
        val module = NetworkModule

        // When
        val networkHelper = module.provideNetworkHelper(context)

        // Then
        assertNotNull(networkHelper)
        assertTrue("NetworkHelper should be properly initialized", networkHelper is NetworkHelper)
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
    fun `provideNetworkHelper method has correct annotations`() {
        // Given
        val method = NetworkModule::class.java.getMethod("provideNetworkHelper", Context::class.java)

        // Then
        assertTrue("provideNetworkHelper should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideNetworkHelper should have @Singleton annotation", 
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
}