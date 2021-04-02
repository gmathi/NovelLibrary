package io.github.gmathi.novellibrary.source

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.NovelsPage
import io.github.gmathi.novellibrary.model.other.SelectorQuery
import io.github.gmathi.novellibrary.model.source.filter.FilterList
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.NEOVEL_API_URL
import io.github.gmathi.novellibrary.util.DataCenter
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_IMPLEMENTATION
import io.github.gmathi.novellibrary.util.Exceptions.NETWORK_ERROR
import io.github.gmathi.novellibrary.util.lang.asJsonNullFreeString
import io.github.gmathi.novellibrary.util.system.encodeBase64ToString
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response


class NeovelSource : HttpSource() {

    override val id: Long
        get() = Constants.SourceId.NEOVEL
    override val baseUrl: String
        get() = NEOVEL_API_URL
    override val lang: String
        get() = "en"
    override val supportsLatest: Boolean
        get() = true
    override val name: String
        get() = "Neovel"

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", USER_AGENT)
        .add("Referer", baseUrl)

    //region Search Novel
    override fun searchNovelsRequest(page: Int, query: String, filters: FilterList): Request {
        val url =
            baseUrl + "V2/books/search?language=ALL&filter=0&name=${query.encodeBase64ToString()}&sort=6&page=${page - 1}&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=4000"
        return GET(url, headers = headers)
    }

    override fun searchNovelsParse(response: Response): NovelsPage {
        val searchResults: ArrayList<Novel> = ArrayList()
        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val jsonArray = JsonParser.parseString(jsonString).asJsonArray

        jsonArray.forEach { result ->
            val resultObject = result.asJsonObject
            val id = resultObject["id"].asInt.toString()
            val novelUrl = baseUrl + "V1/book/details?bookId=$id&language=EN"
            val novel = Novel(novelUrl, this.id)
            resultObject["name"].asJsonNullFreeString?.let { novel.name = it }
            novel.imageUrl = baseUrl + "V2/book/image?bookId=$id&oldApp=false"
            novel.metadata["id"] = id
            novel.externalNovelId = id
            novel.rating = resultObject["rating"].asFloat.toString()
            searchResults.add(novel)
        }

        return NovelsPage(searchResults, false)
    }

    //endregion

    //region Novel Details
    override fun novelDetailsParse(novel: Novel, response: Response): Novel {
        val jsonString = response.body?.string() ?: return novel
        val rootJsonObject = JsonParser.parseString(jsonString)?.asJsonObject ?: return novel
        val id = rootJsonObject["id"].asInt.toString()

        novel.imageUrl = "https://${HostNames.NEOVEL}/V2/book/image?bookId=$id&oldApp=false"
        novel.longDescription = rootJsonObject.get("bookDescription")?.asString
        novel.rating = rootJsonObject["rating"].asFloat.toString()
        novel.chaptersCount = rootJsonObject["nbrReleases"].asLong
        novel.externalNovelId = id
        novel.metadata["id"] = id

        //If local fetch copy is empty, then get it from network
        if (neovelGenres == null) {
            getNeovelGenres()
        }
        neovelGenres?.let { map ->
            novel.genres = rootJsonObject.getAsJsonArray("genreIds")?.filter { map[it.asInt] != null }?.map { map[it.asInt]!! }
            novel.metadata["Genre(s)"] = rootJsonObject.getAsJsonArray("tagIds")?.filter { map[it.asInt] != null }?.joinToString(", ") { map[it.asInt]!! }
        }

        //If local fetch copy is empty, then get it from network
        if (neovelTags == null) {
            getNeovelTags()
        }
        neovelTags?.let { map ->
            novel.metadata["Tag(s)"] = rootJsonObject.getAsJsonArray("tagIds")?.filter { map[it.asInt] != null }?.joinToString(", ") { map[it.asInt]!! }
        }

        novel.metadata["Author(s)"] = rootJsonObject.getAsJsonArray("authors")?.joinToString(", ") {
            it.asJsonNullFreeString ?: ""
        }
        novel.metadata["Status"] = rootJsonObject["bookState"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Release Frequency"] = rootJsonObject["releaseFrequency"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Chapter Read Count"] = rootJsonObject["chapterReadCount"]?.asInt.toString()
        novel.metadata["Followers"] = rootJsonObject["followers"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Initial publish date"] = rootJsonObject["votes"]?.asJsonNullFreeString ?: "N/A"
        novel.metadata["Votes"] = rootJsonObject["origin_loc"]?.asJsonNullFreeString ?: "N/A"
        return novel
    }
    //endregion

    //region Chapters

    override fun chapterListRequest(novel: Novel): Request {
        val novelId = novel.externalNovelId ?: novel.metadata["id"]
        val url = baseUrl + "V5/chapters?bookId=$novelId&language=EN"
        return GET(url, headers)
    }

    override fun chapterListParse(novel: Novel, response: Response): List<WebPage> {


        val jsonString = response.body?.string() ?: throw Exception(NETWORK_ERROR)
        val releasesArray = JsonParser.parseString(jsonString)?.asJsonArray
            ?: throw Exception(NETWORK_ERROR)

        var orderId = 0L
        val chapters = ArrayList<WebPage>()
        val neovelChaptersArray =  Gson().fromJson(releasesArray.toString(), Array<NeovelChapter>::class.java)

        neovelChaptersArray.sortedWith(Comparator<NeovelChapter> { o1, o2 ->
            val volumeDifference = (o1.chapterVolume * 100).toInt() - (o2.chapterVolume * 100).toInt()
            if (volumeDifference != 0) volumeDifference //returns the volume difference
            val chapterDifference = (o1.chapterNumber * 100).toInt() - (o2.chapterNumber * 100).toInt()
            chapterDifference //else returns the chapter difference
        }).forEach {
            val url = "${baseUrl}read/${it.bookId}/${it.language}/${it.chapterId}"
            val chapterName = arrayListOf(it.chapterVolume, it.chapterNumber, it.chapterName ?: "").filter { name -> name.toString().isNotBlank() }.joinToString(" - ")
            val chapter = WebPage(url, chapterName)
            chapter.orderId = orderId++
            chapter.novelId = novel.id
            chapter.translatorSourceName = it.websiteName
            chapters.add(chapter)
        }

        return chapters
    }
    //endregion

    //region Genres & Tags
    private fun getNeovelGenres() {
        try {

            val request = GET(baseUrl + "V1/genres", headers)
            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: return

            val jsonArray = JsonParser.parseString(jsonString)?.asJsonArray
            neovelGenres = HashMap()
            jsonArray?.forEach {
                val genreObject = it.asJsonObject
                neovelGenres!![genreObject["id"].asInt] = genreObject["en"].asString
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getNeovelTags() {
        try {

            val request = GET(baseUrl + "V1/tags", headers)
            val response = client.newCall(request).execute()
            val jsonString = response.body?.string() ?: return

            val jsonArray = JsonParser.parseString(jsonString)?.asJsonArray
            neovelTags = HashMap()
            jsonArray?.forEach {
                val tagObject = it.asJsonObject
                neovelTags!![tagObject["id"].asInt] = tagObject["en"].asString
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
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

        // Below is to cache Neovel Genres & Tags
        private var neovelGenres: HashMap<Int, String>? = null
        private var neovelTags: HashMap<Int, String>? = null
    }

    private class NeovelChapter(
        @SerializedName("chapterId") val chapterId: Long,
        @SerializedName("bookId") val bookId: Long,
        @SerializedName("chapterName")  val chapterName: String?,
        @SerializedName("chapterVolume") val chapterVolume: Double,
        @SerializedName("chapterNumber") val chapterNumber: Double,
        @SerializedName("chapterUrl") val chapterUrl: String?,
        @SerializedName("trueUrl") val trueUrl: String?,
        @SerializedName("websiteName") val websiteName: String?,
        @SerializedName("postDate") val postDate: String?,
        @SerializedName("downloadable") val downloadable: Boolean,
        @SerializedName("language") val language: String?,
        @SerializedName("alreadyRead") val alreadyRead: Boolean,
        @SerializedName("state") val state: Long,
        @SerializedName("premiumAccess") val premiumAccess: Boolean
    )

}