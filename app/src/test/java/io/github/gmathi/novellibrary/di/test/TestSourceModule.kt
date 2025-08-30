package io.github.gmathi.novellibrary.di.test

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.SourceModule
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.extension.ExtensionDependencyProvider
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.extension.util.ExtensionLoader
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.source.NovelUpdatesSource
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces SourceModule in tests.
 * Provides mock implementations of all source-related dependencies.
 * Since the production SourceModule now uses @Inject constructors,
 * we need to provide mocks for all the @Singleton classes.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SourceModule::class]
)
object TestSourceModule {

    @Provides
    @Singleton
    fun provideTestSourceManager(): SourceManager {
        return mockk<SourceManager>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestExtensionManager(): ExtensionManager {
        return mockk<ExtensionManager>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestNovelUpdatesSource(): NovelUpdatesSource {
        return mockk<NovelUpdatesSource>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestExtensionLoader(): ExtensionLoader {
        return mockk<ExtensionLoader>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestExtensionGithubApi(): ExtensionGithubApi {
        return mockk<ExtensionGithubApi>(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideTestExtensionDependencyProvider(): ExtensionDependencyProvider {
        return mockk<ExtensionDependencyProvider>(relaxed = true)
    }
}