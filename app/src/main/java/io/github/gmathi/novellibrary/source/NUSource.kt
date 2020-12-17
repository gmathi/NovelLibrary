package io.github.gmathi.novellibrary.source

import io.github.gmathi.novellibrary.model.database.Chapter
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.network.asObservableSuccess
import io.github.gmathi.novellibrary.util.Exceptions.INVALID_NOVEL
import io.github.gmathi.novellibrary.util.lang.awaitSingle
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*


class NUSource : ParsedHttpSource() {

    override val baseUrl: String
        get() = "https://${HostNames.NOVEL_UPDATES}"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Updates"

    override val client: OkHttpClient = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/page/$page/?s=${query.replace(" ", "+")}"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val url = element.selectFirst("div.search_title > a")?.attr("abs:href") ?: throw Exception(INVALID_NOVEL)
        val novel = Novel(url)
        element.selectFirst("div.search_title > a")?.text()?.let { novel.name = it }
        novel.imageUrl = element.selectFirst("div.search_img_nu > img[src]")?.attr("abs:src")
        novel.rating = element.select("span.search_ratings").text().trim().replace("(", "").replace(")", "")
        novel.chaptersCount = element.select("div.search_stats.mb span.ss_mb").text().replace("Chapters", "").toLong()
        return novel
    }

    override fun searchNovelsSelector() = "div.search_main_box_nu"
    override fun searchNovelsNextPageSelector() = "div.digg_pagination a.next.page-numbers"

    //Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        document.selectFirst(".seriestitlenu")?.text()?.let { novel.name = it }
        novel.imageUrl = document.selectFirst(".seriesimg > img[src]")?.attr("abs:src")
        novel.longDescription = document.body().selectFirst("#editdescription")?.text()
        novel.rating = document.body().selectFirst("span.uvotes")?.text()?.substring(1, 4)
        novel.genres = document.body().selectFirst("#seriesgenre")?.children()?.map { it.text() }

        document.select(".genre")?.let { elements ->
            elements.select("#authtag")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["Author(s)"] = it }
            elements.select("[gid]")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["Genre(s)"] = it }
            elements.select("#artiststag")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["Artist(s)"] = it }
            elements.select("#etagme")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["Tags"] = it }
            elements.select(".type")?.firstOrNull()?.outerHtml()?.let { novel.metadata["Type"] = it }
            elements.select("a[lid].lang")?.firstOrNull()?.outerHtml()?.let { novel.metadata["Language"] = it }
            elements.select("#myopub")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["Original Publisher"] = it }
            elements.select("#myepub")?.joinToString(", ") { it.outerHtml() }?.let { novel.metadata["English Publisher"] = it }
        }

        document.select("#edityear")?.firstOrNull()?.text()?.let { novel.metadata["Year"] = it }
        document.select("#editstatus")?.firstOrNull()?.text()?.let { novel.metadata["Status in Country of Origin"] = it }
        document.select("#showlicensed")?.firstOrNull()?.text()?.let { novel.metadata["Licensed (in English)"] = it }
        document.select("#showtranslated")?.firstOrNull()?.text()?.let { novel.metadata["Completely Translated"] = it }
        document.select("#editassociated")?.firstOrNull()?.text()?.let { novel.metadata["Associated Names"] = it }
        document.select("#mypostid")?.firstOrNull()?.attr("value")?.let { novel.metadata["PostId"] = it }

        return novel
    }

    override fun chapterListSelector() = "data-id"
    override fun chapterFromElement(element: Element): Chapter {
        val url = "https:" + element.attr("href")
        val name = element.getElementsByAttribute("title").attr("title")
        return Chapter(url, name)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<Chapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).reversed().mapIndexed { index, element ->
            val chapter = chapterFromElement(element)
            chapter.novelId = novel.id
            chapter.orderId = index.toLong()
            chapter
        }
    }

    override fun chapterListRequest(novel: Novel): Request {
        if (!novel.metadata.containsKey("PostId")) throw Exception(INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nd_getchapters")
            .add("mypostid", novelUpdatesNovelId)
            .build()
        return POST(url, body = formBody)
    }

    suspend fun getChapterListWithSources(novel: Novel): List<Chapter> {
        return fetchChapterListWithSources(novel).awaitSingle()
    }

    private fun fetchChapterListWithSources(novel: Novel): Observable<List<Chapter>> {
        return client.newCall(chapterListWithSourcesRequest(novel))
            .asObservableSuccess()
            .map { response ->
                chapterListParse(novel, response)
            }
    }

    private fun chapterListWithSourcesRequest(novel: Novel): Request {
        if (!novel.metadata.containsKey("PostId")) throw Exception(INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "nd_getchapters")
            .add("mypostid", novelUpdatesNovelId)
            .build()
        return POST(url, body = formBody)
    }


    //region stubs
    override fun popularNovelsRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun popularNovelsSelector(): String {
        TODO("Not yet implemented")
    }

    override fun popularNovelsFromElement(element: Element): Novel {
        TODO("Not yet implemented")
    }

    override fun popularNovelNextPageSelector(): String? {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesSelector(): String {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesFromElement(element: Element): Novel {
        TODO("Not yet implemented")
    }

    override fun latestUpdatesNextPageSelector(): String? {
        TODO("Not yet implemented")
    }
//endregion


    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"

        private val LANG_REGEX = "( )?\\((PT-)?BR\\)".toRegex()
        private val IMAGE_REGEX = "_(small|medium|xmedium)\\.".toRegex()

        private val DATE_FORMAT by lazy {
            SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
        }
    }
}