package com.mgn.bingenovelreader.network

import com.mgn.bingenovelreader.models.Novel
import com.mgn.bingenovelreader.models.WebPage
import com.mgn.bingenovelreader.utils.HostNames.USER_AGENT
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI

class NovelApi {

    fun getDocument(url: String): Document {
        return Jsoup.connect(url).get()
    }

    fun getDocumentWithUserAgent(url: String): Document {
        return Jsoup.connect(url).userAgent(USER_AGENT).get()
    }

    fun search(searchTerms: String): Map<String, ArrayList<Novel>> {
        val searchResults = HashMap<String, ArrayList<Novel>>()
        searchNovelUpdates(searchTerms)?.let { searchResults.put("novel-updates", it) }
        return searchResults
    }

    private fun searchNovelUpdates(searchTerms: String): ArrayList<Novel>? {
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

    public fun getChapterUrls(novel: Novel): ArrayList<WebPage> {
        return getChapterUrls(novel.url!!)
    }

    public fun getChapterUrls(url: String): ArrayList<WebPage> {
        var chapters = ArrayList<WebPage>()
        val host = URI(url).host
        when {
            host.contains("novelupdates.com") -> getNUChapterUrls(url, chapters)
            host.contains("royalroad.com") -> println("Not yet implemented for royal road")
        }
        return chapters
    }

    //Get Novel Updates Chapter URLs
    private fun getNUChapterUrls(url: String, chapters: ArrayList<WebPage>) {
        try {
            val document = getDocument(url)
            val tableElement = document.body().getElementsByAttributeValueMatching("id", "myTable").firstOrNull { it.tagName() === "table" }
            val elements = tableElement?.getElementsByClass("chp-release")?.filter { it.tagName() == "a" }
            if (elements != null)
                (0..elements.size).filter { it % 2 == 1 }.mapTo(chapters) { WebPage(url = elements[it].attr("href"), chapter = elements[it].text()) }

            val nextPageElement = document.body().getElementsByClass("next_page").filter { it.text() == "→" }
            if (nextPageElement.isNotEmpty()) {
                val uri = URI(url)
                val nextPageUrl = "${uri.scheme}://${uri.host}${uri.path}${nextPageElement[0].attr("href").replace("./", "")}"
                getNUChapterUrls(nextPageUrl, chapters)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
