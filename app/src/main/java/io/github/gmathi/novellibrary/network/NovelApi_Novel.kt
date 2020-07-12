package io.github.gmathi.novellibrary.network

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.extensions.asJsonNullFreeString
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URI
import java.net.URL


fun NovelApi.getNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    val host = URI(url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> novel = getNUNovelDetails(url)
        host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD) -> novel = getRRNovelDetails(url)
        host.contains(HostNames.WLN_UPDATES) -> novel = getWlnNovelDetails(url)
        host.contains(HostNames.NOVEL_FULL) -> novel = getNovelFullNovelDetails(url)
        host.contains(HostNames.SCRIBBLE_HUB) -> novel = getScribbleHubNovelDetails(url)
        host.contains(HostNames.LNMTL) -> novel = getLNMTLNovelDetails(url)
    }
    return novel
}

fun NovelApi.getNUNovelDetails(url: String): Novel? {
    var novel: Novel? = null
//    try {
    val document = getDocument(url)
    novel = Novel(document.selectFirst(".seriestitlenu")?.text() ?: "NameUnableToFetch", url)
    novel.imageUrl = document.selectFirst(".seriesimg > img[src]")?.attr("abs:src")
    novel.longDescription = document.body().selectFirst("#editdescription")?.text()
    novel.rating = document.body().selectFirst("span.uvotes")?.text()?.substring(1, 4)

    novel.genres = document.body().selectFirst("#seriesgenre")?.children()?.map { it.text() }

    document.select(".genre")?.let { genreClassElements ->
        novel.metaData["Author(s)"] = genreClassElements.select("#authtag")?.joinToString(", ") { it.outerHtml() }
        novel.metaData["Artist(s)"] = genreClassElements.select("#artiststag")?.joinToString(", ") { it.outerHtml() }
        novel.metaData["Genre(s)"] = genreClassElements.select("[gid]")?.joinToString(", ") { it.outerHtml() }
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

    novel.chaptersCount = getChapterCount(novel).toLong()
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
    return novel
}


fun NovelApi.getRRNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocument(url)
        novel = Novel(document.selectFirst("h1[property=name]")?.text() ?: "NameUnableToFetch", url)

        //document.head().getElementsByTag("meta").firstOrNull { it.hasAttr("name") && it.attr("name") == "twitter:title" }?.attr("content")
        novel.imageUrl = document.head().selectFirst("meta[property=og:image]")?.attr("content")
        novel.rating = document.head().selectFirst("meta[property=books:rating:value]")?.attr("content")
        novel.longDescription = document.body().selectFirst("div[property=description]")?.text()
        novel.genres = document.body().select("[property=genre]")?.map { it.text() }
        novel.chaptersCount = getChapterCount(novel).toLong()

        novel.metaData["Author(s)"] = document.head().selectFirst("meta[property=books:author]")?.attr("content")

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getWlnNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        //Extract Important Info - API
        val novelId = URL(url).path.split("/").filter { it.isNotEmpty() }.last().toInt()
        val json = """ {
            "mode": "get-series-id",
            "id": $novelId
        } """

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(Constants.WLNUpdatesAPIUrl)
            .post(body)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonString = response.body?.string() ?: return novel
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject?.getAsJsonObject("data") ?: return novel

        novel = Novel(rootJsonObject["title"].asString, url)
        novel.imageUrl = rootJsonObject.getAsJsonArray("covers")?.firstOrNull()?.asJsonObject?.get("url")?.asString
        novel.longDescription = rootJsonObject.get("description")?.asString?.replace("<p>", "\n")?.replace("</p>", "")
        novel.genres = rootJsonObject.getAsJsonArray("genres")?.map { it.asJsonObject.get("genre").asString }
        novel.chaptersCount = rootJsonObject.getAsJsonArray("releases")?.count()?.toLong() ?: 0L
        val rating: Double = rootJsonObject.getAsJsonObject("rating")?.get("avg")?.asDouble ?: 0.0
        novel.rating = String.format("%.1f", (rating / 2))

        novel.metaData["Author(s)"] = rootJsonObject.getAsJsonArray("authors")?.joinToString(", ") { author ->
            val authorObject = author.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/author-id/${authorObject["id"]}\">${authorObject["author"].asString}</a>"
        }
        novel.metaData["Illustrator(s)"] = rootJsonObject.getAsJsonArray("illustrators")?.joinToString(", ") { illustrator ->
            val illustratorObject = illustrator.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/artist-id/${illustratorObject["id"]}\">${illustratorObject["illustrator"].asString}</a>"
        }
        novel.metaData["Publisher(s)"] = rootJsonObject.getAsJsonArray("publishers")?.joinToString(", ") { publisher ->
            val publisherObject = publisher.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/publishers/${publisherObject["id"]}\">${publisherObject["publisher"].asString}</a>"
        }

        //Genre - Sample Url: https://www.wlnupdates.com/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22genre-category%22%3A%20%7B%22action%22%3A%20%22included%22%7D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22title-search-text%22%3A%20%22%22%7D
        novel.metaData["Genre(s)"] = rootJsonObject.getAsJsonArray("genres")?.joinToString(", ") { genreObject ->
            val genre = genreObject.asJsonObject["genre"].asString
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22genre-category%22%3A%20%7B%22${genre}%22%3A%20%22included%22%7D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22title-search-text%22%3A%20%22%22%7D\">${genre}</a>"
        }

        //Tag - Sample Url: https://www.wlnupdates.com/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22tag-category%22%3A%20%7B%22reincarnated-into-another-world%22%3A%20%22included%22%7D%2C%20%22title-search-text%22%3A%20%22%22%7D
        novel.metaData["Tags"] = rootJsonObject.getAsJsonArray("tags")?.joinToString(", ") { tagObject ->
            val tag = tagObject.asJsonObject["tag"].asString
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22tag-category%22%3A%20%7B%22${tag}%22%3A%20%22included%22%7D%2C%20%22title-search-text%22%3A%20%22%22%7D\">${tag}</a>"
        }


        novel.metaData["Demographic"] = rootJsonObject["demographic"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Homepage"] = rootJsonObject["website"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Type"] = rootJsonObject["type"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["OEL/Translated"] = rootJsonObject["tl_type"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Initial publish date"] = rootJsonObject["pub_date"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Country of Origin"] = rootJsonObject["origin_loc"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Status in Country of Origin"] =  rootJsonObject["orig_status"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Licensed (in English)"] = rootJsonObject["license_en"]?.asJsonNullFreeString ?: "N/A"
        novel.metaData["Alternate Names"] = rootJsonObject.getAsJsonArray("alternatenames")?.joinToString(", ") { it.asString }
        novel.metaData["Language"] = rootJsonObject["orig_lang"]?.asJsonNullFreeString ?: "N/A"

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getNovelFullNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocument(url)

        val booksElement = document.body().select("div.books")
        val infoElements = document.body().select("div.info").first().children()

        novel = Novel(booksElement.select("div.desc > h3").text() ?: "NameUnableToFetch", url)
        novel.imageUrl = booksElement.select("div.book > img").attr("abs:src")
        novel.genres = infoElements[1].select("a").map { it.text() }
        novel.longDescription = document.body().select("div.desc-text > p").joinToString(separator = "\n") { it.text() }
        novel.rating = (document.body().select("div.small > em > strong > span").first().text().toDouble() / 2).toString()

        novel.metaData["Author(s)"] = infoElements[0].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metaData["Genre(s)"] = infoElements[1].select("a").joinToString(", ") { "<a href=\"${it.attr("abs:href")}\">${it.text()}</a>" }
        novel.metaData["Source"] = infoElements[2].text()
        novel.metaData["Status"] = infoElements[3].text()

        novel.chaptersCount = getChapterCount(novel).toLong()

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getScribbleHubNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val document = getDocument(url)

        val pageElement = document.body().select("div#page")

        novel = Novel(pageElement.select("div.fic_title").text() ?: "NameUnableToFetch", url)
        novel.imageUrl = pageElement.select("div.fic_image > img").attr("abs:src")
        novel.metaData["Author(s)"] = pageElement.select("span[property='author'] a").outerHtml()


        val genresElements = pageElement.select("span.wi_fic_genre a.fic_genre")//.select("a.fic_genre")
        novel.genres = genresElements.map { it.text() }
        novel.metaData["Genre(s)"] = genresElements.joinToString(", ") { it.outerHtml() }

        novel.longDescription = pageElement.select("div.wi_fic_desc").text()
        novel.rating = pageElement.select("meta[property='ratingValue']").attr("content")

        novel.metaData["Tags"] = pageElement.select("#etagme")?.joinToString(", ") { it.outerHtml() }
        novel.metaData["PostId"] = document.getElementById("mypostid").attr("value")

        novel.chaptersCount = document.getElementById("chpcounter").attr("value").toLong()

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return novel
}

fun NovelApi.getLNMTLNovelDetails(url: String): Novel? {
    var novel: Novel? = null
    try {
        val doc = getDocument(url)

        val novelElement = doc.selectFirst(".novel .media")
        novel = Novel(novelElement.selectFirst(".novel-name")?.text() ?: "NameUnableToFetch", url)
        novel.imageUrl = novelElement.selectFirst("img[src]")?.attr("abs:src")
        novel.longDescription = novelElement.selectFirst(".description")?.text()

        val negative = novelElement.selectFirst("div.progress-bar-danger")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val neutral = novelElement.selectFirst("div.progress-bar-warning")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val positive = novelElement.selectFirst("div.progress-bar-success")?.text()?.split(" ")?.get(0)?.toInt() ?: 0
        val total = (negative + neutral + positive).toFloat()
        if (total != 0f)
            novel.rating = (positive / total * 5).toString()

        val detailsElement = doc.selectFirst("div.container .row > div:last-child")

        val authors = detailsElement?.selectFirst("dt:contains(Authors)")?.nextElementSibling()?.select("span")
        novel.metaData["Author(s)"] = authors?.joinToString(", ") { it.text() }

        val genres = detailsElement?.selectFirst("div.panel-heading:contains(Genres)")?.nextElementSibling()?.select("ul li")
        novel.genres = genres?.map { it.text() }
        novel.metaData["Genre(s)"] = genres?.joinToString(", ") { it.html() }

        val tags = detailsElement?.selectFirst("div.panel-heading:contains(Tags)")?.nextElementSibling()?.select("ul li")
        novel.metaData["Tags"] = tags?.joinToString(", ") { it.html() }

        novel.chaptersCount = getLNMTLChapterCount(url).toLong()
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return novel
}