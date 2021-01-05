package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.createTranslatorSource
import io.github.gmathi.novellibrary.database.getTranslatorSource
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.NovelApi.getDocument
import io.github.gmathi.novellibrary.network.NovelApi.getDocumentWithFormData
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import io.github.gmathi.novellibrary.util.lang.covertJsonNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.set

fun NovelApi.getChapterUrls(novel: Novel, withSources: Boolean = false): ArrayList<WebPage>? {
    val host = URI(novel.url).host
    when {
        host.contains(HostNames.NOVEL_UPDATES) -> return if (withSources) getNUALLChapterUrlsWithSources(novel) else getNUALLChapterUrls(novel)
        host.contains(HostNames.ROYAL_ROAD_OLD) || host.contains(HostNames.ROYAL_ROAD) -> return getRRChapterUrls(novel)
        host.contains(HostNames.WLN_UPDATES) -> return getWLNUChapterUrls(novel)
        host.contains(HostNames.NOVEL_FULL) -> return getNovelFullChapterUrls(novel)
        host.contains(HostNames.SCRIBBLE_HUB) -> return getScribbleHubChapterUrls(novel)
        host.contains(HostNames.LNMTL) -> return getLNMTLChapterUrls(novel)
        host.contains(HostNames.NEOVEL) -> return getNeovelChapterUrls(novel)
    }
    return ArrayList<WebPage>()
}

//Get RoyalRoad Chapter URLs
fun NovelApi.getRRChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val document = getDocument(novel.url)
        val tableElement = document.body().select("#chapters") ?: return chapters

        var orderId = 0L
        chapters = ArrayList()
        tableElement.select("a[href]")?.forEach {
            val webPage = WebPage(url = it.absUrl("href"), chapter = it.text())
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getWLNUChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val novelId = URL(novel.url).path.split("/").last { it.isNotEmpty() }.toInt()
        val json = """ {
            "mode": "get-series-id",
            "id": $novelId
        } """

        val body = json.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(Constants.WLN_UPDATES_API_URL)
            .post(body)
            .build()
        val response = OkHttpClient().newCall(request).execute()
        val jsonString = response.body?.string() ?: return chapters
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject?.getAsJsonObject("data") ?: return chapters
        val releasesArray = rootJsonObject.getAsJsonArray("releases")

        val sources: HashSet<String> = HashSet()
        releasesArray.forEach { release ->
            val source = release.asJsonObject["tlgroup"].covertJsonNull?.asJsonObject?.get("name")?.asJsonNullFreeString
            source?.let { sources.add(it) }
        }
        val sourcesMap: HashMap<String, Long> = HashMap()
        sources.forEach { source ->
            val sourceId = dbHelper.createTranslatorSource(source)
            sourcesMap[source] = sourceId
        }

        var orderId = 0L
        chapters = ArrayList()
        releasesArray.reversed().asSequence().forEach { release ->
            val releaseObject = release.asJsonObject
            val chapter = releaseObject["chapter"].covertJsonNull?.asInt?.toString() ?: ""
            val fragment = releaseObject["fragment"].covertJsonNull?.asInt?.toString() ?: ""
            val postFix = releaseObject["postfix"].asJsonNullFreeString ?: ""
            val url = releaseObject["srcurl"].asJsonNullFreeString
            val sourceName = releaseObject["tlgroup"].covertJsonNull?.asJsonObject?.get("name")?.asJsonNullFreeString
            val sourceId = if (sourceName != null) sourcesMap[sourceName] else -1L

            url?.let {
                val chapterName = arrayListOf(chapter, fragment, postFix).filter { name -> name.isNotBlank() }.joinToString(" - ")
                val webPage = WebPage(url = it, chapter = chapterName)
                webPage.orderId = orderId++
                webPage.novelId = novel.id
                webPage.translatorSourceId = sourceId ?: -1L
                chapters.add(webPage)
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}


fun getNUALLChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getchapters",
            "mypostid" to novelUpdatesNovelId
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        chapters = ArrayList()
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

fun getNUALLChapterUrlsWithSources(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")

        //val sourceMapList = ArrayList<HashMap<String, Long>>()
        val sourceMapList = getNUChapterUrlsWithSources(novel)

        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getchapters",
            "mypostid" to novelUpdatesNovelId
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        chapters = ArrayList()
        val elements = doc.getElementsByAttribute("data-id")
        elements?.reversed()?.forEach {
            val webPageUrl = "https:" + it?.attr("href")
            val webPage = WebPage(webPageUrl, it.getElementsByAttribute("title").attr("title"))
            webPage.orderId = orderId++
            webPage.novelId = novel.id
            for (sourceMap in sourceMapList) {
                webPage.translatorSourceId = sourceMap[webPageUrl] ?: continue
                break
            }
            chapters.add(webPage)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return chapters
}

private fun getNUALLChapterUrlsForSource(novel: Novel, sourceId: Int? = null, sourceName: String): HashMap<String, Long> {

    val sourceMap = HashMap<String, Long>()

    try {
        val dbSourceId = dbHelper.getTranslatorSource(sourceName)?.id ?: -1L
        if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
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
        if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")

        val novelUpdatesNovelId = novel.metadata["PostId"] ?: ""
        val url = "https://www.novelupdates.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "nd_getgroupnovel",
            "mypostid" to novelUpdatesNovelId,
            "mygrr" to "0"
        )

        val doc = getDocumentWithFormData(url, formData)
        doc.select("div.checkbox")?.forEach {
            dbHelper.createTranslatorSource(it.text())
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

fun getNovelFullChapterUrls(novel: Novel): ArrayList<WebPage>? {
    return try {
        val id = NovelApi.getDocument(novel.url).selectFirst("#rating")?.attr("data-novel-id") ?: return null
        val chaptersDoc = getDocument("https://${HostNames.NOVEL_FULL}/ajax-chapter-option?novelId=$id&currentChapterId=")
        val chapters = chaptersDoc.selectFirst("select.chapter_jump")?.children()?.mapIndexed { i, elem ->
            WebPage(
                url = "https://${HostNames.NOVEL_FULL}${elem.attr("value")}",
                chapter = elem.text(), novelId = novel.id, orderId = i.toLong()
            )
        } ?: return null
        ArrayList(chapters)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getScribbleHubChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")

        val url = "https://www.scribblehub.com/wp-admin/admin-ajax.php"
        val formData: HashMap<String, String> = hashMapOf(
            "action" to "wi_gettocchp",
            "strSID" to novel.metadata["PostId"]!!,
            "strmypostid" to "0",
            "strFic" to "yes"
        )

        val doc = getDocumentWithFormData(url, formData)
        var orderId = 0L
        chapters = ArrayList()
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

fun getLNMTLChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        // Fetch initial info about available volumes and API endpoint route
        val doc = getDocument(novel.url)

        val scripts = doc.select("script")
        val script = scripts.find { it.html().contains("lnmtl.firstResponse =") } ?: return chapters
        val text = script.html().split(";")

        // It can be hardcoded to be `https://lnmtl.com/chapter`, but I decided to parse it just in case.
        val route = text.find { it.startsWith("lnmtl.route =") }?.trim()?.substring(15)?.substringBeforeLast('\'') ?: "https://lnmtl.com/chapter";
        val volumeJson = text.find { it.startsWith("lnmtl.volumes =") }?.substring(15)

        val type = object : TypeToken<Map<String, Any>>() {}.type
        val volumeType = object : TypeToken<List<Map<String, Any>>>() {}.type
        val volumeGson: List<LinkedTreeMap<String, Any>> = Gson().fromJson(volumeJson, volumeType) ?: return chapters

        // Potential optimization is to skip fetching first page of first volume,
        // but that would increase code complexity a lot
        var orderId = 0L
        chapters = ArrayList()
        for (volume in volumeGson) {
            val id = (volume["id"] as Double).toInt()
            var page = 1
            do {
                val pageDoc = Jsoup.connect("${route}?page=${page++}&volumeId=${id}").ignoreContentType(true).execute().body()
                val pageGson: LinkedTreeMap<String, Any> = Gson().fromJson(pageDoc, type) ?: break

                @Suppress("UNCHECKED_CAST")
                val data = pageGson["data"] as List<LinkedTreeMap<String, Any>>

                for (chapter in data) {
                    val url = chapter["site_url"]
                    val title = "c${chapter["position"] ?: (orderId + 1)}${if (chapter["part"] != null) "p${chapter["part"]}" else ""} ${chapter["title"] ?: ""}"
                    if (url !is String) continue
                    val webPage = WebPage(url, title)
                    webPage.orderId = orderId++
                    webPage.novelId = novel.id
                    chapters.add(webPage)
                }

            } while (pageGson["total"] != pageGson["to"])

        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}

fun NovelApi.getNeovelChapterUrls(novel: Novel): ArrayList<WebPage>? {
    var chapters: ArrayList<WebPage>? = null
    try {
        val novelId = novel.metadata["id"]
        val url = "https://${HostNames.NEOVEL}/V5/chapters?bookId=$novelId&language=EN"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = OkHttpClient().newCall(request).execute()
        val jsonString = response.body?.string() ?: return chapters
        val jsonArray = JsonParser.parseString(jsonString)?.asJsonArray ?: return chapters
        val sortedChaptersArray = jsonArray.sortedWith(object : Comparator<JsonElement> {
            override fun compare(element1: JsonElement?, element2: JsonElement?): Int {
                val j1Volume = element1?.asJsonObject?.get("chapterVolume")?.asFloat ?: return -1
                val j2Volume = element2?.asJsonObject?.get("chapterVolume")?.asFloat ?: return 1
                if (j1Volume.equals(j2Volume)) {
                    val j1ChapterNumber = element1.asJsonObject?.get("chapterNumber")?.asFloat ?: return -1
                    val j2ChapterNumber = element2.asJsonObject?.get("chapterNumber")?.asFloat ?: return 1
                    return j1ChapterNumber.compareTo(j2ChapterNumber)
                } else {
                    return j1Volume.compareTo(j2Volume)
                }
            }
        })

        chapters = ArrayList()
        sortedChaptersArray.forEachIndexed { index, jsonElement ->
            val jsonObject = jsonElement.asJsonObject ?: return@forEachIndexed
            val chapterPrefix = "Volume:${jsonObject["chapterVolume"]}, Chapter:${jsonObject["chapterNumber"]}"
            var chapterName = jsonObject["chapterName"].asJsonNullFreeString ?: ""
            if (chapterName != "") {
                chapterName += "\n$chapterPrefix"
            } else {
                chapterName = chapterPrefix
            }
            val chapterUrl = "https://${HostNames.NEOVEL}/read/$novelId/EN/${jsonObject["chapterId"]}"
            val webPage = WebPage(url = chapterUrl, chapter = chapterName)
            webPage.orderId = index.toLong()
            webPage.novelId = novel.id
            webPage.translatorSourceId = -1L
            chapters.add(webPage)
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return chapters
}
