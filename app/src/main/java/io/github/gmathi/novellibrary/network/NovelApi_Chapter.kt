package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.createSource
import io.github.gmathi.novellibrary.database.getSource
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extensions.covertJsonNull
import io.github.gmathi.novellibrary.extensions.jsonNullFreeString
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithFormData
import io.github.gmathi.novellibrary.util.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL


fun NovelApi.getChapterUrls(novel: Novel, withSources: Boolean = false): ArrayList<WebPage> {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return if (withSources) getNUALLChapterUrlsWithSources(novel) else getNUALLChapterUrls(novel)
        host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrls(novel)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrls(novel)
        host.contains(HostNames.NOVEL_FULL) -> return getNovelFullChapterUrls(novel)
        host.contains(HostNames.SCRIBBLE_HUB) -> return getScribbleHubChapterUrls(novel)
        host.contains(HostNames.LNMTL) -> return getLNMTLChapterUrls(novel)
    }
    return ArrayList<WebPage>()
}

//Get RoyalRoad Chapter URLs
fun NovelApi.getRRChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        val document = getDocument(novel.url)
        val tableElement = document.body().select("#chapters") ?: return chapters

        var orderId = 0L
        tableElement.select("a[href]")?.forEach {
            val webPage = WebPage(url = it.attr("abs:href"), chapter = it.text())
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getWLNUChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        val novelId = URL(novel.url).path.split("/").filter { it.isNotEmpty() }.last().toInt()
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
        val jsonString = response.body?.string() ?: return chapters
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject?.getAsJsonObject("data") ?: return chapters
        val releasesArray = rootJsonObject.getAsJsonArray("releases")

        val sources: HashSet<String> = HashSet()
        releasesArray.forEach { release ->
            val source = release.asJsonObject["tlgroup"].covertJsonNull?.asJsonObject?.get("name")?.jsonNullFreeString
            source?.let { sources.add(it) }
        }
        val sourcesMap: HashMap<String, Long> = HashMap()
        sources.forEach { source ->
            val sourceId = dbHelper.createSource(source)
            sourcesMap[source] = sourceId
        }

        var orderId = 0L
        releasesArray.reversed().asSequence().forEach { release ->
            val releaseObject = release.asJsonObject
            val chapter = releaseObject["chapter"].covertJsonNull?.asInt?.toString() ?: ""
            val fragment = releaseObject["fragment"].covertJsonNull?.asInt?.toString() ?: ""
            val postFix = releaseObject["postfix"].jsonNullFreeString ?: ""
            val url = releaseObject["srcurl"].jsonNullFreeString
            val sourceName = releaseObject["tlgroup"].covertJsonNull?.asJsonObject?.get("name")?.jsonNullFreeString
            val sourceId = if (sourceName != null) sourcesMap[sourceName] else -1L

            url?.let {
                val chapterName = arrayListOf(chapter, fragment, postFix).filter { it.isNotBlank() }.joinToString("-")
                val webPage = WebPage(url = it, chapter = chapterName)
                webPage.orderId = orderId++
                webPage.novelId = novel.id
                webPage.sourceId = sourceId ?: -1L
                chapters.add(webPage)
            }
        }


//        val document = getDocument(novel.url)
//        val trElements = document.body().select("tr#release-entry")
//
//        trElements?.asReversed()?.asSequence()?.forEach {
//            val webPage = WebPage(url = it.child(0).child(0).attr("abs:href"), chapter = it.getElementsByClass("numeric").joinToString(separator = ".") { element -> element.text() })
//            webPage.orderId = orderId++
//            webPage.novelId = novel.id
//            chapters.add(webPage)
//        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}


fun getNUALLChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metaData["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getchapters",
            "mypostid" to novelUpdatesNovelId
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        val elements = doc.getElementsByAttribute("data-id")
        elements?.reversed()?.forEach {

            val webPageUrl = "https:" + it?.attr("href")
            val webPage = WebPage(webPageUrl, it.getElementsByAttribute("title").attr("title"))
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return chapters
}

fun getNUALLChapterUrlsWithSources(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

        //val sourceMapList = ArrayList<HashMap<String, Long>>()
        val sourceMapList = getNUChapterUrlsWithSources(novel)

        val novelUpdatesNovelId = novel.metaData["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getchapters",
            "mypostid" to novelUpdatesNovelId
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        val elements = doc.getElementsByAttribute("data-id")
        elements?.reversed()?.forEach {
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
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return chapters
}

private fun getNUALLChapterUrlsForSource(novel: Novel, sourceId: Int? = null, sourceName: String? = null): HashMap<String, Long> {

    val sourceMap = HashMap<String, Long>()

    try {
        val dbSourceId = dbHelper.getSource(sourceName!!)?.first ?: -1L
        if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metaData["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getchapters",
            "mypostid" to novelUpdatesNovelId,
            "mygrr" to "0"
        )
        if (sourceId != null) formData["mygrpfilter"] = "$sourceId"

        val doc = getDocumentWithFormData(url, formData)
        doc.select("a[href][data-id]")?.forEach {
            sourceMap["https:" + it.attr("href")] = dbSourceId
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return sourceMap
}

private fun getNUChapterUrlsWithSources(novel: Novel): ArrayList<HashMap<String, Long>> {

    val sourceMap = ArrayList<HashMap<String, Long>>()
    try {
        if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metaData["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getgroupnovel",
            "mypostid" to novelUpdatesNovelId,
            "mygrr" to "0"
        )

        val doc = getDocumentWithFormData(url, formData)
        doc.select("div.checkbox")?.forEach {
            dbHelper.createSource(it.text())
            val tempSourceMap = getNUALLChapterUrlsForSource(
                novel,
                it.selectFirst("input.grp-filter-attr[value]").attr("value").toInt(),
                it.text()
            )
            sourceMap.add(tempSourceMap)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return sourceMap
}

fun getNovelFullChapterUrls(novel: Novel): ArrayList<WebPage> {
    return try {
        val id = Jsoup.connect(novel.url).get().selectFirst("#rating").attr("data-novel-id")
        val chaptersDoc = Jsoup.connect("https://${HostNames.NOVEL_FULL}/ajax-chapter-option?novelId=$id&currentChapterId=").get()
        ArrayList(chaptersDoc.selectFirst("select.chapter_jump").children().mapIndexed { i, elem ->
            WebPage(
                url = "https://${HostNames.NOVEL_FULL}${elem.attr("value")}",
                chapter = elem.text(), novelId = novel.id, orderId = i.toLong()
            )
        })
    } catch (e: Exception) {
        e.printStackTrace();
        ArrayList<WebPage>()
    }
}

fun getScribbleHubChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")

        val url = "https://www.scribblehub.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "wi_gettocchp",
            "strSID" to novel.metaData["PostId"]!!,
            "strmypostid" to "0",
            "strFic" to "yes"
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        doc.select("a[href]")?.reversed()?.forEach {
            val webPage = WebPage(it.attr("abs:href"), it.attr("title"))
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return chapters
}

fun getLNMTLChapterUrls(novel: Novel): ArrayList<WebPage> {
    val chapters = ArrayList<WebPage>()
    try {
        val doc = Jsoup.connect(novel.url).get()

        val scripts = doc.select("script")
        val script = scripts.find { it.html().contains("lnmtl.firstResponse =") } ?: return chapters
        val text = script.html()

        val json = text.substring(text.indexOf("lnmtl.firstResponse =") + 21)
            .substringBefore(";lnmtl.volumes =")

        val type = object : TypeToken<Map<String, Any>>() {}.type
        val gson: LinkedTreeMap<String, Any> = Gson().fromJson(json, type) ?: return chapters

        @Suppress("UNCHECKED_CAST")
        val data = gson["data"] as List<LinkedTreeMap<String, Any>>

        var orderId = 0L
        for (chapter in data) {
            val url = chapter["site_url"]
            val title = chapter["title"]
            if (url !is String || title !is String) continue
            val webPage = WebPage(url, title)
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}
