package io.github.gmathi.novellibrary.source

import android.os.Build
import androidx.core.net.toUri
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.network.asObservableSuccess
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions.INVALID_NOVEL
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.lang.addPageNumberToUrl
import io.github.gmathi.novellibrary.util.lang.awaitSingle
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import rx.Observable
import rx.schedulers.Schedulers
import java.net.URI
import kotlin.math.max
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.RequestPriority


class NovelUpdatesSource : ParsedHttpSource() {

    override val id: Long
        get() = Constants.SourceId.NOVEL_UPDATES
    override val baseUrl: String
        get() = "https://${HostNames.NOVEL_UPDATES}"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Updates"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nd_ajaxsearchmain")
            .add("strType", "desktop")
            .add("strOne", query)
            .add("strSearchType", "series")
            .build()
        return POST(url, body = formBody)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = ArrayList<Novel>()
        document.select(searchNovelsSelector()).forEach { element ->
            try {
                val novel = searchNovelsFromElement(element)
                novels.add(novel)
            } catch (e: Exception) {
                //Do Nothing
            }
        }

        val hasNextPage = searchNovelsNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null

        return NovelsPage(novels, false)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val url = element.attr("abs:href") ?: throw Exception(INVALID_NOVEL)
        val novel = Novel(url, id)
        novel.name = element.text()
        novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
        novel.rating = "N/A"
        return novel
    }

    override fun searchNovelsSelector() = "a.a_search"
    override fun searchNovelsNextPageSelector() = "div.digg_pagination a.next.page-numbers"
//endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        document.selectFirst(".seriestitlenu")?.text()?.let { novel.name = it }
        novel.imageUrl = document.selectFirst(".seriesimg > img[src],.serieseditimg > img[src]")?.attr("abs:src")
        novel.longDescription = document.body().selectFirst("#editdescription")?.text()
        novel.rating = document.body().selectFirst("span.uvotes")?.text()?.substring(1, 4)
        novel.genres = document.body().selectFirst("#seriesgenre")?.children()?.map { it.text() }

        document.select(".genre").let { elements ->
            elements.select("#authtag").joinToString(", ") { it.outerHtml() }.let { novel.metadata["Author(s)"] = it }
            elements.select("[gid]").joinToString(", ") { it.outerHtml() }.let { novel.metadata["Genre(s)"] = it }
            elements.select("#artiststag").joinToString(", ") { it.outerHtml() }.let { novel.metadata["Artist(s)"] = it }
            elements.select("#etagme").joinToString(", ") { it.outerHtml() }.let { novel.metadata["Tags"] = it }
            elements.select(".type").firstOrNull()?.outerHtml()?.let { novel.metadata["Type"] = it }
            elements.select("a[lid].lang").firstOrNull()?.outerHtml()?.let { novel.metadata["Language"] = it }
            elements.select("#myopub").joinToString(", ") { it.outerHtml() }.let { novel.metadata["Original Publisher"] = it }
            elements.select("#myepub").joinToString(", ") { it.outerHtml() }.let { novel.metadata["English Publisher"] = it }
        }

        document.select("#edityear").firstOrNull()?.text()?.let { novel.metadata["Year"] = it }
        document.select("#editstatus").firstOrNull()?.text()?.let { novel.metadata["Status in Country of Origin"] = it }
        document.select("#showlicensed").firstOrNull()?.text()?.let { novel.metadata["Licensed (in English)"] = it }
        document.select("#showtranslated").firstOrNull()?.text()?.let { novel.metadata["Completely Translated"] = it }
        document.select("#editassociated").firstOrNull()?.text()?.let { novel.metadata["Associated Names"] = it }
        document.select("#mypostid").firstOrNull()?.attr("value")?.let {
            novel.externalNovelId = it
            novel.metadata["PostId"] = it
        }
        document.select("h5.seriesother").firstOrNull { el -> el.ownText() == "Release Frequency" }?.nextSibling()?.let { node ->
            if (node is TextNode) novel.metadata["Release Frequency"] = node.text().trim()
        }

        return novel
    }
    //endregion

    //region Chapters
    suspend fun getUnsortedChapterList(novel: Novel): List<WebPage> {
        return getChapterListForSource(novel, null)
    }

    override suspend fun getChapterList(novel: Novel): List<WebPage> {
        return if (dataCenter.useNUAPIFetch)
            getChaptersFromAPI(novel)
        else
            getChaptersFromDoc(novel)
    }

    private suspend fun getChaptersFromAPI(novel: Novel): List<WebPage> {
        val translatorSources = getTranslatorSourcesList(novel)
        val allChapters = getChapterListForSource(novel, null)
        val translatorSourcesMap = HashMap<String, String>()
        val observableList = translatorSources.map { fetchChapterListWithSources(novel, it) }
        val translatorSourceListOfChapterList = Observable
            .from(observableList)
            .flatMap { task -> task.observeOn(Schedulers.io()) }
            .toList().awaitSingle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            translatorSourceListOfChapterList.parallelStream().forEach { translatorSourceOnlyChapterList ->
                translatorSourcesMap.putAll(createTranslatorSourceMap(translatorSourceOnlyChapterList))
            }
        } else {
            translatorSourceListOfChapterList.forEach { translatorSourceOnlyChapterList ->
                translatorSourcesMap.putAll(createTranslatorSourceMap(translatorSourceOnlyChapterList))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            allChapters.parallelStream().forEach {
                it.translatorSourceName = translatorSourcesMap[it.url]
            }
        } else {
            allChapters.forEach {
                it.translatorSourceName = translatorSourcesMap[it.url]
            }
        }

        return allChapters
    }

    private fun createTranslatorSourceMap(translatorSourceOnlyChapterList: List<WebPage>): HashMap<String, String> {
        if (translatorSourceOnlyChapterList.isEmpty()) return HashMap()
        val translatorSourceName = translatorSourceOnlyChapterList.first().translatorSourceName
            ?: return HashMap()
        val map = HashMap<String, String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            translatorSourceOnlyChapterList.parallelStream().forEach {
                map[it.url] = translatorSourceName
            }
        } else {
            translatorSourceOnlyChapterList.forEach {
                map[it.url] = translatorSourceName
            }
        }
        return map
    }

    override fun chapterListSelector() = "[data-id]"
    override fun chapterFromElement(element: Element): WebPage {
        val url = "https:" + element.attr("href")
        val name = element.getElementsByAttribute("title").attr("title")
        return WebPage(url, name)
    }

    override fun chapterListRequest(novel: Novel): Request {
        val novelUpdatesNovelId = novel.externalNovelId ?: novel.metadata["PostId"] ?: throw Exception(INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nd_getchapters")
            .add("mypostid", novelUpdatesNovelId)
            .build()
        return POST(url, body = formBody)
    }
    //endregion

    //region Translator Sources
    private suspend fun getTranslatorSourcesList(novel: Novel): List<TranslatorSource> {
        return fetchTranslatorSourcesList(novel).awaitSingle()
    }

    private fun fetchTranslatorSourcesList(novel: Novel): Observable<List<TranslatorSource>> {
        return network.newCallWithPriority(translatorSourcesRequest(novel), RequestPriority.NORMAL)
            .asObservableSuccess()
            .map { response ->
                translatorSourcesParse(response)
            }
    }

    private fun translatorSourcesRequest(novel: Novel): Request {
        val novelUpdatesNovelId = novel.externalNovelId ?: novel.metadata["PostId"] ?: throw Exception(INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nd_getgroupnovel")
            .add("mypostid", novelUpdatesNovelId)
            .add("mygrr", "0")
            .build()
        return POST(url, body = formBody)
    }

    private fun translatorSourcesParse(response: Response): List<TranslatorSource> {
        val document = response.asJsoup()
        return document.select("div.checkbox").map { element ->
            TranslatorSource(
                element.selectFirst("input.grp-filter-attr[value]")?.attr("value")?.toLong() ?: 0L,
                element.text()
            )
        }
    }
    //endregion

    //region Chapters for Translator Source Only

    private suspend fun getChapterListForSource(novel: Novel, translatorSource: TranslatorSource?): List<WebPage> {
        return fetchChapterListWithSources(novel, translatorSource).awaitSingle()
    }

    private fun fetchChapterListWithSources(novel: Novel, translatorSource: TranslatorSource?): Observable<List<WebPage>> {
        return network.newCallWithPriority(chapterListWithSourcesRequest(novel, translatorSource), RequestPriority.NORMAL)
            .asObservableSuccess()
            .map { response ->
                chapterListParse(novel, response, translatorSource)
            }
    }

    private fun chapterListWithSourcesRequest(novel: Novel, translatorSource: TranslatorSource?): Request {
        val novelUpdatesNovelId = novel.externalNovelId ?: novel.metadata["PostId"] ?: throw Exception(INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBodyBuilder = FormBody.Builder()
            .add("action", "nd_getchapters")
            .add("mypostid", novelUpdatesNovelId)
            .add("mygrr", "0")
        translatorSource?.let { formBodyBuilder.add("mygrpfilter", it.id.toString()) }
        return POST(url, body = formBodyBuilder.build())
    }

    private fun chapterListParse(novel: Novel, response: Response, translatorSource: TranslatorSource?): List<WebPage> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).reversed().mapIndexed { index, element ->
            val chapter = chapterFromElement(element)
            chapter.novelId = novel.id
            chapter.orderId = index.toLong()
            chapter.translatorSourceName = translatorSource?.name
            chapter
        }
    }

    //endregion

    //region Popular Novels by Rank
    suspend fun getPopularNovels(rank: String?, url: String?, page: Int): NovelsPage {
        return fetchPopularNovels(rank, url, page).awaitSingle()
    }

    private fun fetchPopularNovels(rank: String?, url: String?, page: Int): Observable<NovelsPage> {
        return network.newCallWithPriority(popularNovelsRequest(rank, url, page), RequestPriority.NORMAL)
            .asObservableSuccess()
            .map { response ->
                popularNovelsParse(response)
            }
    }

    private fun popularNovelsRequest(rank: String?, url: String?, page: Int): Request {
        rank?.let {
            val requestUrl = "$baseUrl/series-ranking/?rank=$rank".addPageNumberToUrl(page, "pg")
            return GET(requestUrl, headers)
        }
        url?.let {
            return GET(url.addPageNumberToUrl(page, "pg"), headers)
        }
        throw Exception(MISSING_IMPLEMENTATION)
    }

    override fun popularNovelsSelector(): String = "div.search_main_box_nu"

    override fun popularNovelsFromElement(element: Element): Novel {
        val novelUrl = element.selectFirst("div.search_title > a")?.attr("abs:href") ?: throw Exception(INVALID_NOVEL)
        val novel = Novel(novelUrl, id)
        element.selectFirst("div.search_title > a")?.text()?.let { novel.name = it }
        novel.imageUrl = element.selectFirst("div.search_img_nu img[src]")?.attr("abs:src")
        val originText = element.select("div.search_ratings>span").text()
        if (!originText.isNullOrEmpty()) novel.metadata["OriginMarker"] = originText
        val ratingText = element.select("div.search_ratings").text()
        if (ratingText.contains("(")) {
            novel.rating = ratingText.split("(")[1].trim().replace("(", "").replace(")", "")
        } else
            novel.rating = "N/A"
        return novel
    }

    override fun popularNovelNextPageSelector(): String = "div.digg_pagination a.next_page"

    override fun popularNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()
        val novels = document.select(popularNovelsSelector()).map { element ->
            popularNovelsFromElement(element)
        }
        val hasNextPage = document.select(popularNovelNextPageSelector()) != null
        return NovelsPage(novels, hasNextPage)
    }

    //endregion

    //region stubs
    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> = throw Exception(MISSING_IMPLEMENTATION)

    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesFromElement(element: Element): Novel = throw Exception(MISSING_IMPLEMENTATION)

    override fun latestUpdatesNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

//endregion


    //region old code revival

    private fun fetchChapterListForPage(url: String): Observable<Document> {
        return network.newCallWithPriority(GET(url, headers), RequestPriority.NORMAL)
            .asObservableSuccess()
            .map { response ->
                response.asJsoup()
            }
    }

    private suspend fun getChaptersFromDoc(novel: Novel): List<WebPage> {
        val chapters = ArrayList<WebPage>()
        try {
            val document = fetchChapterListForPage(novel.url).awaitSingle()
            chapters.addAll(getNUChapterUrlsFromDoc(document))
            val pageUrls = getNUPageUrlsNew(document)
            if (pageUrls.isNotEmpty()) {
                val observableList = pageUrls.map { fetchChapterListForPage(it) }
                val allChaptersFromPagesList = Observable
                    .from(observableList)
                    .flatMap { task -> task.observeOn(Schedulers.io()) }
                    .toList().awaitSingle()
                allChaptersFromPagesList.forEach { chapters.addAll(getNUChapterUrlsFromDoc(it)) }
            }
        } catch (e: Exception) {
            // Do Nothing
        }
        chapters.reversed().forEachIndexed { index, chapter ->
            chapter.novelId = novel.id
            chapter.orderId = index.toLong()
        }

        return chapters.reversed()
    }

    private fun getNUChapterUrlsFromDoc(doc: Document): ArrayList<WebPage> {
        val chapters = ArrayList<WebPage>()
        val tableRowElements = doc.body().select("table#myTable > tbody > tr")
        tableRowElements.mapTo(chapters) {
            val translatorSource = it.select("a").firstOrNull()?.text()
            val chapElement = it.select("a.chp-release")
            val webPage = WebPage(chapElement.attr("abs:href"), chapElement.text())
            webPage.translatorSourceName = translatorSource
            webPage
        }
        return chapters
    }

    private fun getNUPageUrlsNew(doc: Document): ArrayList<String> {
        val uri = URI(doc.location())
        val basePath = "${uri.scheme}://${uri.host}${uri.path}"
        val pageUrls = ArrayList<String>()
        val pageElements = doc.body().select("div.digg_pagination > a[href]")
        var maxPageNum = 1
        pageElements.forEach {
            try {
                val pageNum = it.attr("abs:href").toUri().getQueryParameter("pg")?.toIntOrNull() ?: return@forEach
                if (maxPageNum < pageNum)
                    maxPageNum = pageNum
            } catch (_: Exception) {
                //Do Nothing
            }
        }
        if (maxPageNum == 2) {
            pageUrls.add("$basePath?pg=2")
        } else if (maxPageNum > 2)
            (2..maxPageNum).mapTo(pageUrls) { "$basePath?pg=$it" }

        return pageUrls
    }

    private fun getMaxPageNum(doc: Document): Int {
        val pageElements = doc.body().select("div.digg_pagination > a[href]")
        var maxPageNum = 1
        pageElements.forEach {
            try {
                val pageNum = it.attr("abs:href").toUri().getQueryParameter("pg")?.toIntOrNull() ?: return@forEach
                if (maxPageNum < pageNum)
                    maxPageNum = pageNum
            } catch (_: Exception) {
                //Do Nothing
            }
        }
        return maxPageNum
    }
    //endregion


    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}