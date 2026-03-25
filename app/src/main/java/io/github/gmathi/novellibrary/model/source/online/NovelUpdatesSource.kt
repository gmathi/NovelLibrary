package io.github.gmathi.novellibrary.model.source.online

import android.os.Build
import androidx.core.net.toUri
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.TranslatorSource
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.network.asObservableSuccess
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.error.Exceptions.INVALID_NOVEL
import io.github.gmathi.novellibrary.util.logging.Logs
import io.github.gmathi.novellibrary.util.error.Exceptions.MISSING_IMPLEMENTATION
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
        Logs.info(TAG, "SearchKeyword request: POST $url | query='$query'")
        return POST(url, body = formBody)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        // Log raw HTML response for debugging
        Logs.info(TAG, "===== SEARCH KEYWORD RAW RESPONSE =====")
        Logs.info(TAG, "Response URL: ${response.request.url}")
        Logs.info(TAG, "Response Code: ${response.code}")
        val rawHtml = document.outerHtml()
        rawHtml.chunked(3000).forEachIndexed { index, chunk ->
            Logs.info(TAG, "SearchKeyword HTML [$index]: $chunk")
        }
        Logs.info(TAG, "Selector used: ${searchNovelsSelector()}")
        Logs.info(TAG, "Elements found: ${document.select(searchNovelsSelector()).size}")
        Logs.info(TAG, "===== END SEARCH KEYWORD RAW RESPONSE =====")

        val novels = ArrayList<Novel>()
        document.select(searchNovelsSelector()).forEach { element ->
            try {
                val novel = searchNovelsFromElement(element)
                Logs.info(TAG, "SearchKeyword parsed novel: name=${novel.name}, url=${novel.url}, imageUrl=${novel.imageUrl}")
                novels.add(novel)
            } catch (e: Exception) {
                Logs.warning(TAG, "SearchKeyword failed to parse element: ${element.outerHtml()}", e)
            }
        }

        val hasNextPage = searchNovelsNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null

        Logs.info(TAG, "SearchKeyword result: ${novels.size} novels, hasNextPage=$hasNextPage")
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

    //region Series Finder Search
    suspend fun searchSeriesFinder(query: String, page: Int): NovelsPage {
        return fetchSeriesFinderSearch(query, page).awaitSingle()
    }

    private fun fetchSeriesFinderSearch(query: String, page: Int): Observable<NovelsPage> {
        return client.newCall(seriesFinderRequest(query, page))
            .asObservableSuccess()
            .map { response ->
                seriesFinderParse(response)
            }
    }

    private fun seriesFinderRequest(query: String, page: Int): Request {
        val url = "$baseUrl/series-finder/?sf=1&sh=$query&sort=sdate&order=desc".addPageNumberToUrl(page, "pg")
        Logs.info(TAG, "SeriesFinder request: GET $url | query='$query', page=$page")
        return GET(url, headers)
    }

    private fun seriesFinderParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        Logs.info(TAG, "===== SERIES FINDER RAW RESPONSE =====")
        Logs.info(TAG, "Response URL: ${response.request.url}")
        Logs.info(TAG, "Response Code: ${response.code}")
        val rawHtml = document.outerHtml()
        rawHtml.chunked(3000).forEachIndexed { index, chunk ->
            Logs.info(TAG, "SeriesFinder HTML [$index]: $chunk")
        }
        Logs.info(TAG, "Elements found: ${document.select(popularNovelsSelector()).size}")
        Logs.info(TAG, "===== END SERIES FINDER RAW RESPONSE =====")

        // Series-finder uses the same HTML layout as popular/ranking pages
        val novels = document.select(popularNovelsSelector()).mapNotNull { element ->
            try {
                val novel = popularNovelsFromElement(element)
                Logs.info(TAG, "SeriesFinder parsed novel: name=${novel.name}, url=${novel.url}, rating=${novel.rating}, imageUrl=${novel.imageUrl}")
                novel
            } catch (e: Exception) {
                Logs.warning(TAG, "SeriesFinder failed to parse element: ${element.outerHtml()}", e)
                null
            }
        }

        val hasNextPage = popularNovelNextPageSelector().let { selector ->
            document.select(selector).first()
        } != null

        Logs.info(TAG, "SeriesFinder result: ${novels.size} novels, hasNextPage=$hasNextPage")
        return NovelsPage(novels, hasNextPage)
    }
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
        return client.newCall(translatorSourcesRequest(novel))
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
        return client.newCall(chapterListWithSourcesRequest(novel, translatorSource))
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
        return client.newCall(popularNovelsRequest(rank, url, page))
            .asObservableSuccess()
            .map { response ->
                popularNovelsParse(response)
            }
    }

    private fun popularNovelsRequest(rank: String?, url: String?, page: Int): Request {
        rank?.let {
            val requestUrl = "$baseUrl/series-ranking/?rank=$rank".addPageNumberToUrl(page, "pg")
            Logs.info(TAG, "SearchUrl request: GET $requestUrl")
            return GET(requestUrl, headers)
        }
        url?.let {
            val requestUrl = url.addPageNumberToUrl(page, "pg")
            Logs.info(TAG, "SearchUrl request: GET $requestUrl")
            return GET(requestUrl, headers)
        }
        throw Exception(MISSING_IMPLEMENTATION)
    }

    override fun popularNovelsSelector(): String = "div.search_main_box_nu"

    override fun popularNovelsFromElement(element: Element): Novel {
        val novelUrl = element.selectFirst("div.search_title > a")?.attr("abs:href") ?: throw Exception(INVALID_NOVEL)
        val novel = Novel(novelUrl, id)
        element.selectFirst("div.search_title > a")?.text()?.let { novel.name = it }
        novel.imageUrl = element.selectFirst("div.search_img_nu img[src]")?.attr("abs:src")

        // Rank (e.g. "#1")
        val rankText = element.selectFirst("span.genre_rank")?.text()
        if (!rankText.isNullOrEmpty()) novel.metadata["Rank"] = rankText

        // Origin language (e.g. "KR", "CN", "JP")
        val originText = element.selectFirst("span.orgkr, span.orgcn, span.orgjp")?.text()
        if (!originText.isNullOrEmpty()) novel.metadata["OriginMarker"] = originText

        // Rating — try div.search_ratings first, then count stars as fallback
        val ratingsDiv = element.selectFirst("div.search_ratings, span.search_ratings, div.search_rating")
        if (ratingsDiv != null) {
            val ratingsText = ratingsDiv.text() // e.g. "KR (4.5)"
            val ratingMatch = Regex("\\(([\\d.]+)\\)").find(ratingsText)
            if (ratingMatch != null) {
                novel.rating = ratingMatch.groupValues[1]
            } else {
                // Try extracting just the number
                val numberMatch = Regex("([\\d.]+)").find(ratingsText.replace(originText ?: "", "").trim())
                novel.rating = numberMatch?.groupValues?.get(1) ?: "N/A"
            }
        } else {
            // Fallback: count star icons in search_stars div
            val starsDiv = element.selectFirst("div.search_stars")
            if (starsDiv != null) {
                val fullStars = starsDiv.select("i.fa-star").size
                val halfStars = starsDiv.select("i.fa-star-half-o, i.fa-star-half").size
                val rating = fullStars + (halfStars * 0.5f)
                novel.rating = if (rating > 0) String.format("%.1f", rating) else "N/A"
            } else {
                novel.rating = "N/A"
            }
        }

        // Genres
        val genreElements = element.select("div.search_genre a.gennew")
        if (genreElements.isNotEmpty()) {
            novel.genres = genreElements.map { it.text() }
        }

        // Stats: chapters, frequency, readers, reviews, last updated
        // Handle both ss_desk and ss_mb class variants
        val statSpans = element.select("div.search_stats span.ss_desk, div.search_stats span.ss_mb")
        for (stat in statSpans) {
            val text = stat.text().trim()
            when {
                text.contains("Chapters", ignoreCase = true) -> {
                    novel.metadata["Chapters"] = text
                    val count = text.replace(",", "").filter { it.isDigit() }.toLongOrNull()
                    if (count != null) novel.chaptersCount = count
                }
                text.contains("Day(s)", ignoreCase = true) -> novel.metadata["Frequency"] = text
                text.contains("Readers", ignoreCase = true) -> novel.metadata["Readers"] = text
                text.contains("Reviews", ignoreCase = true) -> novel.metadata["Reviews"] = text
                text.matches(Regex(".*\\d{2}-\\d{2}-\\d{4}.*")) -> novel.metadata["LastUpdated"] = text
            }
        }

        // Status from genre area
        val statusElement = element.selectFirst("div.search_genre a.gennew.search.on")
        if (statusElement != null) {
            novel.metadata["Status"] = statusElement.text()
        }

        // Short description — the last div in the search box contains the description text.
        // It has no specific class, just inline style. We look for the hidden full text first,
        // then fall back to the visible truncated text.
        val descDiv = element.children().lastOrNull { it.tagName() == "div" && !it.hasClass("search_genre") && !it.hasClass("search_stats") && !it.hasClass("search_title") && !it.hasClass("search_img_nu") }
            ?: element.selectFirst("div.search_body_nu")
        if (descDiv != null) {
            // Prefer the full hidden text inside span.testhide
            val fullText = descDiv.selectFirst("span.testhide")?.text()?.trim()
            if (!fullText.isNullOrEmpty()) {
                novel.shortDescription = fullText.replace(Regex("\\s*<<less\\s*$"), "").trim()
            } else {
                // Fall back to visible text nodes (before "more>>")
                val textNodes = descDiv.textNodes()
                val descText = textNodes.joinToString("") { it.text() }.trim()
                if (descText.isNotEmpty()) {
                    novel.shortDescription = descText
                }
            }
        }

        return novel
    }

    override fun popularNovelNextPageSelector(): String = "div.digg_pagination a.next_page"

    override fun popularNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        // Log raw HTML response for debugging
        Logs.info(TAG, "===== SEARCH URL (POPULAR) RAW RESPONSE =====")
        Logs.info(TAG, "Response URL: ${response.request.url}")
        Logs.info(TAG, "Response Code: ${response.code}")
        val rawHtml = document.outerHtml()
        // Log in chunks since Android logcat has a size limit
        rawHtml.chunked(3000).forEachIndexed { index, chunk ->
            Logs.info(TAG, "SearchUrl HTML [$index]: $chunk")
        }
        Logs.info(TAG, "Selector used: ${popularNovelsSelector()}")
        Logs.info(TAG, "Elements found: ${document.select(popularNovelsSelector()).size}")
        Logs.info(TAG, "===== END SEARCH URL RAW RESPONSE =====")

        val novels = document.select(popularNovelsSelector()).mapNotNull { element ->
            try {
                val novel = popularNovelsFromElement(element)
                Logs.info(TAG, "SearchUrl parsed novel: name=${novel.name}, url=${novel.url}, rating=${novel.rating}, imageUrl=${novel.imageUrl}")
                novel
            } catch (e: Exception) {
                Logs.warning(TAG, "SearchUrl failed to parse element: ${element.outerHtml()}", e)
                null
            }
        }
        val hasNextPage = document.select(popularNovelNextPageSelector()) != null

        Logs.info(TAG, "SearchUrl result: ${novels.size} novels, hasNextPage=$hasNextPage")
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
        return client.newCall(GET(url, headers))
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
        private const val TAG = "NovelUpdatesSource"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; HD1913) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.7632.46 Mobile Safari/537.36 EdgA/144.0.3719.115"
    }
}