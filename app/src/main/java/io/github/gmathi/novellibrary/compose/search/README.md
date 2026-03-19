# PersistentSearchView - Compose Implementation

A modern Jetpack Compose implementation of the PersistentSearchView, replacing the legacy `org.cryse.widget.persistentsearch.PersistentSearchView` library.

## Features

- ✅ Material 3 Design
- ✅ Search suggestions with history
- ✅ Animated transitions
- ✅ Customizable colors and styling
- ✅ Home button modes (Burger/Arrow)
- ✅ Background tint overlay
- ✅ Keyboard handling
- ✅ State management with `PersistentSearchState`
- ✅ Bridge for gradual migration from XML

## Components

### 1. PersistentSearchView
The main composable component that provides the search UI.

### 2. PersistentSearchState
State holder for managing search state (text, editing mode, suggestions).

### 3. SearchSuggestionsBuilder
Interface for providing search suggestions based on history or custom logic.

### 4. ComposeSearchBridge
Helper for gradual migration from XML-based views to Compose.

## Usage

### Basic Usage in Compose

```kotlin
@Composable
fun MySearchScreen() {
    val searchState = rememberPersistentSearchState(initialLogoText = "Search")
    val searchHistory = listOf("Novel 1", "Novel 2", "Novel 3")
    val suggestionBuilder = HistorySearchSuggestionsBuilder(searchHistory)

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search novels...",
                homeButtonMode = HomeButtonMode.Burger,
                onHomeButtonClick = { /* Open drawer */ },
                onSearch = { query ->
                    // Handle search
                },
                suggestionBuilder = suggestionBuilder
            )
            
            // Your content
        }

        // Background overlay
        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}
```

### Migration from XML (Gradual Approach)

If you have existing XML layouts with the old PersistentSearchView, you can use the bridge:

```kotlin
class SearchFragment : Fragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSearchBinding.inflate(inflater, container, false)
        
        // Replace the old searchView with ComposeView
        val composeSearchView = ComposeView(requireContext())
        setupComposeSearchView(
            composeView = composeSearchView,
            hint = "Search novels",
            homeButtonMode = HomeButtonMode.Burger,
            searchHistory = dataCenter.loadNovelSearchHistory(),
            onHomeButtonClick = {
                // Open drawer
            },
            onSearch = { query ->
                searchNovels(query)
            },
            onSearchEditOpened = {
                binding.searchViewBgTint.visibility = View.VISIBLE
            },
            onSearchEditClosed = {
                binding.searchViewBgTint.visibility = View.GONE
            }
        )
        
        return binding.root
    }
}
```

### Custom Suggestions Builder

```kotlin
class CustomSuggestionsBuilder(
    private val getRecentSearches: () -> List<String>,
    private val getPopularSearches: () -> List<String>
) : SearchSuggestionsBuilder {
    
    override fun buildEmptySearchSuggestion(maxCount: Int): List<SearchSuggestion> {
        val recent = getRecentSearches().take(3)
            .map { SearchSuggestion(it, type = SearchSuggestionType.History) }
        val popular = getPopularSearches().take(maxCount - recent.size)
            .map { SearchSuggestion(it, type = SearchSuggestionType.Suggestion) }
        return recent + popular
    }
    
    override fun buildSearchSuggestion(maxCount: Int, query: String): List<SearchSuggestion> {
        return getRecentSearches()
            .filter { it.contains(query, ignoreCase = true) }
            .take(maxCount)
            .map { SearchSuggestion(it, type = SearchSuggestionType.History) }
    }
}
```

### Custom Colors

```kotlin
PersistentSearchView(
    state = searchState,
    colors = PersistentSearchDefaults.colors(
        backgroundColor = Color.White,
        textColor = Color.Black,
        hintColor = Color.Gray,
        iconColor = Color.DarkGray
    )
)
```

## API Reference

### PersistentSearchView Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `state` | `PersistentSearchState` | State holder for the search view |
| `hint` | `String` | Placeholder text when editing |
| `homeButtonMode` | `HomeButtonMode` | Burger or Arrow icon |
| `onHomeButtonClick` | `() -> Unit` | Callback for home button |
| `onSearch` | `(String) -> Unit` | Callback when search is submitted |
| `onSearchTermChanged` | `(String) -> Unit` | Callback for real-time text changes |
| `onSearchEditOpened` | `() -> Unit` | Callback when entering edit mode |
| `onSearchEditClosed` | `() -> Unit` | Callback when exiting edit mode |
| `onSearchExit` | `() -> Unit` | Callback when back button pressed |
| `onSearchCleared` | `() -> Unit` | Callback when clear button pressed |
| `onSuggestionClick` | `(SearchSuggestion) -> Boolean` | Callback for suggestion clicks |
| `suggestionBuilder` | `SearchSuggestionsBuilder?` | Provider for suggestions |
| `elevation` | `Int` | Card elevation in dp |
| `colors` | `PersistentSearchColors` | Color customization |

### PersistentSearchState Methods

| Method | Description |
|--------|-------------|
| `openSearch()` | Enter edit mode |
| `closeSearch()` | Exit edit mode and clear |
| `performSearch()` | Mark as searching |
| `clearSearch()` | Clear search text |
| `updateSearchText(text)` | Update search text |

## Migration Guide

### Step 1: Add Compose Dependencies
Ensure your `build.gradle` has Compose dependencies (Material 3).

### Step 2: Replace XML Views
Replace `<org.cryse.widget.persistentsearch.PersistentSearchView>` with `<androidx.compose.ui.platform.ComposeView>` in your layouts.

### Step 3: Update Fragment/Activity Code
Use the bridge functions or directly implement Compose UI.

### Step 4: Remove Old Library
Once migration is complete, remove `persistentsearchview.aar` from `app/libs/`.

## Benefits Over Legacy Library

1. **Modern Stack**: Built with Jetpack Compose and Material 3
2. **Better Performance**: Compose's efficient recomposition
3. **Type Safety**: Kotlin-first API
4. **Easier Customization**: Compose modifiers and theming
5. **Active Maintenance**: No dependency on abandoned libraries
6. **Better Testing**: Compose testing APIs
7. **Smaller APK**: No need for AAR library

## Examples

See `SearchExamples.kt` for complete working examples of:
- Search screen with tabs
- Library search with real-time filtering
- Custom suggestion builders
