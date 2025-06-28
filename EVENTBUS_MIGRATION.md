# EventBus Migration Guide

## ✅ Migration Status: COMPLETED

**Date Completed:** December 2024  
**Status:** All EventBus dependencies have been successfully migrated to ModernEventBus using SharedFlow.

### What Was Migrated:
- ✅ `ChapterActionModeEvent` - Migrated to `ModernEventBus.chapterEvents`
- ✅ `NovelSectionEvent` - Migrated to `ModernEventBus.novelSectionEvents`  
- ✅ `ReaderSettingsEvent` - Migrated to `ModernEventBus.readerSettingsEvents`
- ✅ EventBus dependency removed from `build.gradle`
- ✅ All EventBus imports and registrations removed
- ✅ All event posting calls updated to use ModernEventBus
- ✅ Build verification successful

### Files Modified:
- `app/src/main/java/io/github/gmathi/novellibrary/model/other/EventBus.kt` (Created)
- `app/src/main/java/io/github/gmathi/novellibrary/activity/ChaptersPagerActivity.kt`
- `app/src/main/java/io/github/gmathi/novellibrary/activity/LibrarySearchActivity.kt`
- `app/src/main/java/io/github/gmathi/novellibrary/activity/ReaderDBPagerActivity.kt`
- `app/src/main/java/io/github/gmathi/novellibrary/fragment/LibraryFragment.kt`
- `app/src/main/java/io/github/gmathi/novellibrary/fragment/WebPageDBFragment.kt`
- `app/src/main/java/io/github/gmathi/novellibrary/viewmodel/ChaptersFragmentViewModel.kt`
- `app/build.gradle` (EventBus dependency removed)

---

## Overview

This guide outlines the migration from GreenRobot EventBus to modern alternatives using Kotlin Coroutines and SharedFlow.

## Why Migrate?

### Problems with GreenRobot EventBus:
- **Runtime Registration**: Manual register/unregister calls
- **Type Safety Issues**: Events are not type-safe
- **Memory Leaks**: Easy to forget unregistering
- **Testing Complexity**: Hard to mock and test
- **Debugging Difficulty**: Hard to trace event flow
- **Tight Coupling**: Components directly dependent on EventBus

### Benefits of Modern Approach:
- **Type Safety**: Compile-time type checking
- **Lifecycle Awareness**: Automatic lifecycle management
- **Coroutines Integration**: Native Kotlin coroutines support
- **Better Testing**: Easy to mock and test
- **Memory Safety**: Automatic cleanup with lifecycle
- **Debugging**: Clear event flow with structured concurrency

## Migration Options

### Option 1: SharedFlow + ViewModel (Recommended)

This approach uses Kotlin Coroutines and follows your existing MVVM architecture.

#### Step 1: Create Modern EventBus

```kotlin
// app/src/main/java/io/github/gmathi/novellibrary/model/other/EventBus.kt
object ModernEventBus {
    private val _chapterEvents = MutableSharedFlow<ChapterActionModeEvent>()
    val chapterEvents: SharedFlow<ChapterActionModeEvent> = _chapterEvents.asSharedFlow()
    
    suspend fun emitChapterEvent(event: ChapterActionModeEvent) {
        _chapterEvents.emit(event)
    }
    
    fun postChapterEvent(event: ChapterActionModeEvent) {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            emitChapterEvent(event)
        }
    }
}
```

#### Step 2: Update ViewModel

```kotlin
// Before (EventBus)
@Subscribe(threadMode = ThreadMode.MAIN)
fun onChapterActionModeEvent(event: ChapterActionModeEvent) {
    when (event.eventType) {
        EventType.COMPLETE -> loadChapters()
        EventType.UPDATE -> loadChapters()
        // ...
    }
}

// After (SharedFlow)
class ChaptersFragmentViewModel : ViewModel() {
    
    init {
        viewModelScope.launch {
            ModernEventBus.chapterEvents.collect { event ->
                when (event.eventType) {
                    EventType.COMPLETE -> loadChapters()
                    EventType.UPDATE -> loadChapters()
                    // ...
                }
            }
        }
    }
}
```

#### Step 3: Update Event Posting

```kotlin
// Before (EventBus)
EventBus.getDefault().post(ChapterActionModeEvent(eventType = EventType.COMPLETE))

// After (ModernEventBus)
ModernEventBus.postChapterEvent(ChapterActionModeEvent(eventType = EventType.COMPLETE))
```

### Option 2: ViewModel Communication

For parent-child ViewModel communication, use direct ViewModel references:

```kotlin
class ChaptersFragmentViewModel(
    private val parentViewModel: ChaptersViewModel
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            parentViewModel.chapterEvents.collect { event ->
                handleChapterEvent(event)
            }
        }
    }
}
```

### Option 3: Repository Pattern with LiveData

For data changes, use Repository pattern with LiveData:

```kotlin
class ChaptersRepository {
    private val _chapterUpdates = MutableLiveData<ChapterActionModeEvent>()
    val chapterUpdates: LiveData<ChapterActionModeEvent> = _chapterUpdates
    
    fun updateChapters(chapters: List<WebPage>, action: Action) {
        // Perform update
        _chapterUpdates.postValue(ChapterActionModeEvent(eventType = EventType.COMPLETE))
    }
}
```

## Migration Steps

### Phase 1: Create Modern EventBus
1. Create `ModernEventBus.kt` with SharedFlow implementations
2. Add convenience methods for non-suspending contexts
3. Test the new event bus in isolation

### Phase 2: Migrate ViewModels
1. Update `ChaptersFragmentViewModel` to use SharedFlow
2. Remove EventBus registration/unregistration
3. Update event handling logic

### Phase 3: Migrate Activities
1. Update `ChaptersPagerActivity` to use ModernEventBus
2. Replace `EventBus.getDefault().post()` calls
3. Test event flow

### Phase 4: Migrate Fragments
1. Update remaining fragments
2. Remove EventBus dependencies
3. Clean up imports

### Phase 5: Remove EventBus Dependency
1. Remove `org.greenrobot:eventbus:3.3.1` from build.gradle
2. Clean up unused imports
3. Run comprehensive tests

## Example Implementation

### Modern EventBus Usage

```kotlin
// Posting events
ModernEventBus.postChapterEvent(ChapterActionModeEvent(eventType = EventType.COMPLETE))

// Collecting events in ViewModel
class MyViewModel : ViewModel() {
    init {
        viewModelScope.launch {
            ModernEventBus.chapterEvents.collect { event ->
                handleChapterEvent(event)
            }
        }
    }
}

// Collecting events in Fragment
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewLifecycleOwner.lifecycleScope.launch {
            ModernEventBus.chapterEvents.collect { event ->
                handleChapterEvent(event)
            }
        }
    }
}
```

### Lifecycle-Aware Collection

```kotlin
// Using repeatOnLifecycle for automatic lifecycle management
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        ModernEventBus.chapterEvents.collect { event ->
            handleChapterEvent(event)
        }
    }
}
```

## Testing

### Unit Testing ViewModels

```kotlin
@Test
fun `test chapter event handling`() = runTest {
    val viewModel = ChaptersFragmentViewModel()
    
    // Post event
    ModernEventBus.postChapterEvent(ChapterActionModeEvent(eventType = EventType.COMPLETE))
    
    // Verify behavior
    verify { viewModel.loadChapters() }
}
```

### Integration Testing

```kotlin
@Test
fun `test event flow between components`() = runTest {
    val activity = ChaptersPagerActivity()
    val viewModel = ChaptersFragmentViewModel()
    
    // Trigger event from activity
    activity.triggerChapterUpdate()
    
    // Verify ViewModel receives event
    verify { viewModel.handleChapterEvent(any()) }
}
```

## Benefits After Migration

1. **Type Safety**: Compile-time checking prevents runtime errors
2. **Lifecycle Awareness**: Automatic cleanup prevents memory leaks
3. **Better Testing**: Easy to mock and test event flows
4. **Coroutines Integration**: Native support for async operations
5. **Debugging**: Clear event flow with structured concurrency
6. **Performance**: More efficient than reflection-based EventBus
7. **Modern Architecture**: Aligns with current Android development practices

## Dependencies

Add these to your `build.gradle`:

```gradle
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
    implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
}
```

## Timeline

- **Week 1**: Create ModernEventBus and basic tests
- **Week 2**: Migrate ViewModels and core components
- **Week 3**: Migrate Activities and Fragments
- **Week 4**: Remove EventBus dependency and final testing

## Rollback Plan

If issues arise during migration:
1. Keep both EventBus and ModernEventBus running in parallel
2. Gradually migrate components one by one
3. Use feature flags to switch between implementations
4. Monitor for any regressions or performance issues 