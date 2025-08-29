package io.github.gmathi.novellibrary.di.test

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.CoroutineModule
import io.github.gmathi.novellibrary.util.coroutines.CoroutineScopes
import io.github.gmathi.novellibrary.util.coroutines.DispatcherProvider
import io.github.gmathi.novellibrary.util.coroutines.TestDispatcherProvider
import javax.inject.Singleton

/**
 * Test module that replaces CoroutineModule in tests.
 * Provides test implementations of coroutine dependencies with TestDispatchers.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoroutineModule::class]
)
object TestCoroutineModule {

    @Provides
    @Singleton
    fun provideTestCoroutineScopes(): CoroutineScopes {
        // Use test coroutine scopes for predictable testing
        return CoroutineScopes()
    }

    @Provides
    @Singleton
    fun provideTestDispatcherProvider(): DispatcherProvider {
        // Use TestDispatcherProvider for controlled coroutine execution in tests
        return TestDispatcherProvider()
    }
}