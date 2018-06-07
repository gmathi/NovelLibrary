package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.database.createSource
import io.github.gmathi.novellibrary.database.getSource
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.HostNames.USER_AGENT
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI


fun NovelApi.getChapterUrls(novel: Novel): ArrayList<WebPage>? {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return getNUChapterUrlsWithSources(novel)
        host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrls(novel.url)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrls(novel.url)
    }
    return null
}

//Get RoyalRoad Chapter URLs
fun getRRChapterUrls(url: String): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(url).get()
        chapters = ArrayList()
        val tableElement = document.body().getElementById("chapters")
        tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }?.asReversed()?.mapTo(chapters) { WebPage(url = it.absUrl("href"), chapter = it.text()) }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun getWLNUChapterUrls(url: String): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(url).get()
        chapters = ArrayList()
        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }
        trElements?.mapTo(chapters) { WebPage(url = it.child(0).child(0).attr("href"), chapter = it.getElementsByClass("numeric").joinToString(separator = ".") { it.text() }) }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun getNUChapterUrlsFromDoc(doc: Document): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    val tableElement = doc.body().getElementsByAttributeValueMatching("id", "myTable").firstOrNull { it.tagName() == "table" }
    val elements = tableElement?.getElementsByClass("chp-release")?.filter { it.tagName() == "a" }
    if (elements != null)
        (0..elements.size).filter { it % 2 == 1 }.mapTo(chapters) { WebPage(url = elements[it].attr("href"), chapter = elements[it].text()) }
    return chapters
}

fun getNUALLChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val doc = Jsoup.connect(url)
            .data("action", "nd_getchapters")
            .cookies(NovelApi.cookiesMap)
            .data("mypostid", novelUpdatesNovelId)
            .userAgent(USER_AGENT)
            .post()

    doc?.getElementsByAttribute("data-id")?.mapTo(chapters) {
        WebPage("https:" + it?.attr("href")!!, it.getElementsByAttribute("title").attr("title"))
    }

    return chapters
}

fun getNUALLChapterUrlsForSource(novel: Novel, sourceId: Int? = null, sourceName: String? = null): ArrayList<WebPage> {
    val dbSourceId = dbHelper.getSource(sourceName!!)?.first ?: -1L
    val chapters = ArrayList<WebPage>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val connection = Jsoup.connect(url)
            .data("action", "nd_getchapters")
            .data("mygrr", "0")
            .data("mypostid", novelUpdatesNovelId)
            .userAgent(USER_AGENT)

    if (sourceId != null) connection.data("mygrpfilter", "$sourceId")

    val doc = connection.post()

    var orderId = 0L
    doc?.select("a[href][data-id]")?.reversed()?.forEach {
        val webPage = WebPage("https:" + it.attr("href"), it.selectFirst("span[title]").text())
        webPage.sourceId = dbSourceId
        webPage.orderId = orderId++
        webPage.novelId = novel.id
        chapters.add(webPage)
    }

    return chapters
}

fun getNUChapterUrlsWithSources(novel: Novel): ArrayList<WebPage> {

    val chapters = ArrayList<WebPage>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val doc = Jsoup.connect(url)
            .data("action", "nd_getgroupnovel")
            .data("mygrr", "0")
            .data("mypostid", novelUpdatesNovelId)
            .userAgent(USER_AGENT)
            .post()

    doc?.select("div.checkbox")?.forEach {
        dbHelper.createSource(it.text())
        val webPages = getNUALLChapterUrlsForSource(novel,
                it.selectFirst("input.grp-filter-attr[value]").attr("value").toInt(),
                it.text()
        )
        chapters.addAll(webPages)
    }

    return chapters
}
