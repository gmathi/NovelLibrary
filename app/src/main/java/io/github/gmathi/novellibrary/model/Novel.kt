package io.github.gmathi.novellibrary.model

import java.io.Serializable

class Novel(var name: String, var url: String) : Serializable {
    var id: Long = -1
    var imageUrl: String? = null
    var rating: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
    var imageFilePath: String? = null
    var genres: List<String>? = null
    var currentWebPageId: Long = -1
    var orderId: Long = -1
    var chapterCount = 0L
    var newChapterCount = 0L
    var metaData: HashMap<String, String?> = HashMap()


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
            currentWebPageId = if (otherNovel.currentWebPageId != -1L) otherNovel.currentWebPageId else currentWebPageId
            chapterCount = if (otherNovel.chapterCount != 0L) otherNovel.chapterCount else chapterCount
            newChapterCount = if (otherNovel.newChapterCount != 0L) otherNovel.newChapterCount else newChapterCount
            orderId = if (otherNovel.orderId != -1L) otherNovel.orderId else orderId

            otherNovel.metaData.keys.forEach {
                metaData.put(it, otherNovel.metaData[it])
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


}
