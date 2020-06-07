package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.addPageNumberToUrl

fun NovelApi.searchUrl(url: String, pageNumber: Int): ArrayList<Novel>? {
    try {
        //val host = URI(url).host
        when {
            url.contains(HostNames.ROYAL_ROAD_OLD) || url.contains(HostNames.ROYAL_ROAD) -> return searchRoyalRoadUrl(url)
            url.contains(HostNames.NOVEL_UPDATES) -> return searchNovelUpdatesUrl(url, pageNumber)
            url.contains(HostNames.WLN_UPDATES) -> return searchWlnUpdatesUrl(url)
            url.contains(HostNames.NOVEL_FULL) -> return searchNovelFullUrl(url)
            url.contains(HostNames.LNMTL) -> return searchLNMTLUrl(url, pageNumber)
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
        val document = getDocument(searchUrl.addPageNumberToUrl(pageNumber, "pg"))
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


fun NovelApi.searchWlnUpdatesUrl(url: String): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument(url)
        val elements = document.body().select("td > a[href]")
//        if (url.contains("?json="))
//            document.body().select("td > a[href]")
//        else
//            document.body().select("td > a[href]")

        elements?.mapTo(searchResults) {
            Novel(it.text(), it.attr("abs:href"))
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

fun NovelApi.searchLNMTLUrl(searchUrl: String, pageNumber: Int): ArrayList<Novel>? {
    var searchResults: ArrayList<Novel>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument(searchUrl.addPageNumberToUrl(pageNumber, "page"))
        val elements = document.body().select("div.media") ?: return searchResults
        for (element in elements) {
            val e = element.selectFirst(".media-title") ?: continue
            val novelName = e.text() ?: continue
            val novelUrl = e.selectFirst("a[href]").attr("abs:href") ?: continue
            val novel = Novel(novelName, novelUrl)
            novel.imageUrl = element.selectFirst("img[src]")?.attr("abs:src")
            val re = element.selectFirst("div.progress")
            if (re != null) {
                val negative = re.selectFirst("div.progress-bar-danger")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
                val neutral = re.selectFirst("div.progress-bar-warning")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
                val positive = re.selectFirst("div.progress-bar-success")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
                val total = negative + neutral + positive
                if (total != 0)
                    novel.rating = (positive.toDouble() / total * 5).toFloat().toString()
            }
            searchResults.add(novel)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults
}