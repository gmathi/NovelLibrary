package io.github.gmathi.novellibrary.model.source.online

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.CatalogueSource
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.awaitSuccess
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.util.*

/**
 * A simple implementation for sources from a website.
 */
@Suppress("unused", "unused_parameter", "ThrowableNotThrown")
abstract class HttpSource : CatalogueSource {

    /**
     * Network service.
     */
    protected val network: NetworkHelper by injectLazy()

    /**
     * Network service.
     */
    protected val dataCenter: DataCenter by injectLazy()

    /**
     * Base url of the website without the trailing slash, like: http://mysite.com
     */
    abstract val baseUrl: String

    /**
     * Version id used to generate the source id. If the site completely changes and urls are
     * incompatible, you may increase this value and it'll be considered as a new source.
     */
    open val versionId: Int = 1

    /**
     * Id of the source. By default it uses a generated id using the first 16 characters (64 bits)
     * of the MD5 of the string: sourcename/language/versionId
     * Note the generated id sets the sign bit to 0.
     */
    override val id by lazy {
        val key = "${name.lowercase(Locale.ROOT)}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    /**
     * Headers used for requests.
     */
    val headers: Headers by lazy { headersBuilder().build() }

    /**
     * Default network client for doing requests.
     */
    open val client: OkHttpClient
        get() = network.client

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    protected open fun headersBuilder() = Headers.Builder().apply {
        add("User-Agent", DEFAULT_USER_AGENT)
    }

    /**
     * Visible name of the source.
     */
    override fun toString() = "$name (${lang.uppercase(Locale.ROOT)})"


    /**
     * Returns a page with a list of novels. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     */
    override suspend fun getPopularNovels(page: Int): NovelsPage {
        val response = client.newCall(popularNovelsRequest(page)).awaitSuccess()
        return popularNovelsParse(response)
    }

    /**
     * Returns the request for the popular novels given the page.
     *
     * @param page the page number to retrieve.
     */
    protected abstract fun popularNovelsRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun popularNovelsParse(response: Response): NovelsPage


    /**
     * Returns a page with a list of novels. Normally it's not needed to
     * override this method.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    override suspend fun getSearchNovels(page: Int, query: String, filters: FilterList): NovelsPage {
        val response = client.newCall(searchNovelsRequest(page, query, filters)).awaitSuccess()
        return searchNovelsParse(response)
    }

    /**
     * Returns the request for the search novel query given the page.
     *
     * @param page the page number to retrieve.
     * @param query the search query.
     * @param filters the list of filters to apply.
     */
    protected abstract fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun searchNovelsParse(response: Response): NovelsPage


    /**
     * Returns a page with a list of latest novel updates.
     *
     * @param page the page number to retrieve.
     */
    override suspend fun getLatestUpdates(page: Int): NovelsPage {
        val response = client.newCall(latestUpdatesRequest(page)).awaitSuccess()
        return latestUpdatesParse(response)
    }

    /**
     * Returns the request for latest novel given the page.
     *
     * @param page the page number to retrieve.
     */
    protected abstract fun latestUpdatesRequest(page: Int): Request

    /**
     * Parses the response from the site and returns a [NovelsPage] object.
     *
     * @param response the response from the site.
     */
    protected abstract fun latestUpdatesParse(response: Response): NovelsPage


    /**
     * Returns the updated details for a novel. Normally it's not needed to
     * override this method.
     *
     * @param novel the novel to be updated.
     */
    override suspend fun fetchNovelDetails(novel: Novel): Novel {
        val response = client.newCall(novelDetailsRequest(novel)).awaitSuccess()
        return novelDetailsParse(novel, response)
    }

    /**
     * Returns the request for the details of a novel. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param novel the novel to be updated.
     */
    open fun novelDetailsRequest(novel: Novel): Request {
        return GET(novel.url, headers)
    }

    /**
     * Parses the response from the site and returns the details of a novel.
     *
     * @param response the response from the site.
     */
    protected abstract fun novelDetailsParse(novel: Novel, response: Response): Novel


    /**
     * Returns the updated chapter list for a novel. Normally it's not needed to
     * override this method.
     *
     * @param novel the novel to look for chapters.
     */
    override suspend fun getChapterList(novel: Novel): List<WebPage> {
        val response = client.newCall(chapterListRequest(novel)).awaitSuccess()
        return chapterListParse(novel, response)
    }

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param novel the novel to look for chapters.
     */
    protected open fun chapterListRequest(novel: Novel): Request {
        return GET(novel.url, headers)
    }

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    protected abstract fun chapterListParse(novel: Novel, response: Response): List<WebPage>


    /**
     * Assigns the url of the novel without the scheme and domain. It saves some redundancy from
     * database and the urls could still work after a domain change.
     *
     * @param url the full url to the novel.
     */
    fun Novel.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Returns the url of the given string without the scheme and domain.
     *
     * @param orig the full url.
     */
    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }

    /**
     * Called before inserting a new chapter into database. Use it if you need to override chapter
     * fields, like the title or the chapter number. Do not change anything to [novel].
     *
     * @param chapter the chapter to be added.
     * @param novel the novel of the chapter.
     */
    open fun prepareNewChapter(chapter: WebPage, novel: Novel) {
    }

    /**
     * Returns the list of filters for the source.
     */
    override fun getFilterList() = FilterList()

    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0"
    }
}
