package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for source-related dependencies.
 * 
 * All source dependencies are now provided through @Inject constructors:
 * - SourceManager: @Singleton with @Inject constructor
 * - ExtensionManager: @Singleton with @Inject constructor  
 * - NovelUpdatesSource: @Singleton with @Inject constructor
 * - ExtensionLoader: @Singleton with @Inject constructor
 * - ExtensionGithubApi: @Singleton with @Inject constructor

 */
@Module
@InstallIn(SingletonComponent::class)
object SourceModule {
    // No explicit providers needed - all dependencies use @Inject constructors
}