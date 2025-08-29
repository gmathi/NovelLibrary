package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.migration.InjektHiltBridge
import io.github.gmathi.novellibrary.util.migration.MigrationFeatureFlags
import io.github.gmathi.novellibrary.util.migration.MigrationLogger
import io.github.gmathi.novellibrary.util.migration.MigrationRollbackManager
import io.github.gmathi.novellibrary.util.migration.MigrationValidator
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.SourceManager
import javax.inject.Singleton

/**
 * Hilt module providing migration utilities for gradual Injekt to Hilt migration.
 * Supports hybrid mode and rollback mechanisms.
 */
@Module
@InstallIn(SingletonComponent::class)
object MigrationModule {
    
    @Provides
    @Singleton
    fun provideMigrationLogger(): MigrationLogger {
        return MigrationLogger()
    }
    
    @Provides
    @Singleton
    fun provideMigrationFeatureFlags(
        @ApplicationContext context: Context,
        migrationLogger: MigrationLogger
    ): MigrationFeatureFlags {
        return MigrationFeatureFlags(context, migrationLogger)
    }
    
    @Provides
    @Singleton
    fun provideInjektHiltBridge(
        @ApplicationContext context: Context,
        featureFlags: MigrationFeatureFlags,
        migrationLogger: MigrationLogger,
        dbHelper: DBHelper,
        dataCenter: DataCenter,
        networkHelper: NetworkHelper,
        sourceManager: SourceManager,
        extensionManager: ExtensionManager
    ): InjektHiltBridge {
        return InjektHiltBridge(
            context = context,
            featureFlags = featureFlags,
            migrationLogger = migrationLogger,
            hiltDBHelper = dbHelper,
            hiltDataCenter = dataCenter,
            hiltNetworkHelper = networkHelper,
            hiltSourceManager = sourceManager,
            hiltExtensionManager = extensionManager
        )
    }
    
    @Provides
    @Singleton
    fun provideMigrationRollbackManager(
        @ApplicationContext context: Context,
        featureFlags: MigrationFeatureFlags,
        migrationLogger: MigrationLogger,
        migrationValidator: MigrationValidator
    ): MigrationRollbackManager {
        return MigrationRollbackManager(
            context = context,
            featureFlags = featureFlags,
            migrationLogger = migrationLogger,
            migrationValidator = migrationValidator
        )
    }
}