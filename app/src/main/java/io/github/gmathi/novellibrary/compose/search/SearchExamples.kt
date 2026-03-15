package io.github.gmathi.novellibrary.compose.search

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Example usage of PersistentSearchView in a Compose screen
 */
@Composable
fun SearchScreenExample(
    searchHistory: List<String>,
    onHomeClick: () -> Unit,
    onSearch: (String) -> Unit
) {
    val searchState = rememberPersistentSearchState(initialLogoText = "Search Novels")
    val suggestionBuilder = remember(searchHistory) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search novels...",
                homeButtonMode = HomeButtonMode.Burger,
                onHomeButtonClick = onHomeClick,
                onSearch = { query ->
                    onSearch(query)
                },
                onSearchTermChanged = { term ->
                    // Handle real-time search if needed
                },
                onSearchEditOpened = {
                    // Handle search opened
                },
                onSearchEditClosed = {
                    // Handle search closed
                },
                onSearchExit = {
                    // Handle search exit
                },
                onSearchCleared = {
                    // Handle search cleared
                },
                onSuggestionClick = { suggestion ->
                    // Return true to accept the suggestion
                    true
                },
                suggestionBuilder = suggestionBuilder,
                elevation = 4
            )

            // Your content here
            // For example: search results, tabs, etc.
        }

        // Background tint overlay
        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}

/**
 * Example for library search with real-time filtering
 */
@Composable
fun LibrarySearchExample(
    searchHistory: List<String>,
    onBackClick: () -> Unit,
    onSearchTermChanged: (String) -> Unit
) {
    val searchState = rememberPersistentSearchState(initialLogoText = "Search Library")
    val suggestionBuilder = remember(searchHistory) {
        HistorySearchSuggestionsBuilder(searchHistory)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PersistentSearchView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                state = searchState,
                hint = "Search library...",
                homeButtonMode = HomeButtonMode.Arrow,
                onHomeButtonClick = onBackClick,
                onSearch = { query ->
                    // Handle search submission
                },
                onSearchTermChanged = { term ->
                    // Real-time filtering
                    onSearchTermChanged(term)
                },
                suggestionBuilder = suggestionBuilder,
                elevation = 4
            )

            // Your filtered content here
        }

        SearchBackgroundTint(
            visible = searchState.isEditing,
            onClick = { searchState.closeSearch() }
        )
    }
}
