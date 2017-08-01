package io.github.gmathi.novellibrary.network

import android.util.Log
import io.github.gmathi.novellibrary.model.WebPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI
import java.util.concurrent.*


fun NovelApi.getChapterUrls(url: String): ArrayList<WebPage>? {
    val host = URI(url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return getNUChapterUrlsNew(url)
        host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrls(url)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrls(url)
    }
    return null
}

//Get RoyalRoad Chapter URLs
fun NovelApi.getRRChapterUrls(url: String): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(url).get()
        chapters = ArrayList<WebPage>()
        val tableElement = document.body().getElementById("chapters")
        tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }?.asReversed()?.mapTo(chapters) { WebPage(url = "http://${HostNames.ROYAL_ROAD}${it.attr("href")}", chapter = it.text()) }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getWLNUChapterUrls(url: String): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = Jsoup.connect(url).get()
        chapters = ArrayList<WebPage>()
        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }
        trElements?.asReversed()?.mapTo(chapters) { WebPage(url = it.child(0).child(0).attr("href"), chapter = it.getElementsByClass("numeric").map { it.text() }.joinToString(separator = ".")) }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getNUChapterUrlsNew(url: String): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = getDocumentWithUserAgent(url)
        chapters = ArrayList<WebPage>()
        chapters.addAll(getNUChapterUrlsFromDoc(document))
        val pageUrls = getNUPageUrlsNew(document)

        if (pageUrls.isNotEmpty()) {
            val poolSize = Math.min(10, pageUrls.size)
            val threadPool = Executors.newFixedThreadPool(poolSize) as ThreadPoolExecutor
            val futureList = ArrayList<Future<ArrayList<WebPage>>>()
            pageUrls.asSequence().forEach {
                val callable = Callable<ArrayList<WebPage>> {
                    try {
                        val doc = getDocumentWithUserAgent(it)
                        getNUChapterUrlsFromDoc(doc)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        ArrayList<WebPage>()
                    }

                }
                futureList.add(threadPool.submit(callable))
            }
            threadPool.shutdown()
            try {
                threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
                futureList.asSequence().forEach { chapters!!.addAll(it.get()) }
            } catch (e: InterruptedException) {
                Log.w("NovelApi", "Thread pool executor interrupted~")
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getNUChapterUrlsFromDoc(doc: Document): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    val tableElement = doc.body().getElementsByAttributeValueMatching("id", "myTable").firstOrNull { it.tagName() === "table" }
    val elements = tableElement?.getElementsByClass("chp-release")?.filter { it.tagName() == "a" }
    if (elements != null)
        (0..elements.size).filter { it % 2 == 1 }.mapTo(chapters) { WebPage(url = elements[it].attr("href"), chapter = elements[it].text()) }
    return chapters
}

fun NovelApi.getNUPageUrlsNew(doc: Document): ArrayList<String> {
    val uri = URI(doc.location())
    val basePath = "${uri.scheme}://${uri.host}${uri.path}"
    val pageUrls = ArrayList<String>()
    val pageElements = doc.body().getElementsByClass("digg_pagination").firstOrNull { it.tagName() == "div" }?.children()?.filter { it.tagName() == "a" && it.hasAttr("href") }
    var maxPageNum = 1
    pageElements?.forEach {
        val href = it.attr("href") // ./?pg=19 is an example
        if (href.contains("./?pg=")) {
            val pageNum = href.replace("./?pg=", "").toInt()
            if (maxPageNum < pageNum)
                maxPageNum = pageNum
        }
    }
    if (maxPageNum == 2) {
        pageUrls.add(basePath + "?pg=2")
    } else if (maxPageNum > 2)
        (2..maxPageNum).mapTo(pageUrls) { basePath + "?pg=" + it }

    return pageUrls
}

