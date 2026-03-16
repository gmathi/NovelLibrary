package io.github.gmathi.novellibrary.compose.search

import androidx.compose.runtime.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * State holder for PersistentSearchView
 */
@Stable
class PersistentSearchState(
    initialSearchText: String = "",
    initialLogoText: String = ""
) {
    var searchText by mutableStateOf(initialSearchText)
        private set

    var textFieldValue by mutableStateOf(TextFieldValue(initialSearchText, TextRange(initialSearchText.length)))
        private set

    var logoText by mutableStateOf(initialLogoText)
        private set

    var isEditing by mutableStateOf(false)
        private set

    var isSearching by mutableStateOf(false)
        private set

    var suggestions by mutableStateOf<List<SearchSuggestion>>(emptyList())
        private set

    val searchOpen: Boolean
        get() = isEditing

    fun updateSearchText(text: String) {
        searchText = text
        textFieldValue = TextFieldValue(text, TextRange(text.length))
    }

    fun updateTextFieldValue(value: TextFieldValue) {
        textFieldValue = value
        searchText = value.text
    }

    fun updateLogoText(text: String) {
        logoText = text
    }

    fun updateSuggestions(newSuggestions: List<SearchSuggestion>) {
        suggestions = newSuggestions
    }

    fun openSearch() {
        // Place cursor at end of existing text when re-opening
        textFieldValue = TextFieldValue(searchText, TextRange(searchText.length))
        isEditing = true
    }

    fun closeSearch() {
        isEditing = false
        suggestions = emptyList()
        // Keep searchText and isSearching so the term persists in the bar
    }

    fun performSearch() {
        isSearching = true
        isEditing = false
        suggestions = emptyList()
        // searchText is preserved so it shows in the bar after search
    }

    fun clearSearch() {
        searchText = ""
        textFieldValue = TextFieldValue("", TextRange.Zero)
    }

    /**
     * Full reset: clears search text, exits search mode, returns to browse state.
     */
    fun resetSearch() {
        isEditing = false
        isSearching = false
        searchText = ""
        textFieldValue = TextFieldValue("", TextRange.Zero)
        suggestions = emptyList()
    }
}

/**
 * Remember a PersistentSearchState
 */
@Composable
fun rememberPersistentSearchState(
    initialSearchText: String = "",
    initialLogoText: String = ""
): PersistentSearchState {
    return remember {
        PersistentSearchState(
            initialSearchText = initialSearchText,
            initialLogoText = initialLogoText
        )
    }
}

/**
 * Search suggestion data class
 */
data class SearchSuggestion(
    val text: String,
    val value: String = text,
    val type: SearchSuggestionType = SearchSuggestionType.History
)

enum class SearchSuggestionType {
    History,
    Suggestion
}

/**
 * Interface for building search suggestions
 */
interface SearchSuggestionsBuilder {
    fun buildEmptySearchSuggestion(maxCount: Int): List<SearchSuggestion>
    fun buildSearchSuggestion(maxCount: Int, query: String): List<SearchSuggestion>
}

/**
 * Default implementation of SearchSuggestionsBuilder using search history
 */
class HistorySearchSuggestionsBuilder(
    private val searchHistory: List<String>
) : SearchSuggestionsBuilder {

    override fun buildEmptySearchSuggestion(maxCount: Int): List<SearchSuggestion> {
        return searchHistory
            .take(maxCount)
            .map { SearchSuggestion(it, it, SearchSuggestionType.History) }
    }

    override fun buildSearchSuggestion(maxCount: Int, query: String): List<SearchSuggestion> {
        return searchHistory
            .filter { it.startsWith(query, ignoreCase = true) }
            .take(maxCount)
            .map { SearchSuggestion(it, it, SearchSuggestionType.History) }
    }
}
