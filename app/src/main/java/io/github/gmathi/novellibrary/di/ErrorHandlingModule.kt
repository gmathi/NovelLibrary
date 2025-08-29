package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.util.error.HiltDebugUtils
import io.github.gmathi.novellibrary.util.error.HiltErrorHandler
import io.github.gmathi.novellibrary.util.error.HiltTroubleshootingGuide
import io.github.gmathi.novellibrary.util.validation.HiltCompileTimeValidator
import io.github.gmathi.novellibrary.util.validation.HiltDependencyResolver
import io.github.gmathi.novellibrary.util.validation.HiltRuntimeValidator
import javax.inject.Singleton

/**
 * Hilt module providing error handling and debugging utilities
 * Supports comprehensive error handling for dependency injection failures
 */
@Module
@InstallIn(SingletonComponent::class)
object ErrorHandlingModule {
    
    /**
     * Provides centralized error handler for Hilt dependency injection failures
     */
    @Provides
    @Singleton
    fun provideHiltErrorHandler(
        @ApplicationContext context: Context
    ): HiltErrorHandler {
        return HiltErrorHandler(context)
    }
    
    /**
     * Provides debugging utilities for Hilt component tree visualization
     */
    @Provides
    @Singleton
    fun provideHiltDebugUtils(
        @ApplicationContext context: Context
    ): HiltDebugUtils {
        return HiltDebugUtils(context)
    }
    
    /**
     * Provides comprehensive troubleshooting guide for Hilt issues
     */
    @Provides
    @Singleton
    fun provideHiltTroubleshootingGuide(
        @ApplicationContext context: Context
    ): HiltTroubleshootingGuide {
        return HiltTroubleshootingGuide(context)
    }
    
    /**
     * Provides compile-time validation utilities for Hilt dependency injection
     */
    @Provides
    @Singleton
    fun provideHiltCompileTimeValidator(
        @ApplicationContext context: Context
    ): HiltCompileTimeValidator {
        return HiltCompileTimeValidator(context)
    }
    
    /**
     * Provides runtime validation utilities for Hilt dependency injection
     */
    @Provides
    @Singleton
    fun provideHiltRuntimeValidator(
        @ApplicationContext context: Context
    ): HiltRuntimeValidator {
        return HiltRuntimeValidator(context)
    }
    
    /**
     * Provides dependency resolution debugging tools
     */
    @Provides
    @Singleton
    fun provideHiltDependencyResolver(
        @ApplicationContext context: Context
    ): HiltDependencyResolver {
        return HiltDependencyResolver(context)
    }
}