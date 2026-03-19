# PersistentSearchView Migration Guide

## Overview

This guide helps you migrate from the legacy `org.cryse.widget.persistentsearch.PersistentSearchView` (AAR library) to the new Jetpack Compose implementation.

## What's New

The new implementation provides:
- ✅ Modern Jetpack Compose UI
- ✅ Material 3 Design
- ✅ Better performance and animations
- ✅ Type-safe Kotlin API
- ✅ Easier customization
- ✅ No external AAR dependency

## Files Created

All new files are in `app/src/main/java/io/github/gmathi/novellibrary/compose/search/`:

1. **PersistentSearchView.kt** - Main composable component
2. **PersistentSearchState.kt** - State management
3. **ComposeSearchBridge.kt** - Migration helpers
4. **SearchExamples.kt** - Usage examples
5. **SearchFragmentCompose.kt** - Migrated SearchFragment example
6. **SearchDemoActivity.kt** - Standalone demo
7. **README.md** - Detailed documentation

## Migration Steps

### Step 1: Test the Demo

First, test the new component to see it in action:

1. Add the demo activity to your `AndroidManifest.xml`:
```xml
<activity
    android:name=".compose.search.SearchDemoActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

2. Run the app and test the search functionality

### Step 2: Migrate SearchFragment

#### Old Implementation (XML + View Binding)

```kotlin
// fragment_search.xml
<org.cryse.widget.persistentsearch.PersistentSearchView
    android:id="@+id/searchView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:persistentSV_editHintText="@string/search_novel"
    app:persistentSV_homeButtonMode="burger" />

// SearchFragment.kt
binding.searchView.setSearchListener(object : PersistentSearchView.SearchListener {
    override fun onSearch(query: String?) {
        searchNovels(query)
    }
    // ... other callbacks
})
```

#### New Implementation (Compose)

```kotlin
// SearchFragment.kt
override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
): View {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val searchState = rememberPersistentSearchState()
            val suggestionBuilder = HistorySearchSuggestionsBuilder(
                dataCenter.loadNovelSearchHistory()
            )

            Box(modifier = Modifier.fillMaxSize()) {
                Column {
                    PersistentSearchView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        state = searchState,
                        hint = "Search novels...",
                        homeButtonMode = HomeButtonMode.Burger,
                        onHomeButtonClick = { /* open drawer */ },
                        onSearch = { query ->
                            query.addToNovelSearchHistory()
                            searchNovels(query)
                        },
                        suggestionBuilder = suggestionBuilder
                    )
                    
                    // Your content here
                }

                SearchBackgroundTint(
                    visible = searchState.isEditing,
                    onClick = { searchState.closeSearch() }
                )
            }
        }
    }
}
```

### Step 3: Migrate LibrarySearchActivity

#### Old Implementation

```kotlin
// activity_library_search.xml
<org.cryse.widget.persistentsearch.PersistentSearchView
    android:id="@+id/searchView"
    app:persistentSV_homeButtonMode="arrow" />

// LibrarySearchActivity.kt
binding.searchView.setSearchListener(object : PersistentSearchView.SearchListener {
    override fun onSearchTermChanged(term: String?) {
        searchNovels(term)
    }
})
```

#### New Implementation

```kotlin
// LibrarySearchActivity.kt
class LibrarySearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                LibrarySearchScreen()
            }
        }
    }

    @Composable
    fun LibrarySearchScreen() {
        val searchState = rememberPersistentSearchState()
        val suggestionBuilder = HistorySearchSuggestionsBuilder(
            dataCenter.loadLibrarySearchHistory()
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                PersistentSearchView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    state = searchState,
                    hint = "Search library...",
                    homeButtonMode = HomeButtonMode.Arrow,
                    onHomeButtonClick = { finish() },
                    onSearchTermChanged = { term ->
                        searchNovels(term)
                    },
                    suggestionBuilder = suggestionBuilder
                )
                
                // RecyclerView content
            }

            SearchBackgroundTint(
                visible = searchState.isEditing,
                onClick = { searchState.closeSearch() }
            )
        }
    }
}
```

### Step 4: Update NavDrawerActivity

The NavDrawerActivity references the search view to close it. Update the reference:

#### Old Code
```kotlin
val searchView = existingSearchFrag.view?.findViewById<PersistentSearchView>(R.id.searchView)
if (searchView != null && (searchView.isEditing || searchView.isSearching)) {
    (existingSearchFrag as SearchFragment).closeSearch()
}
```

#### New Code
```kotlin
// If using the new Compose version, call the fragment's closeSearch() method directly
if (existingSearchFrag is SearchFragmentCompose) {
    existingSearchFrag.closeSearch()
}
```

### Step 5: Remove Old Dependencies

Once migration is complete:

1. Remove the AAR file:
```bash
rm app/libs/persistentsearchview.aar
```

2. Remove old imports from files:
```kotlin
// Remove these
import org.cryse.widget.persistentsearch.PersistentSearchView
import org.cryse.widget.persistentsearch.SearchItem

// Add these
import io.github.gmathi.novellibrary.compose.search.*
```

3. Update `SuggestionsBuilder.kt` to work with the new interface or delete it if no longer needed.

## API Mapping

### Listener Callbacks

| Old API | New API |
|---------|---------|
| `onSearch(query: String?)` | `onSearch: (String) -> Unit` |
| `onSearchEditOpened()` | `onSearchEditOpened: () -> Unit` |
| `onSearchEditClosed()` | `onSearchEditClosed: () -> Unit` |
| `onSearchExit()` | `onSearchExit: () -> Unit` |
| `onSearchCleared()` | `onSearchCleared: () -> Unit` |
| `onSearchTermChanged(term: String?)` | `onSearchTermChanged: (String) -> Unit` |
| `onSuggestion(searchItem: SearchItem?): Boolean` | `onSuggestionClick: (SearchSuggestion) -> Boolean` |
| `onSearchEditBackPressed(): Boolean` | Handled automatically by state |

### XML Attributes

| Old XML Attribute | New Compose Parameter |
|-------------------|----------------------|
| `persistentSV_editHintText` | `hint: String` |
| `persistentSV_homeButtonMode` | `homeButtonMode: HomeButtonMode` |
| `persistentSV_logoString` | `state.logoText` |
| `persistentSV_searchCardElevation` | `elevation: Int` |
| `persistentSV_homeButtonColor` | `colors.iconColor` |
| `persistentSV_editHintTextColor` | `colors.hintColor` |

### Methods

| Old Method | New Method |
|------------|------------|
| `searchView.closeSearch()` | `searchState.closeSearch()` |
| `searchView.isEditing` | `searchState.isEditing` |
| `searchView.isSearching` | `searchState.isSearching` |
| `searchView.searchOpen` | `searchState.searchOpen` |
| `searchView.setSuggestionBuilder()` | `suggestionBuilder` parameter |

## Testing Checklist

- [ ] Search opens when clicking search icon
- [ ] Search closes when clicking back button
- [ ] Suggestions appear when typing
- [ ] History suggestions show when field is empty
- [ ] Search executes on keyboard "Search" action
- [ ] Clear button clears text
- [ ] Home button opens drawer/navigates back
- [ ] Background tint appears/disappears correctly
- [ ] Keyboard shows/hides appropriately
- [ ] Search history is saved and loaded

## Troubleshooting

### Issue: Compose not rendering
**Solution**: Ensure Compose is enabled in `build.gradle`:
```gradle
buildFeatures {
    compose true
}
```

### Issue: State not persisting
**Solution**: Use `rememberSaveable` for state that should survive configuration changes:
```kotlin
val searchState = rememberSaveable(saver = PersistentSearchStateSaver) {
    PersistentSearchState()
}
```

### Issue: Keyboard not showing
**Solution**: Ensure `FocusRequester` is properly set up (already handled in the component).

### Issue: Suggestions not updating
**Solution**: Make sure `suggestionBuilder` is recreated when history changes:
```kotlin
val suggestionBuilder = remember(searchHistory) {
    HistorySearchSuggestionsBuilder(searchHistory)
}
```

## Benefits of Migration

1. **No External Dependencies**: Remove the AAR library
2. **Better Performance**: Compose's efficient recomposition
3. **Modern UI**: Material 3 design system
4. **Type Safety**: Kotlin-first API with compile-time checks
5. **Easier Customization**: Compose modifiers and theming
6. **Better Testing**: Compose testing APIs
7. **Future-Proof**: Active development and support

## Need Help?

- Check `SearchExamples.kt` for complete examples
- Run `SearchDemoActivity` to see it in action
- Read the detailed `README.md` in the compose/search package
- Review `SearchFragmentCompose.kt` for a full migration example
