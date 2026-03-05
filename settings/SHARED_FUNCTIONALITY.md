# Shared Functionality Documentation

This document describes the common functionality extracted from the settings module that can be reused across all settings screens.

## Overview

The settings module provides several shared utilities and patterns to ensure consistency and reduce code duplication:

1. **Validation Logic** - Common validation functions for settings values
2. **State Management** - Base ViewModel with standard state management patterns
3. **Navigation Helpers** - Reusable navigation functions
4. **Error Handling** - Type-safe error handling and result types

## 1. Validation Logic

**Location**: `settings/src/main/java/io/github/gmathi/novellibrary/settings/util/SettingsValidation.kt`

### Purpose

Provides reusable validation functions to ensure settings values are within acceptable ranges and formats before being persisted.

### Available Functions

#### Text Size Validation
```kotlin
SettingsValidation.validateTextSize(size: Int): Int
```
Validates text size is within acceptable range [12, 32] sp.

**Example**:
```kotlin
val validSize = SettingsValidation.validateTextSize(userInput)
viewModel.setTextSize(validSize)
```

#### Scroll Length Validation
```kotlin
SettingsValidation.validateScrollLength(length: Int): Int
```
Validates scroll length is within acceptable range [50, 500] pixels.

**Example**:
```kotlin
val validLength = SettingsValidation.validateScrollLength(userInput)
viewModel.setVolumeScrollLength(validLength)
```

#### Scroll Interval Validation
```kotlin
SettingsValidation.validateScrollInterval(interval: Int): Int
```
Validates scroll interval is within acceptable range [500, 5000] milliseconds.

**Example**:
```kotlin
val validInterval = SettingsValidation.validateScrollInterval(userInput)
viewModel.setAutoScrollInterval(validInterval)
```

#### Backup Frequency Validation
```kotlin
SettingsValidation.validateBackupFrequency(hours: Int): Int
```
Validates backup frequency is within acceptable range [1, 168] hours (1 hour to 1 week).

**Example**:
```kotlin
val validFrequency = SettingsValidation.validateBackupFrequency(userInput)
viewModel.setBackupFrequency(validFrequency)
```

#### Language Code Validation
```kotlin
SettingsValidation.validateLanguageCode(languageCode: String): String
```
Validates language code is supported. Returns "en" if unsupported.

Supported languages: en, es, fr, de, it, pt, ru, ja, ko, zh

**Example**:
```kotlin
val validLanguage = SettingsValidation.validateLanguageCode(userInput)
viewModel.setLanguage(validLanguage)
```

#### Email Validation
```kotlin
SettingsValidation.isValidEmail(email: String): Boolean
```
Validates email format using regex pattern.

**Example**:
```kotlin
if (SettingsValidation.isValidEmail(email)) {
    viewModel.setGdAccountEmail(email)
} else {
    showError("Invalid email format")
}
```

#### Color Validation
```kotlin
SettingsValidation.isValidColor(color: Int): Boolean
```
Validates color value is a valid ARGB integer (non-zero).

**Example**:
```kotlin
if (SettingsValidation.isValidColor(selectedColor)) {
    viewModel.setDayModeBackgroundColor(selectedColor)
}
```

#### File Path Validation
```kotlin
SettingsValidation.isValidFilePath(path: String): Boolean
```
Validates file path doesn't contain invalid characters.

**Example**:
```kotlin
if (SettingsValidation.isValidFilePath(fontPath)) {
    viewModel.setFontPath(fontPath)
} else {
    showError("Invalid file path")
}
```

#### Backup Interval Validation
```kotlin
SettingsValidation.validateBackupInterval(interval: String): String
```
Validates backup interval is one of: "daily", "weekly", "monthly". Defaults to "daily".

**Example**:
```kotlin
val validInterval = SettingsValidation.validateBackupInterval(userSelection)
viewModel.setGdBackupInterval(validInterval)
```

#### Internet Type Validation
```kotlin
SettingsValidation.validateInternetType(type: String): String
```
Validates internet type is one of: "wifi", "any". Defaults to "wifi".

**Example**:
```kotlin
val validType = SettingsValidation.validateInternetType(userSelection)
viewModel.setGdInternetType(validType)
```

#### Timestamp Validation
```kotlin
SettingsValidation.validateTimestamp(timestamp: Long): Long
```
Validates timestamp is not in the future. Clamps to current time if needed.

**Example**:
```kotlin
val validTimestamp = SettingsValidation.validateTimestamp(backupTime)
viewModel.setLastBackup(validTimestamp)
```

## 2. State Management

**Location**: `settings/src/main/java/io/github/gmathi/novellibrary/settings/viewmodel/BaseSettingsViewModel.kt`

### Purpose

Provides common state management patterns used across all settings ViewModels to ensure consistent behavior and reduce boilerplate code.

### Base ViewModel

All settings ViewModels should extend `BaseSettingsViewModel`:

```kotlin
class MySettingsViewModel(
    repository: SettingsRepositoryDataStore
) : BaseSettingsViewModel(repository) {
    // ViewModel implementation
}
```

### Available Patterns

#### Converting Flow to StateFlow
```kotlin
protected fun <T> Flow<T>.asStateFlow(initialValue: T): StateFlow<T>
```

Converts a repository Flow to StateFlow with standard lifecycle handling.

**Example**:
```kotlin
val textSize: StateFlow<Int> = repository.textSize
    .asStateFlow(initialValue = 16)
```

**Benefits**:
- Automatic lifecycle management with WhileSubscribed(5000)
- Consistent sharing configuration across all ViewModels
- Reduces boilerplate code

#### Updating Settings
```kotlin
protected fun updateSetting(block: suspend () -> Unit)
```

Launches a coroutine in viewModelScope to update a setting value.

**Example**:
```kotlin
fun setTextSize(size: Int) {
    updateSetting {
        repository.setTextSize(size)
    }
}
```

#### Updating Settings with Validation
```kotlin
protected fun <T> updateSettingWithValidation(
    value: T,
    validator: (T) -> T,
    updater: suspend (T) -> Unit
)
```

Launches a coroutine to update a setting with validation.

**Example**:
```kotlin
fun setTextSize(size: Int) {
    updateSettingWithValidation(
        value = size,
        validator = SettingsValidation::validateTextSize,
        updater = repository::setTextSize
    )
}
```

### Standard Sharing Configuration

The base ViewModel uses `SharingStarted.WhileSubscribed(5000)`:
- Keeps upstream flow active for 5 seconds after last subscriber unsubscribes
- Allows quick resubscription without restarting the flow
- Recommended configuration for UI state in Android

## 3. Navigation Helpers

**Location**: `settings/src/main/java/io/github/gmathi/novellibrary/settings/util/SettingsNavigation.kt`

### Purpose

Provides reusable navigation patterns to ensure consistent navigation behavior across all settings screens.

### Available Functions

#### Navigate To
```kotlin
SettingsNavigation.navigateTo(
    navController: NavController,
    route: String,
    singleTop: Boolean = true
)
```

Navigates to a destination with standard animation and back stack handling.

**Example**:
```kotlin
SettingsNavigation.navigateTo(navController, SettingsRoute.Reader.route)
```

#### Navigate Back
```kotlin
SettingsNavigation.navigateBack(navController: NavController): Boolean
```

Navigates back to the previous screen. Returns true if successful.

**Example**:
```kotlin
if (!SettingsNavigation.navigateBack(navController)) {
    // Already at root, exit settings
    onNavigateBack()
}
```

#### Navigate Back To
```kotlin
SettingsNavigation.navigateBackTo(
    navController: NavController,
    route: String,
    inclusive: Boolean = false
): Boolean
```

Navigates back to a specific destination in the back stack.

**Example**:
```kotlin
// Navigate back to main settings, removing intermediate screens
SettingsNavigation.navigateBackTo(
    navController,
    SettingsRoute.Main.route,
    inclusive = false
)
```

#### Navigate and Clear Back Stack
```kotlin
SettingsNavigation.navigateAndClearBackStack(
    navController: NavController,
    route: String
)
```

Navigates to a destination and clears the back stack. Useful for "home" screens.

**Example**:
```kotlin
// Navigate to main settings and clear back stack
SettingsNavigation.navigateAndClearBackStack(
    navController,
    SettingsRoute.Main.route
)
```

#### Navigate and Replace
```kotlin
SettingsNavigation.navigateAndReplace(
    navController: NavController,
    route: String
)
```

Navigates to a destination, replacing the current screen in the back stack.

**Example**:
```kotlin
// Replace current screen with reader settings
SettingsNavigation.navigateAndReplace(
    navController,
    SettingsRoute.Reader.route
)
```

#### Can Navigate Back
```kotlin
SettingsNavigation.canNavigateBack(navController: NavController): Boolean
```

Checks if the navigation controller can navigate back.

**Example**:
```kotlin
if (SettingsNavigation.canNavigateBack(navController)) {
    // Show back button
}
```

#### Get Current Route
```kotlin
SettingsNavigation.getCurrentRoute(navController: NavController): String?
```

Gets the current route from the navigation controller.

**Example**:
```kotlin
val currentRoute = SettingsNavigation.getCurrentRoute(navController)
if (currentRoute == SettingsRoute.Main.route) {
    // At main settings screen
}
```

#### Standard Navigation Options
```kotlin
SettingsNavigation.standardNavOptions(): NavOptionsBuilder.() -> Unit
```

Provides standard navigation options for consistent behavior:
- Single top: Avoids duplicate destinations
- Restore state: Preserves screen state when navigating back
- Save state: Saves screen state when navigating away

**Example**:
```kotlin
navController.navigate(route, SettingsNavigation.standardNavOptions())
```

## 4. Error Handling

**Location**: `settings/src/main/java/io/github/gmathi/novellibrary/settings/util/SettingsError.kt`

### Purpose

Provides type-safe error handling and result types for settings operations.

### Error Types

#### SettingsError (Sealed Class)

Base class for all settings errors:

- `ReadError` - Error reading a setting value from storage
- `WriteError` - Error writing a setting value to storage
- `ValidationError` - Error validating a setting value
- `BackupError` - Error during backup operation
- `RestoreError` - Error during restore operation
- `SyncError` - Error during sync operation
- `NetworkError` - Network error during remote operations
- `UnknownError` - Unknown or unexpected error

**Example**:
```kotlin
when (error) {
    is SettingsError.ReadError -> handleReadError(error)
    is SettingsError.WriteError -> handleWriteError(error)
    is SettingsError.ValidationError -> handleValidationError(error)
    // ... handle other error types
}
```

### Result Type

#### SettingsResult<T>

Type-safe result type for operations that can fail:

```kotlin
sealed class SettingsResult<out T> {
    data class Success<T>(val value: T) : SettingsResult<T>()
    data class Failure(val error: SettingsError) : SettingsResult<Nothing>()
}
```

**Available Methods**:

- `getOrNull()` - Returns value or null
- `getOrDefault(default)` - Returns value or default
- `getOrThrow()` - Returns value or throws error
- `onSuccess(block)` - Executes block if successful
- `onFailure(block)` - Executes block if failed

**Example**:
```kotlin
val result = performBackup()
result
    .onSuccess { backup ->
        showMessage("Backup successful: ${backup.size}")
    }
    .onFailure { error ->
        showError(SettingsErrorHandler.handleError(error))
    }
```

### Error Handler

#### SettingsErrorHandler

Provides reusable error handling logic:

##### Handle Error
```kotlin
SettingsErrorHandler.handleError(error: SettingsError): String
```

Handles an error by logging and returning a user-friendly message.

**Example**:
```kotlin
try {
    performOperation()
} catch (e: Exception) {
    val error = SettingsError.UnknownError(e.message ?: "Unknown error", e)
    val message = SettingsErrorHandler.handleError(error)
    showError(message)
}
```

##### With Error Handling (Suspend)
```kotlin
suspend fun <T> SettingsErrorHandler.withErrorHandling(
    block: suspend () -> T
): SettingsResult<T>
```

Wraps a suspend function with error handling.

**Example**:
```kotlin
suspend fun createBackup(): SettingsResult<Backup> {
    return SettingsErrorHandler.withErrorHandling {
        // Perform backup operation
        performBackupOperation()
    }
}
```

##### With Error Handling (Sync)
```kotlin
fun <T> SettingsErrorHandler.withErrorHandlingSync(
    block: () -> T
): SettingsResult<T>
```

Wraps a regular function with error handling.

**Example**:
```kotlin
fun validateSettings(): SettingsResult<Boolean> {
    return SettingsErrorHandler.withErrorHandlingSync {
        // Perform validation
        performValidation()
    }
}
```

##### Create Validation Error
```kotlin
SettingsErrorHandler.createValidationError(
    fieldName: String,
    value: Any?,
    reason: String
): SettingsError.ValidationError
```

Creates a validation error for an invalid value.

**Example**:
```kotlin
if (textSize < 12 || textSize > 32) {
    val error = SettingsErrorHandler.createValidationError(
        fieldName = "textSize",
        value = textSize,
        reason = "Text size must be between 12 and 32"
    )
    return SettingsResult.Failure(error)
}
```

## Usage Guidelines

### When to Use Validation

Always validate user input before persisting to the repository:

```kotlin
fun setTextSize(size: Int) {
    val validSize = SettingsValidation.validateTextSize(size)
    updateSetting {
        repository.setTextSize(validSize)
    }
}
```

### When to Use Base ViewModel

All settings ViewModels should extend `BaseSettingsViewModel` to ensure consistent state management:

```kotlin
class MySettingsViewModel(
    repository: SettingsRepositoryDataStore
) : BaseSettingsViewModel(repository) {
    
    val mySetting: StateFlow<String> = repository.mySetting
        .asStateFlow(initialValue = "default")
    
    fun setMySetting(value: String) {
        updateSetting {
            repository.setMySetting(value)
        }
    }
}
```

### When to Use Navigation Helpers

Use navigation helpers for all navigation operations to ensure consistency:

```kotlin
// In a Composable
Button(onClick = {
    SettingsNavigation.navigateTo(navController, SettingsRoute.Reader.route)
}) {
    Text("Reader Settings")
}

// For back navigation
IconButton(onClick = {
    if (!SettingsNavigation.navigateBack(navController)) {
        onNavigateBack() // Exit settings
    }
}) {
    Icon(Icons.Default.ArrowBack, "Back")
}
```

### When to Use Error Handling

Use error handling for operations that can fail:

```kotlin
suspend fun performBackup(): SettingsResult<Backup> {
    return SettingsErrorHandler.withErrorHandling {
        // Perform backup operation
        val backup = createBackup()
        
        // Validate backup
        if (!isValidBackup(backup)) {
            throw IllegalStateException("Invalid backup created")
        }
        
        backup
    }
}

// In UI
viewModelScope.launch {
    performBackup()
        .onSuccess { backup ->
            _uiState.value = UiState.Success("Backup created: ${backup.size}")
        }
        .onFailure { error ->
            _uiState.value = UiState.Error(SettingsErrorHandler.handleError(error))
        }
}
```

## Benefits

### Code Reusability
- Reduces code duplication across ViewModels and screens
- Ensures consistent behavior across all settings

### Maintainability
- Changes to validation logic only need to be made in one place
- Easier to update navigation behavior across all screens
- Centralized error handling makes debugging easier

### Type Safety
- Sealed error types provide compile-time safety
- Result types prevent forgetting to handle errors
- Validation functions ensure values are always valid

### Consistency
- All settings screens use the same patterns
- Users experience consistent behavior
- Developers can easily understand and modify code

## Testing

All shared functionality is designed to be easily testable:

### Testing Validation
```kotlin
@Test
fun `validateTextSize clamps to valid range`() {
    assertEquals(12, SettingsValidation.validateTextSize(5))
    assertEquals(16, SettingsValidation.validateTextSize(16))
    assertEquals(32, SettingsValidation.validateTextSize(50))
}
```

### Testing Error Handling
```kotlin
@Test
fun `withErrorHandling returns Success for successful operation`() = runTest {
    val result = SettingsErrorHandler.withErrorHandling {
        "success"
    }
    assertTrue(result is SettingsResult.Success)
    assertEquals("success", result.getOrNull())
}

@Test
fun `withErrorHandling returns Failure for failed operation`() = runTest {
    val result = SettingsErrorHandler.withErrorHandling {
        throw Exception("test error")
    }
    assertTrue(result is SettingsResult.Failure)
}
```

### Testing Navigation
```kotlin
@Test
fun `navigateTo navigates to correct route`() {
    val navController = TestNavHostController(context)
    SettingsNavigation.navigateTo(navController, "test_route")
    assertEquals("test_route", navController.currentDestination?.route)
}
```

## Future Enhancements

Potential improvements to shared functionality:

1. **Analytics Integration** - Add analytics tracking to navigation helpers
2. **Offline Support** - Add offline detection to error handling
3. **Retry Logic** - Add automatic retry for transient errors
4. **Validation Messages** - Add localized validation error messages
5. **Custom Validators** - Allow registering custom validation functions
6. **Navigation Animations** - Add customizable navigation animations
7. **Error Recovery** - Add automatic error recovery strategies

## Related Documentation

- [Integration Guide](INTEGRATION.md) - How to integrate the settings module
- [Architecture Overview](../design.md) - Overall architecture design
- [Testing Strategy](../design.md#testing-strategy) - Testing approach
