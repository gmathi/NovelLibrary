import io.github.gmathi.novellibrary.model.WebPage
import org.jsoup.nodes.Document

//package io.github.gmathi.novellibrary.network
//
//import io.github.gmathi.novellibrary.model.WebPage
//import io.github.gmathi.novellibrary.util.Constants
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import java.io.IOException
//import java.net.URI
//
//
//fun NovelApi.getChapterUrlsForPage(url: String, pageNum: Int): ArrayList<WebPage>? {
//    val host = URI(url).host
//    when {
//        host.contains(HostNames.NOVEL_UPDATES) -> return getNUChapterUrlsForPage(url, pageNum)
//        host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrlsForPage(url, pageNum)
//        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrlsForPage(url, pageNum)
//    }
//    return null
//}
//
////Get RoyalRoad Chapter URLs
//fun NovelApi.getRRChapterUrlsForPage(url: String, pageNum: Int): ArrayList<WebPage>? {
//    var chapters: ArrayList<WebPage>? = null
//    try {
//        val document = Jsoup.connect(url).get()
//        chapters = ArrayList()
//        val tableElement = document.body().getElementById("chapters")
//        val chapterElements = tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }?.asReversed()
//
//        val indexStartRange = (pageNum * Constants.CHAPTER_PAGE_SIZE)
//        if (chapterElements != null && chapterElements.size > indexStartRange) {
//            val endIndex = Math.min(chapterElements.size - 1, indexStartRange + Constants.CHAPTER_PAGE_SIZE - 1)
//            (indexStartRange..endIndex).mapTo(chapters) { WebPage(url = chapterElements[it].absUrl("href"), chapter = chapterElements[it].text()) }
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//    }
//    return chapters
//}
//
//fun NovelApi.getWLNUChapterUrlsForPage(url: String, pageNum: Int): ArrayList<WebPage>? {
//    var chapters: ArrayList<WebPage>? = null
//    try {
//        val document = Jsoup.connect(url).get()
//        chapters = ArrayList()
//        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }
//        val indexStartRange = (pageNum * Constants.CHAPTER_PAGE_SIZE)
//        if (trElements != null && trElements.size > indexStartRange) {
//            val endIndex = Math.min(trElements.size - 1, indexStartRange + Constants.CHAPTER_PAGE_SIZE - 1)
//            (indexStartRange..endIndex).mapTo(chapters) { WebPage(url = trElements[it].absUrl("href"), chapter = trElements[it].text()) }
//        }
//    } catch (e: IOException) {
//        e.printStackTrace()
//    }
//    return chapters
//}
//
//
//fun NovelApi.getNUPageUrlsNew(doc: Document): ArrayList<String> {
//    val uri = URI(doc.location())
//    val basePath = "${uri.scheme}://${uri.host}${uri.path}"
//    val pageUrls = ArrayList<String>()
//    val pageElements = doc.body().getElementsByClass("digg_pagination").firstOrNull { it.tagName() == "div" }?.children()?.filter { it.tagName() == "a" && it.hasAttr("href") }
//    var maxPageNum = 1
//    pageElements?.forEach {
//        val href = it.attr("href") // ./?pg=19 is an example
//        if (href.contains("./?pg=")) {
//            val pageNum = href.replace("./?pg=", "").toInt()
//            if (maxPageNum < pageNum)
//                maxPageNum = pageNum
//        }
//    }
//    if (maxPageNum == 2) {
//        pageUrls.add(basePath + "?pg=2")
//    } else if (maxPageNum > 2)
//        (2..maxPageNum).mapTo(pageUrls) { basePath + "?pg=" + it }
//
//    return pageUrls
//}
//
//
//fun NovelApi.getNUChapterUrlsForPage(novelUrl: String, pageNum: Int): ArrayList<WebPage>? {
//    try {
//        val uri = URI(novelUrl)
//        val basePath = "${uri.scheme}://${uri.host}${uri.path}"
//        val doc = getDocumentWithUserAgent(basePath + "?pg=" + (pageNum + 1))
//        return getNUChapterUrlsFromDoc(doc)
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//    return null
//}
//
//fun getNUChapterUrlsFromDoc(doc: Document): ArrayList<WebPage> {
//    val chapters = ArrayList<WebPage>()
//    val tableElement = doc.body().getElementsByAttributeValueMatching("id", "myTable").firstOrNull { it.tagName() == "table" }
//    val elements = tableElement?.getElementsByClass("chp-release")?.filter { it.tagName() == "a" }
//    if (elements != null)
//        (0..elements.size).filter { it % 2 == 1 }.mapTo(chapters) { WebPage(url = elements[it].attr("href"), chapter = elements[it].text()) }
//    return chapters
//}

