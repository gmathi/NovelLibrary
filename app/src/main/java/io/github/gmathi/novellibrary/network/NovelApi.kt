package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.HostNames.*
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
        searchNovelUpdates(searchTerms)?.let { searchResults.put(NOVEL_UPDATES, it) }
        searchRoyalRoad(searchTerms)?.let { searchResults.put(ROYAL_ROAD, it) }
        return searchResults
    }

    fun searchUrl(url: String): ArrayList<Novel>? {
        val host = URI(url).host
        when {
            host.contains(ROYAL_ROAD) -> return searchRoyalRoadUrl(url)
            host.contains(NOVEL_UPDATES) -> return searchNovelUpdatesUrl(url)
        }
        return null
    }

    fun searchRoyalRoadUrl(searchUrl: String): ArrayList<Novel>? {
        var searchResults: ArrayList<Novel>? = null
        try {
            searchResults = ArrayList()
            val document = Jsoup.connect(searchUrl).get()
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

    fun searchNovelUpdatesUrl(searchUrl: String): ArrayList<Novel>? {
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
                novel.rating = element.getElementsByTag("div").firstOrNull { it.hasAttr("title")  && it.attr("title").contains("Rating: ")}?.attr("title")?.replace("Rating: ", "")?.trim()
                searchResults.add(novel)
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return searchResults
    }

    public fun searchRoyalRoad(searchTerms: String): ArrayList<Novel>? {
        var searchResults: ArrayList<Novel>? = null
        try {
            searchResults = ArrayList()
            val document = Jsoup.connect("https://royalroadl.com/fictions/search?keyword=$searchTerms&name=&author=&minPages=0&maxPages=10000&minRating=0&maxRating=5&status=ALL&orderBy=popularity&dir=desc&type=ALL").get()
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

    public fun searchNovelUpdates(searchTerms: String): ArrayList<Novel>? {
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

    public fun getChapterUrls(url: String): ArrayList<WebPage> {
        var chapters = ArrayList<WebPage>()
        val host = URI(url).host
        when {
            host.contains(NOVEL_UPDATES) -> getNUChapterUrls(url, chapters)
            host.contains(ROYAL_ROAD) -> getRRChapterUrls(url, chapters)
        }
        return chapters
    }

    //Get Novel-Updates Chapter URLs
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

    //Get RoyalRoad Chapter URLs
    private fun getRRChapterUrls(url: String, chapters: ArrayList<WebPage>) {
        try {
            val document = Jsoup.connect(url).get()
            val tableElement = document.body().getElementById("chapters")
            tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }?.asReversed()?.mapTo(chapters) { WebPage(url = "http://$ROYAL_ROAD${it.attr("href")}", chapter = it.text()) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun getNovelDetails(url: String): Novel? {
        var novel: Novel? = null
        val host = URI(url).host
        when {
            host.contains(NOVEL_UPDATES) -> novel = getNUNovelDetails(url)
            host.contains(ROYAL_ROAD) -> novel = getRRNovelDetails(url)
        }
        return novel
    }

    fun getNUNovelDetails(url: String): Novel? {
        var novel: Novel? = null
        try {
            val document = getDocumentWithUserAgent(url)
            novel = Novel()
            novel.name = document.getElementsByClass("seriestitlenu").firstOrNull()?.text()
            novel.imageUrl = document.getElementsByClass("seriesimg").firstOrNull()?.getElementsByTag("img")?.attr("src")
            novel.genres = document.body().getElementById("seriesgenre")?.children()?.map { it.text() }
            novel.longDescription = document.body().getElementById("editdescription")?.text()


            novel.metaData.put("Author(s)",
                document.getElementsByClass("genre").filter { it.id() == "authtag" }.map { it.text() }.joinToString(", "))
            novel.metaData.put("Artist(s)",
                document.getElementsByClass("genre").filter { it.id() == "artiststag" }.map { it.text() }.joinToString(", "))
            novel.metaData.put("Year",
                document.getElementById("edityear").text())
            novel.metaData.put("Type",
                document.getElementsByClass("genre type").firstOrNull()?.text())
            novel.metaData.put("Tags",
                document.getElementsByClass("genre").filter { it.id() == "etagme" }.map { it.text() }.joinToString(", "))
            novel.metaData.put("Language",
                document.getElementById("showlang").text())
            novel.metaData.put("Status in Country of Origin",
                document.getElementById("editstatus").text())
            novel.metaData.put("Licensed (in English)",
                document.getElementById("showlicensed").text())
            novel.metaData.put("Licensed (in English)",
                document.getElementById("showlicensed").text())
            novel.metaData.put("Completely Translated",
                document.getElementById("showtranslated").text())
            novel.metaData.put("Original Publisher",
                document.getElementsByClass("genre").filter { it.id() == "myopub" }.map { it.text() }.joinToString(", "))
            novel.metaData.put("Completely Translated",
                document.getElementById("showtranslated").text())
            novel.metaData.put("English Publisher",
                document.getElementsByClass("genre").filter { it.id() == "myepub" }.map { it.text() }.joinToString(", "))
            novel.metaData.put("Associated Names",
                document.getElementById("editassociated").text())


        } catch (e: IOException) {
            e.printStackTrace()
        }
        return novel
    }


    fun getRRNovelDetails(url: String): Novel? {
        var novel: Novel? = null
        try {
            val document = getDocumentWithUserAgent(url)
            novel = Novel()

            novel.name = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("name") && it.attr("name") == "twitter:title" }?.attr("content")
            novel.imageUrl = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "og:image" }?.attr("content")
            novel.rating = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:rating:value" }?.attr("content")
            novel.longDescription = document.body().getElementsByAttributeValue("property", "description").firstOrNull { it.tagName() == "div" }?.text()
            novel.genres = document.body().getElementsByAttributeValue("property", "genre")?.map { it.text() }
            novel.metaData.put("Author(s)",
                document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:author" }?.attr("content"))

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return novel
    }


}
