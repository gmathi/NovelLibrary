package io.github.gmathi.novellibrary.model.database

import java.io.Serializable

data class WebPageSettings(var url: String, var novelId: Long) : Serializable {

    var title: String? = null
    var isRead: Int = 0
    var filePath: String? = null
    var redirectedUrl: String? = null
    var metadata: HashMap<String, String?> = HashMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as WebPageSettings?
        return if (webPage != null) url == webPage.url else false
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + novelId.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + isRead
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (redirectedUrl?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebPageSettings(url='$url', novelId=$novelId, title=$title, isRead=$isRead, filePath=$filePath, redirectedUrl=$redirectedUrl, metadata=$metadata)"
    }

}
