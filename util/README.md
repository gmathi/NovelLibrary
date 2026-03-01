# Util Module

## Purpose

The **util** module provides independent utility classes, Kotlin extensions, and helper functions that are shared across the Novel Library application. It contains pure utility implementations without business logic or app-specific dependencies.

This module focuses on **reusable utilities** and **extension functions** that enhance the Android framework and Kotlin standard library with commonly needed functionality.

## Module Independence

The util module has **zero dependencies** on other project modules:
- ❌ No dependency on `core` module
- ❌ No dependency on `common` module
- ❌ No dependency on `app` module
- ❌ No dependency on `settings` module

This independence ensures that util remains a pure utility layer that can be used by any module without creating circular dependencies.

## Contents

### System Utilities

#### Base64 Extensions
**Location**: `io.github.gmathi.novellibrary.util.system.Base64Ext`

Extension functions for Base64 encoding and decoding.

**Functions**:
- `String.encodeBase64ToString(): String`: Encode string to Base64 string
- `String.encodeBase64ToByteArray(): ByteArray`: Encode string to Base64 byte array
- `ByteArray.encodeBase64ToString(): String`: Encode byte array to Base64 string
- `String.decodeBase64(): String`: Decode Base64 string to string
- `String.decodeBase64ToByteArray(): ByteArray`: Decode Base64 string to byte array
- `ByteArray.decodeBase64ToString(): String`: Decode Base64 byte array to string
- `ByteArray.encodeBase64(): ByteArray`: Encode byte array to Base64
- `ByteArray.decodeBase64(): ByteArray`: Decode Base64 byte array

**Usage Example**:
```kotlin
val encoded = "Hello World".encodeBase64ToString()
val decoded = encoded.decodeBase64()
```

### Language Utilities

#### Hash
**Location**: `io.github.gmathi.novellibrary.util.lang.Hash`

Object providing cryptographic hash functions.

**Functions**:
- `sha256(bytes: ByteArray): String`: Compute SHA-256 hash of byte array
- `sha256(string: String): String`: Compute SHA-256 hash of string
- `md5(bytes: ByteArray): String`: Compute MD5 hash of byte array
- `md5(string: String): String`: Compute MD5 hash of string

**Usage Example**:
```kotlin
val hash = Hash.sha256("my-data")
val md5Hash = Hash.md5("my-data")
```

#### Coroutines Extensions
**Location**: `io.github.gmathi.novellibrary.util.lang.CoroutinesExtensions`

Extension functions for simplified coroutine usage.

**Functions**:
- `launchUI(block: suspend CoroutineScope.() -> Unit): Job`: Launch coroutine on Main dispatcher
- `launchIO(block: suspend CoroutineScope.() -> Unit): Job`: Launch coroutine on IO dispatcher
- `launchNow(block: suspend CoroutineScope.() -> Unit): Job`: Launch coroutine immediately on Main dispatcher
- `CoroutineScope.launchUI(block: suspend CoroutineScope.() -> Unit): Job`: Launch on Main within scope
- `CoroutineScope.launchIO(block: suspend CoroutineScope.() -> Unit): Job`: Launch on IO within scope
- `suspend fun <T> withUIContext(block: suspend CoroutineScope.() -> T): T`: Execute block on Main dispatcher
- `suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T`: Execute block on IO dispatcher

**Usage Example**:
```kotlin
launchIO {
    val data = fetchData()
    withUIContext {
        updateUI(data)
    }
}
```

#### Date Extensions
**Location**: `io.github.gmathi.novellibrary.util.lang.DateExtensions`

Extension functions for date and time operations.

**Functions**:
- `Date.toDateTimestampString(dateFormatter: DateFormat): String`: Format date with time
- `Date.toTimestampString(): String`: Format as short time string
- `Long.toDateKey(): Date`: Convert epoch to date key (midnight)
- `Long.toCalendar(): Calendar?`: Convert epoch to Calendar instance

**Usage Example**:
```kotlin
val timestamp = Date().toTimestampString()
val dateKey = System.currentTimeMillis().toDateKey()
```

### View Utilities

#### ProgressLayout
**Location**: `io.github.gmathi.novellibrary.util.view.ProgressLayout`

Custom view for managing loading, content, empty, and error states.

**States**:
- `CONTENT`: Show content views
- `LOADING`: Show loading indicator
- `EMPTY`: Show empty state
- `ERROR`: Show error state

**Key Methods**:
- `showContent()`: Display content views
- `showContent(skipIds: List<Int>)`: Display content, skip specific view IDs
- `showLoading(rawId, drawableId, message, buttonText, onClickListener)`: Show loading state
- `updateLoadingStatus(value: String)`: Update loading message
- `showEmpty(rawId, drawableId, message, buttonText, onClickListener)`: Show empty state
- `showError(rawId, drawableId, message, buttonText, onClickListener)`: Show error state

**State Queries**:
- `getState(): String`: Get current state
- `isContent(): Boolean`: Check if showing content
- `isLoading(): Boolean`: Check if loading
- `isEmpty(): Boolean`: Check if empty
- `isError(): Boolean`: Check if error

**Usage Example**:
```kotlin
progressLayout.showLoading(
    rawId = null,
    drawableId = null,
    message = "Loading...",
    buttonText = null,
    onClickListener = null
)

// After data loads
progressLayout.showContent()

// On error
progressLayout.showError(
    rawId = null,
    drawableId = R.drawable.error_icon,
    message = "Failed to load data",
    buttonText = "Retry",
    onClickListener = { retry() }
)
```

#### CustomDividerItemDecoration
**Location**: `io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration`

RecyclerView item decoration that hides the divider after the last item.

**Constructor**:
- `CustomDividerItemDecoration(context: Context, orientation: Int)`

**Usage Example**:
```kotlin
val divider = CustomDividerItemDecoration(context, DividerItemDecoration.VERTICAL)
recyclerView.addItemDecoration(divider)
```

### View Extensions

#### View Extensions
**Location**: `io.github.gmathi.novellibrary.util.view.extensions.ViewExt`

Extension functions for View operations.

**Functions**:
- `View.getCoordinates(): Point`: Get center coordinates of view
- `View.snack(message: String, length: Int, f: Snackbar.() -> Unit): Snackbar`: Show snackbar
- `View.setTooltip(@StringRes stringRes: Int)`: Add tooltip on long press
- `View.popupMenu(@MenuRes menuRes: Int, initMenu, onMenuItemClick): PopupMenu`: Show popup menu
- `ExtendedFloatingActionButton.shrinkOnScroll(recycler: RecyclerView)`: Shrink FAB on scroll
- `ChipGroup.setChips(items: List<String>?, onClick)`: Replace chips in group
- `View.applyInsets(block: (view: View, systemInsets: Insets) -> Unit)`: Apply window insets
- `View.applyTopSystemWindowInsetsPadding()`: Apply top insets as padding

**Usage Example**:
```kotlin
// Show snackbar
view.snack("Item deleted") {
    setAction("Undo") { restore() }
}

// Apply window insets for edge-to-edge
toolbar.applyTopSystemWindowInsetsPadding()

// Popup menu
button.popupMenu(R.menu.options_menu) { menuItem ->
    when (menuItem.itemId) {
        R.id.action_edit -> handleEdit()
        R.id.action_delete -> handleDelete()
    }
    true
}
```

#### TextView Extensions
**Location**: `io.github.gmathi.novellibrary.util.view.extensions.TextViewExt`

Extension functions for TextView operations (if any).

#### ViewGroup Extensions
**Location**: `io.github.gmathi.novellibrary.util.view.extensions.ViewGroupExt`

Extension functions for ViewGroup operations (if any).

#### Window Extensions
**Location**: `io.github.gmathi.novellibrary.util.view.extensions.WindowExt`

Extension functions for Window operations (if any).

## When to Add Utilities to Util

Add new utilities to the util module when:

✅ **Pure utility functions**: The function is a general-purpose helper without business logic

✅ **Framework extensions**: Extending Android framework or Kotlin standard library with commonly needed functionality

✅ **Shared across modules**: Multiple modules need the same utility function

✅ **No dependencies**: The utility doesn't depend on app-specific classes or business logic

✅ **Independent**: The utility can exist without knowing about core abstractions or common models

❌ **Don't add to util if**:
- The utility contains business logic (keep it in app module)
- The utility depends on database, network, or app-specific services (keep it in app module)
- The utility is only used in one module (keep it local)
- The utility requires dependencies on core, common, or app modules (violates independence)
- It's a data model (those belong in common module)
- It's an abstraction or interface (those belong in core module)

## Guidelines

### Adding New Utilities

When adding a new utility to util:

1. Keep it pure - no side effects or state
2. Make it generic and reusable
3. Use extension functions for framework enhancements
4. Avoid business logic - utilities should be domain-agnostic
5. Document usage with KDoc comments and examples
6. Consider thread safety for concurrent usage
7. Add unit tests for all utility functions

### Package Organization

Organize utilities by category:
- `util.system`: System-level utilities (encoding, file operations, etc.)
- `util.lang`: Language extensions (coroutines, dates, hashing, etc.)
- `util.view`: View-related utilities and extensions
- `util.view.extensions`: View extension functions

### Extension Functions

When creating extension functions:
1. Use `inline` for simple functions to reduce overhead
2. Provide default parameters for flexibility
3. Use `@StringRes`, `@DrawableRes`, etc. for resource IDs
4. Document parameters and return values
5. Consider nullability carefully

## Build Configuration

**Namespace**: `io.github.gmathi.novellibrary.util`  
**Min SDK**: 23  
**Target SDK**: 36  
**Product Flavors**: mirror, canary, normal

**Key Dependencies**:
- AndroidX AppCompat
- AndroidX ConstraintLayout
- AndroidX Preference
- AndroidX CardView
- AndroidX RecyclerView
- Material Design Components
- Lottie (for animations in ProgressLayout)
- Kotlin Coroutines

**No project module dependencies** - util is completely independent.

## Resources

The util module contains:
- Layout files for ProgressLayout states (loading, empty, error)
- Drawable resources for utility views
- Color resources for utility components
- Minimal string resources

## Package Structure

```
io.github.gmathi.novellibrary.util/
├── system/
│   └── Base64Ext.kt
├── lang/
│   ├── CoroutinesExtensions.kt
│   ├── DateExtensions.kt
│   └── Hash.kt
└── view/
    ├── CustomDividerItemDecoration.kt
    ├── ProgressLayout.java
    └── extensions/
        ├── TextViewExt.kt
        ├── ViewExt.kt
        ├── ViewGroupExt.kt
        └── WindowExt.kt
```

## Testing

The util module includes:
- Unit tests for utility functions (hash, encoding, date operations)
- Unit tests for ProgressLayout state transitions
- Unit tests for view extensions
- Property-based tests verifying zero project dependencies

## Common Use Cases

### Edge-to-Edge Display
```kotlin
// In activity
override fun applyWindowInsets() {
    toolbar.applyTopSystemWindowInsetsPadding()
}
```

### State Management
```kotlin
// Show loading
progressLayout.showLoading(null, null, "Loading data...", null, null)

// Show content when ready
progressLayout.showContent()

// Show error with retry
progressLayout.showError(
    null,
    R.drawable.ic_error,
    "Failed to load",
    "Retry"
) { loadData() }
```

### Coroutines
```kotlin
launchIO {
    val result = performNetworkCall()
    withUIContext {
        updateUI(result)
    }
}
```

### Hashing
```kotlin
val hash = Hash.sha256(userInput)
val encoded = data.encodeBase64ToString()
```

## Related Modules

- **core**: Provides abstractions and base classes (no dependency relationship with util)
- **common**: Provides models and adapters (no dependency relationship with util)
- **app**: Depends on util and uses its utilities
- **settings**: Depends on util and uses its utilities

---

**Remember**: Util provides pure, reusable utilities without business logic. Keep it independent, generic, and focused on framework enhancements.
