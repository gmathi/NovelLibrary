# Hilt Test Infrastructure Documentation

## Overview

This document describes the comprehensive test infrastructure created for the Hilt migration. The infrastructure provides robust testing capabilities for dependency injection, integration testing, and performance validation.

## Test Infrastructure Components

### 1. Test Modules (`di/test/`)

#### TestDatabaseModule
- Replaces `DatabaseModule` in tests
- Provides mock implementations of `DBHelper` and `DataCenter`
- Uses `@TestInstallIn` to replace production modules

#### TestNetworkModule
- Replaces `NetworkModule` in tests
- Provides mock `NetworkHelper` and real `Gson`/`Json` instances
- Configured for network testing scenarios

#### TestSourceModule
- Replaces `SourceModule` in tests
- Provides mock `SourceManager` and `ExtensionManager`
- Handles source and extension testing

#### TestAnalyticsModule
- Replaces `AnalyticsModule` in tests
- Provides mock `FirebaseAnalytics`
- Prevents real analytics calls during testing

#### TestCoroutineModule
- Replaces `CoroutineModule` in tests
- Provides `TestDispatcherProvider` for controlled coroutine execution
- Uses `StandardTestDispatcher` for predictable testing

#### TestMigrationModule
- Replaces `MigrationModule` in tests
- Provides mock migration utilities
- Supports migration testing scenarios

### 2. Base Test Classes

#### BaseHiltTest
- Base class for unit tests with Hilt
- Extends from this class for automatic Hilt setup
- Includes MockK initialization and cleanup
- Uses Robolectric for Android context

#### BaseHiltAndroidTest
- Base class for Android instrumentation tests
- Provides real Android environment with Hilt
- Uses AndroidJUnit4 test runner
- Includes MockK setup for integration tests

### 3. Test Utilities

#### HiltTestUtils
- Common mock creation utilities
- Provides pre-configured mocks for major dependencies
- Includes cleanup utilities
- Offers `HiltMockRule` for combined setup

#### HiltMockingUtils
- Specialized mocking utilities for Hilt dependencies
- Pre-configured mock behaviors for common scenarios
- Test data creation utilities
- Scenario-specific mock configurations

#### TestConfiguration
- Central configuration for test settings
- Provides test dispatchers and scopes
- Common test data constants
- Test timeout configurations

### 4. Test Categories

#### Unit Tests
- **ViewModel Tests**: Test ViewModels with injected dependencies
- **Module Tests**: Test Hilt module providers
- **Utility Tests**: Test helper classes and utilities

#### Integration Tests
- **End-to-End Tests**: Test complete dependency chains
- **Activity/Fragment Tests**: Test Android component injection
- **Service/Worker Tests**: Test background component injection

#### Performance Tests
- **Injection Performance**: Measure dependency injection speed
- **Memory Usage**: Test singleton behavior and memory efficiency
- **Startup Performance**: Measure application startup impact

## Usage Examples

### Basic Unit Test with Hilt

```kotlin
class MyViewModelTest : BaseHiltTest() {
    
    @Inject
    lateinit var dbHelper: DBHelper
    
    @Inject
    lateinit var dataCenter: DataCenter
    
    private lateinit var viewModel: MyViewModel
    
    override fun onSetUp() {
        viewModel = MyViewModel(dbHelper, dataCenter)
    }
    
    @Test
    fun `test functionality`() = TestConfiguration.runTestWithDispatcher {
        // Test implementation
    }
}
```

### Integration Test

```kotlin
class MyIntegrationTest : BaseHiltTest() {
    
    @Inject
    lateinit var dbHelper: DBHelper
    
    @Test
    fun `test complete flow`() {
        // Configure mocks
        HiltMockingUtils.configureForSuccessfulNovelLoading(dbHelper, networkHelper)
        
        // Test complete functionality
        val result = performComplexOperation()
        
        // Verify results
        assertNotNull(result)
    }
}
```

### Android Instrumentation Test

```kotlin
class MyAndroidTest : BaseHiltAndroidTest() {
    
    @Test
    fun `test activity injection`() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        scenario.onActivity { activity ->
            // Test activity with real dependencies
            assertNotNull(activity)
        }
    }
}
```

## Test Execution

### Running All Tests
```bash
./gradlew test
```

### Running Specific Test Categories
```bash
# Unit tests only
./gradlew testDebugUnitTest

# Integration tests only
./gradlew testDebugUnitTest --tests "*integration*"

# Performance tests only
./gradlew testDebugUnitTest --tests "*Performance*"
```

### Running Test Suite
```bash
./gradlew testDebugUnitTest --tests "HiltTestSuite"
```

## Test Configuration

### Dependencies Required
```gradle
// Hilt testing
testImplementation 'com.google.dagger:hilt-android-testing:2.48'
kaptTest 'com.google.dagger:hilt-compiler:2.48'

// Android testing
androidTestImplementation 'com.google.dagger:hilt-android-testing:2.48'
kaptAndroidTest 'com.google.dagger:hilt-compiler:2.48'

// MockK
testImplementation 'io.mockk:mockk:1.13.8'
androidTestImplementation 'io.mockk:mockk-android:1.13.8'

// Coroutine testing
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3'

// Robolectric
testImplementation 'org.robolectric:robolectric:4.11.1'
```

### Test Application Configuration
The test infrastructure uses `HiltTestApplication` for unit tests and the main application for integration tests.

## Best Practices

### 1. Test Organization
- Use appropriate base classes (`BaseHiltTest` vs `BaseHiltAndroidTest`)
- Group related tests in the same package
- Use descriptive test names with backticks

### 2. Mock Configuration
- Use `HiltMockingUtils` for common mock setups
- Configure mocks in `onSetUp()` method
- Clean up mocks in `tearDown()` methods

### 3. Coroutine Testing
- Use `TestConfiguration.runTestWithDispatcher` for coroutine tests
- Use `TestDispatcherProvider` for controlled execution
- Verify async operations with proper timeouts

### 4. Performance Testing
- Use `measureTimeMillis` for performance measurements
- Set reasonable performance thresholds
- Test both single and concurrent operations

## Troubleshooting

### Common Issues

1. **Hilt injection not working**: Ensure `@HiltAndroidTest` annotation is present
2. **Mock not configured**: Use `HiltMockingUtils` or configure mocks in `onSetUp()`
3. **Coroutine tests hanging**: Use `TestConfiguration.runTestWithDispatcher`
4. **Memory leaks in tests**: Ensure proper cleanup in `tearDown()` methods

### Debug Tips

1. Use `hiltRule.inject()` to manually trigger injection
2. Check that test modules are properly replacing production modules
3. Verify that `@TestInstallIn` annotations are correct
4. Ensure test dependencies are properly configured in `build.gradle`

## Migration from Injekt Tests

When migrating existing Injekt-based tests:

1. Replace `@RunWith(AndroidJUnit4::class)` with extending `BaseHiltTest`
2. Remove manual mock creation and use injected dependencies
3. Replace `injectLazy()` patterns with `@Inject` annotations
4. Update test setup to use `onSetUp()` method
5. Use `TestConfiguration.runTestWithDispatcher` for coroutine tests

This infrastructure ensures comprehensive testing coverage for the Hilt migration while maintaining test reliability and performance.