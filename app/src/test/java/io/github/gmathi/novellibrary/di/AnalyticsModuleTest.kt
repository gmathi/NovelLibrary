package io.github.gmathi.novellibrary.di

import com.google.firebase.analytics.FirebaseAnalytics
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AnalyticsModuleTest {

    @Test
    fun `provideFirebaseAnalytics returns valid instance`() {
        // Given
        val module = AnalyticsModule

        // When
        val analytics = module.provideFirebaseAnalytics()

        // Then
        assertNotNull(analytics)
        assertTrue("FirebaseAnalytics should be properly initialized", analytics is FirebaseAnalytics)
    }

    @Test
    fun `FirebaseAnalytics is singleton instance`() {
        // Given
        val module = AnalyticsModule

        // When
        val analytics1 = module.provideFirebaseAnalytics()
        val analytics2 = module.provideFirebaseAnalytics()

        // Then
        assertSame("FirebaseAnalytics should be singleton", analytics1, analytics2)
    }

    @Test
    fun `AnalyticsModule has correct annotations`() {
        // Given
        val moduleClass = AnalyticsModule::class.java

        // Then
        assertTrue("AnalyticsModule should have @Module annotation", 
            moduleClass.isAnnotationPresent(dagger.Module::class.java))
        assertTrue("AnalyticsModule should have @InstallIn annotation", 
            moduleClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `provideFirebaseAnalytics method has correct annotations`() {
        // Given
        val method = AnalyticsModule::class.java.getMethod("provideFirebaseAnalytics")

        // Then
        assertTrue("provideFirebaseAnalytics should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideFirebaseAnalytics should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `FirebaseAnalytics instance is properly configured`() {
        // Given
        val module = AnalyticsModule

        // When
        val analytics = module.provideFirebaseAnalytics()

        // Then
        assertNotNull("FirebaseAnalytics should not be null", analytics)
        // Additional configuration tests could be added here if needed
        // For example, testing if analytics collection is enabled/disabled based on build config
    }
}