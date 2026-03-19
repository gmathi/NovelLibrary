# Testing Note

## Test File Location

The test file `PersistentSearchViewTest.kt` was intentionally not included in the main source directory to avoid compilation issues during the build process.

If you want to add tests for the PersistentSearchView component, create the test file in the appropriate test directory:

### For Unit Tests
```
app/src/test/java/io/github/gmathi/novellibrary/compose/search/PersistentSearchViewTest.kt
```

### For Instrumented Tests (UI Tests)
```
app/src/androidTest/java/io/github/gmathi/novellibrary/compose/search/PersistentSearchViewTest.kt
```

## Test Example

Here's a simple test example you can use:

```kotlin
package io.github.gmathi.novellibrary.compose.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class PersistentSearchViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchView_initialState_displaysCorrectly() {
        val searchState = PersistentSearchState(initialLogoText = "Search")

        composeTestRule.setContent {
            PersistentSearchView(
                state = searchState,
                hint = "Search novels..."
            )
        }

        composeTestRule.onNodeWithText("Search").assertIsDisplayed()
    }

    @Test
    fun searchView_clickSearchIcon_opensEditMode() {
        val searchState = PersistentSearchState()

        composeTestRule.setContent {
            PersistentSearchView(
                state = searchState,
                hint = "Search novels..."
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()
        assert(searchState.isEditing)
    }
}
```

## Required Dependencies

Make sure you have these dependencies in your `build.gradle`:

```gradle
androidTestImplementation "androidx.compose.ui:ui-test-junit4"
debugImplementation "androidx.compose.ui:ui-test-manifest"
```

## Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```
