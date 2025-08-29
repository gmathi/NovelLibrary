package io.github.gmathi.novellibrary.di.test

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.NetworkModule
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.mockk
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Test module that replaces NetworkModule in tests.
 * Provides mock implementations of network dependencies.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object TestNetworkModule {

    @Provides
    @Singleton
    fun provideTestNetworkHelper(): NetworkHelper {
        return mockk<NetworkHelper>(relaxed = true) {
            // Configure common mock behaviors for network operations
        }
    }

    @Provides
    @Singleton
    fun provideTestGson(): Gson {
        // Use real Gson for JSON serialization in tests
        return Gson()
    }

    @Provides
    @Singleton
    fun provideTestJson(): Json {
        // Use real Json for serialization in tests
        return Json { ignoreUnknownKeys = true }
    }
}