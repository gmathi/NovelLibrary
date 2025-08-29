package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import javax.inject.Singleton
import javax.inject.Provider

/**
 * Hilt module for lazy initialization of expensive components
 * 
 * This module provides lazy providers for components that are expensive to initialize
 * but may not be needed immediately at app startup.
 */
@Module
@InstallIn(SingletonComponent::class)
object LazyModule {
    
    /**
     * Provides lazy DBHelper to defer database initialization until first access
     */
    @Provides
    @Singleton
    fun provideLazyDBHelper(provider: Provider<DBHelper>): Lazy<DBHelper> {
        return lazy { provider.get() }
    }
    
    /**
     * Provides lazy NetworkHelper to defer network setup until first network call
     */
    @Provides
    @Singleton
    fun provideLazyNetworkHelper(provider: Provider<NetworkHelper>): Lazy<NetworkHelper> {
        return lazy { provider.get() }
    }
    
    /**
     * Provides lazy SourceManager to defer source loading until first use
     */
    @Provides
    @Singleton
    fun provideLazySourceManager(provider: Provider<SourceManager>): Lazy<SourceManager> {
        return lazy { provider.get() }
    }
    
    /**
     * Provides lazy ExtensionManager to defer extension loading until needed
     */
    @Provides
    @Singleton
    fun provideLazyExtensionManager(provider: Provider<ExtensionManager>): Lazy<ExtensionManager> {
        return lazy { provider.get() }
    }
}