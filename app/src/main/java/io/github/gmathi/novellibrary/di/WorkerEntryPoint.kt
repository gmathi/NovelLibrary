package io.github.gmathi.novellibrary.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.api.ExtensionGithubApi
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper

/**
 * Entry point for accessing Hilt dependencies in Worker classes
 * Workers cannot use direct injection, so we use EntryPoint to access dependencies
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun dbHelper(): DBHelper
    fun dataCenter(): DataCenter
    fun extensionGithubApi(): ExtensionGithubApi
    fun networkHelper(): NetworkHelper
}