package io.github.gmathi.novellibrary.network

import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI


fun NovelApi.getChapterCount(novelUrl: String): Int {
    val host = URI(novelUrl).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return getNUChapterCount(novelUrl)
        host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterCount(novelUrl)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterCount(novelUrl)
    }
    return 0
}

fun NovelApi.getNUChapterCount(url: String): Int {
    var chapterCount = 0
    try {
        val document = getDocument(url)
        chapterCount = getNUChapterCount(document)

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapterCount
}

fun NovelApi.getNUChapterCount(document: Document): Int {
    var chapterCount = 0
    try {
        chapterCount = getNUChapterCountFromPageDoc(document)

        if (chapterCount == 15) {
            val lastPage = getNUPageCount(document)
            if (lastPage != 1) {
                chapterCount = (lastPage - 1) * 15
                val uri = URI(document.location())
                val basePath = "${uri.scheme}://${uri.host}${uri.path}"
                val lastPageDoc = getDocumentWithUserAgent(basePath + "?pg=" + lastPage)
                chapterCount += getNUChapterCountFromPageDoc(lastPageDoc)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return chapterCount
}

fun getNUChapterCountFromPageDoc(doc: Document): Int {
    val tableElement = doc.body().getElementsByAttributeValueMatching("id", "myTable").firstOrNull { it.tagName() === "table" }
    val elements = tableElement?.getElementsByClass("chp-release")?.filter { it.tagName() == "a" }
    if (elements != null)
        return elements.size/2
    return 0
}

fun getNUPageCount(doc: Document): Int {
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
    return maxPageNum
}


fun NovelApi.getRRChapterCount(url: String): Int {
    try {
        val document = getDocument(url)
        return getRRChapterCount(document)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

fun NovelApi.getRRChapterCount(document: Document): Int {
    try {
        val tableElement = document.body().getElementById("chapters")
        val chapters = tableElement?.getElementsByTag("a")?.filter { it.attributes().hasKey("href") }
        if (chapters != null) return chapters.size
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}


fun NovelApi.getWLNUChapterCount(url: String): Int {
    try {
        val document = getDocument(url)
        return getWLNUChapterCount(document)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

fun NovelApi.getWLNUChapterCount(document: Document): Int {
    try {
        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }
        if (trElements != null) return trElements.size
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

