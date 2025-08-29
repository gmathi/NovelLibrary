package io.github.gmathi.novellibrary.di.test

import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.AnalyticsModule
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces AnalyticsModule in tests.
 * Provides mock implementations of analytics dependencies.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AnalyticsModule::class]
)
object TestAnalyticsModule {

    @Provides
    @Singleton
    fun provideTestFirebaseAnalytics(): FirebaseAnalytics {
        return mockk<FirebaseAnalytics>(relaxed = true) {
            // Configure common mock behaviors for analytics
        }
    }
}