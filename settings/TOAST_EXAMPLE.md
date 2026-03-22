# Toast Implementation in Jetpack Compose

This document explains how to show Toast messages when the developer flag is set in the MainSettingsViewModel.

## Architecture Overview

The implementation uses a **Channel-based approach** for one-time events (like showing toasts) in Compose:

1. **ViewModel**: Emits toast messages through a Channel
2. **Composable**: Observes the Channel and displays Toast when messages arrive

## Implementation Details

### 1. ViewModel (MainSettingsViewModel.kt)

```kotlin
class MainSettingsViewModel(
    private val repository: SettingsRepositoryDataStore
) : ViewModel() {

    var count: Int = 0

    // Channel for one-time events like showing toasts
    private val _toastMessage = Channel<String>(Channel.BUFFERED)
    val toastMessage = _toastMessage.receiveAsFlow()

    val isDeveloper: StateFlow<Boolean> = repository.isDeveloper
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setDeveloper() {
        if (isDeveloper.value || count < 21) {
            count++
            return
        }
        viewModelScope.launch {
            repository.setIsDeveloper(true)
            // Send toast message when developer mode is enabled
            _toastMessage.send("Developer mode enabled!")
        }
    }
}
```

**Key Points:**
- `Channel<String>` is used instead of `StateFlow` because toasts are **one-time events**
- `Channel.BUFFERED` ensures messages aren't lost if the UI isn't ready
- `receiveAsFlow()` converts the Channel to a Flow for Compose observation
- `_toastMessage.send()` emits the toast message

### 2. Composable (MainSettingsScreen.kt)

```kotlin
@Composable
fun MainSettingsScreen(
    viewModel: MainSettingsViewModel,
    onNavigateToReader: () -> Unit,
    onNavigateToBackupSync: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    val context = LocalContext.current
    
    // Observe toast messages and display them
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    MainSettingsScreenContent(
        isDarkTheme = isDarkTheme,
        isDeveloper = isDeveloper,
        onNavigateToReader = onNavigateToReader,
        onNavigateToBackupSync = onNavigateToBackupSync,
        onNavigateToGeneral = onNavigateToGeneral,
        onNavigateToAdvanced = onNavigateToAdvanced,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateBack = onNavigateBack,
        onToggleDeveloper = { viewModel.setDeveloper() },
        modifier = modifier
    )
}
```

**Key Points:**
- `LocalContext.current` gets the Android Context needed for Toast
- `LaunchedEffect(Unit)` runs once when the composable enters the composition
- `collectLatest` collects from the Flow and cancels previous collection if a new value arrives
- `Toast.makeText()` creates and shows the toast

### 3. How It Works

1. User taps on a hidden area (or About section) 21+ times
2. `viewModel.setDeveloper()` is called
3. After 21 taps, the ViewModel:
   - Sets `isDeveloper` to `true` in the repository
   - Sends "Developer mode enabled!" message through the Channel
4. The `LaunchedEffect` in the Composable:
   - Collects the message from the Flow
   - Shows a Toast with the message

## Why Use Channel Instead of StateFlow?

| Aspect | Channel | StateFlow |
|--------|---------|-----------|
| **Purpose** | One-time events | Continuous state |
| **Behavior** | Consumed once | Always has a value |
| **Use Case** | Toasts, navigation, snackbars | UI state, settings |
| **Recomposition** | Doesn't trigger recomposition | Triggers recomposition |

**Example Problem with StateFlow:**
```kotlin
// ❌ BAD: Using StateFlow for toast
val toastMessage = MutableStateFlow<String?>(null)

// If you show a toast and then navigate away and back,
// the toast will show again because StateFlow retains the value!
```

**Solution with Channel:**
```kotlin
// ✅ GOOD: Using Channel for toast
val toastMessage = Channel<String>()

// Each message is consumed once and won't show again
// even if you navigate away and back
```

## Alternative: Using Snackbar (Compose Material3)

For a more Compose-native approach, you can use Snackbar instead of Toast:

```kotlin
@Composable
fun MainSettingsScreen(
    viewModel: MainSettingsViewModel,
    // ... other parameters
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Your content here
    }
}
```

## Testing

To test toast functionality:

```kotlin
@Test
fun `when developer mode enabled, toast message is sent`() = runTest {
    // Given
    val repository = mockk<SettingsRepositoryDataStore>()
    coEvery { repository.setIsDeveloper(true) } just Runs
    val viewModel = MainSettingsViewModel(repository)
    
    // Collect toast messages
    val messages = mutableListOf<String>()
    val job = launch {
        viewModel.toastMessage.collect { messages.add(it) }
    }
    
    // When - tap 21 times to enable developer mode
    repeat(21) { viewModel.setDeveloper() }
    
    // Then
    coVerify { repository.setIsDeveloper(true) }
    assertEquals("Developer mode enabled!", messages.first())
    
    job.cancel()
}
```

## Summary

The implementation follows these principles:

1. **Separation of Concerns**: ViewModel handles business logic, Composable handles UI
2. **One-Time Events**: Channel ensures toasts are shown once, not on every recomposition
3. **Reactive**: LaunchedEffect automatically observes and reacts to new messages
4. **Testable**: Channel-based approach is easy to test in unit tests

This pattern can be reused for other one-time events like:
- Navigation events
- Error messages
- Success confirmations
- Dialog triggers
