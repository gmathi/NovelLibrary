package io.github.gmathi.novellibrary.di.test

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.gmathi.novellibrary.di.MigrationModule
import io.github.gmathi.novellibrary.util.migration.MigrationFallback
import io.github.gmathi.novellibrary.util.migration.MigrationLogger
import io.github.gmathi.novellibrary.util.migration.MigrationValidator
import io.mockk.mockk
import javax.inject.Singleton

/**
 * Test module that replaces MigrationModule in tests.
 * Provides mock implementations of migration utilities.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MigrationModule::class]
)
object TestMigrationModule {

    @Provides
    @Singleton
    fun provideTestMigrationValidator(): MigrationValidator {
        return mockk<MigrationValidator>(relaxed = true) {
            // Configure common mock behaviors for migration validation
        }
    }

    @Provides
    @Singleton
    fun provideTestMigrationLogger(): MigrationLogger {
        return mockk<MigrationLogger>(relaxed = true) {
            // Configure common mock behaviors for migration logging
        }
    }

    @Provides
    @Singleton
    fun provideTestMigrationFallback(): MigrationFallback {
        return mockk<MigrationFallback>(relaxed = true) {
            // Configure common mock behaviors for migration fallback
        }
    }
}