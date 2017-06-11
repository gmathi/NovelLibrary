package com.mgn.bingenovelreader.network

import com.mgn.bingenovelreader.models.Novel
import org.jsoup.Jsoup
import java.io.IOException

class NovelApi {

    fun search(searchTerms: String): Map<String, ArrayList<Novel>> {
        val searchResults = HashMap<String, ArrayList<Novel>>()
        searchNovelUpdates(searchTerms)?.let { searchResults.put("novel-updates", it) }
        return searchResults
    }

    private fun searchNovelUpdates(searchTerms: String): ArrayList<Novel>? {
        var searchResults: ArrayList<Novel>? = null
        try {
            searchResults = ArrayList()
            val document = Jsoup.connect("http://www.novelupdates.com/?s=" + searchTerms).get()
            val elements = document.body().getElementsByClass("w-blog-entry-h").filter { it.tagName() === "div" }
            for (element in elements) {
                val novel = Novel()
                novel.url = element.getElementsByTag("a").firstOrNull()?.attr("href")
                novel.imageUrl = element.getElementsByTag("img").firstOrNull()?.attr("src")
                novel.name = element.getElementsByTag("span").firstOrNull { it.hasClass("w-blog-entry-title-h") }?.text()
                novel.rating = element.getElementsByTag("span").firstOrNull { it.hasClass("userrate") }?.text()?.replace("Rating: ", "")?.trim()?.toDouble()
//                novel. = element.getElementsByTag("span").firstOrNull { it.className() === "s-genre" }?.children()?.map { it.text() }
                novel.shortDescription = element.getElementsByTag("div").firstOrNull { it.className() == "w-blog-entry-short" }?.textNodes()?.get(0)?.text()
                novel.longDescription = element.getElementsByTag("span").firstOrNull { it.attr("style") == "display:none" }?.textNodes()?.map { it.text() }?.joinToString (separator = "\n"){ it }
                searchResults.add(novel)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return searchResults
    }

}
