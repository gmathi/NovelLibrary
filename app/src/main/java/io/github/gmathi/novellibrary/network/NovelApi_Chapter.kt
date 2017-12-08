package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI


fun NovelApi.getChapterUrls(novel: Novel): ArrayList<WebPage>? {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return getNUALLChapterUrls(novel)
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
    val request = Request.Builder()
        .url("https://www.novelupdates.com/wp-admin/admin-ajax.php")
        .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"), "action=nd_getchapters&mypostid=$novelUpdatesNovelId"))
        .build()
    val response = OkHttpClient().newCall(request).execute()
    response.use {
        if (!it.isSuccessful) throw IOException("Unexpected code " + it)

        val htmlString = it.body()?.string()
        val doc = Jsoup.parse(htmlString)

        doc?.getElementsByAttribute("data-id")?.mapTo(chapters) {
            WebPage("https:" + it?.attr("href")!!, it.getElementsByAttribute("title").attr("title"))
        }
    }
    return chapters
}
