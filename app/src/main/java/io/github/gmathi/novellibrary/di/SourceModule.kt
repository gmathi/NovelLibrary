package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.extension.util.ExtensionLoader
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides
    @Singleton
    fun provideExtensionManager(
        @ApplicationContext context: Context,
        dataCenter: DataCenter,
        extensionLoader: ExtensionLoader,
        api: io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
    ): ExtensionManager {
        return ExtensionManager(context, dataCenter, extensionLoader, api)
    }

    @Provides
    @Singleton
    fun provideSourceManager(
        @ApplicationContext context: Context,
        extensionManager: ExtensionManager
    ): SourceManager {
        return SourceManager(context).also { 
            extensionManager.init(it) 
        }
    }
}