package io.github.gmathi.novellibrary.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.network.NetworkHelper
import kotlinx.serialization.json.Json

/**
 * Entry point for accessing network-related dependencies in object classes and utilities
 * Used when constructor injection is not possible (e.g., object classes, extension functions)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NetworkEntryPoint {
    fun networkHelper(): NetworkHelper
    fun json(): Json
}