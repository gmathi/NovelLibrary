package io.github.gmathi.novellibrary.model.realm

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects


open class RWebPage : RealmObject() {

    var url: String? = null
    var redirectedUrl: String? = null
    var title: String? = null
    var chapter: String? = null
    var filePath: String? = null
    var novelId: Long = -1L
    var source: String? = null
    var isRead: Int = 0
    var orderId: Long = -1L

    @LinkingObjects("webPages")
    val novels: RealmResults<RNovel>? = null
    var metaDatas: RealmList<RMetadata> = RealmList()


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as RWebPage?
        return if (webPage != null) url == webPage.url else false
    }

    fun copyFrom(other: RWebPage?) {
        if (other != null) {
            url = if (other.url != null) other.url else url
            redirectedUrl = if (other.redirectedUrl != null) other.redirectedUrl else redirectedUrl
            title = if (other.title != null) other.title else title
            chapter = if (other.chapter != null) other.chapter else chapter
            filePath = if (other.filePath != null) other.filePath else filePath
            novelId = if (other.novelId != -1L) other.novelId else novelId
            source = if (other.source != null) other.source else source
            isRead = if (other.isRead != 0) other.isRead else isRead
            orderId = if (other.orderId != -1L) other.orderId else orderId
        }
    }

}