package io.github.gmathi.novellibrary.source

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.ParsedHttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Exceptions
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder


class ScribbleHubSource : ParsedHttpSource() {

    override val id: Long
        get() = Constants.SourceId.SCRIBBLE_HUB
    override val baseUrl: String
        get() = "https://www.${HostNames.SCRIBBLE_HUB}"
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Scribble Hub"

    override val client: OkHttpClient
        get() = network.cloudflareClient

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/?s=${encodedQuery.replace(" ", "+")}&post_type=fictionposts&paged=$page"
        return GET(url, headers)
    }

    override fun searchNovelsFromElement(element: Element): Novel {
        val urlElement = element.select("div.search_title > a[href]")
        val novel = Novel(urlElement.text(), urlElement.attr("abs:href"), id)
        novel.imageUrl = element.select("div.search_img > img[src]").attr("abs:src")
        novel.rating = element.select("div.search_img").select("div.search_ratings").text().replace("(", "").replace(")", "")
        return novel
    }

    override fun searchNovelsSelector() = "div.search_main_box"
    override fun searchNovelsNextPageSelector() = "a.page-link.next"
    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, document: Document): Novel {
        val pageElement = document.body().select("div#page")

        novel.imageUrl = pageElement.select("div.fic_image > img").attr("abs:src")
        novel.metadata["Author(s)"] = pageElement.select("span[property='author'] a").outerHtml()


        val genresElements = pageElement.select("span.wi_fic_genre a.fic_genre")
        novel.genres = genresElements.map { it.text() }
        novel.metadata["Genre(s)"] = genresElements.joinToString(", ") { it.outerHtml() }

        novel.longDescription = pageElement.select("div.wi_fic_desc").text()
        novel.rating = pageElement.select("meta[property='ratingValue']").attr("content")

        novel.metadata["Tags"] = pageElement.select("#etagme")?.joinToString(", ") { it.outerHtml() }
        novel.externalNovelId = document.getElementById("mypostid").attr("value")
        novel.metadata["PostId"] = novel.externalNovelId
        novel.chaptersCount = document.getElementById("chpcounter").attr("value").toLong()

        return novel
    }
    //endregion

    //region Chapters


    override fun chapterListSelector() = "a[href]"
    override fun chapterFromElement(element: Element) = WebPage(element.attr("abs:href"), element.attr("title"))

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).reversed().mapIndexed { index, element ->
            val chapter = chapterFromElement(element)
            chapter.novelId = novel.id
            chapter.orderId = index.toLong()
            chapter
        }
    }

    override fun chapterListRequest(novel: Novel): Request {
        val scribbleNovelId = novel.externalNovelId ?: novel.metadata["PostId"] ?: throw Exception(Exceptions.INVALID_NOVEL)
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formBody: RequestBody = FormBody.Builder()
            .add("action", "wi_gettocchp")
            .add("strSID", scribbleNovelId)
            .add("strmypostid", "0")
            .add("strFic", "yes")
            .build()
        return POST(url, body = formBody)
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