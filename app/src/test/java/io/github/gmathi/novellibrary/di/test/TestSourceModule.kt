package io.github.gmathi.novellibrary.di.test

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.SourceModule
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces SourceModule in tests.
 * Provides mock implementations of source and extension dependencies.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SourceModule::class]
)
object TestSourceModule {

    @Provides
    @Singleton
    fun provideTestExtensionManager(): ExtensionManager {
        return mockk<ExtensionManager>(relaxed = true) {
            // Configure common mock behaviors for extension management
        }
    }

    @Provides
    @Singleton
    fun provideTestSourceManager(): SourceManager {
        return mockk<SourceManager>(relaxed = true) {
            // Configure common mock behaviors for source management
        }
    }
}