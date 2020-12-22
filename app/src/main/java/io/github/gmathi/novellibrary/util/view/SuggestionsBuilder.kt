package io.github.gmathi.novellibrary.util.view

import org.cryse.widget.persistentsearch.SearchItem
import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder

class SuggestionsBuilder(val searchHistory: ArrayList<String>) : SearchSuggestionsBuilder {

    private val maxResults = 5

    override fun buildEmptySearchSuggestion(maxCount: Int): Collection<SearchItem> {
        if (maxResults > searchHistory.size) return searchHistory.subList(0, searchHistory.size).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
        return searchHistory.subList(0, maxResults).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
    }

    override fun buildSearchSuggestion(maxCount: Int, query: String): Collection<SearchItem> {
        //val searchHistory = dataCenter.loadSearchHistory()
        val items = ArrayList<String>()
        searchHistory.filterTo(items) { it.startsWith(query, ignoreCase = true) }
        if (maxResults > items.size) return items.subList(0, items.size).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
        return items.subList(0, maxResults).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
    }
}