package io.github.gmathi.novellibrary.source

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder


class RoyalRoadSource : ParsedHttpSource() {

    override val id: Long
        get() = Constants.SourceId.ROYAL_ROAD
    override val baseUrl: String
        get() = "https://www.${HostNames.ROYAL_ROAD}"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Royal Road"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/fictions/search?title=${encodedQuery.replace(" ", "+")}&page=$page"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val titleElement = element.selectFirst(".fiction-title > a[href]")
        val novel = Novel(titleElement.text(), titleElement.attr("abs:href"), id)
        novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
        novel.metadata["Author(s)"] = element.selectFirst("span.author")?.text()?.substring(3)
        if (novel.metadata["Author(s)"] == null && novel.imageUrl?.startsWith("https://www.royalroadcdn.com/") == true)
            novel.metadata["Author(s)"] = novel.imageUrl?.substring(
                29, novel.imageUrl?.indexOf('/', 29)
                    ?: 0
            )
        novel.rating = element.selectFirst("span.star[title]").attr("title")
        novel.longDescription = element.selectFirst("div.fiction-description")?.text()
            ?: element.selectFirst("div.margin-top-10.col-xs-12")?.text()
        novel.shortDescription = novel.longDescription?.split("\n")?.firstOrNull()
        return novel
    }

    override fun searchNovelsSelector() = "div.fiction-list > div"
    override fun searchNovelsNextPageSelector() = "a:contains(Last)"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {

        novel.imageUrl = document.head().selectFirst("meta[property=og:image]")?.attr("content")
        novel.rating = document.head().selectFirst("meta[property=books:rating:value]")?.attr("content")
        novel.longDescription = document.body().selectFirst("div[property=description]")?.text()
        novel.genres = document.body().select("[property=genre]")?.map { it.text() }

        novel.metadata["Author(s)"] = document.head().selectFirst("meta[property=books:author]")?.attr("content")

        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListSelector() = "table#chapters a[href]"
    override fun chapterFromElement(element: Element) = WebPage(element.absUrl("href"), element.text())

    override fun chapterListRequest(novel: Novel): Request {
        return GET(novel.url, headers = headers)
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