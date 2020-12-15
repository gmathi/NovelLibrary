//package io.github.gmathi.novellibrary.model.realm
//
//import io.realm.RealmList
//import io.realm.RealmObject
//import io.realm.annotations.Index
//import io.realm.annotations.PrimaryKey
//
//
//open class RNovel : RealmObject() {
//
//    @Index
//    var name: String = ""
//
//    @PrimaryKey
//    var url: String = ""
//
//    var imageUrl: String? = null
//    var rating: String? = null
//    var longDescription: String? = null
//    var imageFilePath: String? = null
//    var currentChapterUrl: String? = null
//    var orderId: Long = -1
//    var newReleasesCount = 0L
//    var updatedChapterCount = 0L
//
//    var genres: RealmList<RGenre>? = RealmList()
//    var webPages: RealmList<RWebPage>? = RealmList()
//    var metadata: RealmList<RMetadata>? = RealmList()
//
//
//    fun copyFrom(otherNovel: RNovel?) {
//        if (otherNovel != null) {
//            url = if (otherNovel.url != "") otherNovel.url else url
//            name = if (otherNovel.name != "") otherNovel.name else name
//            rating = if (otherNovel.rating != null) otherNovel.rating else rating
//            imageUrl = if (otherNovel.imageUrl != null) otherNovel.imageUrl else imageUrl
//            imageFilePath = if (otherNovel.imageFilePath != null) otherNovel.imageFilePath else imageFilePath
//            longDescription = if (otherNovel.longDescription != null) otherNovel.longDescription else longDescription
//            currentChapterUrl = if (otherNovel.currentChapterUrl != null) otherNovel.currentChapterUrl else currentChapterUrl
//            newReleasesCount = if (otherNovel.newReleasesCount != 0L) otherNovel.newReleasesCount else newReleasesCount
//            updatedChapterCount = if (otherNovel.updatedChapterCount != 0L) otherNovel.updatedChapterCount else updatedChapterCount
//            orderId = if (otherNovel.orderId != -1L) otherNovel.orderId else orderId
//        }
//    }
//
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as RNovel
//
//        if (url != other.url) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        return url.hashCode()
//    }
//
//
//}