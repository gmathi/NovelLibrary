package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.util.coroutines.CoroutineScopes
import io.github.gmathi.novellibrary.util.coroutines.DefaultDispatcherProvider
import io.github.gmathi.novellibrary.util.coroutines.DispatcherProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideCoroutineScopes(): CoroutineScopes {
        return CoroutineScopes()
    }

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}