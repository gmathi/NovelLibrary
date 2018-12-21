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
            url.contains(HostNames.NOVEL_FULL) -> return searchNovelFullUrl(url)
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
        val elements = document.body().getElementsByClass("fiction-list-item").filter { it.tagName() == "div" }
        for (element in elements) {
            val urlElement = element.getElementsByTag("a")?.firstOrNull()
            val novel = Novel(urlElement?.text()!!, "https://www.royalroadl.com${urlElement.attr("href")}")
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
        val document = getDocumentWithUserAgent(searchUrl)
        val elements = document.body().getElementsByClass("bdrank").filter { it.tagName() == "tr" }
        for (element in elements) {
            val novel = Novel(element.getElementsByTag("img").firstOrNull()?.attr("alt")!!, element.getElementsByTag("a").firstOrNull()?.attr("href")!!)
            novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
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
        elements.mapTo(searchResults) { Novel(it.getElementsByTag("a").firstOrNull()?.text()!!, it.getElementsByTag("a").firstOrNull()?.absUrl("href")!!) }

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelFullUrl(url: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument(url)
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

