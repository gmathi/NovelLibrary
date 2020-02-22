package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithUserAgent


fun NovelApi.searchRoyalRoad(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent("https://www.royalroad.com/fictions/search?title=${searchTerms.replace(" ", "+")}&page=$pageNumber")
        val elements = document.body().select("div.fiction-list-item") ?: return searchResults
        for (element in elements) {
            val urlElement = element.selectFirst("a[href]") ?: continue
            val novel = Novel(urlElement.text(), urlElement.attr("abs:href"))
            novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
            if (novel.imageUrl?.startsWith("https://www.royalroadcdn.com/") == true)
                novel.metaData["Author(s)"] = novel.imageUrl?.substring(29, novel.imageUrl?.indexOf('/', 29) ?: 0)
            novel.rating = element.selectFirst("span.star")?.attr("title")
            novel.longDescription = element.selectFirst("div.margin-top-10.col-xs-12")?.text()
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
        val document = getDocumentWithUserAgent("https://www.novelupdates.com/page/$pageNumber/?s=${searchTerms.replace(" ", "+")}")
        val titleElements = document.body().select("h2.w-blog-entry-title") ?: return searchResults
        val dataElements = document.body().select("div.w-blog-entry") ?: return searchResults

        (0 until dataElements.size).forEach { i ->

            val novelName = titleElements[i].selectFirst("span.w-blog-entry-title-h")?.text()
                    ?: return@forEach
            val novelUrl = titleElements[i].selectFirst("a[href]")?.attr("abs:href")
                    ?: return@forEach
            val novel = Novel(novelName, novelUrl)

            novel.imageUrl = dataElements[i].selectFirst("img[src]")?.attr("abs:src")
            novel.rating = dataElements[i].selectFirst("span.userrate")?.text()?.replace("Rating: ", "")?.trim()
            novel.genres = dataElements[i].selectFirst("span.s-genre")?.children()?.map { it.text() }
            novel.shortDescription = dataElements[i].selectFirst("div.w-blog-entry-short")?.textNodes()?.get(0)?.text()
            novel.longDescription = dataElements[i].selectFirst("span[style=display:none]")?.textNodes()?.map { it.text() }?.joinToString(separator = "\n") { it }
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdates_New(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent("https://www.novelupdates.com/page/$pageNumber/?s=${searchTerms.replace(" ", "+")}")
        val elements = document.body().select("div.search_main_box_nu") ?: return searchResults
        for (element in elements) {
            val novelName = element.selectFirst("div.search_title > a")?.text() ?: continue
            val novelUrl = element.selectFirst("div.search_title > a")?.attr("abs:href") ?: continue
            val novel = Novel(novelName, novelUrl)
            novel.imageUrl = element.selectFirst("div.search_img_nu > img[src]")?.attr("abs:src")

//            val ratingsElement = element.selectFirst("div.search_ratings") ?: continue
//            ratingsElement.children().remove()
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
        val document = getDocumentWithUserAgent("https://www.wlnupdates.com/search?title=${searchTerms.replace(" ", "+")}")
        val elements = document.body().select("td") ?: return searchResults
        elements.mapTo(searchResults) {
            Novel(it.select("a[href]").text(), it.select("a[href]").attr("abs:href"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}


fun NovelApi.searchNovelFull(searchTerms: String, pageNumber: Int = 1): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent("http://novelfull.com/search?keyword=${searchTerms.replace(" ", "+")}&page=$pageNumber")
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
        val document = getDocumentWithUserAgent("https://www.scribblehub.com/?s=${searchTerms.replace(" ", "+")}&post_type=fictionposts&paged=$pageNumber")
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
private class LNMTLNovelJson(@SerializedName("id") val id: Int,
                             @SerializedName("name") val name: String,
                             @SerializedName("slug") val slug: String,
                             @SerializedName("name_acronym") val name_acronym: String,
                             @SerializedName("name_original") val name_original: String,
                             @SerializedName("name_spelling") val name_spelling: String,
                             @SerializedName("name_spelling_clean") val name_spelling_clean: String,
                             @SerializedName("image") val image: String,
                             @SerializedName("url") val url: String)

private var novelsLNMTL: ArrayList<Novel>? = null

@Synchronized
private fun getNovelsLNMTL() {
    if (novelsLNMTL != null)
        return

    val document = getDocumentWithUserAgent("https://lnmtl.com/")
    val scripts = document.select("script[type]") ?: return
    novelsLNMTL = ArrayList()
    val script = scripts.last() ?: return
    val text = script.html() ?: return

    // script will be in a format:
    // "{some javascript} local: [ {json} ] {some javascript}"
    // we need to extract the pure json
    // to do so, take the substring between "local: [" and "]"
    val left = text.substringBefore("local:")
    val json = text.substring(left.length + 7)
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

