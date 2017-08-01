package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import java.io.IOException


fun NovelApi.searchRoyalRoad(searchTerms: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://royalroadl.com/fictions/search?keyword=$searchTerms&name=&author=&minPages=0&maxPages=10000&minRating=0&maxRating=5&status=ALL&orderBy=popularity&dir=desc&type=ALL")
        val elements = document.body().getElementsByClass("search-item").filter { it.tagName() === "li" }
        for (element in elements) {
            val searchContentElement = element.getElementsByClass("search-content").firstOrNull()
            if (searchContentElement != null) {
                val novel = Novel()
                val urlElement = searchContentElement.getElementsByTag("a")?.firstOrNull()
                novel.name = urlElement?.text()
                novel.url = "https://www.royalroadl.com${urlElement?.attr("href")}"
                novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
                novel.metaData.put("Author(s)", searchContentElement.getElementsByClass("author")?.firstOrNull { it.tagName() == "span" }?.text()?.substring(3))
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
        val document = getDocument("http://www.novelupdates.com/?s=" + searchTerms)
        val elements = document.body().getElementsByClass("w-blog-entry-h").filter { it.tagName() === "div" }
        for (element in elements) {
            val novel = Novel()
            novel.url = element.getElementsByTag("a").firstOrNull()?.attr("href")
            novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
            novel.name = element.getElementsByTag("span").firstOrNull { it.hasClass("w-blog-entry-title-h") }?.text()
            novel.rating = element.getElementsByTag("span").firstOrNull { it.hasClass("userrate") }?.text()?.replace("Rating: ", "")?.trim()
            novel.genres = element.getElementsByTag("span").firstOrNull { it.className() == "s-genre" }?.children()?.map { it.text() }
            novel.shortDescription = element.getElementsByTag("div").firstOrNull { it.className() == "w-blog-entry-short" }?.textNodes()?.get(0)?.text()
            novel.longDescription = element.getElementsByTag("span").firstOrNull { it.attr("style") == "display:none" }?.textNodes()?.map { it.text() }?.joinToString(separator = "\n") { it }
            searchResults.add(novel)
        }

    } catch (e: IOException) {
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
        for (element in elements) {
            val novel = Novel()
            novel.url = element.getElementsByTag("a").firstOrNull()?.absUrl("href")
            novel.name = element.getElementsByTag("a").firstOrNull()?.text()
            searchResults.add(novel)
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}
