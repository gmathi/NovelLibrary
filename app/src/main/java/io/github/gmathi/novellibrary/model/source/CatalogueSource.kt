package io.github.gmathi.novellibray.source

import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.Source
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
     * Returns an observable containing a page with a list of novels.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage>

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