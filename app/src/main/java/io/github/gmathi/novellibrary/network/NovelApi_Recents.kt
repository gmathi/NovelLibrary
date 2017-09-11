package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.RecenlytUpdatedItem


fun NovelApi.getRecentlyUpdatedNovels(): ArrayList<RecenlytUpdatedItem>? {
    var searchResults: ArrayList<RecenlytUpdatedItem>? = null
    try {
        searchResults = ArrayList()
        val document = getDocumentWithUserAgent("http://www.novelupdates.com/")
        val elements = document.body()?.getElementsByTag("td")?.filter { it.className().contains("sid") }
        if (elements != null)
            for (element in elements) {
                val item = RecenlytUpdatedItem()
                item.novelUrl = element.getElementsByTag("a")?.firstOrNull()?.attr("href")
                item.novelName = element.getElementsByTag("a")?.firstOrNull()?.attr("title")
                item.chapterName = element.getElementsByTag("a")?.firstOrNull { it.className() == "chp-release" }?.text()
                item.publisherName = element.getElementsByTag("span")?.firstOrNull { it.className() == "mob_group" }?.getElementsByTag("a")?.firstOrNull()?.attr("title")

                searchResults.add(item)
            }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults

}