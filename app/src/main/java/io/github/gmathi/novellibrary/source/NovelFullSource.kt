package io.github.gmathi.novellibrary.source

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_EXTERNAL_ID
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder


class NovelFullSource : ParsedHttpSource() {

    override val id: Long
        get() = Constants.SourceId.NOVEL_FULL
    override val baseUrl: String
        get() = "https://${HostNames.NOVEL_FULL}"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Novel Full"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/search?keyword=${encodedQuery.replace(" ", "+")}&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val document = response.asJsoup()

        val novels = document.select(searchNovelsSelector()).map { element ->
            searchNovelsFromElement(element)
        }

        val hasNextPage = searchNovelsNextPageSelector().let { selector ->
            document.select(selector).first()
        } == null

        return NovelsPage(novels, hasNextPage)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val novel = Novel(element.select("h3.truyen-title").text(), element.select("h3.truyen-title").select("a[href]").attr("abs:href"), this.id)
        novel.imageUrl = element.select("img.cover").attr("abs:src")
        return novel
    }

    override fun searchNovelsSelector() = "div.list.list-truyen div.row"
    override fun searchNovelsNextPageSelector() = "li.next.disabled"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val booksElement = document.body().select("div.books")
        val infoElements = document.body().select("div.info").first().children()

        novel.imageUrl = booksElement.select("div.book > img").attr("abs:src")
        novel.genres = infoElements[1].select("a").map { it.text() }
        novel.longDescription = document.body().select("div.desc-text > p").joinToString(separator = "\n") { it.text() }
        novel.rating = (document.body().select("div.small > em > strong > span").first().text().toDouble() / 2).toString()
        novel.externalNovelId = document.selectFirst("#rating")?.attr("data-novel-id")


        novel.metadata["Author(s)"] = infoElements[0].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metadata["Genre(s)"] = infoElements[1].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metadata["Source"] = infoElements[2].text()
        novel.metadata["Status"] = infoElements[3].text()

        return novel
    }
    //endregion

    //region Chapters


    override fun chapterListSelector() = "select.chapter_jump option"
    override fun chapterFromElement(element: Element): WebPage {
        val url = "https://${HostNames.NOVEL_FULL}${element.attr("value")}"
        val name = element.text()
        return WebPage(url, name)
    }

    override fun chapterListRequest(novel: Novel): Request {
        val id = novel.externalNovelId ?: throw Exception(MISSING_EXTERNAL_ID)
        val url = "$baseUrl/ajax-chapter-option?novelId=$id&currentChapterId="
        return GET(url, headers)
    }

    //endregion

    //region stubs

    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesSelector(): String = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesFromElement(element: Element): Novel = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsSelector(): String = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsFromElement(element: Element): Novel = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelNextPageSelector(): String = throw Exception(MISSING_IMPLEMENTATION)

//endregion


    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}