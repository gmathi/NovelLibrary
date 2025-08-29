package io.github.gmathi.novellibrary.di.test

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.di.DatabaseModule
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces DatabaseModule in tests.
 * Provides mock implementations of database dependencies.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideTestDBHelper(): DBHelper {
        return mockk<DBHelper>(relaxed = true) {
            // Configure common mock behaviors
        }
    }

    @Provides
    @Singleton
    fun provideTestDataCenter(): DataCenter {
        return mockk<DataCenter>(relaxed = true) {
            // Configure common mock behaviors
        }
    }
}