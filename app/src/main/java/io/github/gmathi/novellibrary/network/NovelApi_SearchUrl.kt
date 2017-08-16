package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import java.io.IOException


fun NovelApi.searchUrl(url: String): ArrayList<Novel>? {
    try {
        //val host = URI(url).host
        when {
            url.contains(HostNames.ROYAL_ROAD) -> return searchRoyalRoadUrl(url)
            url.contains(HostNames.NOVEL_UPDATES) -> return searchNovelUpdatesUrl(url)
            url.contains(HostNames.WLN_UPDATES) -> return searchWlnUpdatesUrl(url)
        }
    } catch (e: Exception) {
        //if url is malformed
        e.printStackTrace()
    }
    return null
}

fun NovelApi.searchRoyalRoadUrl(searchUrl: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument(searchUrl)
        val elements = document.body().getElementsByClass("fiction-list-item").filter { it.tagName() === "div" }
        for (element in elements) {
            val novel = Novel()
            val urlElement = element.getElementsByTag("a")?.firstOrNull()
            novel.name = urlElement?.text()
            novel.url = "https://www.royalroadl.com${urlElement?.attr("href")}"
            novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
            novel.rating = element.getElementsByClass("star").firstOrNull { it.tagName() == "span" }?.attr("title")
            searchResults.add(novel)
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdatesUrl(searchUrl: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument(searchUrl)
        val elements = document.body().getElementsByClass("bdrank").filter { it.tagName() === "tr" }
        for (element in elements) {
            val novel = Novel()
            novel.url = element.getElementsByTag("a").firstOrNull()?.attr("href")
            novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
            novel.name = element.getElementsByTag("img").firstOrNull()?.attr("alt")
            novel.rating = element.getElementsByTag("div").firstOrNull { it.hasAttr("title") && it.attr("title").contains("Rating: ") }?.attr("title")?.replace("Rating: ", "")?.trim()
            searchResults.add(novel)
        }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchWlnUpdatesUrl(url: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent(url)
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
