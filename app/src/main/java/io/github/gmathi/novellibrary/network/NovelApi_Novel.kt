package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import java.io.IOException
import java.net.URI
import java.util.regex.Pattern


fun NovelApi.getNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    val host = URI(url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> novel = getNUNovelDetails(url)
        host.contains(HostNames.ROYAL_ROAD) -> novel = getRRNovelDetails(url)
        host.contains(HostNames.WLN_UPDATES) -> novel = getWlnNovelDetails(url)
    }
    return novel
}

fun NovelApi.getNUNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.getElementsByClass("seriestitlenu").firstOrNull()?.text() ?: "NameUnableToFetch", url)
        novel.imageUrl = document.getElementsByClass("seriesimg").firstOrNull()?.getElementsByTag("img")?.attr("src")
        novel.longDescription = document.body().getElementById("editdescription")?.text()
        novel.rating = document.body().getElementsByClass("uvotes")?.firstOrNull { it.id() == "span" }?.text()?.substring(1, 4)

        novel.genres = document.body().getElementById("seriesgenre")?.children()?.map { it.text() }

        novel.metaData["Author(s)"] = document.getElementsByClass("genre").filter { it.id() == "authtag" }.joinToString(", ") { it.outerHtml() }
        novel.metaData["Artist(s)"] = document.getElementsByClass("genre").filter { it.id() == "artiststag" }.joinToString(", ") { it.outerHtml() }
        novel.metaData["Genre(s)"] = document.getElementsByClass("genre").filter { it.hasAttr("gid") }.joinToString(", ") { it.outerHtml() }
        novel.metaData["Year"] = document.getElementById("edityear").text()
        novel.metaData["Type"] = document.getElementsByClass("genre type").firstOrNull()?.outerHtml()
        novel.metaData["Tags"] = document.getElementsByClass("genre").filter { it.id() == "etagme" }.joinToString(", ") { it.outerHtml() }
        novel.metaData["Language"] = document.getElementsByClass("genre lang").firstOrNull { it.tagName() == "a" && it.hasAttr("lid") }?.outerHtml()
        novel.metaData["Status in Country of Origin"] = document.getElementById("editstatus").text()
        novel.metaData["Licensed (in English)"] = document.getElementById("showlicensed").text()
        novel.metaData["Completely Translated"] = document.getElementById("showtranslated").outerHtml()
        novel.metaData["Original Publisher"] = document.getElementsByClass("genre").filter { it.id() == "myopub" }.map { it.outerHtml() }.joinToString(", ")
        novel.metaData["Completely Translated"] = document.getElementById("showtranslated").text()
        novel.metaData["English Publisher"] = document.getElementsByClass("genre").filter { it.id() == "myepub" }.map { it.outerHtml() }.joinToString(", ")
        novel.metaData["Associated Names"] = document.getElementById("editassociated").text()
        novel.metaData["PostId"] = document.getElementById("mypostid").attr("value")

        novel.chapterCount = getNUChapterCount(novel).toLong()
        novel.newChapterCount = novel.chapterCount

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}


fun NovelApi.getRRNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.getElementsByAttributeValue("property", "name")?.firstOrNull { it.tagName() == "h1" }?.text() ?: "NameUnableToFetch", url)

        //document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("name") && it.attr("name") == "twitter:title" }?.attr("content")
        novel.imageUrl = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "og:image" }?.attr("content")
        novel.rating = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:rating:value" }?.attr("content")
        novel.longDescription = document.body().getElementsByAttributeValue("property", "description").firstOrNull { it.tagName() == "div" }?.text()
        novel.genres = document.body().getElementsByAttributeValue("property", "genre")?.map { it.text() }
        novel.chapterCount = getRRChapterCount(document).toLong()

        novel.metaData["Author(s)"] = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:author" }?.attr("content")

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getWlnNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.body().getElementsByTag("h2")?.firstOrNull()?.text() ?: "NameUnableToFetch", url)
        novel.imageUrl = document.body().getElementsByClass("coverimg")?.firstOrNull { it.tagName() == "img" }?.absUrl("src")

        val scriptContent = document.getElementsByTag("script")?.outerHtml()
        if (scriptContent != null) {
            val p = Pattern.compile("initialRating\\s[:]\\s(.*?),", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
            val m = p.matcher(scriptContent)
            if (m.find()) {
                novel.rating = m.group(1)
                try {
                    novel.rating = (novel.rating!!.toInt() / 2).toString()
                } catch (e: Exception) {
                }
            }
        }

        novel.longDescription = document.body().getElementsByClass("description")?.firstOrNull { it.tagName() == "span" }?.getElementsByTag("p")?.text()
        novel.genres = document.body().getElementsByTag("a")?.filter { it.hasAttr("href") && it.attr("href").contains("/genre-id/") }?.map { it.text() }
        novel.chapterCount = getWLNUChapterCount(document).toLong()

        novel.metaData["Author(s)"] = document.getElementsByTag("span")?.filter { it.id() == "author" }?.joinToString(", ") {
            val linkElement = it.getElementsByTag("a")?.firstOrNull()
            if (linkElement != null) {
                "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Artist(s)"] = document.getElementsByTag("span")?.filter { it.id() == "illustrators" }?.joinToString(", ") {
            val linkElement = it.getElementsByTag("a")?.firstOrNull()
            if (linkElement != null) {
                "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Tags"] = document.getElementsByTag("span")?.filter { it.id() == "tag" }?.joinToString(", ") {
            val linkElement = it.getElementsByTag("a")?.firstOrNull()
            if (linkElement != null) {
                "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Genre(s)"] = document.body().getElementsByTag("a")?.filter { it.hasAttr("href") && it.attr("href").contains("/genre-id/") }?.joinToString(", ") { "<a href=\"${it.absUrl("href")}\">${it.text()}</a>" }
        novel.metaData["Type"] = document.getElementById("type")?.getElementsByClass("dropitem-text")?.text()
        novel.metaData["Language"] = document.getElementById("orig_lang")?.text()
        novel.metaData["Country of Origin"] = document.getElementById("origin_loc")?.getElementsByClass("dropitem-text")?.text()
        novel.metaData["Status in Country of Origin"] = document.getElementById("orig_status")?.text()
        novel.metaData["Licensed (in English)"] = document.getElementById("license_en")?.getElementsByClass("dropitem-text")?.text()
        novel.metaData["Publisher(s)"] = document.getElementsByTag("span")?.filter { it.id() == "publisher" }?.joinToString(", ") { "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>" }
        novel.metaData["OEL/Translated"] = document.getElementById("tl_type")?.text()
        novel.metaData["Demographic"] = document.getElementById("demographic")?.text()
        novel.metaData["General Text"] = document.getElementById("region")?.getElementsByClass("dropitem-text")?.text()
        novel.metaData["Initial publish date"] = document.getElementById("pub_date")?.text()
        novel.metaData["Alternate Names"] = document.getElementsByTag("span")?.filter { it.id() == "altnames" }?.joinToString(", ") { it.text() }
        novel.metaData["Homepage"] = document.getElementById("website")?.getElementsByTag("a")?.firstOrNull()?.outerHtml()


    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}

