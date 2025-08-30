# Single Activity Architecture Design Document

## Overview

This design document outlines the migration strategy for transforming the Novel Library Android application from a multi-activity architecture to a modern single activity architecture using Navigation Component. The migration builds upon the recently completed Hilt dependency injection migration and existing modern architecture infrastructure including coroutines, ViewModels, and UiState management.

The current architecture uses multiple activities for different features (NavDrawerActivity as main, NovelDetailsActivity, ReaderDBPagerActivity, etc.) with some fragment-based content. The new architecture will use a single MainActivity with Navigation Component managing all navigation between fragment destinations, leveraging the existing BaseFragment, BaseViewModel, and Hilt injection patterns.

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                              │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────┐ │
│  │  Navigation     │  │  Bottom/Drawer  │  │   Toolbar    │ │
│  │  Host Fragment  │  │  Navigation     │  │   Manager    │ │
│  └─────────────────┘  └─────────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                Navigation Component                          │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              Fragment Destinations                      │ │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────────┐   │ │
│  │  │Library  │ │Search   │ │Reader   │ │Settings     │   │ │
│  │  │Fragment │ │Fragment │ │Fragment │ │Fragments    │   │ │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────────┘   │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Presentation Layer                       │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  @HiltViewModel + UiState + BaseViewModel + Coroutines │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     Business Layer                          │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │     Existing Repositories + Hilt Injection             │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  DBHelper + NetworkHelper + Hilt Modules               │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Architecture Layers (Building on Existing Infrastructure)

#### 1. Presentation Layer (Enhanced with Navigation Component)
- **MainActivity**: Single entry point with Navigation Component integration
- **BaseFragment**: Existing @AndroidEntryPoint fragments with ViewBinding support
- **BaseViewModel**: Existing @HiltViewModel with coroutines and UiState management
- **Navigation**: Navigation Component with Safe Args for type-safe navigation
- **UiState**: Existing sealed classes for consistent state representation

#### 2. Business Layer (Leveraging Existing Hilt Infrastructure)
- **Existing Repositories**: DBHelper, NetworkHelper, SourceManager with Hilt injection
- **Existing Services**: DownloadNovelService, TTSService with @AndroidEntryPoint
- **Hilt Modules**: Existing DatabaseModule, NetworkModule, SourceModule, CoroutineModule
- **EntryPoints**: Existing patterns for object class dependency access

#### 3. Data Layer (Already Modernized)
- **Hilt Injection**: Complete Hilt dependency injection throughout
- **Coroutines**: Existing CoroutineScopes and DispatcherProvider infrastructure
- **Database**: DBHelper with Hilt singleton scoping
- **Network**: NetworkHelper with proper Hilt configuration

## Components and Interfaces

### Core Navigation Architecture

#### MainActivity (Building on BaseActivity)
```kotlin
@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    // Use existing Hilt ViewModel pattern
    private val mainViewModel: MainViewModel by viewModels()
    
    // Navigation setup and deep link handling
    // Global UI components (toolbar, bottom nav, drawer)
    // System UI management (status bar, navigation bar)
    // Leverage existing BaseActivity Hilt injection
}
```

#### Navigation Graph Structure
```xml
<navigation>
    <!-- Main Flow -->
    <fragment id="@+id/libraryFragment" />
    <fragment id="@+id/searchFragment" />
    
    <!-- Novel Flow -->
    <navigation id="@+id/novel_graph">
        <fragment id="@+id/novelDetailsFragment" />
        <fragment id="@+id/chaptersFragment" />
        <fragment id="@+id/readerFragment" />
    </navigation>
    
    <!-- Settings Flow -->
    <navigation id="@+id/settings_graph">
        <fragment id="@+id/mainSettingsFragment" />
        <fragment id="@+id/readerSettingsFragment" />
        <!-- Other settings fragments -->
    </navigation>
    
    <!-- Extensions Flow -->
    <navigation id="@+id/extensions_graph">
        <fragment id="@+id/extensionsFragment" />
        <fragment id="@+id/extensionDetailsFragment" />
    </navigation>
</navigation>
```

### Fragment Architecture

#### Fragment Architecture (Using Existing BaseFragment)
```kotlin
// Existing BaseFragment already has @AndroidEntryPoint and Hilt injection
@AndroidEntryPoint
open class BaseFragment : Fragment(), DataAccessor {
    @Inject override lateinit var firebaseAnalytics: FirebaseAnalytics
    @Inject override lateinit var dataCenter: DataCenter
    @Inject override lateinit var dbHelper: DBHelper
    @Inject override lateinit var sourceManager: SourceManager
    @Inject override lateinit var networkHelper: NetworkHelper
}
```

#### Feature Fragment Example (Following Architecture Guide)
```kotlin
@AndroidEntryPoint
class LibraryFragment : BaseFragment<FragmentLibraryBinding>() {
    private val viewModel: LibraryViewModel by viewModels()
    
    override fun getLayoutId() = R.layout.fragment_library
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showContent(state.data)
                is UiState.Error -> showError(state.message)
            }
        }
    }
}
```

### State Management Architecture

#### State Management (Using Existing Infrastructure)
```kotlin
// Existing UiState sealed class with extensions
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
}

// Feature-specific state classes
sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val novels: List<Novel>,
        val isRefreshing: Boolean = false,
        val selectedNovels: Set<Long> = emptySet()
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}
```

#### ViewModel Architecture (Using Existing BaseViewModel)
```kotlin
// Existing BaseViewModel with coroutines and error handling
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val dbHelper: DBHelper,
    private val sourceManager: SourceManager
) : BaseViewModel() {
    
    private val _libraryState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()
    
    fun loadLibraryNovels() {
        executeWithLoading {
            try {
                val novels = dbHelper.getAllNovels()
                _libraryState.value = LibraryUiState.Success(novels)
            } catch (e: Exception) {
                _libraryState.value = LibraryUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### Navigation Management

#### Navigation Manager (Hilt Singleton)
```kotlin
@Singleton
class NavigationManager @Inject constructor() {
    
    fun navigateToNovelDetails(navController: NavController, novelId: Long) {
        navController.navigate(
            LibraryFragmentDirections.actionLibraryToNovelDetails(novelId)
        )
    }
    
    fun navigateToReader(navController: NavController, novelId: Long, chapterId: Long) {
        navController.navigate(
            NovelDetailsFragmentDirections.actionNovelDetailsToReader(novelId, chapterId)
        )
    }
    
    // Other navigation methods with Safe Args
}
```

#### Deep Link Handling (Hilt Singleton)
```kotlin
@Singleton
class DeepLinkHandler @Inject constructor() {
    
    fun handleDeepLink(intent: Intent): NavDeepLinkRequest? {
        return when {
            intent.hasExtra("novel") -> {
                val novel = intent.getSerializableExtra("novel") as Novel
                NavDeepLinkRequest.Builder
                    .fromUri("app://novellibrary/novel/${novel.id}".toUri())
                    .build()
            }
            // Other deep link patterns
            else -> null
        }
    }
}
```

## Data Models

### Domain Models
```kotlin
// Pure domain entities
data class Novel(
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val rating: Float?,
    val shortDescription: String?,
    val longDescription: String?,
    val genres: List<String>,
    val authors: List<String>,
    val status: NovelStatus,
    val lastReadChapter: Chapter?,
    val totalChapters: Int,
    val isInLibrary: Boolean
)

data class Chapter(
    val id: Long,
    val novelId: Long,
    val name: String,
    val url: String,
    val orderId: Int,
    val isRead: Boolean,
    val isDownloaded: Boolean,
    val content: String?
)
```

### UI Models
```kotlin
// UI-specific models with presentation logic
data class NovelUiModel(
    val novel: Novel,
    val progressPercentage: Float,
    val isSelected: Boolean = false,
    val downloadStatus: DownloadStatus = DownloadStatus.NotDownloaded
) {
    val displayProgress: String
        get() = "${(progressPercentage * 100).toInt()}%"
}
```

### Data Access (Using Existing Hilt Infrastructure)
```kotlin
// Leverage existing DBHelper with Hilt injection
@Singleton
class DBHelper @Inject constructor(/* existing dependencies */) {
    // Existing methods already available
    fun getAllNovels(): List<Novel>
    fun getNovelDetails(novelId: Long): Novel?
    fun searchNovels(query: String): List<Novel>
    fun addToLibrary(novel: Novel)
    fun removeFromLibrary(novelId: Long)
    fun updateNovel(novel: Novel)
    
    fun getChapters(novelId: Long): List<Chapter>
    fun getChapter(chapterId: Long): Chapter?
    fun markAsRead(chapterId: Long)
    // ... other existing methods
}

// Use existing NetworkHelper for network operations
@Singleton
class NetworkHelper @Inject constructor(/* existing dependencies */) {
    // Existing network methods
}
```

## Error Handling

### Error Types
```kotlin
sealed class AppError : Exception() {
    object NetworkError : AppError()
    object DatabaseError : AppError()
    data class ValidationError(val field: String) : AppError()
    data class UnknownError(override val cause: Throwable?) : AppError()
}
```

### Error Handling Strategy
```kotlin
class ErrorHandler @Inject constructor() {
    
    fun handleError(error: Throwable): AppError {
        return when (error) {
            is IOException -> AppError.NetworkError
            is SQLException -> AppError.DatabaseError
            else -> AppError.UnknownError(error)
        }
    }
    
    fun getErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> "Network connection error"
            is AppError.DatabaseError -> "Database error occurred"
            is AppError.ValidationError -> "Invalid ${error.field}"
            is AppError.UnknownError -> "An unexpected error occurred"
        }
    }
}
```

### Global Error Handling
```kotlin
class GlobalErrorHandler @Inject constructor(
    private val errorHandler: ErrorHandler
) {
    
    fun handleGlobalError(error: Throwable, context: Context) {
        val appError = errorHandler.handleError(error)
        val message = errorHandler.getErrorMessage(appError)
        
        // Show appropriate UI feedback
        when (appError) {
            is AppError.NetworkError -> showNetworkErrorSnackbar(context, message)
            is AppError.DatabaseError -> showDatabaseErrorDialog(context, message)
            else -> showGenericErrorToast(context, message)
        }
        
        // Log error for analytics
        logError(appError)
    }
}
```

## Testing Strategy

### Unit Testing Architecture
```kotlin
// ViewModel Testing
class LibraryViewModelTest {
    
    @Mock private lateinit var getLibraryNovelsUseCase: GetLibraryNovelsUseCase
    @Mock private lateinit var updateNovelUseCase: UpdateNovelUseCase
    
    private lateinit var viewModel: LibraryViewModel
    
    @Test
    fun `when loading library novels, should emit loading then success state`() = runTest {
        // Given
        val novels = listOf(createTestNovel())
        whenever(getLibraryNovelsUseCase()).thenReturn(flowOf(novels))
        
        // When
        viewModel = LibraryViewModel(getLibraryNovelsUseCase, updateNovelUseCase)
        
        // Then
        viewModel.libraryState.test {
            assertEquals(LibraryUiState.Loading, awaitItem())
            assertEquals(LibraryUiState.Success(novels), awaitItem())
        }
    }
}
```

### Integration Testing
```kotlin
// Repository Testing
@RunWith(AndroidJUnit4::class)
class NovelRepositoryTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: NovelDatabase
    private lateinit var repository: NovelRepositoryImpl
    
    @Test
    fun `when adding novel to library, should persist in database`() = runTest {
        // Given
        val novel = createTestNovel()
        
        // When
        repository.addToLibrary(novel)
        
        // Then
        val libraryNovels = repository.getLibraryNovels().first()
        assertTrue(libraryNovels.contains(novel))
    }
}
```

### Navigation Testing
```kotlin
// Navigation Testing
@RunWith(AndroidJUnit4::class)
class NavigationTest {
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    @Test
    fun `when clicking on novel, should navigate to novel details`() {
        // Given
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        
        // When
        onView(withId(R.id.novel_item)).perform(click())
        
        // Then
        assertEquals(R.id.novelDetailsFragment, navController.currentDestination?.id)
    }
}
```

### Fragment Testing
```kotlin
// Fragment Testing
@RunWith(AndroidJUnit4::class)
class LibraryFragmentTest {
    
    @Test
    fun `when novels are loaded, should display novel list`() {
        // Given
        val novels = listOf(createTestNovel())
        val scenario = launchFragmentInContainer<LibraryFragment>()
        
        // When
        scenario.onFragment { fragment ->
            fragment.viewModel.updateNovels(novels)
        }
        
        // Then
        onView(withId(R.id.novels_recycler_view))
            .check(matches(hasChildCount(novels.size)))
    }
}
```

## Migration Strategy (Building on Existing Infrastructure)

### Phase 1: Navigation Foundation
1. **Create MainActivity**: New single activity extending existing BaseActivity with @AndroidEntryPoint
2. **Setup Navigation Graph**: Define all destinations using existing fragment structure
3. **Navigation Component Integration**: Add Navigation Component to existing project structure
4. **Safe Args Setup**: Configure Safe Args for type-safe navigation parameters

### Phase 2: Core Fragment Migration
1. **Library Feature**: Update existing LibraryFragment and LibraryPagerFragment for Navigation Component
2. **Search Feature**: Update existing SearchFragment with Navigation Component integration
3. **Navigation Drawer**: Integrate existing drawer with Navigation Component
4. **ViewModels**: Create @HiltViewModel classes using existing BaseViewModel pattern

### Phase 3: Activity to Fragment Migration
1. **Novel Details**: Convert NovelDetailsActivity to fragment using existing patterns
2. **Reader**: Convert ReaderDBPagerActivity to fragment with existing ViewBinding
3. **Chapters**: Convert ChaptersPagerActivity to fragment
4. **Downloads**: Convert NovelDownloadsActivity to fragment

### Phase 4: Advanced Features Migration
1. **Extensions**: Convert ExtensionsPagerActivity using existing ExtensionsFragment
2. **Settings Screens**: Convert all settings activities to fragments
3. **TTS Integration**: Integrate existing TTS service with fragment architecture
4. **Image Preview**: Convert ImagePreviewActivity to fragment or dialog

### Phase 5: Testing and Integration
1. **Hilt Testing**: Use existing @HiltAndroidTest infrastructure for fragment testing
2. **Navigation Testing**: Add Navigation Component testing to existing test suite
3. **Performance Validation**: Ensure no regressions with existing performance monitoring
4. **Deep Link Migration**: Update existing deep link handling for single activity

### Migration Safety (Leveraging Existing Infrastructure)
1. **Existing Error Handling**: Use existing error handling and logging infrastructure
2. **Existing Monitoring**: Leverage existing Firebase Analytics and crash reporting
3. **Data Migration**: Ensure all user data and preferences are preserved
4. **Rollback Plan**: Maintain ability to rollback to previous architecture
5. **Monitoring**: Implement crash reporting and performance monitoring

## Performance Considerations

### Memory Optimization
- **Fragment Lifecycle**: Proper fragment lifecycle management to prevent memory leaks
- **ViewModel Scoping**: Appropriate ViewModel scoping to shared data efficiently
- **Image Loading**: Optimize image loading with proper caching strategies
- **Database Queries**: Implement efficient database queries with proper indexing

### Navigation Performance
- **Fragment Caching**: Cache frequently accessed fragments
- **Lazy Loading**: Implement lazy loading for heavy fragments
- **Transition Animations**: Optimize fragment transitions for smooth UX
- **Back Stack Management**: Efficient back stack management to prevent memory issues

### State Management Performance
- **State Persistence**: Efficient state persistence across configuration changes
- **Data Flow**: Optimize data flow between layers to minimize unnecessary updates
- **Coroutine Usage**: Proper coroutine usage for background operations
- **Cache Strategy**: Implement intelligent caching for frequently accessed data

## Security Considerations

### Data Protection
- **Sensitive Data**: Ensure sensitive user data is properly encrypted
- **Network Security**: Maintain existing network security configurations
- **Local Storage**: Secure local storage of user preferences and data
- **Authentication**: Preserve existing Firebase authentication integration

### Navigation Security
- **Deep Link Validation**: Validate all deep links to prevent malicious navigation
- **Intent Filtering**: Proper intent filtering for external app interactions
- **Permission Handling**: Maintain existing permission handling patterns
- **Secure Communication**: Ensure secure communication between components

This design provides a comprehensive foundation for migrating to a single activity architecture while maintaining all existing functionality and improving the overall app architecture.