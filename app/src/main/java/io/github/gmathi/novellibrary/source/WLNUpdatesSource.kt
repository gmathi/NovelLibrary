package io.github.gmathi.novellibrary.source

import com.google.gson.JsonParser
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.WLN_UPDATES_API_URL
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.Exceptions.NETWORK_ERROR
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import io.github.gmathi.novellibrary.util.lang.covertJsonNull
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.net.URL
import java.net.URLEncoder


class WLNUpdatesSource : HttpSource() {

    override val id: Long
        get() = Constants.SourceId.WLN_UPDATES
    override val baseUrl: String
        get() = WLN_UPDATES_API_URL
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "WLN Updates"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val json = """ {
            "title": "$query",
            "mode": "search-title"
            } """
        val body = json.toRequestBody("application/json".toMediaType())
        return POST(baseUrl, headers, body)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val searchResults: ArrayList<Novel> = ArrayList()
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        val dataObject = jsonObject["data"].asJsonObject
        val resultsArray = dataObject["results"].asJsonArray
        resultsArray.forEach { result ->
            val resultObject = result.asJsonObject
            val allMatchesArray = resultObject["match"].asJsonArray
            val sid = resultObject["sid"].asInt
            val novelUrl = "https://www.${HostNames.WLN_UPDATES}/series-id/$sid/"

            allMatchesArray.forEach { match ->
                val matchArray = match.asJsonArray
                val novelName = matchArray[1].asString
                searchResults.add(Novel(novelName, novelUrl, id))
            }
        }
        return NovelsPage(searchResults, false)
    }

    //endregion

    //region Novel Details
    override fun novelDetailsRequest(novel: Novel): Request {
        val novelId = URL(novel.url).path.split("/").last { it.isNotEmpty() }.toInt()
        val json = """ {
            "mode": "get-series-id",
            "id": $novelId
        } """

        val body = json.toRequestBody("application/json".toMediaType())

        return POST(baseUrl, headers, body)
    }

    override fun novelDetailsParse(novel: Novel, response: Response): Novel {
        val jsonString = response.body?.string() ?: return novel
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject?.getAsJsonObject("data")
            ?: return novel

        novel.imageUrl = rootJsonObject.getAsJsonArray("covers")?.firstOrNull()?.asJsonObject?.get("url")?.asJsonNullFreeString
        novel.longDescription = rootJsonObject.get("description")?.asJsonNullFreeString?.replace("<p>", "\n")?.replace("</p>", "")
        novel.genres = rootJsonObject.getAsJsonArray("genres")?.mapNotNull { it.asJsonObject.get("genre").asJsonNullFreeString }
        novel.chaptersCount = rootJsonObject.getAsJsonArray("releases")?.count()?.toLong() ?: 0L
        var rating = 0.0
        try {
            rating = rootJsonObject.getAsJsonObject("rating")?.get("avg")?.asDouble ?: 0.0
        } catch (e: Exception) {
            //Do Nothing
        }
        novel.rating = String.format("%.1f", (rating / 2))

        novel.metadata["Author(s)"] = rootJsonObject.getAsJsonArray("authors")?.joinToString(", ") { author ->
            val authorObject = author.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/author-id/${authorObject["id"]}\">${authorObject["author"].asJsonNullFreeString}</a>"
        }
        novel.metadata["Illustrator(s)"] = rootJsonObject.getAsJsonArray("illustrators")?.joinToString(", ") { illustrator ->
            val illustratorObject = illustrator.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/artist-id/${illustratorObject["id"]}\">${illustratorObject["illustrator"].asJsonNullFreeString}</a>"
        }
        novel.metadata["Publisher(s)"] = rootJsonObject.getAsJsonArray("publishers")?.joinToString(", ") { publisher ->
            val publisherObject = publisher.asJsonObject
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/publishers/${publisherObject["id"]}\">${publisherObject["publisher"].asJsonNullFreeString}</a>"
        }

        //Genre - Sample Url: https://www.wlnupdates.com/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22genre-category%22%3A%20%7B%22action%22%3A%20%22included%22%7D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22title-search-text%22%3A%20%22%22%7D
        novel.metadata["Genre(s)"] = rootJsonObject.getAsJsonArray("genres")?.joinToString(", ") { genreObject ->
            val genre = genreObject.asJsonObject["genre"].asJsonNullFreeString
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22genre-category%22%3A%20%7B%22${genre}%22%3A%20%22included%22%7D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22title-search-text%22%3A%20%22%22%7D\">${genre}</a>"
        }

        //Tag - Sample Url: https://www.wlnupdates.com/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22tag-category%22%3A%20%7B%22reincarnated-into-another-world%22%3A%20%22included%22%7D%2C%20%22title-search-text%22%3A%20%22%22%7D
        novel.metadata["Tags"] = rootJsonObject.getAsJsonArray("tags")?.joinToString(", ") { tagObject ->
            val tag = tagObject.asJsonObject["tag"].asJsonNullFreeString
            "<a href=\"https://www.${HostNames.WLN_UPDATES}/search?json=%20%7B%22chapter-limits%22%3A%20%5B0%2C%20false%5D%2C%20%22series-type%22%3A%20%7B%7D%2C%20%22sort-mode%22%3A%20%22update%22%2C%20%22tag-category%22%3A%20%7B%22${tag}%22%3A%20%22included%22%7D%2C%20%22title-search-text%22%3A%20%22%22%7D\">${tag}</a>"
        }


        novel.metadata["Demographic"] = rootJsonObject["demographic"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Homepage"] = rootJsonObject["website"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Type"] = rootJsonObject["type"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["OEL/Translated"] = rootJsonObject["tl_type"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Initial publish date"] = rootJsonObject["pub_date"]?.asJsonNullFreeString
            ?: "N/A"
        novel.metadata["Country of Origin"] = rootJsonObject["origin_loc"]?.asJsonNullFreeString
            ?: "N/A"
        novel.metadata["Status in Country of Origin"] = rootJsonObject["orig_status"]?.asJsonNullFreeString
            ?: "N/A"
        novel.metadata["Licensed (in English)"] = rootJsonObject["license_en"]?.asJsonNullFreeString
            ?: "N/A"
        novel.metadata["Alternate Names"] = rootJsonObject.getAsJsonArray("alternatenames")?.joinToString(", ") { it.asJsonNullFreeString ?: "" }
        novel.metadata["Language"] = rootJsonObject["orig_lang"]?.asJsonNullFreeString ?: "N/A"

        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListRequest(novel: Novel): Request {
        val novelId = URL(novel.url).path.split("/").last { it.isNotEmpty() }.toInt()
        val json = """ {
            "mode": "get-series-id",
            "id": $novelId
        } """
        val body = json.toRequestBody("application/json".toMediaType())
        return POST(baseUrl, headers, body)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject?.getAsJsonObject("data")
            ?: throw Exception(NETWORK_ERROR)
        val releasesArray = rootJsonObject.getAsJsonArray("releases")

        var orderId = 0L
        val chapters = ArrayList<WebPage>()
        releasesArray.reversed().asSequence().forEach { release ->
            val releaseObject = release.asJsonObject
            val chapterNumber = releaseObject["chapter"].covertJsonNull?.asInt?.toString() ?: ""
            val fragment = releaseObject["fragment"].covertJsonNull?.asInt?.toString() ?: ""
            val postFix = releaseObject["postfix"].asJsonNullFreeString ?: ""
            val url = releaseObject["srcurl"].asJsonNullFreeString
            val sourceName = releaseObject["tlgroup"].covertJsonNull?.asJsonObject?.get("name")?.asJsonNullFreeString

            url?.let {
                val chapterName = arrayListOf(chapterNumber, fragment, postFix).filter { name -> name.isNotBlank() }.joinToString(" - ")
                val chapter = WebPage(it, chapterName)
                chapter.orderId = orderId++
                chapter.novelId = novel.id
                chapter.translatorSourceName = sourceName
                chapters.add(chapter)
            }
        }
        return chapters
    }
    //endregion

    //region stubs
    override fun popularNovelsRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun popularNovelsParse(response: Response): NovelsPage = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesRequest(page: Int): Request = throw Exception(MISSING_IMPLEMENTATION)
    override fun latestUpdatesParse(response: Response): NovelsPage = throw Exception(MISSING_IMPLEMENTATION)

    //endregion

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.193 Safari/537.36"
    }
}