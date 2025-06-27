# MVVM Architecture Implementation

This document describes the MVVM (Model-View-ViewModel) architecture implementation for the NovelLibrary project, specifically focusing on the ChaptersFragment.

## Architecture Overview

The project has been updated to follow the MVVM pattern with the following components:

### 1. View (Fragment)
- **ChaptersFragment**: Handles UI interactions and observes ViewModel data
- Uses ViewBinding for type-safe view access
- Observes LiveData from ViewModel for reactive UI updates
- Maintains separation of concerns by delegating business logic to ViewModel

### 2. ViewModel
- **ChaptersFragmentViewModel**: Manages UI state and business logic
- **ChaptersViewModel**: Parent ViewModel for the activity (existing)
- Uses LiveData for reactive data binding
- Handles lifecycle events and configuration changes
- Manages UI state through sealed class `ChaptersUiState`

### 3. Repository
- **ChaptersRepository**: Single source of truth for data operations
- Handles database operations and network requests
- Provides clean API for data access
- Implements Repository pattern for data abstraction

### 4. Model
- **Novel**: Data model for novel information
- **WebPage**: Data model for chapter information
- **WebPageSettings**: Data model for chapter settings

## Key Features Implemented

### UI State Management
```kotlin
sealed class ChaptersUiState {
    object Loading : ChaptersUiState()
    object Success : ChaptersUiState()
    object Empty : ChaptersUiState()
    object NoInternet : ChaptersUiState()
    data class Error(val message: String) : ChaptersUiState()
}
```

### LiveData Observers
- **uiState**: Manages loading, success, error, and empty states
- **chapters**: Observable list of chapters
- **chapterSettings**: Observable list of chapter settings
- **selectedChapters**: Observable set of selected chapters
- **scrollToPosition**: Observable scroll position for auto-scrolling

### Error Handling
- Network connectivity checks
- Database operation error handling
- User-friendly error messages with retry functionality
- Proper error logging with Logs utility

### Loading States
- Loading indicators during data fetching
- Progress updates for long-running operations
- Swipe-to-refresh functionality
- Empty state handling

## File Structure

```
app/src/main/java/io/github/gmathi/novellibrary/
├── fragment/
│   └── ChaptersFragment.kt                    # View layer
├── viewmodel/
│   ├── ChaptersFragmentViewModel.kt           # Fragment ViewModel
│   └── ChaptersViewModel.kt                   # Activity ViewModel (existing)
├── data/
│   └── repository/
│       └── ChaptersRepository.kt              # Repository layer
└── model/
    └── database/
        ├── Novel.kt                           # Data models
        ├── WebPage.kt
        └── WebPageSettings.kt
```

## Usage Example

### Fragment Implementation
```kotlin
class ChaptersFragment : BaseFragment() {
    private val viewModel: ChaptersFragmentViewModel by viewModels()
    
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        
        // Initialize ViewModel
        viewModel.init(novel, translatorSourceName, this, requireContext(), parentViewModel)
        
        // Setup observers
        setupObservers()
    }
    
    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is ChaptersUiState.Loading -> showLoading()
                is ChaptersUiState.Success -> showContent()
                is ChaptersUiState.Error -> showError(uiState.message)
                // ... other states
            }
        }
    }
}
```

### ViewModel Implementation
```kotlin
class ChaptersFragmentViewModel : ViewModel() {
    private val _uiState = MutableLiveData<ChaptersUiState>()
    val uiState: LiveData<ChaptersUiState> = _uiState
    
    fun loadChapters() {
        viewModelScope.launch {
            try {
                _uiState.value = ChaptersUiState.Loading
                val chapters = repository.getChaptersFromDatabase(novel.id)
                _uiState.value = ChaptersUiState.Success
            } catch (e: Exception) {
                _uiState.value = ChaptersUiState.Error(e.message)
            }
        }
    }
}
```

## Benefits of MVVM Implementation

1. **Separation of Concerns**: Clear separation between UI logic and business logic
2. **Testability**: ViewModels can be easily unit tested
3. **Lifecycle Awareness**: Automatic lifecycle management with LiveData
4. **Configuration Changes**: Data survives configuration changes
5. **Reactive Programming**: Reactive UI updates with LiveData
6. **Error Handling**: Centralized error handling and user feedback
7. **Loading States**: Proper loading state management
8. **Repository Pattern**: Clean data access layer

## Dependencies Used

- `androidx.lifecycle:lifecycle-viewmodel-ktx`: ViewModel implementation
- `androidx.lifecycle:lifecycle-livedata-ktx`: LiveData for reactive programming
- `kotlinx-coroutines`: Asynchronous programming
- `uy.kohesive.injekt`: Dependency injection

## Migration Notes

The implementation maintains backward compatibility while introducing MVVM patterns:

- Existing functionality is preserved
- UI loading states and error logs are maintained
- EventBus integration is handled through ViewModel
- Parent-child ViewModel communication is established
- Repository pattern abstracts data operations

## Future Improvements

1. **Unit Testing**: Add comprehensive unit tests for ViewModels
2. **Dependency Injection**: Consider migrating to Hilt for better DI
3. **Navigation**: Implement Navigation Component for better navigation
4. **Data Binding**: Enhance with Data Binding for more reactive UI
5. **Paging**: Implement Paging 3 for large chapter lists 