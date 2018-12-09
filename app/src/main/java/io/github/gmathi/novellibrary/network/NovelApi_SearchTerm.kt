package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import java.io.IOException
import java.lang.Exception


fun NovelApi.searchRoyalRoad(searchTerms: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://royalroadl.com/fictions/search?keyword=$searchTerms&name=&author=&minPages=0&maxPages=10000&minRating=0&maxRating=5&status=ALL&orderBy=popularity&dir=desc&type=ALL")
        val elements = document.body().getElementsByClass("search-item").filter { it.tagName() == "li" }
        for (element in elements) {
            val searchContentElement = element.getElementsByClass("search-content").firstOrNull()
            if (searchContentElement != null) {
                val urlElement = searchContentElement.getElementsByTag("a")?.firstOrNull()
                val novel = Novel(urlElement?.text()!!, "https://www.royalroadl.com${urlElement.attr("href")}")
                novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
                novel.metaData["Author(s)"] = searchContentElement.getElementsByClass("author")?.firstOrNull { it.tagName() == "span" }?.text()?.substring(3)
                novel.rating = "N/A"
                novel.longDescription = searchContentElement.getElementsByTag("div")?.firstOrNull { it.hasClass("fiction-description") }?.text()
                novel.shortDescription = novel.longDescription?.split("\n")?.firstOrNull()
                searchResults.add(novel)
            }
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdates(searchTerms: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent("http://www.novelupdates.com/?s=$searchTerms")
        val titleElements = document.body().getElementsByClass("w-blog-entry-title").filter { it.tagName() == "h2" }
        val dataElements = document.body().getElementsByClass("w-blog-entry").filter { it.tagName() == "div" }

        var i = 0
        while (i < dataElements.size) {

            val novel = Novel(titleElements[i].getElementsByTag("span").firstOrNull { it.hasClass("w-blog-entry-title-h") }?.text()!!, titleElements[i].getElementsByTag("a").firstOrNull()?.attr("href")!!)
            novel.imageUrl = dataElements[i].getElementsByTag("img").firstOrNull()?.attr("src")
            novel.rating = dataElements[i].getElementsByTag("span").firstOrNull { it.hasClass("userrate") }?.text()?.replace("Rating: ", "")?.trim()
            novel.genres = dataElements[i].getElementsByTag("span").firstOrNull { it.className() == "s-genre" }?.children()?.map { it.text() }
            novel.shortDescription = dataElements[i].getElementsByTag("div").firstOrNull { it.className() == "w-blog-entry-short" }?.textNodes()?.get(0)?.text()
            novel.longDescription = dataElements[i].getElementsByTag("span").firstOrNull { it.attr("style") == "display:none" }?.textNodes()?.map { it.text() }?.joinToString(separator = "\n") { it }
            searchResults.add(novel)

            i++
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
        val document = getDocument("https://www.wlnupdates.com/search?title=" + searchTerms)
        val elements = document.body().getElementsByTag("td")
        elements.mapTo(searchResults) { Novel(it.getElementsByTag("a").firstOrNull()?.text()!!, it.getElementsByTag("a").firstOrNull()?.absUrl("href")!!) }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}
