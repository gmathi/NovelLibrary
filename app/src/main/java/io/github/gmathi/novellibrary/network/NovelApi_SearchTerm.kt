package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import io.github.gmathi.novellibrary.extensions.asJsonNullFreeString
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.NovelApi.getDocument
import io.github.gmathi.novellibrary.util.Constants.NEOVEL_API_URL
import io.github.gmathi.novellibrary.util.Constants.WLN_UPDATES_API_URL
import io.github.gmathi.novellibrary.util.encodeBase64ToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*
import kotlin.collections.ArrayList


fun NovelApi.searchRoyalRoad(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://www.royalroad.com/fictions/search?title=${searchTerms.replace(" ", "+")}&page=$pageNumber")
        val elements = document.body().select("div.fiction-list > div") ?: return searchResults
        for (element in elements) {
            val urlElement = element.selectFirst("a[href]") ?: continue
            val novel = Novel(urlElement.text(), urlElement.attr("abs:href"))
            novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
            novel.metaData["Author(s)"] = element.selectFirst("span.author")?.text()?.substring(3)
            if (novel.metaData["Author(s)"] == null && novel.imageUrl?.startsWith("https://www.royalroadcdn.com/") == true)
                novel.metaData["Author(s)"] = novel.imageUrl?.substring(29, novel.imageUrl?.indexOf('/', 29) ?: 0)
            novel.rating = element.selectFirst("span[title]").attr("title")
            novel.longDescription = element.selectFirst("div.fiction-description")?.text()
                ?: element.selectFirst("div.margin-top-10.col-xs-12")?.text()
            novel.shortDescription = novel.longDescription?.split("\n")?.firstOrNull()
            searchResults.add(novel)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdates(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://www.novelupdates.com/page/$pageNumber/?s=${searchTerms.replace(" ", "+")}")
        val elements = document.body().select("div.search_main_box_nu") ?: return searchResults
        for (element in elements) {
            val novelName = element.selectFirst("div.search_title > a")?.text() ?: continue
            val novelUrl = element.selectFirst("div.search_title > a")?.attr("abs:href") ?: continue
            val novel = Novel(novelName, novelUrl)
            novel.imageUrl = element.selectFirst("div.search_img_nu > img[src]")?.attr("abs:src")
            novel.rating = element.select("span.search_ratings").text().trim().replace("(", "").replace(")", "")
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}


fun NovelApi.searchWlnUpdates(searchTerms: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()

        val json = """ {
            "title": "$searchTerms",
            "mode": "search-title"
            } """

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(WLN_UPDATES_API_URL)
            .post(body)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonString = response.body?.string() ?: return searchResults
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        val dataObject = jsonObject["data"].asJsonObject
        val resultsArray = dataObject["results"].asJsonArray
        resultsArray.forEach { result ->
            val resultObject = result.asJsonObject
            val allMatchesArray = resultObject["match"].asJsonArray
            val sid = resultObject["sid"].asInt
            val novelUrl = "https://www.${HostNames.WLN_UPDATES}/series-id/$sid/"
            allMatchesArray.forEach { match ->
                val matchArray = match.asJsonArray
                val novelName = matchArray[1].asString
                searchResults.add(Novel(novelName, novelUrl))
            }
        }

//        val document = getDocument("https://www.wlnupdates.com/search?title=${searchTerms.replace(" ", "+")}")
//        val elements = document.body().select("td") ?: return searchResults
//        elements.mapTo(searchResults) {
//            Novel(it.select("a[href]").text(), it.select("a[href]").attr("abs:href"))
//        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}


fun NovelApi.searchNovelFull(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("http://novelfull.com/search?keyword=${searchTerms.replace(" ", "+")}&page=$pageNumber")
        val listElement = document.body().select("div.list.list-truyen")[0]
        val novelElements = listElement.select("div.row")
        novelElements.forEach {
            val novel = Novel(it.select("h3.truyen-title").text(), it.select("h3.truyen-title").select("a[href]").attr("abs:href"))
            novel.imageUrl = it.select("img.cover").attr("abs:src")
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchScribbleHub(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://www.scribblehub.com/?s=${searchTerms.replace(" ", "+")}&post_type=fictionposts&paged=$pageNumber")
        val novelElements = document.body().select("div.search_main_box")
        novelElements.forEach {
            val urlElement = it.select("div.search_title > a[href]")
            val novel = Novel(urlElement.text(), urlElement.attr("abs:href"))
            novel.imageUrl = it.select("div.search_img > img[src]").attr("abs:src")
            novel.rating = it.select("div.search_img").select("span.search_ratings").text().replace("(", "").replace(")", "")
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
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

private var novelsLNMTL: ArrayList<Novel>? = null

@Synchronized
private fun getNovelsLNMTL() {
    if (novelsLNMTL != null)
        return

    val document = getDocument("https://lnmtl.com/")
    val scripts = document.select("script[type]") ?: return
    novelsLNMTL = ArrayList()
    val script = scripts.last() ?: return
    val text = script.html() ?: return

    // script will be in a format:
    // "{some javascript} local: [ {json} ] {some javascript}"
    // we need to extract the pure json
    // to do so, take the substring between "local: [" and "]"
    val json = text.substring(text.indexOf("local:") + 7)
        .substringBefore(']') + ']'

    val novels: List<LNMTLNovelJson> = Gson().fromJson(json, Array<LNMTLNovelJson>::class.java).toList()
    for (novelLNMTL in novels) {
        val novel = Novel(novelLNMTL.name, novelLNMTL.url)
        novel.imageUrl = novelLNMTL.image
        novelsLNMTL?.add(novel)
    }
}

fun NovelApi.searchLNMTL(searchTerms: String): ArrayList<Novel>? {
    if (novelsLNMTL == null)
        getNovelsLNMTL()

    return if (novelsLNMTL != null)
        ArrayList(novelsLNMTL!!.filter { it.name.contains(searchTerms, true) })
    else
        ArrayList()
}

fun NovelApi.searchNeovel(searchTerms: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()

        val request = Request.Builder()
            .url(NEOVEL_API_URL+"V2/books/search?language=ALL&filter=0&name=${searchTerms.encodeBase64ToString()}&sort=6&page=0&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000")
            .get()
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonString = response.body?.string() ?: return searchResults
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray

        jsonArray.forEach { result ->
            val resultObject = result.asJsonObject
            val id = resultObject["id"].asInt.toString()
            val novelUrl = "https://${HostNames.NEOVEL}/V1/book/details?bookId=$id&language=EN"
            val novel = resultObject["name"].asJsonNullFreeString?.let { Novel(it, novelUrl) } ?: return@forEach
            novel.imageUrl = "https://${HostNames.NEOVEL}/V2/book/image?bookId=$id&oldApp=false"
            novel.metaData["id"] = id
            novel.rating = resultObject["rating"].asFloat.toString()
            searchResults.add(novel)
        }

//        val document = getDocument("https://www.wlnupdates.com/search?title=${searchTerms.replace(" ", "+")}")
//        val elements = document.body().select("td") ?: return searchResults
//        elements.mapTo(searchResults) {
//            Novel(it.select("a[href]").text(), it.select("a[href]").attr("abs:href"))
//        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

