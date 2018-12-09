package io.github.gmathi.novellibrary.network

import CloudFlareByPasser
import io.github.gmathi.novellibrary.database.createSource
import io.github.gmathi.novellibrary.database.getSource
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI


fun NovelApi.getChapterUrls(novel: Novel, withSources: Boolean = false): ArrayList<WebPage>? {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return if (withSources) getNUALLChapterUrlsWithSources(novel) else getNUALLChapterUrls(novel)
        host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrls(novel)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrls(novel)
    }
    return null
}

//Get RoyalRoad Chapter URLs
fun NovelApi.getRRChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(novel.url).get()
        chapters = ArrayList()
        val tableElement = document.body().getElementById("chapters")

        var orderId = 0L
        tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }?.forEach {
            val webPage = WebPage(url = it.absUrl("href"), chapter = it.text())
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getWLNUChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(novel.url).get()
        chapters = ArrayList()
        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }

        var orderId = 0L
        trElements?.asReversed()?.asSequence()?.forEach {
            val webPage = WebPage(url = it.child(0).child(0).attr("href"), chapter = it.getElementsByClass("numeric").joinToString(separator = ".") { it.text() })
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}


fun NovelApi.getNUALLChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val doc = Jsoup.connect(url)
            .data("action", "nd_getchapters")
            .referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap())
            .ignoreHttpErrors(true)
            .timeout(30000)
            .userAgent(HostNames.USER_AGENT)
            .data("mypostid", novelUpdatesNovelId)
            .post()

    var orderId = 0L
    doc?.getElementsByAttribute("data-id")?.reversed()?.forEach {

        val webPageUrl = "https:" + it?.attr("href")
        val webPage = WebPage(webPageUrl, it.getElementsByAttribute("title").attr("title"))
        webPage.orderId = orderId++
        webPage.novelId = novel.id
        chapters.add(webPage)
    }

    return chapters
}

fun NovelApi.getNUALLChapterUrlsWithSources(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    //val sourceMapList = ArrayList<HashMap<String, Long>>()
    val sourceMapList = getNUChapterUrlsWithSources(novel)

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val doc = Jsoup.connect(url)
            .data("action", "nd_getchapters")
            .referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap())
            .ignoreHttpErrors(true)
            .timeout(30000)
            .userAgent(HostNames.USER_AGENT)
            .data("mypostid", novelUpdatesNovelId)
            .post()

    var orderId = 0L
    doc?.getElementsByAttribute("data-id")?.reversed()?.forEach {

        val webPageUrl = "https:" + it?.attr("href")
        val webPage = WebPage(webPageUrl, it.getElementsByAttribute("title").attr("title"))
        webPage.orderId = orderId++
        webPage.novelId = novel.id
        for (sourceMap in sourceMapList) {
            webPage.sourceId = sourceMap[webPageUrl] ?: continue
            break
        }
        chapters.add(webPage)
    }

    return chapters
}

private fun NovelApi.getNUALLChapterUrlsForSource(novel: Novel, sourceId: Int? = null, sourceName: String? = null): HashMap<String, Long> {

    val sourceMap = HashMap<String, Long>()

    val dbSourceId = dbHelper.getSource(sourceName!!)?.first ?: -1L
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val connection = Jsoup.connect(url)
            .data("action", "nd_getchapters")
            .referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap())
            .ignoreHttpErrors(true)
            .timeout(30000)
            .userAgent(HostNames.USER_AGENT)
            .data("mygrr", "0")
            .data("mypostid", novelUpdatesNovelId)

    if (sourceId != null) connection.data("mygrpfilter", "$sourceId")

    val doc = connection.post()

    //var orderId = 0L
    doc?.select("a[href][data-id]")?.forEach {
        sourceMap["https:" + it.attr("href")] = dbSourceId
    }

    return sourceMap
}

private fun NovelApi.getNUChapterUrlsWithSources(novel: Novel): ArrayList<HashMap<String, Long>> {

    val sourceMap = ArrayList<HashMap<String, Long>>()
    if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

    val novelUpdatesNovelId = novel.metaData["PostId"]
    val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"

    val doc = Jsoup.connect(url)
            .data("action", "nd_getgroupnovel")
            .data("mygrr", "0")
            .referrer(url)
            .cookies(CloudFlareByPasser.getCookieMap())
            .ignoreHttpErrors(true)
            .timeout(30000)
            .userAgent(HostNames.USER_AGENT)
            .data("mypostid", novelUpdatesNovelId)
            .post()

    doc?.select("div.checkbox")?.forEach {
        dbHelper.createSource(it.text())
        val tempSourceMap = getNUALLChapterUrlsForSource(novel,
                it.selectFirst("input.grp-filter-attr[value]").attr("value").toInt(),
                it.text()
        )
        sourceMap.add(tempSourceMap)
    }

    return sourceMap
}
