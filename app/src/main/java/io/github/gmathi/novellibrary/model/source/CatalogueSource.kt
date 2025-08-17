package io.github.gmathi.novellibrary.model.source

import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList

interface CatalogueSource : Source {

    /**
     * An ISO 639-1 compliant language code (two letters in lower case).
     */
    val lang: String

    /**
     * Whether the source has support for latest updates.
     */
    val supportsLatest: Boolean

    /**
     * Returns a page with a list of novel.
     *
     * @param page the page number to retrieve.
     */
    suspend fun getPopularNovels(page: Int): NovelsPage

    /**
     * Returns a page with a list of novels.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    suspend fun getSearchNovels(page: Int, query: String, filters: FilterList = FilterList(emptyList())): NovelsPage

    /**
     * Returns a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    suspend fun getLatestUpdates(page: Int): NovelsPage

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList
}