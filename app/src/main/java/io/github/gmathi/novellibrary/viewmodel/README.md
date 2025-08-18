# ViewModel Injection Patterns with Hilt

This document provides comprehensive guidance on using Hilt dependency injection with ViewModels in the Novel Library Android application.

## Table of Contents

1. [Basic ViewModel Pattern](#basic-viewmodel-pattern)
2. [ViewModel with Repository Injection](#viewmodel-with-repository-injection)
3. [ViewModel with Multiple Dependencies](#viewmodel-with-multiple-dependencies)
4. [Testing ViewModels with Hilt](#testing-viewmodels-with-hilt)
5. [Common Patterns and Best Practices](#common-patterns-and-best-practices)
6. [Migration from Injekt](#migration-from-injekt)
7. [Troubleshooting](#troubleshooting)

## Basic ViewModel Pattern

### Simple ViewModel with Hilt

```kotlin
@HiltViewModel
class SimpleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    
    // ViewModel implementation
    fun doSomething() {
        launchSafely {
            // Use inherited methods from BaseViewModel
            setLoading(true)
            // Perform operations
            setLoading(false)
        }
    }
}
```

### Usage in Fragment

```kotlin
@AndroidEntryPoint
class MyFragment : BaseFragment<FragmentMyBinding>() {
    
    private val viewModel: SimpleViewModel by viewModels()
    
    override fun getLayoutId() = R.layout.fragment_my
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showLoading() else hideLoading()
        }
        
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { showError(it) }
        }
    }
}
```

## ViewModel with Repository Injection

### Repository Pattern

```kotlin
@HiltViewModel
class NovelListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val novelRepository: NovelRepository,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {
    
    private val _novels = MutableLiveData<UiState<List<Novel>>>()
    val novels: LiveData<UiState<List<Novel>>> = _novels
    
    fun loadNovels() {
        executeWithLoading {
            try {
                val novelList = novelRepository.getAllNovels()
                _novels.value = UiState.Success(novelList)
                firebaseAnalytics.logEvent("novels_loaded") {
                    param("count", novelList.size.toLong())
                }
            } catch (e: Exception) {
                _novels.value = UiState.Error(e.message ?: "Failed to load novels")
            }
        }
    }
    
    fun refreshNovels() {
        launchSafely {
            setLoading(true)
            try {
                val novelList = novelRepository.refreshNovels()
                _novels.value = UiState.Success(novelList)
            } catch (e: Exception) {
                handleError(e)
            } finally {
                setLoading(false)
            }
        }
    }
}
```

## ViewModel with Multiple Dependencies

### Complex ViewModel Example

```kotlin
@HiltViewModel
class ChapterReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {
    
    companion object {
        private const val KEY_NOVEL_ID = "novel_id"
        private const val KEY_CHAPTER_URL = "chapter_url"
    }
    
    // State management
    private val _chapterContent = MutableLiveData<UiState<String>>()
    val chapterContent: LiveData<UiState<String>> = _chapterContent
    
    private val _readingProgress = MutableLiveData<Float>()
    val readingProgress: LiveData<Float> = _readingProgress
    
    // Properties from SavedStateHandle
    private val novelId: Long
        get() = savedStateHandle.get<Long>(KEY_NOVEL_ID) ?: -1L
    
    private val chapterUrl: String
        get() = savedStateHandle.get<String>(KEY_CHAPTER_URL) ?: ""
    
    fun initialize(novelId: Long, chapterUrl: String) {
        savedStateHandle.set(KEY_NOVEL_ID, novelId)
        savedStateHandle.set(KEY_CHAPTER_URL, chapterUrl)
        loadChapter()
    }
    
    private fun loadChapter() {
        executeWithLoading {
            try {
                val novel = dbHelper.getNovel(novelId)
                    ?: throw IllegalArgumentException("Novel not found")
                
                val source = sourceManager.get(novel.sourceId)
                    ?: throw IllegalArgumentException("Source not found")
                
                val content = if (networkHelper.isConnectedToNetwork()) {
                    source.getChapterContent(chapterUrl)
                } else {
                    loadCachedChapter(chapterUrl)
                }
                
                _chapterContent.value = UiState.Success(content)
                
                // Log reading event
                firebaseAnalytics.logEvent("chapter_read") {
                    param("novel_id", novelId)
                    param("chapter_url", chapterUrl)
                }
                
            } catch (e: Exception) {
                _chapterContent.value = UiState.Error(
                    e.message ?: "Failed to load chapter"
                )
            }
        }
    }
    
    private suspend fun loadCachedChapter(url: String): String {
        val webPageSettings = dbHelper.getWebPageSettings(url)
        return webPageSettings?.filePath?.let { filePath ->
            File(filePath).readText()
        } ?: throw IllegalStateException("Chapter not cached")
    }
    
    fun updateReadingProgress(progress: Float) {
        _readingProgress.value = progress
        
        // Save progress to database
        launchSafely {
            val webPageSettings = dbHelper.getWebPageSettings(chapterUrl)
            webPageSettings?.let {
                it.metadata["reading_progress"] = progress.toString()
                dbHelper.updateWebPageSettings(it)
            }
        }
    }
}
```

## Testing ViewModels with Hilt

### Unit Test Setup

```kotlin
@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NovelListViewModelTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
    
    private lateinit var viewModel: NovelListViewModel
    private lateinit var mockRepository: NovelRepository
    private lateinit var mockFirebaseAnalytics: FirebaseAnalytics
    
    @Before
    fun setup() {
        hiltRule.inject()
        
        mockRepository = mockk(relaxed = true)
        mockFirebaseAnalytics = mockk(relaxed = true)
        
        viewModel = NovelListViewModel(
            SavedStateHandle(),
            mockRepository,
            mockFirebaseAnalytics
        )
    }
    
    @Test
    fun `loadNovels should emit success state with novels`() = runTest {
        // Given
        val expectedNovels = listOf(
            Novel(id = 1, name = "Test Novel 1"),
            Novel(id = 2, name = "Test Novel 2")
        )
        coEvery { mockRepository.getAllNovels() } returns expectedNovels
        
        // When
        viewModel.loadNovels()
        
        // Then
        val result = viewModel.novels.getOrAwaitValue()
        assertTrue(result is UiState.Success)
        assertEquals(expectedNovels, (result as UiState.Success).data)
        
        verify { mockFirebaseAnalytics.logEvent("novels_loaded", any()) }
    }
    
    @Test
    fun `loadNovels should emit error state on exception`() = runTest {
        // Given
        val errorMessage = "Network error"
        coEvery { mockRepository.getAllNovels() } throws Exception(errorMessage)
        
        // When
        viewModel.loadNovels()
        
        // Then
        val result = viewModel.novels.getOrAwaitValue()
        assertTrue(result is UiState.Error)
        assertEquals(errorMessage, (result as UiState.Error).message)
    }
}
```

### Test Module for Mocking

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
object TestRepositoryModule {
    
    @Provides
    @Singleton
    fun provideTestNovelRepository(): NovelRepository = mockk(relaxed = true)
    
    @Provides
    @Singleton
    fun provideTestFirebaseAnalytics(): FirebaseAnalytics = mockk(relaxed = true)
}
```

## Common Patterns and Best Practices

### 1. State Management with UiState

```kotlin
@HiltViewModel
class DataViewModel @Inject constructor(
    private val repository: DataRepository
) : BaseViewModel() {
    
    private val _uiState = MutableLiveData<UiState<List<DataItem>>>()
    val uiState: LiveData<UiState<List<DataItem>>> = _uiState
    
    fun loadData() {
        _uiState.value = UiState.Loading
        
        launchSafely {
            try {
                val data = repository.getData()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### 2. Handling User Actions

```kotlin
@HiltViewModel
class ActionViewModel @Inject constructor(
    private val repository: ActionRepository,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {
    
    private val _actionResult = MutableLiveData<UiState<String>>()
    val actionResult: LiveData<UiState<String>> = _actionResult
    
    fun performAction(actionType: String, data: Any) {
        executeWithLoading {
            try {
                val result = repository.performAction(actionType, data)
                _actionResult.value = UiState.Success(result)
                
                // Log action
                firebaseAnalytics.logEvent("user_action") {
                    param("action_type", actionType)
                    param("success", true)
                }
                
            } catch (e: Exception) {
                _actionResult.value = UiState.Error(e.message ?: "Action failed")
                
                firebaseAnalytics.logEvent("user_action") {
                    param("action_type", actionType)
                    param("success", false)
                    param("error", e.message ?: "Unknown error")
                }
            }
        }
    }
}
```

### 3. SavedStateHandle Usage

```kotlin
@HiltViewModel
class StatefulViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: Repository
) : BaseViewModel() {
    
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SELECTED_ITEMS = "selected_items"
    }
    
    // Reactive properties backed by SavedStateHandle
    val searchQuery: LiveData<String> = savedStateHandle.getLiveData(KEY_SEARCH_QUERY, "")
    val selectedItems: LiveData<List<String>> = savedStateHandle.getLiveData(KEY_SELECTED_ITEMS, emptyList())
    
    fun updateSearchQuery(query: String) {
        savedStateHandle.set(KEY_SEARCH_QUERY, query)
        performSearch(query)
    }
    
    fun toggleItemSelection(item: String) {
        val currentItems = selectedItems.value ?: emptyList()
        val updatedItems = if (currentItems.contains(item)) {
            currentItems - item
        } else {
            currentItems + item
        }
        savedStateHandle.set(KEY_SELECTED_ITEMS, updatedItems)
    }
}
```

## Migration from Injekt

### Before (Injekt)

```kotlin
class OldViewModel(private val state: SavedStateHandle) : ViewModel(), DataAccessor {
    
    override val dbHelper: DBHelper by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    override val networkHelper: NetworkHelper by injectLazy()
    override val sourceManager: SourceManager by injectLazy()
    override lateinit var firebaseAnalytics: FirebaseAnalytics
    
    fun init(context: Context) {
        firebaseAnalytics = Firebase.analytics
    }
}
```

### After (Hilt)

```kotlin
@HiltViewModel
class NewViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager,
    private val firebaseAnalytics: FirebaseAnalytics
) : BaseViewModel() {
    
    // No need for init method - all dependencies injected via constructor
}
```

## Troubleshooting

### Common Issues and Solutions

1. **Missing @HiltViewModel annotation**
   ```
   Error: Cannot create an instance of class MyViewModel
   Solution: Add @HiltViewModel annotation to the ViewModel class
   ```

2. **Missing @Inject constructor**
   ```
   Error: MyViewModel cannot be provided without an @Inject constructor
   Solution: Add @Inject to the constructor
   ```

3. **Circular dependency**
   ```
   Error: Found a dependency cycle
   Solution: Review dependency graph and break cycles using interfaces or lazy injection
   ```

4. **Missing Hilt modules**
   ```
   Error: Cannot find binding for MyDependency
   Solution: Create appropriate @Module with @Provides methods
   ```

### Best Practices Checklist

- ✅ Always use `@HiltViewModel` annotation
- ✅ Inject dependencies via constructor with `@Inject`
- ✅ Extend `BaseViewModel` for common functionality
- ✅ Use `UiState` for consistent state management
- ✅ Handle errors gracefully with try-catch blocks
- ✅ Use `SavedStateHandle` for state that survives process death
- ✅ Log important events to Firebase Analytics
- ✅ Write comprehensive unit tests
- ✅ Use `executeWithLoading` for operations that need loading states
- ✅ Clean up resources in `onCleared()` when necessary

### Performance Tips

- Use `launchSafely` for fire-and-forget operations
- Use `executeWithLoading` for operations that need loading indication
- Avoid heavy operations in ViewModel constructors
- Use `StateFlow` instead of `LiveData` for better performance when appropriate
- Cache expensive computations using `lazy` delegates