---
inclusion: always
---

# Novel Library Android Architecture Guide

## Absolute Requirements (Never Violate)

- **NEVER create Activities** - This is a single-activity architecture using only Fragments
- **NEVER use Injekt** - All dependency injection must use Hilt with `@Inject` annotations
- **NEVER use findViewById** - All UI binding must use ViewBinding
- **NEVER perform manual fragment transactions** - Use Navigation Component exclusively

## Mandatory Patterns

### Fragment Architecture
- All screens are Fragments extending `BaseFragment<T>` where T is the ViewBinding type
- Use `@AndroidEntryPoint` annotation on all Fragments
- Inject ViewModels with `by viewModels()` delegate
- Override `getLayoutId()` to return layout resource ID

### ViewModel Architecture  
- All ViewModels extend `BaseViewModel` and use `@HiltViewModel` annotation
- Constructor injection with `@Inject constructor()` 
- Use `MutableLiveData` for private state, expose as `LiveData`
- Wrap all operations in `viewModelScope.launch` for coroutines

### State Management
- Use sealed `UiState<T>` classes: `Loading`, `Success<T>`, `Error`
- Observe state in Fragment's `onViewCreated()` using `viewLifecycleOwner`
- Handle all three states explicitly in when expressions

### Dependency Injection
- Create `@Module` classes with `@InstallIn(SingletonComponent::class)`
- Use `@Provides @Singleton` for repository implementations
- Inject repositories into ViewModels, never DAOs or APIs directly

## Required Code Templates

When creating new components, use these exact patterns:

### Fragment Pattern
```kotlin
@AndroidEntryPoint
class [Name]Fragment : BaseFragment<Fragment[Name]Binding>() {
    private val viewModel: [Name]ViewModel by viewModels()
    
    override fun getLayoutId() = R.layout.fragment_[name]
    
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

### ViewModel Pattern
```kotlin
@HiltViewModel
class [Name]ViewModel @Inject constructor(
    private val repository: [Name]Repository
) : BaseViewModel() {
    
    private val _uiState = MutableLiveData<UiState<[DataType]>>()
    val uiState: LiveData<UiState<[DataType]>> = _uiState
    
    fun load[Data]() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val data = repository.get[Data]()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
```

### Hilt Module Pattern
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object [Name]Module {
    
    @Provides
    @Singleton
    fun provide[Name]Repository(
        api: [Name]Api,
        dao: [Name]Dao
    ): [Name]Repository = [Name]RepositoryImpl(api, dao)
}
```

## Package Organization

Place new files in these specific packages:
```
io.github.gmathi.novellibrary/
├── fragment/          # All UI screens (Fragments only)
├── viewmodel/         # ViewModels with @HiltViewModel  
├── model/            # Data classes and sealed classes
├── database/         # Room DAOs and entities
├── network/          # Retrofit APIs and response DTOs
├── repository/       # Repository interfaces and implementations
├── di/               # Hilt dependency injection modules
├── util/             # Extension functions and utilities
└── adapter/          # RecyclerView adapters with ViewBinding
```

## Navigation Implementation

- Generate Safe Args classes for type-safe navigation arguments
- Navigate using: `findNavController().navigate([Fragment]Directions.action[Target]())`
- Handle all deep links exclusively in MainActivity
- Use `popBackStack()` for backward navigation, never manual transactions

## Legacy Code Migration Rules

When modifying existing code:
- Replace `injectLazy()` calls with `@Inject constructor()` parameters
- Convert Injekt object modules to Hilt `@Module` classes with `@InstallIn`
- Migrate findViewById calls to ViewBinding properties
- Replace manual fragment transactions with Navigation Component actions

## Code Quality Standards

- All ViewModels require unit tests using MockK for mocking
- Use `@HiltAndroidTest` for integration testing with dependency injection
- Mock external dependencies (network, database) in all tests
- Test every UI state transition (Loading → Success/Error)
- Use `viewLifecycleOwner` for all Fragment lifecycle-aware observers
- Implement proper cleanup in ViewModel `onCleared()` when needed

## Performance & Security

- Use Glide with proper memory caching for all image loading
- Validate and sanitize all external inputs, especially from novel content sources
- Enforce HTTPS for all network requests
- Configure WebView security settings when displaying novel content
- Avoid memory leaks by using weak references for long-lived callbacks