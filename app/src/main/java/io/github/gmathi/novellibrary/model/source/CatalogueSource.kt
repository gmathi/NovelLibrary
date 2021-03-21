package io.github.gmathi.novellibrary.model.source

import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.util.lang.awaitSingle
import rx.Observable

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
     * Returns an observable containing a page with a list of novel.
     *
     * @param page the page number to retrieve.
     */
    fun fetchPopularNovels(page: Int): Observable<NovelsPage>

    /**
     * [1.x API] Return all the popular novels for this source.
     */
    suspend fun getPopularNovels(page: Int): NovelsPage {
        return fetchPopularNovels(page).awaitSingle()
    }

    /**
     * Returns an observable containing a page with a list of novels.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage>

    /**
     * [1.x API] Search and return all the novels that return for that query (search term).
     */
    suspend fun getSearchNovels(page: Int, query: String, filters: FilterList = FilterList(emptyList())): NovelsPage {
        return fetchSearchNovels(page, query, filters).awaitSingle()
    }

    /**
     * Returns an observable containing a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    fun fetchLatestUpdates(page: Int): Observable<NovelsPage>

    /**
     * Returns the list of filters for the source.
     */
    fun getFilterList(): FilterList
}