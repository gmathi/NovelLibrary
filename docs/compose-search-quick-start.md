# Quick Start: Compose PersistentSearchView

## 🚀 Get Started in 5 Minutes

### 1. Run the Demo (1 minute)

Add this to your `AndroidManifest.xml`:

```xml
<activity
    android:name="io.github.gmathi.novellibrary.compose.search.SearchDemoActivity"
    android:exported="true"
    android:label="Search Demo">
    <!-- Optional: Make it a launcher activity temporarily to test -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

Run the app and test the search functionality!

### 2. Basic Usage (2 minutes)

Add to any Composable screen:

```kotlin
import io.github.gmathi.novellibrary.compose.search.*

@Composable
fun MyScreen() {
    val searchState = rememberPersistentSearchState()
    val searchHistory = listOf("Previous search 1", "Previous search 2")
    val suggestionBuilder = HistorySearchSuggestionsBuilder(searchHistory)

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search...",
                onSearch = { query ->
                    // Handle search
                    println("Searching for: $query")
                },
                suggestionBuilder = suggestionBuilder
            )
            
            // Your content here
        }

        // Background overlay when searching
        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}
```

### 3. Add to Existing Fragment (2 minutes)

Replace your XML view with Compose:

```kotlin
class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme {
                    MySearchScreen()
                }
            }
        }
    }

    @Composable
    fun MySearchScreen() {
        val searchState = rememberPersistentSearchState()
        
        PersistentSearchView(
            state = searchState,
            hint = "Search...",
            onSearch = { query -> 
                // Your search logic
            }
        )
    }
}
```

## 📚 Common Use Cases

### Real-time Search (Library Search)

```kotlin
PersistentSearchView(
    state = searchState,
    hint = "Search library...",
    onSearchTermChanged = { term ->
        // Filter results as user types
        filteredResults = allItems.filter { 
            it.contains(term, ignoreCase = true) 
        }
    }
)
```

### With Navigation Drawer

```kotlin
PersistentSearchView(
    state = searchState,
    homeButtonMode = HomeButtonMode.Burger,
    onHomeButtonClick = {
        // Open drawer
        drawerState.open()
    }
)
```

### With Back Navigation

```kotlin
PersistentSearchView(
    state = searchState,
    homeButtonMode = HomeButtonMode.Arrow,
    onHomeButtonClick = {
        // Navigate back
        navController.popBackStack()
    }
)
```

### Save Search History

```kotlin
PersistentSearchView(
    state = searchState,
    onSearch = { query ->
        // Save to history
        searchHistory.add(0, query)
        if (searchHistory.size > 10) {
            searchHistory.removeAt(searchHistory.lastIndex)
        }
        // Perform search
        performSearch(query)
    }
)
```

## 🎨 Customization

### Custom Colors

```kotlin
PersistentSearchView(
    state = searchState,
    colors = PersistentSearchDefaults.colors(
        backgroundColor = Color(0xFFF5F5F5),
        textColor = Color.Black,
        hintColor = Color.Gray,
        iconColor = Color.DarkGray
    )
)
```

### Custom Elevation

```kotlin
PersistentSearchView(
    state = searchState,
    elevation = 8  // Default is 4
)
```

### Custom Suggestions

```kotlin
class MyCustomSuggestionsBuilder : SearchSuggestionsBuilder {
    override fun buildEmptySearchSuggestion(maxCount: Int): List<SearchSuggestion> {
        return listOf(
            SearchSuggestion("Popular search 1", type = SearchSuggestionType.Suggestion),
            SearchSuggestion("Popular search 2", type = SearchSuggestionType.Suggestion)
        )
    }
    
    override fun buildSearchSuggestion(maxCount: Int, query: String): List<SearchSuggestion> {
        // Return filtered suggestions based on query
        return mySearchData
            .filter { it.contains(query, ignoreCase = true) }
            .take(maxCount)
            .map { SearchSuggestion(it) }
    }
}
```

## 🔧 State Management

### Access State Properties

```kotlin
val searchState = rememberPersistentSearchState()

// Check if editing
if (searchState.isEditing) {
    // Show keyboard, overlay, etc.
}

// Check if searching
if (searchState.isSearching) {
    // Show loading indicator
}

// Get current search text
val currentQuery = searchState.searchText
```

### Control State Programmatically

```kotlin
// Open search
searchState.openSearch()

// Close search
searchState.closeSearch()

// Clear text
searchState.clearSearch()

// Update text
searchState.updateSearchText("new query")

// Perform search
searchState.performSearch()
```

## 🧪 Testing

```kotlin
@Test
fun testSearch() {
    composeTestRule.setContent {
        PersistentSearchView(
            state = rememberPersistentSearchState(),
            hint = "Search..."
        )
    }
    
    // Click search icon
    composeTestRule.onNodeWithContentDescription("Search").performClick()
    
    // Type text
    composeTestRule.onNodeWithText("Search...").performTextInput("test")
    
    // Verify
    assert(searchState.searchText == "test")
}
```

## 📱 Complete Example

```kotlin
@Composable
fun CompleteSearchExample() {
    var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }
    val searchHistory = remember { mutableStateListOf<String>() }
    val searchState = rememberPersistentSearchState(initialLogoText = "Search Novels")
    val suggestionBuilder = remember(searchHistory.toList()) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search novels...",
                homeButtonMode = HomeButtonMode.Burger,
                onHomeButtonClick = { /* Open drawer */ },
                onSearch = { query ->
                    // Save to history
                    if (!searchHistory.contains(query)) {
                        searchHistory.add(0, query)
                    }
                    // Perform search
                    searchResults = performSearch(query)
                },
                onSearchTermChanged = { term ->
                    // Real-time filtering
                    if (term.isNotEmpty()) {
                        searchResults = performSearch(term)
                    }
                },
                onSearchCleared = {
                    searchResults = emptyList()
                },
                suggestionBuilder = suggestionBuilder,
                elevation = 4
            )

            // Results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { /* Handle click */ }
                    ) {
                        Text(
                            text = result,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Background overlay
        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}

fun performSearch(query: String): List<String> {
    // Your search logic here
    return listOf("Result 1", "Result 2", "Result 3")
}
```

## 🎯 Next Steps

1. ✅ Run the demo activity
2. ✅ Try the basic example
3. ✅ Customize colors and behavior
4. ✅ Integrate into your screens
5. 📖 Read the full documentation in `README.md`
6. 🔄 Follow the migration guide in `PERSISTENT_SEARCH_MIGRATION_GUIDE.md`

## 💡 Tips

- Use `rememberPersistentSearchState()` to preserve state across recompositions
- Always add `SearchBackgroundTint` for better UX
- Update `suggestionBuilder` when history changes using `remember(history)`
- Use `onSearchTermChanged` for real-time filtering
- Use `onSearch` for final search submission

## 🐛 Troubleshooting

**Search not opening?**
- Make sure you're calling `state.openSearch()` or clicking the search icon

**Suggestions not showing?**
- Verify `suggestionBuilder` is not null
- Check that history list is not empty
- Ensure `state.isEditing` is true

**Keyboard not appearing?**
- This is handled automatically when `state.isEditing` becomes true

**State not persisting?**
- Use `rememberSaveable` if you need to survive configuration changes

## 📞 Need Help?

- Check `SearchExamples.kt` for more examples
- Run `SearchDemoActivity` to see it in action
- Read the detailed `README.md`
- Review the test file `PersistentSearchViewTest.kt`
