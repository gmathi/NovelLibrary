package io.github.gmathi.novellibrary.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.migration.MigrationFallback
import io.github.gmathi.novellibrary.util.migration.MigrationLogger
import io.github.gmathi.novellibrary.util.migration.MigrationValidator
import javax.inject.Singleton

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
    fun provideMigrationValidator(
        dbHelper: DBHelper,
        dataCenter: DataCenter,
        networkHelper: NetworkHelper
    ): MigrationValidator {
        return MigrationValidator(dbHelper, dataCenter, networkHelper)
    }

    @Provides
    @Singleton
    fun provideMigrationFallback(
        migrationLogger: MigrationLogger
    ): MigrationFallback {
        return MigrationFallback(migrationLogger)
    }
}