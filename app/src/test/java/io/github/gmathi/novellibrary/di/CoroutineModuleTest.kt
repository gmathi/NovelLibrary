package io.github.gmathi.novellibrary.di

import io.github.gmathi.novellibrary.util.coroutines.CoroutineScopes
import io.github.gmathi.novellibrary.util.coroutines.DefaultDispatcherProvider
import io.github.gmathi.novellibrary.util.coroutines.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CoroutineModuleTest {

    @Test
    fun `provideCoroutineScopes returns valid instance`() {
        // Given
        val module = CoroutineModule

        // When
        val scopes = module.provideCoroutineScopes()

        // Then
        assertNotNull(scopes)
        assertTrue("CoroutineScopes should be properly initialized", scopes is CoroutineScopes)
    }

    @Test
    fun `provideDispatcherProvider returns DefaultDispatcherProvider`() {
        // Given
        val module = CoroutineModule

        // When
        val provider = module.provideDispatcherProvider()

        // Then
        assertNotNull(provider)
        assertTrue("DispatcherProvider should be DefaultDispatcherProvider", provider is DefaultDispatcherProvider)
    }

    @Test
    fun `CoroutineScopes and DispatcherProvider are singleton instances`() {
        // Given
        val module = CoroutineModule

        // When
        val scopes1 = module.provideCoroutineScopes()
        val scopes2 = module.provideCoroutineScopes()
        val provider1 = module.provideDispatcherProvider()
        val provider2 = module.provideDispatcherProvider()

        // Then
        assertSame("CoroutineScopes should be singleton", scopes1, scopes2)
        assertSame("DispatcherProvider should be singleton", provider1, provider2)
    }

    @Test
    fun `DefaultDispatcherProvider provides correct dispatchers`() {
        // Given
        val provider = DefaultDispatcherProvider()

        // When & Then
        assertEquals("Main dispatcher should be Main", Dispatchers.Main, provider.main)
        assertEquals("IO dispatcher should be IO", Dispatchers.IO, provider.io)
        assertEquals("Default dispatcher should be Default", Dispatchers.Default, provider.default)
        assertEquals("Unconfined dispatcher should be Unconfined", Dispatchers.Unconfined, provider.unconfined)
    }

    @Test
    fun `CoroutineModule has correct annotations`() {
        // Given
        val moduleClass = CoroutineModule::class.java

        // Then
        assertTrue("CoroutineModule should have @Module annotation", 
            moduleClass.isAnnotationPresent(dagger.Module::class.java))
        assertTrue("CoroutineModule should have @InstallIn annotation", 
            moduleClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `provideCoroutineScopes method has correct annotations`() {
        // Given
        val method = CoroutineModule::class.java.getMethod("provideCoroutineScopes")

        // Then
        assertTrue("provideCoroutineScopes should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideCoroutineScopes should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideDispatcherProvider method has correct annotations`() {
        // Given
        val method = CoroutineModule::class.java.getMethod("provideDispatcherProvider")

        // Then
        assertTrue("provideDispatcherProvider should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideDispatcherProvider should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }
}