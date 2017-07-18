package com.mgn.bingenovelreader.model

import java.io.Serializable

class Novel : Serializable {
    var id: Long = -1
    var name: String? = null
    var url: String? = null
    var author: String? = null
    var imageUrl: String? = null
    var rating: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
    var imageFilePath: String? = null
    var genres: List<String>? = null
    var currentWebPageId: Long = -1
    var chapterCount: String? = null
    var metaData: HashMap<String, String?> = HashMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val novel = other as Novel?
        return if (name != null) name == novel!!.name else novel!!.name == null
    }

    override fun hashCode(): Int {
        return if (name != null) name!!.hashCode() else 0
    }

    fun copyFrom(otherNovel: Novel?) {
        if (otherNovel != null) {
            id = if (otherNovel.id != -1L) otherNovel.id else id
            name = if (otherNovel.name != null) otherNovel.name else name
            metaData = otherNovel.metaData
            genres = if (otherNovel.genres != null) otherNovel.genres else genres
            rating = if (otherNovel.rating != null) otherNovel.rating else rating
            imageUrl = if (otherNovel.imageUrl != null) otherNovel.imageUrl else imageUrl
            longDescription = if (otherNovel.longDescription != null) otherNovel.longDescription else longDescription
        }
    }


}
