# PersistentSearchView Architecture

## Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    PersistentSearchView                      │
│  (Main Composable - UI Layer)                               │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Card (Material 3)                                  │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │  Row (Search Bar)                             │  │    │
│  │  │  ┌──────┐  ┌──────────────┐  ┌──────────┐   │  │    │
│  │  │  │ Home │  │ TextField/   │  │ Clear/   │   │  │    │
│  │  │  │ Icon │  │ Logo Text    │  │ Search   │   │  │    │
│  │  │  └──────┘  └──────────────┘  └──────────┘   │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  │  ┌──────────────────────────────────────────────┐  │    │
│  │  │  LazyColumn (Suggestions)                     │  │    │
│  │  │  ┌────────────────────────────────────────┐  │  │    │
│  │  │  │ SuggestionItem 1                        │  │  │    │
│  │  │  ├────────────────────────────────────────┤  │  │    │
│  │  │  │ SuggestionItem 2                        │  │  │    │
│  │  │  ├────────────────────────────────────────┤  │  │    │
│  │  │  │ SuggestionItem 3                        │  │  │    │
│  │  │  └────────────────────────────────────────┘  │  │    │
│  │  └──────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ uses
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              PersistentSearchState                           │
│  (State Management Layer)                                    │
│                                                              │
│  • searchText: String                                        │
│  • logoText: String                                          │
│  • isEditing: Boolean                                        │
│  • isSearching: Boolean                                      │
│  • suggestions: List<SearchSuggestion>                       │
│                                                              │
│  Methods:                                                    │
│  • openSearch()                                              │
│  • closeSearch()                                             │
│  • performSearch()                                           │
│  • clearSearch()                                             │
│  • updateSearchText(text)                                    │
│  • updateSuggestions(suggestions)                            │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ provides data to
                            ▼
┌─────────────────────────────────────────────────────────────┐
│         SearchSuggestionsBuilder (Interface)                 │
│  (Data Provider Layer)                                       │
│                                                              │
│  • buildEmptySearchSuggestion(maxCount): List<Suggestion>   │
│  • buildSearchSuggestion(maxCount, query): List<Suggestion> │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ implemented by
                            ▼
┌─────────────────────────────────────────────────────────────┐
│       HistorySearchSuggestionsBuilder                        │
│  (Default Implementation)                                    │
│                                                              │
│  • searchHistory: List<String>                               │
│  • Filters and returns suggestions based on history         │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

```
User Action → State Update → UI Recomposition → Callback Execution

Example: User types "Solo"
┌──────────────┐
│ User types   │
│ "Solo"       │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────┐
│ TextField.onValueChange      │
│ state.updateSearchText("Solo")│
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│ State.searchText = "Solo"    │
│ (triggers recomposition)     │
└──────┬───────────────────────┘
       │
       ├─────────────────────────┐
       │                         │
       ▼                         ▼
┌──────────────────┐    ┌──────────────────────┐
│ LaunchedEffect   │    │ onSearchTermChanged  │
│ updates          │    │ callback executed    │
│ suggestions      │    └──────────────────────┘
└──────┬───────────┘
       │
       ▼
┌──────────────────────────────┐
│ suggestionBuilder            │
│ .buildSearchSuggestion(5,    │
│   "Solo")                    │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│ state.updateSuggestions(     │
│   [Solo Leveling, Solo Max]) │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────────────────────┐
│ LazyColumn recomposes with   │
│ new suggestions              │
└──────────────────────────────┘
```

## State Lifecycle

```
┌─────────────┐
│   Initial   │
│  State      │
│             │
│ isEditing:  │
│   false     │
│ searchText: │
│   ""        │
└──────┬──────┘
       │
       │ User clicks search icon
       │ or openSearch() called
       ▼
┌─────────────┐
│  Editing    │
│  Mode       │
│             │
│ isEditing:  │
│   true      │
│ suggestions:│
│   [history] │
└──────┬──────┘
       │
       │ User types text
       │ updateSearchText()
       ▼
┌─────────────┐
│  Typing     │
│  State      │
│             │
│ searchText: │
│   "query"   │
│ suggestions:│
│   [filtered]│
└──────┬──────┘
       │
       │ User presses Enter
       │ or performSearch()
       ▼
┌─────────────┐
│ Searching   │
│  State      │
│             │
│ isSearching:│
│   true      │
│ isEditing:  │
│   false     │
└──────┬──────┘
       │
       │ User clicks back
       │ or closeSearch()
       ▼
┌─────────────┐
│   Closed    │
│   State     │
│             │
│ isEditing:  │
│   false     │
│ searchText: │
│   ""        │
│ suggestions:│
│   []        │
└─────────────┘
```

## Callback Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    User Interactions                         │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Click Home   │    │ Click Search │    │ Type Text    │
│ Button       │    │ Icon         │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│onHomeButton  │    │state.open    │    │state.update  │
│Click()       │    │Search()      │    │SearchText()  │
└──────────────┘    └──────┬───────┘    └──────┬───────┘
                           │                   │
                           ▼                   ▼
                    ┌──────────────┐    ┌──────────────┐
                    │onSearchEdit  │    │onSearchTerm  │
                    │Opened()      │    │Changed()     │
                    └──────────────┘    └──────────────┘

        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Press Enter  │    │ Click Clear  │    │ Click Back   │
│              │    │              │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│state.perform │    │state.clear   │    │state.close   │
│Search()      │    │Search()      │    │Search()      │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│onSearch()    │    │onSearchCleared│   │onSearchExit()│
└──────────────┘    └──────────────┘    └──────────────┘
```

## Integration Patterns

### Pattern 1: Fragment with ViewPager
```
┌─────────────────────────────────────┐
│         SearchFragment              │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  PersistentSearchView         │ │
│  └───────────────────────────────┘ │
│  ┌───────────────────────────────┐ │
│  │  ViewPager (Search Results)   │ │
│  │  ┌─────────┬─────────┬──────┐ │ │
│  │  │ Tab 1   │ Tab 2   │ Tab 3│ │ │
│  │  └─────────┴─────────┴──────┘ │ │
│  └───────────────────────────────┘ │
│                                     │
│  SearchBackgroundTint (overlay)    │
└─────────────────────────────────────┘
```

### Pattern 2: Activity with RecyclerView
```
┌─────────────────────────────────────┐
│    LibrarySearchActivity            │
│                                     │
│  ┌───────────────────────────────┐ │
│  │  PersistentSearchView         │ │
│  └───────────────────────────────┘ │
│  ┌───────────────────────────────┐ │
│  │  LazyColumn (Filtered Items)  │ │
│  │  ┌─────────────────────────┐  │ │
│  │  │ Item 1                  │  │ │
│  │  ├─────────────────────────┤  │ │
│  │  │ Item 2                  │  │ │
│  │  ├─────────────────────────┤  │ │
│  │  │ Item 3                  │  │ │
│  │  └─────────────────────────┘  │ │
│  └───────────────────────────────┘ │
│                                     │
│  SearchBackgroundTint (overlay)    │
└─────────────────────────────────────┘
```

### Pattern 3: Compose Screen
```
┌─────────────────────────────────────┐
│      SearchScreen (Composable)      │
│                                     │
│  Box {                              │
│    Column {                         │
│      PersistentSearchView()         │
│      Content()                      │
│    }                                │
│    SearchBackgroundTint()           │
│  }                                  │
└─────────────────────────────────────┘
```

## Threading Model

```
┌─────────────────────────────────────┐
│         Main Thread (UI)            │
│                                     │
│  • Compose recomposition            │
│  • State updates                    │
│  • User interactions                │
│  • Animations                       │
└─────────────────────────────────────┘
                │
                │ LaunchedEffect
                ▼
┌─────────────────────────────────────┐
│      Coroutine (Background)         │
│                                     │
│  • Suggestion building              │
│  • History filtering                │
│  • Search operations                │
└─────────────────────────────────────┘
                │
                │ State update
                ▼
┌─────────────────────────────────────┐
│         Main Thread (UI)            │
│                                     │
│  • UI recomposition                 │
│  • Display results                  │
└─────────────────────────────────────┘
```

## Memory Management

```
┌─────────────────────────────────────┐
│      Composition Lifecycle          │
│                                     │
│  remember { }                       │
│    ↓                                │
│  Created once per composition       │
│  Survives recomposition             │
│  Cleared on disposal                │
│                                     │
│  rememberSaveable { }               │
│    ↓                                │
│  Survives configuration changes     │
│  Saved to Bundle                    │
│  Restored on recreation             │
└─────────────────────────────────────┘
```

## Performance Optimization

```
┌─────────────────────────────────────┐
│     Recomposition Scope             │
│                                     │
│  PersistentSearchView               │
│    ├─ Card (stable)                 │
│    ├─ Row (stable)                  │
│    │   ├─ IconButton (stable)       │
│    │   ├─ TextField (recomposes)    │
│    │   └─ IconButton (stable)       │
│    └─ LazyColumn (recomposes)       │
│        └─ items() (efficient)       │
│                                     │
│  Only TextField and LazyColumn      │
│  recompose on state changes         │
└─────────────────────────────────────┘
```

## Testing Architecture

```
┌─────────────────────────────────────┐
│         Test Layer                  │
│                                     │
│  ComposeTestRule                    │
│    ↓                                │
│  setContent { PersistentSearchView }│
│    ↓                                │
│  onNodeWithText().performClick()    │
│    ↓                                │
│  assert(state.isEditing)            │
└─────────────────────────────────────┘
```

## Dependency Graph

```
PersistentSearchView
  ├─ PersistentSearchState (required)
  ├─ SearchSuggestionsBuilder (optional)
  │   └─ HistorySearchSuggestionsBuilder
  ├─ PersistentSearchColors (optional)
  └─ HomeButtonMode (enum)

SearchBackgroundTint
  └─ (standalone, no dependencies)

ComposeSearchBridge
  ├─ PersistentSearchView
  ├─ PersistentSearchState
  └─ LegacySuggestionsBuilderAdapter
      └─ SuggestionsBuilder (legacy)
```

## Extension Points

```
┌─────────────────────────────────────┐
│      Customization Points           │
│                                     │
│  1. SearchSuggestionsBuilder        │
│     → Custom suggestion logic       │
│                                     │
│  2. PersistentSearchColors          │
│     → Custom theming                │
│                                     │
│  3. Callbacks                       │
│     → Custom behavior               │
│                                     │
│  4. Modifiers                       │
│     → Custom layout/styling         │
└─────────────────────────────────────┘
```
