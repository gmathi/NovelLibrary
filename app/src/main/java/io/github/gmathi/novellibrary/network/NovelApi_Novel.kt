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
        novel = Novel()
        novel.name = document.getElementsByClass("seriestitlenu").firstOrNull()?.text()
        novel.imageUrl = document.getElementsByClass("seriesimg").firstOrNull()?.getElementsByTag("img")?.attr("src")
        novel.genres = document.body().getElementById("seriesgenre")?.children()?.map { it.text() }
        novel.longDescription = document.body().getElementById("editdescription")?.text()
        novel.chapterCount = getNUChapterCount(document).toLong()

        novel.metaData.put("Author(s)",
            document.getElementsByClass("genre").filter { it.id() == "authtag" }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Artist(s)",
            document.getElementsByClass("genre").filter { it.id() == "artiststag" }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Genre(s)",
            document.getElementsByClass("genre").filter { it.hasAttr("gid") }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Year",
            document.getElementById("edityear").text())
        novel.metaData.put("Type",
            document.getElementsByClass("genre type").firstOrNull()?.outerHtml())
        novel.metaData.put("Tags",
            document.getElementsByClass("genre").filter { it.id() == "etagme" }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Language",
            document.getElementsByClass("genre lang").firstOrNull { it.tagName() == "a" && it.hasAttr("lid") }?.outerHtml())
        novel.metaData.put("Status in Country of Origin",
            document.getElementById("editstatus").text())
        novel.metaData.put("Licensed (in English)",
            document.getElementById("showlicensed").text())
        novel.metaData.put("Completely Translated",
            document.getElementById("showtranslated").outerHtml())
        novel.metaData.put("Original Publisher",
            document.getElementsByClass("genre").filter { it.id() == "myopub" }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Completely Translated",
            document.getElementById("showtranslated").text())
        novel.metaData.put("English Publisher",
            document.getElementsByClass("genre").filter { it.id() == "myepub" }.map { it.outerHtml() }.joinToString(", "))
        novel.metaData.put("Associated Names",
            document.getElementById("editassociated").text())
        novel.metaData.put("PostId", document.getElementById("mypostid").attr("value"))


    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}


fun NovelApi.getRRNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel()

        novel.name = document.getElementsByAttributeValue("property", "name")?.firstOrNull { it.tagName() == "h1" }?.text()
        //document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("name") && it.attr("name") == "twitter:title" }?.attr("content")
        novel.imageUrl = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "og:image" }?.attr("content")
        novel.rating = document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:rating:value" }?.attr("content")
        novel.longDescription = document.body().getElementsByAttributeValue("property", "description").firstOrNull { it.tagName() == "div" }?.text()
        novel.genres = document.body().getElementsByAttributeValue("property", "genre")?.map { it.text() }
        novel.chapterCount = getRRChapterCount(document).toLong()

        novel.metaData.put("Author(s)",
            document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("property") && it.attr("property") == "books:author" }?.attr("content"))

    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getWlnNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel()

        novel.name = document.body().getElementsByTag("h2")?.firstOrNull()?.text()
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

        novel.metaData.put("Author(s)",
            document.getElementsByTag("span")?.filter { it.id() == "author" }?.map {
                val linkElement = it.getElementsByTag("a")?.firstOrNull()
                if (linkElement != null) {
                    "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
                } else {
                    it.text()
                }
            }?.joinToString(", "))

        novel.metaData.put("Artist(s)",
            document.getElementsByTag("span")?.filter { it.id() == "illustrators" }?.map {
                val linkElement = it.getElementsByTag("a")?.firstOrNull()
                if (linkElement != null) {
                    "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
                } else {
                    it.text()
                }
            }?.joinToString(", "))

        novel.metaData.put("Tags",
            document.getElementsByTag("span")?.filter { it.id() == "tag" }?.map {
                val linkElement = it.getElementsByTag("a")?.firstOrNull()
                if (linkElement != null) {
                    "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>"
                } else {
                    it.text()
                }
            }?.joinToString(", "))

        novel.metaData.put("Genre(s)",
            document.body().getElementsByTag("a")?.filter { it.hasAttr("href") && it.attr("href").contains("/genre-id/") }?.map { "<a href=\"${it.absUrl("href")}\">${it.text()}</a>" }?.joinToString(", "))
        novel.metaData.put("Type",
            document.getElementById("type")?.getElementsByClass("dropitem-text")?.text())
        novel.metaData.put("Language",
            document.getElementById("orig_lang")?.text())
        novel.metaData.put("Country of Origin",
            document.getElementById("origin_loc")?.getElementsByClass("dropitem-text")?.text())
        novel.metaData.put("Status in Country of Origin",
            document.getElementById("orig_status")?.text())
        novel.metaData.put("Licensed (in English)",
            document.getElementById("license_en")?.getElementsByClass("dropitem-text")?.text())
        novel.metaData.put("Publisher(s)",
            document.getElementsByTag("span")?.filter { it.id() == "publisher" }?.map { "<a href=\"${it.getElementsByTag("a")?.firstOrNull()?.absUrl("href")}\">${it.getElementsByTag("a")?.firstOrNull()?.text()}</a>" }?.joinToString(", "))
        novel.metaData.put("OEL/Translated",
            document.getElementById("tl_type")?.text())
        novel.metaData.put("Demographic",
            document.getElementById("demographic")?.text())
        novel.metaData.put("General Text",
            document.getElementById("region")?.getElementsByClass("dropitem-text")?.text())
        novel.metaData.put("Initial publish date",
            document.getElementById("pub_date")?.text())
        novel.metaData.put("Alternate Names",
            document.getElementsByTag("span")?.filter { it.id() == "altnames" }?.map { it.text() }?.joinToString(", "))
        novel.metaData.put("Homepage",
            document.getElementById("website")?.getElementsByTag("a")?.firstOrNull()?.outerHtml())


    } catch (e: IOException) {
        e.printStackTrace()
    }
    return novel
}

