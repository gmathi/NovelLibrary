# Settings Screens Compose UI Tests

This directory contains comprehensive Compose UI tests for all 6 settings screens in the settings module.

## Test Coverage

### 1. MainSettingsScreenTest
Tests for the main settings entry point screen.

**Coverage:**
- ✅ Verify all 5 categories are displayed (Reader, Backup & Sync, General, Advanced, About)
- ✅ Test navigation to each category screen
- ✅ Test back navigation
- ✅ Test developer mode toggle

**Test Count:** 7 tests

### 2. ReaderSettingsScreenTest
Tests for the reader settings screen (consolidates 4 old activities).

**Coverage:**
- ✅ Verify 4 sections displayed (Text & Display, Theme, Scroll Behavior, Auto-Scroll)
- ✅ Test text size slider functionality
- ✅ Test font dropdown selection
- ✅ Test limit image width switch
- ✅ Test theme settings (day/night mode, text colors)
- ✅ Test scroll behavior switches (reader mode, Japanese swipe, volume keys, etc.)
- ✅ Test conditional visibility (volume scroll distance, auto-scroll settings)
- ✅ Verify state updates for all settings
- ✅ Test back navigation

**Test Count:** 14 tests

### 3. BackupAndSyncScreenTest
Tests for the backup and sync screen with tabbed interface (consolidates 5 old activities).

**Coverage:**
- ✅ Verify 2 tabs displayed (Backup and Sync)
- ✅ Test tab navigation and switching
- ✅ Test backup tab: local backup section (create, restore)
- ✅ Test backup tab: Google Drive section (interval, network type dropdowns)
- ✅ Test sync tab: enable sync switch
- ✅ Test sync tab: login button
- ✅ Test sync tab: conditional sync options visibility
- ✅ Test sync options switches (add novels, delete novels, bookmarks)
- ✅ Verify callback triggers for all actions
- ✅ Test back navigation

**Test Count:** 13 tests

### 4. GeneralSettingsScreenTest
Tests for the general settings screen (consolidates 3 old activities).

**Coverage:**
- ✅ Verify 4 sections displayed (Appearance, Language, Notifications, Other Settings)
- ✅ Test dark theme switch and description updates
- ✅ Test inline language selection dropdown (reduces navigation depth)
- ✅ Test notification switches (enable notifications, chapters left badge)
- ✅ Test other settings switches (JavaScript, library screen, developer mode)
- ✅ Verify state updates for all switches
- ✅ Test back navigation

**Test Count:** 11 tests

### 5. AdvancedSettingsScreenTest
Tests for the advanced settings screen (consolidates technical settings).

**Coverage:**
- ✅ Verify 4 sections displayed (Network, Cache, Debug, Data)
- ✅ Test network settings (Cloudflare bypass, JavaScript, timeout)
- ✅ Test cache settings (clear cache, cache management)
- ✅ Test debug settings (developer mode, debug logging)
- ✅ Test data settings (migration tools, reset settings)
- ✅ Verify technical settings are properly grouped
- ✅ Verify callback triggers for all actions
- ✅ Test back navigation

**Test Count:** 10 tests

### 6. AboutScreenTest
Tests for the about screen (consolidates 3 old activities).

**Coverage:**
- ✅ Verify version info displayed correctly
- ✅ Test app information section
- ✅ Test credits section (contributors, licenses)
- ✅ Test legal section (copyright, privacy policy, terms)
- ✅ Test navigation to sub-screens (contributors, copyright, licenses)
- ✅ Test check for updates functionality
- ✅ Verify all navigation items present
- ✅ Test back navigation

**Test Count:** 11 tests

## Total Test Count

**66 Compose UI tests** covering all 6 settings screens.

## Running the Tests

### Prerequisites
- Android device or emulator must be connected
- Minimum API level: 23

### Run All Settings Screen Tests
```bash
./gradlew :settings:connectedNormalDebugAndroidTest
```

### Run Specific Test Class
```bash
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.MainSettingsScreenTest"
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.ReaderSettingsScreenTest"
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.BackupAndSyncScreenTest"
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.GeneralSettingsScreenTest"
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.AdvancedSettingsScreenTest"
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.AboutScreenTest"
```

### Run Specific Test Method
```bash
./gradlew :settings:connectedNormalDebugAndroidTest --tests "*.MainSettingsScreenTest.mainSettingsScreen_displaysAllFiveCategories"
```

## Test Architecture

### Test Structure
Each test file follows this structure:
1. **Setup**: Create ViewModels with fake data stores
2. **Compose Content**: Set up the screen with test callbacks
3. **Interactions**: Perform user actions (clicks, toggles, etc.)
4. **Assertions**: Verify UI state and callback invocations

### Key Testing Patterns

#### 1. State Verification
```kotlin
val initialState = runBlocking { viewModel.setting.first() }
composeTestRule.onNodeWithText("Setting").performClick()
composeTestRule.waitForIdle()
val updatedState = runBlocking { viewModel.setting.first() }
assert(updatedState != initialState)
```

#### 2. Callback Verification
```kotlin
var callbackCalled = false
composeTestRule.setContent {
    Screen(onAction = { callbackCalled = true })
}
composeTestRule.onNodeWithText("Action").performClick()
assert(callbackCalled)
```

#### 3. Conditional Visibility
```kotlin
viewModel.setEnabled(true)
composeTestRule.waitForIdle()
composeTestRule.onNodeWithText("Conditional Setting").assertIsDisplayed()
```

#### 4. Navigation Testing
```kotlin
var navigated = false
composeTestRule.setContent {
    Screen(onNavigate = { navigated = true })
}
composeTestRule.onNodeWithText("Navigate").performClick()
assert(navigated)
```

## Test Dependencies

The following dependencies are required (already configured in `settings/build.gradle`):

```groovy
androidTestImplementation platform(libs.compose.bom)
androidTestImplementation libs.androidx.junit
androidTestImplementation libs.androidx.espresso
androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
debugImplementation libs.compose.ui.tooling
debugImplementation 'androidx.compose.ui:ui-test-manifest'
```

## Test Data

All tests use `FakeSettingsDataStore` to provide isolated, predictable test data without requiring actual DataStore persistence.

## Continuous Integration

These tests can be integrated into CI/CD pipelines using:
- Firebase Test Lab
- AWS Device Farm
- Local emulator with Gradle commands

## Maintenance

When adding new settings or modifying existing screens:
1. Update the corresponding test file
2. Add tests for new UI elements
3. Update tests for modified behavior
4. Ensure all tests pass before merging

## Requirements Satisfied

This test suite satisfies **Requirement 10.3** from the requirements document:
- ✅ Compose UI tests for all settings screens
- ✅ Test rendering of all UI components
- ✅ Test user interactions (clicks, toggles, selections)
- ✅ Test state changes and updates
- ✅ Test navigation between screens
- ✅ Test conditional visibility of UI elements
