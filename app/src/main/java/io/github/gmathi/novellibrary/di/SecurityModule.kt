package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.security.DependencySecurityValidator
import io.github.gmathi.novellibrary.util.security.SecureNetworkHelper
import javax.inject.Singleton

/**
 * Hilt module providing security-validated dependencies
 * Ensures all injected components meet security requirements
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideDependencySecurityValidator(): DependencySecurityValidator {
        return DependencySecurityValidator()
    }
    
    @Provides
    @Singleton
    fun provideSecureNetworkHelper(
        @ApplicationContext context: Context,
        validator: DependencySecurityValidator,
        dataCenter: DataCenter
    ): SecureNetworkHelper {
        return SecureNetworkHelper(context, validator, dataCenter)
    }
}