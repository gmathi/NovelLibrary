# Recent Novels Feature - Clean Architecture

## Layer Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                        │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  RecentNovelsActivity (ComponentActivity)                    │
│         │                                                     │
│         ├─> RecentNovelsScreen (@Composable)                 │
│         │      │                                              │
│         │      ├─> RecentlyUpdatedTab                        │
│         │      │      └─> RecentlyUpdatedItemView            │
│         │      │                                              │
│         │      └─> RecentlyViewedTab                         │
│         │             └─> NovelItem                           │
│         │                                                     │
│         └─> RecentNovelsViewModel                            │
│                │                                              │
│                ├─> recentlyUpdatedState: StateFlow           │
│                └─> recentlyViewedState: StateFlow            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ calls
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER                            │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Use Cases (Business Logic)                                  │
│                                                               │
│  ┌─────────────────────────────────────────────┐            │
│  │ GetRecentlyUpdatedNovelsUseCase             │            │
│  │  - Fetches from NovelUpdates.com            │            │
│  │  - Returns Result<List<RecentlyUpdatedItem>>│            │
│  └─────────────────────────────────────────────┘            │
│                                                               │
│  ┌─────────────────────────────────────────────┐            │
│  │ GetRecentlyViewedNovelsUseCase              │            │
│  │  - Reads from local database                │            │
│  │  - Returns List<Novel>                      │            │
│  └─────────────────────────────────────────────┘            │
│                                                               │
│  ┌─────────────────────────────────────────────┐            │
│  │ ClearRecentlyViewedNovelsUseCase            │            │
│  │  - Clears history from database             │            │
│  └─────────────────────────────────────────────┘            │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ uses
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                       DATA LAYER                             │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐        ┌──────────────────┐          │
│  │   DBHelper       │        │  NetworkHelper   │          │
│  │  (SQLite DB)     │        │  (Connectivity)  │          │
│  └──────────────────┘        └──────────────────┘          │
│                                                               │
│  ┌──────────────────────────────────────────────┐          │
│  │   WebPageDocumentFetcher                     │          │
│  │   (Jsoup - Web Scraping)                     │          │
│  └──────────────────────────────────────────────┘          │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### Recently Updated Novels Flow
```
User Action (Pull to Refresh / Load)
    │
    ▼
RecentNovelsViewModel.loadRecentlyUpdatedNovels()
    │
    ├─> Check NetworkHelper.isConnectedToNetwork()
    │   └─> If no internet: emit NoInternet state
    │
    ├─> Call GetRecentlyUpdatedNovelsUseCase()
    │       │
    │       ├─> WebPageDocumentFetcher.document("novelupdates.com")
    │       ├─> Parse HTML with Jsoup
    │       └─> Return Result<List<RecentlyUpdatedItem>>
    │
    └─> Emit Success/Error state
            │
            ▼
    RecentNovelsScreen observes state
            │
            ▼
    UI updates (LazyColumn with items)
```

### Recently Viewed Novels Flow
```
User Action (Tab Switch / Resume)
    │
    ▼
RecentNovelsViewModel.loadRecentlyViewedNovels()
    │
    ├─> Call GetRecentlyViewedNovelsUseCase()
    │       │
    │       ├─> DBHelper.getLargePreference(RVN_HISTORY)
    │       ├─> Gson.fromJson() to List<Novel>
    │       └─> Return reversed list
    │
    └─> Emit Success/Empty state
            │
            ▼
    RecentNovelsScreen observes state
            │
            ▼
    UI updates (LazyColumn with NovelItem)
```

### Clear History Flow
```
User Action (Click Delete Icon)
    │
    ▼
RecentNovelsViewModel.clearRecentlyViewedNovels()
    │
    ├─> Call ClearRecentlyViewedNovelsUseCase()
    │       │
    │       └─> DBHelper.createOrUpdateLargePreference(RVN_HISTORY, "[]")
    │
    └─> Call loadRecentlyViewedNovels() to refresh
            │
            ▼
    UI updates to show empty state
```

## State Management

### RecentlyUpdatedUiState
- `Loading` - Initial load or refresh
- `Success(items)` - Data loaded successfully
- `Error(message)` - Network or parsing error
- `NoInternet` - No network connection

### RecentlyViewedUiState
- `Loading` - Reading from database
- `Success(novels)` - Novels loaded from history
- `Empty` - No history available

## Dependency Injection

Uses existing Injekt framework:
```kotlin
private val networkHelper: NetworkHelper by injectLazy()
private val dbHelper: DBHelper by injectLazy()
private val gson: Gson by injectLazy()
```

## Benefits of This Architecture

1. **Testability**: Each layer can be tested independently
2. **Maintainability**: Clear separation of concerns
3. **Scalability**: Easy to add new features or modify existing ones
4. **Reusability**: Use cases can be reused in other features
5. **Modern**: Uses latest Android development practices (Compose, Flow, Coroutines)
