package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.util.navigation.DeepLinkHandler
import io.github.gmathi.novellibrary.util.navigation.NavigationManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideNavigationManager(): NavigationManager {
        return NavigationManager()
    }

    @Provides
    @Singleton
    fun provideDeepLinkHandler(): DeepLinkHandler {
        return DeepLinkHandler()
    }
}