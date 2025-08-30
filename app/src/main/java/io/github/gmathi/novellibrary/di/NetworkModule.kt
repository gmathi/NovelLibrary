package io.github.gmathi.novellibrary.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.network.api.RetrofitServiceFactory
import io.github.gmathi.novellibrary.network.interceptor.CloudflareInterceptor
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // NetworkHelper is now auto-provided by Hilt via @Inject constructor

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json { ignoreUnknownKeys = true }
    }

    @Provides
    @Singleton
    fun provideCloudflareInterceptor(
        @ApplicationContext context: Context
    ): CloudflareInterceptor {
        return CloudflareInterceptor(context)
    }

    @Provides
    @Singleton
    fun provideAndroidCookieJar(
        dataCenter: io.github.gmathi.novellibrary.model.preference.DataCenter
    ): io.github.gmathi.novellibrary.network.AndroidCookieJar {
        return io.github.gmathi.novellibrary.network.AndroidCookieJar(dataCenter)
    }

    @Provides
    @Singleton
    fun provideRetrofitServiceFactory(
        networkHelper: NetworkHelper
    ): io.github.gmathi.novellibrary.network.api.RetrofitServiceFactory {
        return io.github.gmathi.novellibrary.network.api.RetrofitServiceFactory(networkHelper)
    }

    @Provides
    @Singleton
    fun provideWebPageDocumentFetcher(
        dataCenter: io.github.gmathi.novellibrary.model.preference.DataCenter,
        dbHelper: io.github.gmathi.novellibrary.database.DBHelper,
        sourceManager: io.github.gmathi.novellibrary.model.source.SourceManager,
        networkHelper: NetworkHelper,
        timeoutConfig: io.github.gmathi.novellibrary.network.NetworkTimeoutConfig,
        errorHandler: io.github.gmathi.novellibrary.network.NetworkErrorHandler
    ): WebPageDocumentFetcher {
        val instance = WebPageDocumentFetcher(dataCenter, dbHelper, sourceManager, networkHelper, timeoutConfig, errorHandler)
        WebPageDocumentFetcher.setInstance(instance)
        return instance
    }
}