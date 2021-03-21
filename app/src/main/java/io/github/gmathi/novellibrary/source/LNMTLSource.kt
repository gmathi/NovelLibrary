package io.github.gmathi.novellibrary.source

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.LNMTL_BASE_URL
import io.github.gmathi.novellibrary.util.Exceptions
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.network.asJsoup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.net.URLEncoder


class LNMTLSource : HttpSource() {

    override val id: Long
        get() = Constants.SourceId.LNMTL
    override val baseUrl: String
        get() = LNMTL_BASE_URL
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "LNMTL"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel

    override fun fetchSearchNovels(page: Int, query: String, filters: FilterList): Observable<NovelsPage> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchResults = novelsLNMTL?.filter { it.name.contains(encodedQuery, true) } ?: ArrayList()
        val novelsPage = NovelsPage(searchResults, hasNextPage = false)
        return Observable.just(novelsPage)
    }

    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, response: Response): Novel {
        val doc = response.asJsoup()
        val novelElement = doc.selectFirst(".novel .media")
        novel.imageUrl = novelElement.selectFirst("img[src]")?.attr("abs:src")
        novel.longDescription = novelElement.selectFirst(".description")?.text()

        val negative = novelElement.selectFirst("div.progress-bar-danger")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val neutral = novelElement.selectFirst("div.progress-bar-warning")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val positive = novelElement.selectFirst("div.progress-bar-success")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val total = (negative + neutral + positive).toFloat()
        if (total != 0f)
            novel.rating = (positive / total * 5).toString()

        val detailsElement = doc.selectFirst("div.container .row > div:last-child")

        val authors = detailsElement?.selectFirst("dt:contains(Authors)")?.nextElementSibling()?.select("span")
        novel.metadata["Author(s)"] = authors?.joinToString(", ") { it.text() }

        val genres = detailsElement?.selectFirst("div.panel-heading:contains(Genres)")?.nextElementSibling()?.select("ul li")
        novel.genres = genres?.map { it.text() }
        novel.metadata["Genre(s)"] = genres?.joinToString(", ") { it.html() }

        val tags = detailsElement?.selectFirst("div.panel-heading:contains(Tags)")?.nextElementSibling()?.select("ul li")
        novel.metadata["Tags"] = tags?.joinToString(", ") { it.html() }

        return novel
    }
    //endregion

    //region Chapters
    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val chapters = ArrayList<WebPage>()
        val doc = response.asJsoup()
        val scripts = doc.select("script")
        val script = scripts.find { it.html().contains("lnmtl.firstResponse =") } ?: throw Exception(Exceptions.PARSING_ERROR)
        val text = script.html().split(";")

        // It can be hardcoded to be `https://lnmtl.com/chapter`, but I decided to parse it just in case.
        val route = text.find { it.startsWith("lnmtl.route =") }?.trim()?.substring(15)?.substringBeforeLast('\'') ?: "https://lnmtl.com/chapter"
        val volumeJson = text.find { it.startsWith("lnmtl.volumes =") }?.substring(15)

        val type = object : TypeToken<Map<String, Any>>() {}.type
        val volumeType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val volumeGson: List<LinkedTreeMap<String, Any>> = Gson().fromJson(volumeJson, volumeType) ?: throw Exception(Exceptions.PARSING_ERROR)

        // Potential optimization is to skip fetching first page of first volume,
        // but that would increase code complexity a lot
        var orderId = 0L
        for (volume in volumeGson) {
            val id = (volume["id"] as Double).toInt()
            var page = 1
            do {
                val request = GET("${route}?page=${page++}&volumeId=${id}")
                val pageResponse = client.newCall(request).execute().body?.string() ?: "{}"
                val pageGson: LinkedTreeMap<String, Any> = Gson().fromJson(pageResponse, type) ?: break

                @Suppress("UNCHECKED_CAST")
                val data = pageGson["data"] as List<LinkedTreeMap<String, Any>>

                for (chapter in data) {
                    val url = chapter["site_url"]
                    val title = "c${chapter["position"] ?: (orderId + 1)}${if (chapter["part"] != null) "p${chapter["part"]}" else ""} ${chapter["title"] ?: ""}"
                    if (url !is String) continue
                    val webPage = WebPage(url, title)
                    webPage.orderId = orderId++
                    webPage.novelId = novel.id
                    chapters.add(webPage)
                }

            } while (pageGson["total"] != pageGson["to"])
        }
        return chapters
    }
    //endregion


    //region Other Helper Methods

    @Synchronized
    private fun getNovelsLNMTL() {
        if (novelsLNMTL != null)
            return

        val request = GET(baseUrl)
        val response = client.newCall(request).execute()
        val document = response.asJsoup()

        //Check for the novels list script tag
        val scripts = document.select("script[type]") ?: return
        val script = scripts.last() ?: return
        val text = script.html() ?: return

        // script will be in a format:
        // "{some javascript} local: [ {json} ] {some javascript}"
        // we need to extract the pure json
        // to do so, take the substring between "local: [" and "]"
        val json = text.substring(text.indexOf("local:") + 7)
            .substringBefore(']') + ']'

        novelsLNMTL = ArrayList()
        val novels: List<LNMTLNovelJson> = Gson().fromJson(json, Array<LNMTLNovelJson>::class.java).toList()
        for (novelLNMTL in novels) {
            val novel = Novel(novelLNMTL.name, novelLNMTL.url, id)
            novel.imageUrl = novelLNMTL.image
            novelsLNMTL?.add(novel)
        }
    }

    //endregion

    //region stubs
    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsParse(response: Response): NovelsPage = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesParse(response: Response): NovelsPage = throw Exception(MISSING_IMPLEMENTATION)
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun searchNovelsParse(response: Response): NovelsPage = throw Exception(MISSING_IMPLEMENTATION)
    //endregion

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"

        // Below is to cache Neovel Genres & Tags
        private var novelsLNMTL: ArrayList<Novel>? = null
    }

    init {
        getNovelsLNMTL()
    }

    @Suppress("unused")
    private class LNMTLNovelJson(
        @SerializedName("id") val id: Int,
        @SerializedName("name") val name: String,
        @SerializedName("slug") val slug: String,
        @SerializedName("name_acronym") val name_acronym: String,
        @SerializedName("name_original") val name_original: String,
        @SerializedName("name_spelling") val name_spelling: String,
        @SerializedName("name_spelling_clean") val name_spelling_clean: String,
        @SerializedName("image") val image: String,
        @SerializedName("url") val url: String
    )
}