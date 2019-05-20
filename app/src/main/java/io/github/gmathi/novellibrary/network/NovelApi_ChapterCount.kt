package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI


fun NovelApi.getChapterCount(novel: Novel): Int {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return getNUChapterCount(novel)
        host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterCount(novel.url)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterCount(novel.url)
        host.contains(HostNames.NOVEL_FULL) -> return getNovelFullChapterCount(novel)
    }
    return 0
}

fun getNUChapterCount(novel: Novel): Int {
    return getNUALLChapterUrls(novel).size
}

fun NovelApi.getRRChapterCount(url: String): Int {
    try {
        val document = getDocumentWithUserAgent(url)
        return getRRChapterCount(document)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

fun getRRChapterCount(document: Document): Int {
    try {
        val tableElement = document.body().getElementById("chapters")
        val chapters = tableElement?.select("a[href]")
        if (chapters != null) return chapters.size
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}


fun NovelApi.getWLNUChapterCount(url: String): Int {
    try {
        val document = getDocumentWithUserAgent(url)
        return getWLNUChapterCount(document)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

fun getWLNUChapterCount(document: Document): Int {
    try {
        val trElements = document.body().getElementsByTag("tr")?.filter { it.id() == "release-entry" }
        if (trElements != null) return trElements.size
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

fun getNovelFullChapterCount(novel: Novel): Int {
    return getNovelFullChapterUrls(novel)?.size ?: 0
}

