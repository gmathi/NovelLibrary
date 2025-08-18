# Single Activity Architecture Design Document

## Overview

This design document outlines the comprehensive migration strategy for transforming the Novel Library Android application from a multi-activity architecture to a modern single activity architecture. The migration will consolidate 20+ activities into a single MainActivity while implementing clean architecture principles, Navigation Component, and advanced architectural patterns.

The current architecture uses multiple activities for different features (NavDrawerActivity as main, NovelDetailsActivity, ReaderDBPagerActivity, etc.) with fragment-based content within some activities. The new architecture will use a single MainActivity with Navigation Component managing all navigation between fragment destinations.

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
│  │  ViewModels + State Management + Navigation Logic      │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                     Domain Layer                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │     Use Cases + Business Logic + Domain Models         │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                      Data Layer                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Repositories + Data Sources + Network + Database      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Clean Architecture Layers

#### 1. Presentation Layer
- **MainActivity**: Single entry point managing global UI state
- **Fragments**: Feature-specific UI components
- **ViewModels**: UI state management and business logic coordination
- **Navigation**: Centralized navigation logic using Navigation Component
- **UI State**: Sealed classes representing different UI states

#### 2. Domain Layer
- **Use Cases**: Business logic encapsulation (ReadNovelUseCase, SearchNovelsUseCase, etc.)
- **Domain Models**: Pure business entities
- **Repository Interfaces**: Abstraction for data access
- **Domain Events**: Cross-feature communication

#### 3. Data Layer
- **Repositories**: Implementation of domain interfaces
- **Data Sources**: Local (Database) and Remote (Network) data sources
- **Mappers**: Data transformation between layers
- **Cache Management**: Intelligent caching strategies

## Components and Interfaces

### Core Navigation Architecture

#### MainActivity
```kotlin
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    
    // Global UI state management
    private val mainViewModel: MainViewModel by viewModels()
    
    // Navigation setup and deep link handling
    // Global UI components (toolbar, bottom nav, drawer)
    // System UI management (status bar, navigation bar)
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

#### Base Fragment Structure
```kotlin
abstract class BaseFragment<VB : ViewBinding, VM : BaseViewModel> : Fragment() {
    protected abstract val binding: VB
    protected abstract val viewModel: VM
    
    // Common lifecycle management
    // State observation setup
    // Error handling
    // Loading state management
}
```

#### Feature Fragment Example
```kotlin
class LibraryFragment : BaseFragment<FragmentLibraryBinding, LibraryViewModel>() {
    override val binding by viewBinding(FragmentLibraryBinding::bind)
    override val viewModel: LibraryViewModel by viewModels()
    
    // Fragment-specific implementation
}
```

### State Management Architecture

#### UI State Representation
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val exception: Throwable) : UiState<Nothing>()
}

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

#### ViewModel Architecture
```kotlin
abstract class BaseViewModel : ViewModel() {
    protected val _uiState = MutableStateFlow<UiState<*>>(UiState.Loading)
    
    // Common error handling
    // Loading state management
    // Coroutine scope management
}

class LibraryViewModel(
    private val getLibraryNovelsUseCase: GetLibraryNovelsUseCase,
    private val updateNovelUseCase: UpdateNovelUseCase
) : BaseViewModel() {
    
    private val _libraryState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val libraryState: StateFlow<LibraryUiState> = _libraryState.asStateFlow()
    
    // Business logic implementation
}
```

### Navigation Management

#### Navigation Manager
```kotlin
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
    
    // Other navigation methods
}
```

#### Deep Link Handling
```kotlin
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

### Repository Interfaces
```kotlin
interface NovelRepository {
    suspend fun getLibraryNovels(): Flow<List<Novel>>
    suspend fun getNovelDetails(novelId: Long): Novel?
    suspend fun searchNovels(query: String): List<Novel>
    suspend fun addToLibrary(novel: Novel)
    suspend fun removeFromLibrary(novelId: Long)
    suspend fun updateNovel(novel: Novel)
}

interface ChapterRepository {
    suspend fun getChapters(novelId: Long): Flow<List<Chapter>>
    suspend fun getChapter(chapterId: Long): Chapter?
    suspend fun markAsRead(chapterId: Long)
    suspend fun downloadChapter(chapterId: Long)
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

## Migration Strategy

### Phase 1: Foundation Setup
1. **Create MainActivity**: New single activity with Navigation Component setup
2. **Setup Navigation Graph**: Define all destinations and navigation actions
3. **Create Base Classes**: BaseFragment, BaseViewModel, and common utilities
4. **Implement State Management**: UI state classes and error handling
5. **Setup Dependency Injection**: Update DI modules for new architecture

### Phase 2: Core Feature Migration
1. **Library Feature**: Migrate LibraryPagerFragment and related components
2. **Search Feature**: Migrate SearchFragment with proper navigation
3. **Navigation Drawer**: Integrate drawer navigation with Navigation Component
4. **Settings Foundation**: Create settings navigation graph structure

### Phase 3: Complex Feature Migration
1. **Novel Details**: Migrate NovelDetailsActivity to fragment
2. **Reader**: Migrate ReaderDBPagerActivity to fragment with proper state management
3. **Chapters**: Migrate ChaptersPagerActivity to fragment
4. **Downloads**: Migrate NovelDownloadsActivity to fragment

### Phase 4: Advanced Features
1. **Extensions**: Migrate ExtensionsPagerActivity to fragment
2. **Settings Screens**: Migrate all settings activities to fragments
3. **TTS Integration**: Integrate TextToSpeechControlsActivity functionality
4. **Image Preview**: Migrate ImagePreviewActivity to fragment or dialog

### Phase 5: Testing and Optimization
1. **Comprehensive Testing**: Unit, integration, and UI tests
2. **Performance Optimization**: Memory usage, navigation performance
3. **Deep Link Migration**: Update all deep link handling
4. **Intent Handling**: Migrate external intent handling

### Migration Safety Measures
1. **Feature Flags**: Use feature flags to enable/disable new architecture
2. **Gradual Rollout**: Migrate features incrementally with fallback options
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