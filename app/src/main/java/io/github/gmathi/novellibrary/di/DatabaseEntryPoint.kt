package io.github.gmathi.novellibrary.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter

/**
 * Entry point for accessing database-related dependencies in object classes and utilities
 * Used when constructor injection is not possible (e.g., object classes, extension functions)
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseEntryPoint {
    fun dbHelper(): DBHelper
    fun dataCenter(): DataCenter
}