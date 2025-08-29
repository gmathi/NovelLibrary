package io.github.gmathi.novellibrary.integration

import io.github.gmathi.novellibrary.di.*
import io.github.gmathi.novellibrary.viewmodel.*
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite for Hilt migration validation.
 * Runs all Hilt-related tests to ensure complete functionality.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // DI Module Tests
    DatabaseModuleTest::class,
    NetworkModuleTest::class,
    SourceModuleTest::class,
    AnalyticsModuleTest::class,
    CoroutineModuleTest::class,
    HiltModuleProvidersTest::class,
    
    // ViewModel Tests
    ChaptersViewModelTest::class,
    GoogleBackupViewModelTest::class,
    ExampleViewModelTest::class,
    
    // Integration Tests
    EndToEndDependencyInjectionTest::class,
    ServiceWorkerHiltIntegrationTest::class,
    HiltPerformanceComparisonTest::class
)
class HiltTestSuite {
    // Test suite class - no implementation needed
    // JUnit will run all specified test classes
}