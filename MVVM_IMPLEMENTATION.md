# MVVM Architecture Implementation

This document describes the MVVM (Model-View-ViewModel) architecture implementation for the NovelLibrary project, specifically focusing on the ChaptersFragment and File Management components.

## Architecture Overview

The project has been updated to follow the MVVM pattern with the following components:

### 1. View (Fragment/Activity)
- **ChaptersFragment**: Handles UI interactions and observes ViewModel data
- **NovelDownloadsActivity**: Manages download queue UI and user interactions
- Uses ViewBinding for type-safe view access
- Observes LiveData from ViewModel for reactive UI updates
- Maintains separation of concerns by delegating business logic to ViewModel

### 2. ViewModel
- **ChaptersFragmentViewModel**: Manages UI state and business logic for chapters
- **ChaptersViewModel**: Parent ViewModel for the activity (existing)
- **FileManagementViewModel**: Manages file operations and storage
- **DownloadManagementViewModel**: Manages download operations and queue
- Uses LiveData for reactive data binding
- Handles lifecycle-aware data management
- Implements proper error handling and loading states

### 3. Repository
- **ChaptersRepository**: Handles chapter data operations
- **FileManagementRepository**: Handles file operations and storage management
- **DownloadManagementRepository**: Handles download operations and queue management
- Abstracts data sources (database, network, local storage)
- Provides clean API for ViewModels
- Implements proper error handling and logging

### 4. Model
- **Database Models**: Novel, WebPage, Download, WebPageSettings
- **UI State Models**: Sealed classes for different UI states
- **Data Transfer Objects**: For API responses and UI data

## Components Implementation

### ChaptersFragment MVVM Implementation

#### ChaptersFragmentViewModel
```kotlin
class ChaptersFragmentViewModel : ViewModel() {
    // UI State Management
    private val _uiState = MutableLiveData<ChaptersUiState>()
    val uiState: LiveData<ChaptersUiState> = _uiState
    
    // Data Management
    private val _chapters = MutableLiveData<List<WebPage>>()
    val chapters: LiveData<List<WebPage>> = _chapters
    
    // Operations
    fun loadChapters()
    fun updateChapterSelection()
    fun scrollToChapter()
}
```

#### ChaptersRepository
```kotlin
class ChaptersRepository {
    suspend fun getChapters(novel: Novel, translatorSourceName: String): List<WebPage>
    suspend fun updateChapterStatus(webPage: WebPage, isRead: Boolean)
    suspend fun updateChapterFavorite(webPage: WebPage, isFavorite: Boolean)
}
```

### File Management MVVM Implementation

#### FileManagementViewModel
```kotlin
class FileManagementViewModel : ViewModel() {
    // UI State Management
    private val _uiState = MutableLiveData<FileManagementUiState>()
    val uiState: LiveData<FileManagementUiState> = _uiState
    
    // Storage Information
    private val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo
    
    // File Operations
    fun deleteDownloadedChapters(context: Context, novel: Novel)
    fun copyFile(src: File, dst: File)
    fun cleanupTempFiles(context: Context)
}
```

#### FileManagementRepository
```kotlin
class FileManagementRepository {
    suspend fun getNovelDirectory(context: Context, novelName: String, novelId: Long): File
    suspend fun deleteDownloadedChapters(context: Context, novel: Novel)
    suspend fun getDirectorySize(directory: File): Long
    suspend fun copyFile(src: File, dst: File)
    suspend fun zipDirectory(sourceDir: File, zipFile: File)
    suspend fun unzipFile(contentResolver: ContentResolver, zipUri: Uri, destinationDir: File)
}
```

### Download Management MVVM Implementation

#### DownloadManagementViewModel
```kotlin
class DownloadManagementViewModel : ViewModel(), DownloadListener {
    // UI State Management
    private val _uiState = MutableLiveData<DownloadManagementUiState>()
    val uiState: LiveData<DownloadManagementUiState> = _uiState
    
    // Download Data
    private val _downloads = MutableLiveData<List<Download>>()
    val downloads: LiveData<List<Download>> = _downloads
    
    // Download Statistics
    private val _downloadStatistics = MutableLiveData<DownloadStatistics>()
    val downloadStatistics: LiveData<DownloadStatistics> = _downloadStatistics
    
    // Operations
    fun addChaptersToDownloadQueue(webPages: List<WebPage>, novel: Novel, context: Context)
    fun pauseDownloadsForNovel(novelId: Long)
    fun resumeDownloadsForNovel(novelId: Long, context: Context)
    fun deleteDownloadsForNovel(novelId: Long)
}
```

#### DownloadManagementRepository
```kotlin
class DownloadManagementRepository {
    suspend fun getAllDownloads(): List<Download>
    suspend fun getDownloadsForNovel(novelId: Long): List<Download>
    suspend fun addChaptersToDownloadQueue(webPages: List<WebPage>, novel: Novel, progressCallback: ((Int) -> Unit)?)
    suspend fun updateDownloadStatusByNovelId(status: Int, novelId: Long)
    suspend fun deleteDownloadsForNovel(novelId: Long)
    suspend fun getDownloadStatistics(): Map<String, Any>
}
```

## UI State Management

### Sealed Classes for UI States
```kotlin
sealed class ChaptersUiState {
    object Idle : ChaptersUiState()
    object Loading : ChaptersUiState()
    object Success : ChaptersUiState()
    data class Error(val message: String) : ChaptersUiState()
}

sealed class FileManagementUiState {
    object Idle : FileManagementUiState()
    object Loading : FileManagementUiState()
    object Success : FileManagementUiState()
    data class Error(val message: String) : FileManagementUiState()
}

sealed class DownloadManagementUiState {
    object Idle : DownloadManagementUiState()
    object Loading : DownloadManagementUiState()
    object Success : DownloadManagementUiState()
    data class Error(val message: String) : DownloadManagementUiState()
}
```

### Data Classes for UI Models
```kotlin
data class StorageInfo(
    val isAvailable: Boolean,
    val availableSpace: Long,
    val totalSpace: Long,
    val usedSpace: Long
) {
    val usedPercentage: Float
        get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100 else 0f
}

data class DownloadStatistics(
    val totalDownloads: Int,
    val queuedDownloads: Int,
    val runningDownloads: Int,
    val pausedDownloads: Int,
    val uniqueNovels: Int
) {
    val completedDownloads: Int
        get() = totalDownloads - queuedDownloads - runningDownloads - pausedDownloads
}

data class NovelDownloadProgress(
    val novelId: Long,
    val totalDownloads: Int,
    val queuedDownloads: Int,
    val runningDownloads: Int,
    val pausedDownloads: Int,
    val hasDownloadsInQueue: Boolean
) {
    val completedDownloads: Int
        get() = totalDownloads - queuedDownloads - runningDownloads - pausedDownloads
    
    val progressPercentage: Float
        get() = if (totalDownloads > 0) (completedDownloads.toFloat() / totalDownloads.toFloat()) * 100 else 0f
}
```

## Key Features

### 1. Reactive UI Updates
- LiveData observers automatically update UI when data changes
- Proper lifecycle management prevents memory leaks
- UI state management with loading, success, and error states

### 2. Separation of Concerns
- ViewModels handle business logic and UI state
- Repositories handle data operations
- Activities/Fragments handle UI interactions only

### 3. Error Handling
- Comprehensive error handling at all layers
- User-friendly error messages
- Proper logging for debugging

### 4. Loading States
- Loading indicators for all async operations
- Progress tracking for long-running operations
- Proper state management during operations

### 5. Event Handling
- EventBus integration for cross-component communication
- Proper event registration and unregistration
- Thread-safe event handling

## Usage Examples

### Observing UI State in Activity/Fragment
```kotlin
viewModel.uiState.observe(this, Observer { uiState ->
    when (uiState) {
        is DownloadManagementViewModel.DownloadManagementUiState.Loading -> {
            // Show loading indicator
        }
        is DownloadManagementViewModel.DownloadManagementUiState.Success -> {
            // Hide loading indicator
        }
        is DownloadManagementViewModel.DownloadManagementUiState.Error -> {
            // Show error message
        }
        else -> {
            // Handle idle state
        }
    }
})
```

### Performing Operations
```kotlin
// Add chapters to download queue
viewModel.addChaptersToDownloadQueue(webPages, novel, context)

// Pause downloads for a novel
viewModel.pauseDownloadsForNovel(novelId)

// Delete downloaded chapters
viewModel.deleteDownloadedChapters(context, novel)
```

### Observing Data Changes
```kotlin
// Observe novel downloads
viewModel.novelDownloads.observe(this, Observer { novelDownloads ->
    val novelIds = novelDownloads.keys.toList()
    adapter.updateItems(ArrayList(novelIds))
})

// Observe download statistics
viewModel.downloadStatistics.observe(this, Observer { stats ->
    updateStatisticsUI(stats)
})
```

## Benefits of MVVM Implementation

1. **Testability**: ViewModels can be easily unit tested
2. **Maintainability**: Clear separation of concerns makes code easier to maintain
3. **Reusability**: ViewModels can be shared between different UI components
4. **Lifecycle Awareness**: Proper handling of configuration changes and lifecycle events
5. **Reactive Programming**: LiveData provides reactive UI updates
6. **Error Handling**: Centralized error handling and user feedback
7. **Loading States**: Consistent loading state management across the app

## Migration Notes

- Existing functionality has been preserved
- Service binding is maintained for backward compatibility
- EventBus integration is preserved for cross-component communication
- Database operations are now abstracted through repositories
- UI state management is now centralized in ViewModels

## Future Enhancements

1. **Data Binding**: Implement two-way data binding for forms
2. **Navigation Component**: Integrate with Navigation Component for better navigation
3. **Room Database**: Migrate to Room for better database management
4. **Coroutines**: Expand coroutine usage for better async operations
5. **Dependency Injection**: Implement proper DI framework (Hilt/Koin)
6. **Unit Testing**: Add comprehensive unit tests for ViewModels and Repositories

## Modern Event Bus System

### Overview

This document outlines the modern event bus system implementation for the NovelLibrary Android application. The system replaces the traditional EventBus with better performance, type safety, and lifecycle awareness.

### Key Features

1. **Type Safety**: Compile-time type checking for all events
2. **Lifecycle Awareness**: Automatic subscription management based on lifecycle
3. **Better Performance**: Uses Kotlin Coroutines and SharedFlow
4. **Memory Safety**: No memory leaks due to proper lifecycle management
5. **Thread Safety**: Built-in thread safety with coroutines

### Event Types Supported

1. **DownloadNovelEvent**: Novel download status changes
2. **DownloadWebPageEvent**: Individual chapter download events
3. **ChapterActionModeEvent**: Chapter selection and action mode events
4. **NovelSectionEvent**: Novel section navigation events
5. **ReaderSettingsEvent**: Reader settings changes
6. **NovelEvent**: General novel events
7. **SyncEvent**: Synchronization events
8. **ServiceEvent**: Service status events
9. **DownloadActionEvent**: Download action events
10. **ChapterEvent**: Chapter-related events

### Usage Examples

1. **Posting Events**
```kotlin
// Suspend function (preferred)
ModernEventBus.post(DownloadNovelEvent(EventType.INSERT, novelId))

// Async function (for non-suspend contexts)
ModernEventBus.postAsync(ChapterActionModeEvent(eventType = EventType.COMPLETE))
```

2. **Subscribing to Events in Activities/Fragments**
```kotlin
// Subscribe to download novel events
subscribeToDownloadNovelEvents { event ->
    when (event.type) {
        EventType.INSERT -> handleNewDownload(event.novelId)
        EventType.DELETE -> handleDownloadRemoved(event.novelId)
        EventType.PAUSED -> handleDownloadPaused(event.novelId)
    }
}

// Subscribe to chapter action mode events
subscribeToChapterActionModeEvents { event ->
    when (event.eventType) {
        EventType.UPDATE -> refreshUI()
        EventType.COMPLETE -> clearSelection()
    }
}
```

3. **Subscribing to Events in ViewModels**
```kotlin
// Subscribe to download web page events
subscribeToDownloadWebPageEvents { event ->
    updateDownloadProgress(event.download.novelId)
}
```

### Migration from EventBus

1. **Before (EventBus):**
```kotlin
// Posting
EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))

// Subscribing
@Subscribe(threadMode = ThreadMode.MAIN)
fun onChapterActionModeEvent(event: ChapterActionModeEvent) {
    // Handle event
}
```

2. **After (ModernEventBus):**
```kotlin
// Posting
ModernEventBus.postAsync(ChapterActionModeEvent(eventType = EventType.COMPLETE))

// Subscribing
subscribeToChapterActionModeEvents { event ->
    // Handle event
}
```

3. **Removing EventBus Registration**
```kotlin
// Remove from onStart()
EventBus.getDefault().register(this)

// Remove from onStop()
EventBus.getDefault().unregister(this)
```

4. **Updating ViewModels**
```kotlin
// Add to init block or setup method
setupEventSubscriptions()

private fun setupEventSubscriptions() {
    subscribeToDownloadNovelEvents { event ->
        // Handle event
    }
}
```

### Event Testing
```kotlin
@Test
fun testEventPosting() {
    // Test event posting
    ModernEventBus.postAsync(TestEvent())
    
    // Verify event was received
    // Use test observers or mock repositories
}
```

### ViewModel Testing
```kotlin
@Test
fun testViewModelEventHandling() {
    val viewModel = TestViewModel()
    
    // Post test event
    ModernEventBus.postAsync(TestEvent())
    
    // Verify ViewModel state changes
    assertEquals(expectedState, viewModel.uiState.value)
}
```

### Best Practices

1. **Event Design**
- Keep events simple and focused
- Use sealed classes for event types
- Include only necessary data in events

2. **Subscription Management**
- Use lifecycle-aware subscriptions
- Avoid manual subscription management
- Clean up subscriptions in ViewModel onCleared()

3. **Error Handling**
- Handle events gracefully
- Log errors appropriately
- Provide fallback behavior

4. **Performance**
- Avoid posting events in tight loops
- Use appropriate dispatchers for event posting
- Consider event batching for multiple updates

### Future Enhancements

1. **Event Persistence**
- Consider persisting important events
- Implement event replay for app restarts
- Add event history for debugging

2. **Event Filtering**
- Add event filtering capabilities
- Implement event priorities
- Add event routing based on conditions

3. **Analytics Integration**
- Track event usage patterns
- Monitor event performance
- Add event-based analytics

### Conclusion

The modern event bus system provides a robust, maintainable, and performant architecture for the NovelLibrary application. The replacement of EventBus with SharedFlow-based events offers significant improvements in type safety, performance, and developer experience while maintaining the same functionality and user experience.

The architecture is designed to be scalable, testable, and follows Android best practices, making it easier to maintain and extend the application in the future. 