package io.github.gmathi.novellibrary.util

import io.github.gmathi.novellibrary.dataCenter
import org.cryse.widget.persistentsearch.SearchItem
import org.cryse.widget.persistentsearch.SearchSuggestionsBuilder

class SuggestionsBuilder : SearchSuggestionsBuilder {

    private val maxResults = 5

    override fun buildEmptySearchSuggestion(maxCount: Int): Collection<SearchItem> {
        val searchHistory = dataCenter.loadSearchHistory()
        if (maxResults > searchHistory.size) return searchHistory.subList(0, searchHistory.size).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
        return searchHistory.subList(0, maxResults).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
    }

    override fun buildSearchSuggestion(maxCount: Int, query: String): Collection<SearchItem> {
        val searchHistory = dataCenter.loadSearchHistory()
        val items = ArrayList<String>()

        //region commented suggestions
//        if (query.startsWith("@")) {
//            val peopleSuggestion = SearchItem(
//                    "Search People: " + query.substring(1),
//                    query,
//                    SearchItem.TYPE_SEARCH_ITEM_SUGGESTION
//            )
//            items.add(peopleSuggestion)
//        } else if (query.startsWith("#")) {
//            val toppicSuggestion = SearchItem(
//                    "Search Topic: " + query.substring(1),
//                    query,
//                    SearchItem.TYPE_SEARCH_ITEM_SUGGESTION
//            )
//            items.add(toppicSuggestion)
//        } else {
//            val peopleSuggestion = SearchItem(
//                    "Search People: " + query,
//                    "@" + query,
//                    SearchItem.TYPE_SEARCH_ITEM_SUGGESTION
//            )
//            items.add(peopleSuggestion)
//            val topicSuggestion = SearchItem(
//                    "Search Topic: " + query,
//                    "#" + query,
//                    SearchItem.TYPE_SEARCH_ITEM_SUGGESTION
//            )
//            items.add(topicSuggestion)
//        }
        //endregion

        searchHistory.filterTo(items) { it.startsWith(query, ignoreCase = true) }

        if (maxResults > items.size) return items.subList(0, items.size).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
        return items.subList(0, maxResults).map { SearchItem(it, it, SearchItem.TYPE_SEARCH_ITEM_HISTORY) }
    }
}