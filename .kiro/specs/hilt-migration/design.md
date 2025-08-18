# Hilt Migration Design Document

## Overview

This design document outlines the comprehensive migration strategy for transforming the Novel Library Android application from Injekt dependency injection to Hilt (Dagger-Hilt). The migration will replace the current Injekt-based system with Google's recommended dependency injection solution while maintaining all existing functionality.

The current architecture uses Injekt with manual registration in `AppModule` and lazy injection via `by injectLazy()` throughout the codebase. The new architecture will use Hilt's annotation-based system with compile-time safety, better Android integration, and improved testing capabilities.

## Architecture

### Current Injekt Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                NovelLibraryApplication                      │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Injekt = InjektScope(DefaultRegistrar())              │ │
│  │  Injekt.importModule(AppModule(this))                  │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                      AppModule                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  addSingletonFactory { DBHelper.getInstance(app) }     │ │
│  │  addSingletonFactory { DataCenter(app) }               │ │
│  │  addSingletonFactory { NetworkHelper(app) }            │ │
│  │  addSingletonFactory { SourceManager(app) }            │ │
│  │  addSingletonFactory { ExtensionManager(app) }         │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                   Component Usage                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  val dbHelper: DBHelper by injectLazy()                │ │
│  │  val dataCenter: DataCenter by injectLazy()            │ │
│  │  val networkHelper: NetworkHelper by injectLazy()      │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### Target Hilt Architecture

```
┌─────────────────────────────────────────────────────────────┐
│            @HiltAndroidApp                                  │
│            NovelLibraryApplication                          │
├─────────────────────────────────────────────────────────────┤
│                   Hilt Modules                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  @Module @InstallIn(SingletonComponent::class)         │ │
│  │  DatabaseModule, NetworkModule, SourceModule           │ │
│  │  ExtensionModule, UtilityModule                        │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                Android Components                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  @AndroidEntryPoint Activities                         │ │
│  │  @AndroidEntryPoint Fragments                          │ │
│  │  @AndroidEntryPoint Services                           │ │
│  │  @HiltViewModel ViewModels                             │ │
│  └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                 Dependency Injection                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  @Inject constructor(                                  │ │
│  │    private val dbHelper: DBHelper,                     │ │
│  │    private val dataCenter: DataCenter                  │ │
│  │  )                                                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Application Setup

#### Hilt Application Class
```kotlin
@HiltAndroidApp
class NovelLibraryApplication : Application(), LifecycleObserver {
    
    override fun onCreate() {
        super.onCreate()
        
        // Remove Injekt initialization
        // Injekt = InjektScope(DefaultRegistrar())
        // Injekt.importModule(AppModule(this))
        
        // Hilt handles DI automatically
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        cleanupDatabase()
        
        // Rest of initialization remains the same
        setupDirectories()
        configureSSL()
        setupNotifications()
        setupRemoteConfig()
    }
}
```

### Hilt Modules Architecture

#### Database Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDBHelper(@ApplicationContext context: Context): DBHelper {
        return DBHelper.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideDataCenter(@ApplicationContext context: Context): DataCenter {
        return DataCenter(context)
    }
}
```

#### Network Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideNetworkHelper(@ApplicationContext context: Context): NetworkHelper {
        return NetworkHelper(context)
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json { ignoreUnknownKeys = true }
    }
}
```

#### Source and Extension Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SourceModule {
    
    @Provides
    @Singleton
    fun provideExtensionManager(@ApplicationContext context: Context): ExtensionManager {
        return ExtensionManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSourceManager(
        @ApplicationContext context: Context,
        extensionManager: ExtensionManager
    ): SourceManager {
        return SourceManager(context).also { 
            extensionManager.init(it) 
        }
    }
}
```

#### Analytics Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics {
        return Firebase.analytics
    }
}
```

#### Coroutine Module
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    
    @Provides
    @Singleton
    fun provideCoroutineScopes(): CoroutineScopes {
        return CoroutineScopes()
    }
    
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        return DefaultDispatcherProvider()
    }
}
```

### ViewModel Migration

#### Current ViewModel Pattern
```kotlin
class ChaptersViewModel(private val state: SavedStateHandle) : ViewModel(), LifecycleObserver, DataAccessor {
    
    override val dbHelper: DBHelper by injectLazy()
    override val dataCenter: DataCenter by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
}
```

#### Target Hilt ViewModel Pattern
```kotlin
@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager,
    private val firebaseAnalytics: FirebaseAnalytics
) : ViewModel(), LifecycleObserver, DataAccessor {
    
    // Remove injectLazy properties
    // All dependencies injected via constructor
}
```

### Fragment Migration

#### Current Fragment Pattern
```kotlin
class LibraryFragment : Fragment() {
    
    private val dbHelper: DBHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Use injected dependencies
    }
}
```

#### Target Hilt Fragment Pattern
```kotlin
@AndroidEntryPoint
class LibraryFragment : Fragment() {
    
    @Inject lateinit var dbHelper: DBHelper
    @Inject lateinit var dataCenter: DataCenter
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Dependencies automatically injected
    }
}
```

### Activity Migration

#### Current Activity Pattern
```kotlin
class MainActivity : AppCompatActivity() {
    
    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use injected dependencies
    }
}
```

#### Target Hilt Activity Pattern
```kotlin
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject lateinit var dataCenter: DataCenter
    @Inject lateinit var dbHelper: DBHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dependencies automatically injected
    }
}
```

### Service Migration

#### Current Service Pattern
```kotlin
class DownloadNovelService : Service() {
    
    private val dbHelper: DBHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()
}
```

#### Target Hilt Service Pattern
```kotlin
@AndroidEntryPoint
class DownloadNovelService : Service() {
    
    @Inject lateinit var dbHelper: DBHelper
    @Inject lateinit var dataCenter: DataCenter
    @Inject lateinit var networkHelper: NetworkHelper
}
```

## Data Models

### Dependency Scoping Strategy

#### Singleton Components
```kotlin
// Application-level singletons
@Singleton
@Provides
fun provideDBHelper(@ApplicationContext context: Context): DBHelper

@Singleton  
@Provides
fun provideDataCenter(@ApplicationContext context: Context): DataCenter

@Singleton
@Provides
fun provideNetworkHelper(@ApplicationContext context: Context): NetworkHelper

@Singleton
@Provides
fun provideSourceManager(context: Context, extensionManager: ExtensionManager): SourceManager
```

#### Activity Scoped Components
```kotlin
// For components that should be scoped to activity lifecycle
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    
    @Provides
    @ActivityScoped
    fun provideActivitySpecificComponent(): ActivitySpecificComponent {
        return ActivitySpecificComponent()
    }
}
```

#### Fragment Scoped Components
```kotlin
// For components that should be scoped to fragment lifecycle
@Module
@InstallIn(FragmentComponent::class)
object FragmentModule {
    
    @Provides
    fun provideFragmentSpecificComponent(): FragmentSpecificComponent {
        return FragmentSpecificComponent()
    }
}
```

### Interface Abstractions

#### Repository Pattern with Hilt
```kotlin
interface NovelRepository {
    suspend fun getLibraryNovels(): Flow<List<Novel>>
    suspend fun getNovelDetails(novelId: Long): Novel?
    suspend fun addToLibrary(novel: Novel)
}

@Singleton
class NovelRepositoryImpl @Inject constructor(
    private val dbHelper: DBHelper,
    private val networkHelper: NetworkHelper
) : NovelRepository {
    
    override suspend fun getLibraryNovels(): Flow<List<Novel>> {
        return dbHelper.getLibraryNovelsFlow()
    }
    
    // Other implementations
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindNovelRepository(
        novelRepositoryImpl: NovelRepositoryImpl
    ): NovelRepository
}
```

## Error Handling

### Migration Error Handling

#### Compile-Time Error Detection
```kotlin
// Hilt will detect missing bindings at compile time
@HiltViewModel
class ChaptersViewModel @Inject constructor(
    private val missingDependency: MissingService // Compile error if not provided
) : ViewModel()
```

#### Runtime Error Handling
```kotlin
// Graceful handling of injection failures
@AndroidEntryPoint
class LibraryFragment : Fragment() {
    
    @Inject lateinit var dbHelper: DBHelper
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            // Use injected dependencies
            loadLibraryData()
        } catch (e: UninitializedPropertyAccessException) {
            // Handle injection failure gracefully
            Logs.error("LibraryFragment", "Dependency injection failed", e)
            showErrorState()
        }
    }
}
```

#### Migration Validation
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {
    
    @Provides
    @Singleton
    fun provideMigrationValidator(
        dbHelper: DBHelper,
        dataCenter: DataCenter,
        networkHelper: NetworkHelper
    ): MigrationValidator {
        return MigrationValidator().apply {
            validateDependencies(dbHelper, dataCenter, networkHelper)
        }
    }
}
```

## Testing Strategy

### Unit Testing with Hilt

#### Test Module Setup
```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    
    @Provides
    @Singleton
    fun provideTestDBHelper(): DBHelper {
        return mockk<DBHelper>()
    }
    
    @Provides
    @Singleton
    fun provideTestDataCenter(): DataCenter {
        return mockk<DataCenter>()
    }
}
```

#### ViewModel Testing
```kotlin
@HiltAndroidTest
class ChaptersViewModelTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var dbHelper: DBHelper
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `test chapters loading`() = runTest {
        // Given
        val mockChapters = listOf(createTestChapter())
        every { dbHelper.getChapters(any()) } returns mockChapters
        
        // When
        val viewModel = ChaptersViewModel(
            SavedStateHandle(),
            dbHelper,
            mockk(),
            mockk(),
            mockk(),
            mockk()
        )
        
        // Then
        verify { dbHelper.getChapters(any()) }
    }
}
```

#### Fragment Testing
```kotlin
@HiltAndroidTest
class LibraryFragmentTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Test
    fun `test fragment injection`() {
        // Given
        val scenario = launchFragmentInContainer<LibraryFragment>()
        
        // When
        scenario.onFragment { fragment ->
            // Then
            assertNotNull(fragment.dbHelper)
            assertNotNull(fragment.dataCenter)
        }
    }
}
```

### Integration Testing

#### Repository Testing
```kotlin
@HiltAndroidTest
class NovelRepositoryTest {
    
    @get:Rule
    var hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: NovelRepository
    
    @Before
    fun init() {
        hiltRule.inject()
    }
    
    @Test
    fun `test repository integration`() = runTest {
        // Test repository with real dependencies
        val novels = repository.getLibraryNovels().first()
        assertTrue(novels.isNotEmpty())
    }
}
```

## Migration Strategy

### Phase 1: Foundation Setup (1-2 days)

#### 1.1 Add Hilt Dependencies
```gradle
// app/build.gradle
plugins {
    id 'dagger.hilt.android.plugin'
    id 'kotlin-kapt'
}

dependencies {
    implementation "com.google.dagger:hilt-android:2.48"
    kapt "com.google.dagger:hilt-compiler:2.48"
    
    // For ViewModels
    implementation 'androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03'
    kapt 'androidx.hilt:hilt-compiler:1.0.0'
    
    // For testing
    testImplementation 'com.google.dagger:hilt-android-testing:2.48'
    kaptTest 'com.google.dagger:hilt-compiler:2.48'
    androidTestImplementation 'com.google.dagger:hilt-android-testing:2.48'
    kaptAndroidTest 'com.google.dagger:hilt-compiler:2.48'
}
```

#### 1.2 Update Application Class
```kotlin
@HiltAndroidApp
class NovelLibraryApplication : Application(), LifecycleObserver {
    // Keep existing functionality
    // Remove Injekt initialization
}
```

#### 1.3 Create Base Hilt Modules
- DatabaseModule
- NetworkModule  
- SourceModule
- AnalyticsModule
- CoroutineModule

### Phase 2: Core Component Migration (2-3 days)

#### 2.1 Migrate ViewModels
- Convert `by injectLazy()` to constructor injection
- Add `@HiltViewModel` annotation
- Update ViewModel factories

#### 2.2 Migrate Activities
- Add `@AndroidEntryPoint` annotation
- Convert `by injectLazy()` to `@Inject lateinit var`
- Test activity lifecycle integration

#### 2.3 Migrate Fragments
- Add `@AndroidEntryPoint` annotation
- Convert injection patterns
- Test fragment lifecycle integration

### Phase 3: Service and Worker Migration (1-2 days)

#### 3.1 Migrate Services
- DownloadNovelService
- TTSService
- Other background services

#### 3.2 Migrate Workers
- Update WorkManager integration
- Ensure proper dependency injection in workers

### Phase 4: Testing and Validation (2-3 days)

#### 4.1 Create Test Modules
- Set up test-specific Hilt modules
- Create mock providers for testing

#### 4.2 Update Existing Tests
- Migrate unit tests to use Hilt
- Update integration tests
- Add new Hilt-specific tests

#### 4.3 Validation Testing
- Comprehensive regression testing
- Performance comparison
- Memory usage validation

### Phase 5: Cleanup and Optimization (1 day)

#### 5.1 Remove Injekt Dependencies
```gradle
// Remove from app/build.gradle
// implementation "uy.kohesive.injekt:injekt-core:1.16.1"
```

#### 5.2 Clean Up Code
- Remove AppModule.kt
- Remove all Injekt imports
- Clean up unused code

#### 5.3 Documentation Update
- Update architecture documentation
- Create migration guide
- Update developer onboarding docs

## Performance Considerations

### Build Performance

#### KSP vs KAPT
```gradle
// Prefer KSP for better build performance
plugins {
    id 'com.google.devtools.ksp'
}

dependencies {
    ksp "com.google.dagger:hilt-compiler:2.48"
    // Instead of kapt
}
```

#### Incremental Compilation
```gradle
// Enable incremental compilation
android {
    compileOptions {
        incremental true
    }
}
```

### Runtime Performance

#### Lazy Initialization
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object OptimizedModule {
    
    @Provides
    @Singleton
    fun provideExpensiveComponent(): ExpensiveComponent {
        // Use lazy initialization for expensive components
        return lazy { ExpensiveComponent() }.value
    }
}
```

#### Memory Optimization
```kotlin
// Proper scoping to prevent memory leaks
@ActivityScoped
@Provides
fun provideActivityScopedComponent(): ActivityScopedComponent {
    return ActivityScopedComponent()
}
```

## Security Considerations

### Dependency Validation
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    @Provides
    @Singleton
    fun provideSecureNetworkHelper(
        @ApplicationContext context: Context
    ): NetworkHelper {
        // Validate context and ensure secure configuration
        require(context is Application) { "Context must be Application context" }
        return NetworkHelper(context)
    }
}
```

### Testing Security
```kotlin
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [SecurityModule::class]
)
object TestSecurityModule {
    
    @Provides
    @Singleton
    fun provideTestNetworkHelper(): NetworkHelper {
        // Provide test-specific secure implementation
        return TestNetworkHelper()
    }
}
```

This design provides a comprehensive foundation for migrating from Injekt to Hilt while maintaining all existing functionality and improving the overall architecture with better compile-time safety, testing capabilities, and Android integration.