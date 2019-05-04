package io.github.gmathi.novellibrary.network

import io.github.gmathi.novellibrary.model.Novel
import java.net.URI
import java.util.regex.Pattern


fun NovelApi.getNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    val host = URI(url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> novel = getNUNovelDetails(url)
        host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD) -> novel = getRRNovelDetails(url)
        host.contains(HostNames.WLN_UPDATES) -> novel = getWlnNovelDetails(url)
        host.contains(HostNames.NOVEL_FULL) -> novel = getNovelFullNovelDetails(url)
    }
    return novel
}

fun NovelApi.getNUNovelDetails(url: String): Novel? {
    var novel: Novel? = null
//    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.selectFirst(".seriestitlenu")?.text() ?: "NameUnableToFetch", url)
        novel.imageUrl = document.selectFirst(".seriesimg > img[src]")?.attr("abs:src")
        novel.longDescription = document.body().selectFirst("#editdescription")?.text()
        novel.rating = document.body().selectFirst("span.uvotes")?.text()?.substring(1, 4)

        novel.genres = document.body().selectFirst("#seriesgenre")?.children()?.map { it.text() }

        document.select(".genre")?.let { genreClassElements ->
            novel.metaData["Author(s)"] = genreClassElements.select("#authtag")?.joinToString(", ") { it.outerHtml() }
            novel.metaData["Artist(s)"] = genreClassElements.select("#artiststag")?.joinToString(", ") { it.outerHtml() }
            novel.metaData["Genre(s)"] =  genreClassElements.select("[gid]")?.joinToString(", ") { it.outerHtml() }
            novel.metaData["Tags"] = genreClassElements.select("#etagme")?.joinToString(", ") { it.outerHtml() }
            novel.metaData["Type"] = genreClassElements.select(".type").firstOrNull()?.outerHtml()
            novel.metaData["Language"] = genreClassElements.select("a[lid].lang").firstOrNull()?.outerHtml()
            novel.metaData["Original Publisher"] = genreClassElements.select("#myopub").joinToString(", ") { it.outerHtml() }
            novel.metaData["English Publisher"] = genreClassElements.select("#myepub").joinToString(", ") { it.outerHtml() }
        }

        novel.metaData["Year"] = document.getElementById("edityear").text()
        novel.metaData["Status in Country of Origin"] = document.getElementById("editstatus").text()
        novel.metaData["Licensed (in English)"] = document.getElementById("showlicensed").text()
        novel.metaData["Completely Translated"] = document.getElementById("showtranslated").outerHtml()
        novel.metaData["Completely Translated"] = document.getElementById("showtranslated").text()
        novel.metaData["Associated Names"] = document.getElementById("editassociated").text()
        novel.metaData["PostId"] = document.getElementById("mypostid").attr("value")

        novel.chaptersCount = getNUChapterCount(novel).toLong()
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
    return novel
}


fun NovelApi.getRRNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.selectFirst("h1[property=name]")?.text() ?: "NameUnableToFetch", url)

        //document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("name") && it.attr("name") == "twitter:title" }?.attr("content")
        novel.imageUrl = document.head().selectFirst("meta[property=og:image]")?.attr("content")
        novel.rating = document.head().selectFirst("meta[property=books:rating:value]")?.attr("content")
        novel.longDescription = document.body().selectFirst("div[property=description]")?.text()
        novel.genres = document.body().select("[property=genre]")?.map { it.text() }
        novel.chaptersCount = getRRChapterCount(document).toLong()

        novel.metaData["Author(s)"] = document.head().selectFirst("meta[property=books:author]")?.attr("content")

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getWlnNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)
        novel = Novel(document.body().selectFirst("h2")?.text() ?: "NameUnableToFetch", url)
        novel.imageUrl = document.body().selectFirst("img[src].coverimg")?.attr("abs:src")

        //Fetch rating using pattern matching
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

        novel.longDescription = document.body().select("span.description > p")?.text()
        novel.genres = document.body().select("a[href*=/genre-id/]")?.map { it.text() }
        novel.chaptersCount = getWLNUChapterCount(document).toLong()

        novel.metaData["Author(s)"] = document.select("span#author")?.joinToString(", ") {
            val linkElement = it.selectFirst("a[href]")
            if (linkElement != null) {
                "<a href=\"${linkElement.attr("abs:href")}\">${linkElement.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Artist(s)"] = document.select("span#illustrators")?.joinToString(", ") {
            val linkElement = it.selectFirst("a[href]")
            if (linkElement != null) {
                "<a href=\"${linkElement.attr("abs:href")}\">${linkElement.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Tags"] = document.select("span#tag")?.joinToString(", ") {
            val linkElement = it.selectFirst("a[href]")
            if (linkElement != null) {
                "<a href=\"${linkElement.attr("abs:href")}\">${linkElement.text()}</a>"
            } else {
                it.text()
            }
        }

        novel.metaData["Genre(s)"] = document.body().select("a[href*=/genre-id/]")?.joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metaData["Type"] = document.select("#type.dropitem-text")?.text()
        novel.metaData["Language"] = document.select("#orig_lang")?.text()
        novel.metaData["Country of Origin"] = document.select("#origin_loc.dropitem-text")?.text()
        novel.metaData["Status in Country of Origin"] = document.select("#orig_status")?.text()
        novel.metaData["Licensed (in English)"] = document.select("#license_en.dropitem-text")?.text()
        novel.metaData["Publisher(s)"] = document.select("span#publisher")?.joinToString(", ") { "<a href=\"${it.selectFirst("a")?.attr("abs:href")}\">${it.selectFirst("a")?.text()}</a>" }
        novel.metaData["OEL/Translated"] = document.select("#tl_type")?.text()
        novel.metaData["Demographic"] = document.select("#demographic")?.text()
        novel.metaData["General Text"] = document.select("#region.dropitem-text")?.text()
        novel.metaData["Initial publish date"] = document.select("#pub_date")?.text()
        novel.metaData["Alternate Names"] = document.select("span#altnames")?.joinToString(", ") { it.text() }
        novel.metaData["Homepage"] = document.selectFirst("a#website")?.outerHtml()

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getNovelFullNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocumentWithUserAgent(url)

        val booksElement = document.body().select("div.books")
        val infoElements = document.body().select("div.info").first().children()

        novel = Novel(booksElement.select("div.desc > h3").text() ?: "NameUnableToFetch", url)
        novel.imageUrl = booksElement.select("div.book > img").attr("abs:src")
        novel.genres = infoElements[1].select("a").map { it.text() }
        novel.longDescription = document.body().select("div.desc-text > p").joinToString (separator = "\n") { it.text() }
        novel.rating = (document.body().select("div.small > em > strong > span").first().text().toDouble() / 2).toString()

        novel.metaData["Author(s)"] = infoElements[0].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metaData["Genre(s)"] = infoElements[1].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metaData["Source"] = infoElements[2].text()
        novel.metaData["Status"] = infoElements[3].text()

        novel.chaptersCount = getNovelFullChapterUrls(novel)?.size?.toLong() ?: 0

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

