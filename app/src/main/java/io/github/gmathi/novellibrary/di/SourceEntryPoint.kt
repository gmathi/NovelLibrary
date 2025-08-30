package io.github.gmathi.novellibrary.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager

/**
 * Entry point for accessing source and extension management dependencies in object classes and utilities
 * Used when constructor injection is not possible (e.g., object classes, extension functions)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SourceEntryPoint {
    fun extensionManager(): ExtensionManager
    fun sourceManager(): SourceManager
}