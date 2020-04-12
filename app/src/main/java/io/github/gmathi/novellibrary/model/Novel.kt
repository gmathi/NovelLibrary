package io.github.gmathi.novellibrary.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Novel(@SerializedName("name") var name: String, @SerializedName("url") var url: String) : Serializable {
    var id: Long = -1L
    @SerializedName("imageUrl") var imageUrl: String? = null
    var rating: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
    var imageFilePath: String? = null
    var genres: List<String>? = null
//    var currentWebPageId: Long = -1

    @SerializedName("currentlyReading") var currentWebPageUrl: String? = null
    var orderId: Long = -1L
    var newReleasesCount = 0L
    var chaptersCount = 0L
    @SerializedName("metaData") var metaData: HashMap<String, String?> = HashMap()
    @SerializedName("novelSectionId") var novelSectionId: Long = -1L


    fun copyFrom(otherNovel: Novel?) {
        if (otherNovel != null) {
            id = if (otherNovel.id != -1L) otherNovel.id else id
            url = otherNovel.url
            name = otherNovel.name
            genres = if (otherNovel.genres != null) otherNovel.genres else genres
            rating = if (otherNovel.rating != null) otherNovel.rating else rating
            imageUrl = if (otherNovel.imageUrl != null) otherNovel.imageUrl else imageUrl
            imageFilePath = if (otherNovel.imageFilePath != null) otherNovel.imageFilePath else imageFilePath
            longDescription = if (otherNovel.longDescription != null) otherNovel.longDescription else longDescription
            shortDescription = if (otherNovel.shortDescription != null) otherNovel.shortDescription else shortDescription
//            currentWebPageId = if (otherNovel.currentWebPageId != -1L) otherNovel.currentWebPageId else currentWebPageId
            currentWebPageUrl = if (otherNovel.currentWebPageUrl != null) otherNovel.currentWebPageUrl else currentWebPageUrl
            newReleasesCount = if (otherNovel.newReleasesCount != 0L) otherNovel.newReleasesCount else newReleasesCount
            chaptersCount = if (otherNovel.chaptersCount != 0L) otherNovel.chaptersCount else chaptersCount
            orderId = if (otherNovel.orderId != -1L) otherNovel.orderId else orderId
            novelSectionId = if (otherNovel.novelSectionId != -1L) otherNovel.novelSectionId else novelSectionId

            otherNovel.metaData.keys.forEach {
                metaData[it] = otherNovel.metaData[it]
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Novel

        if (name != other.name) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + url.hashCode()
        return result
    }

    override fun toString(): String {
        return "Novel(name='$name', url='$url', id=$id, imageUrl=$imageUrl, rating=$rating, shortDescription=$shortDescription, longDescription=$longDescription, imageFilePath=$imageFilePath, genres=$genres, currentWebPageUrl=$currentWebPageUrl, orderId=$orderId, newReleasesCount=$newReleasesCount, chaptersCount=$chaptersCount, metaData=$metaData, novelSectionId=$novelSectionId)"
    }
}
