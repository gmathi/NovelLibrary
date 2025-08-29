package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.util.security.DependencySecurityValidator
import io.github.gmathi.novellibrary.util.security.SecureNetworkHelper
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module for security components
 * Provides mock implementations for security testing
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SecurityModule::class]
)
object TestSecurityModule {
    
    @Provides
    @Singleton
    fun provideTestDependencySecurityValidator(): DependencySecurityValidator {
        return mockk<DependencySecurityValidator>(relaxed = true)
    }
    
    @Provides
    @Singleton
    fun provideTestSecureNetworkHelper(): SecureNetworkHelper {
        return mockk<SecureNetworkHelper>(relaxed = true)
    }
}