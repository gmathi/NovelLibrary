package io.github.gmathi.novellibrary.model

import android.os.Parcel
import android.os.Parcelable

class Novel: Parcelable {
    constructor(name: String, url: String) {
        this.name = name
        this.url = url
    }

    var name: String
    var url: String
    var id: Long = -1
    var imageUrl: String? = null
    var rating: String? = null
    var shortDescription: String? = null
    var longDescription: String? = null
    var imageFilePath: String? = null
    var genres: List<String>? = null
//    var currentWebPageId: Long = -1

    var currentWebPageUrl: String? = null
    var orderId: Long = -1
    var newReleasesCount = 0L
    var chaptersCount = 0L
    var metaData: HashMap<String, String?> = HashMap()
    var novelSectionId: Long = -1L

    fun copyFrom(otherNovel: Novel?) {
        if (otherNovel != null) {
            name = otherNovel.name
            url = otherNovel.url
            id = if (otherNovel.id != -1L) otherNovel.id else id
            imageUrl = if (otherNovel.imageUrl != null) otherNovel.imageUrl else imageUrl
            rating = if (otherNovel.rating != null) otherNovel.rating else rating
            shortDescription = if (otherNovel.shortDescription != null) otherNovel.shortDescription else shortDescription
            longDescription = if (otherNovel.longDescription != null) otherNovel.longDescription else longDescription
            imageFilePath = if (otherNovel.imageFilePath != null) otherNovel.imageFilePath else imageFilePath
            genres = if (otherNovel.genres != null) otherNovel.genres else genres
//            currentWebPageId = if (otherNovel.currentWebPageId != -1L) otherNovel.currentWebPageId else currentWebPageId

            currentWebPageUrl = if (otherNovel.currentWebPageUrl != null) otherNovel.currentWebPageUrl else currentWebPageUrl
            orderId = if (otherNovel.orderId != -1L) otherNovel.orderId else orderId
            newReleasesCount = if (otherNovel.newReleasesCount != 0L) otherNovel.newReleasesCount else newReleasesCount
            chaptersCount = if (otherNovel.chaptersCount != 0L) otherNovel.chaptersCount else chaptersCount
            otherNovel.metaData.keys.forEach{
                metaData[it] = otherNovel.metaData[it]
            }
            novelSectionId = if (otherNovel.novelSectionId != -1L) otherNovel.novelSectionId else novelSectionId
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

    // region Parcelable
    constructor(parcel: Parcel) {
        name = parcel.readString()!!
        url = parcel.readString()!!
        id = parcel.readLong()
        imageUrl = parcel.readString()
        rating = parcel.readString()
        shortDescription = parcel.readString()
        longDescription = parcel.readString()
        imageFilePath = parcel.readString()
        genres = parcel.createStringArrayList()
        currentWebPageUrl = parcel.readString()
        orderId = parcel.readLong()
        newReleasesCount = parcel.readLong()
        chaptersCount = parcel.readLong()
        @Suppress("UNCHECKED_CAST")
        metaData = parcel.readSerializable() as HashMap<String, String?>
        novelSectionId = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(url)
        parcel.writeLong(id)
        parcel.writeString(imageUrl)
        parcel.writeString(rating)
        parcel.writeString(shortDescription)
        parcel.writeString(longDescription)
        parcel.writeString(imageFilePath)
        parcel.writeStringList(genres)
//        parcel.writeLong(currentWebPageId)

        parcel.writeString(currentWebPageUrl)
        parcel.writeLong(orderId)
        parcel.writeLong(newReleasesCount)
        parcel.writeLong(chaptersCount)
        parcel.writeSerializable(metaData)
        parcel.writeLong(novelSectionId)
    }

    override fun describeContents(): Int {
        return hashCode()
    }

    companion object CREATOR : Parcelable.Creator<Novel> {
        override fun createFromParcel(parcel: Parcel): Novel {
            return Novel(parcel)
        }

        override fun newArray(size: Int): Array<Novel?> {
            return arrayOfNulls(size)
        }
    }
    // endregion

}
