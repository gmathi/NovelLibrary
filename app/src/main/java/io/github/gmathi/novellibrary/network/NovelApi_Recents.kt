package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.RecenlytUpdatedItem


fun NovelApi.getRecentlyUpdatedNovels(): ArrayList<RecenlytUpdatedItem>? {
    var searchResults: ArrayList<RecenlytUpdatedItem>? = null
    try {
        searchResults = ArrayList()
        val document = getDocument("https://www.novelupdates.com/")
        val elements = document.body()?.getElementsByTag("td")?.filter { it.className().contains("sid") }
        if (elements != null)
            for (element in elements) {
                val item = RecenlytUpdatedItem()
                item.novelUrl = element.selectFirst("a[href]")?.attr("abs:href")
                item.novelName = element.selectFirst("a[title]")?.attr("title")
                item.chapterName = element.selectFirst("a.chp-release")?.text()
                item.publisherName = element.selectFirst("span.mob_group > a")?.attr("title")

                searchResults.add(item)
            }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return searchResults

}