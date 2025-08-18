package io.github.gmathi.novellibrary.util.migration

import org.junit.Before
import org.junit.Test

class MigrationLoggerTest {

    private lateinit var migrationLogger: MigrationLogger

    @Before
    fun setup() {
        migrationLogger = MigrationLogger()
    }

    @Test
    fun `logPhaseStart does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logPhaseStart("TestPhase")
    }

    @Test
    fun `logPhaseComplete does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logPhaseComplete("TestPhase")
    }

    @Test
    fun `logComponentMigration does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logComponentMigration("TestComponent", "Injekt", "Hilt", "completed")
        migrationLogger.logComponentMigration("TestComponent", "Injekt", "Hilt", "failed")
        migrationLogger.logComponentMigration("TestComponent", "Injekt", "Hilt", "started")
    }

    @Test
    fun `logValidationResult does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logValidationResult("TestComponent", true)
        migrationLogger.logValidationResult("TestComponent", false, "Validation details")
    }

    @Test
    fun `logFallbackActivation does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logFallbackActivation("TestComponent", "Hilt injection failed")
    }

    @Test
    fun `logPerformanceMetric does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logPerformanceMetric("TestOperation", 100L)
    }

    @Test
    fun `logError does not throw exception`() {
        // Given
        val error = RuntimeException("Test error")

        // When & Then (should not throw)
        migrationLogger.logError("TestContext", error)
    }

    @Test
    fun `logMigrationSummary does not throw exception`() {
        // When & Then (should not throw)
        migrationLogger.logMigrationSummary(10, 8, 2, 5000L)
    }
}