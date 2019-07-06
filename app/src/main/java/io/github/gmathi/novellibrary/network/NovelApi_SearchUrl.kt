package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.addPageNumberToUrl

fun NovelApi.searchUrl(url: String, pageNumber: Int): ArrayList<Novel>? {
    try {
        //val host = URI(url).host
        when {
            url.contains(HostNames.ROYAL_ROAD_OLD) || url.contains(HostNames.ROYAL_ROAD) -> return searchRoyalRoadUrl(url)
            url.contains(HostNames.NOVEL_UPDATES) -> return searchNovelUpdatesUrl_New(url, pageNumber)
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
        val document = getDocumentWithUserAgent(searchUrl)
        val elements = document.body().select("div.fiction-list-item") ?: return searchResults
        for (element in elements) {
            val urlElement = element.selectFirst("a") ?: continue
            val novel = Novel(urlElement.text(), urlElement.attr("abs:href"))
            novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
            novel.rating = element.selectFirst("span.star")?.attr("title")
            searchResults.add(novel)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdatesUrl(searchUrl: String, pageNumber: Int): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent(searchUrl.addPageNumberToUrl(pageNumber, "pg"))
        val elements = document.body().select("tr.bdrank") ?: return searchResults
        for (element in elements) {
            val novelName = element.selectFirst("img[alt]")?.attr("alt") ?: continue
            val novelUrl = element.selectFirst("a[href]")?.attr("abs:href") ?: continue
            val novel = Novel(novelName, novelUrl)
            novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
            novel.rating = element.selectFirst("div[title*=Rating: ]")?.attr("title")?.replace("Rating: ", "")?.trim()
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelUpdatesUrl_New(searchUrl: String, pageNumber: Int): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent(searchUrl.addPageNumberToUrl(pageNumber, "pg"))
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


fun NovelApi.searchWlnUpdatesUrl(url: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent(url)
        val elements = document.body().select("td") ?: return searchResults
        elements.mapTo(searchResults) {
            Novel(it.select("a[href]").text(), it.selectFirst("a[href]").attr("abs:href"))
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}

fun NovelApi.searchNovelFullUrl(url: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent(url)
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

