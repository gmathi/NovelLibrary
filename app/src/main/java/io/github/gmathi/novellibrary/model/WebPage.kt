package io.github.gmathi.novellibrary.model

import java.io.Serializable

class WebPage : Serializable {

    constructor() {
        //Empty Constructor
    }

    constructor(url: String, chapter: String) : super() {
        this.url = url
        this.chapter = chapter
    }

    constructor(url: String, chapter: String, pageData: String) : this(url, chapter) {
        this.pageData = pageData
    }

    var id: Long = -1L
    var url: String? = null
    var redirectedUrl: String? = null
    var title: String? = null
    var chapter: String? = null
    var filePath: String? = null
    var novelId: Long = -1L
    var pageData: String? = null
    var isRead: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as WebPage?
        return if (webPage != null) url == webPage.url else false
    }

    fun copyFrom(other: WebPage) {
        id = if (other.id != -1L) other.id else id
        url = if (other.url != null) other.url else url
        redirectedUrl = if (other.redirectedUrl != null) other.redirectedUrl else redirectedUrl
        title = if (other.title != null) other.title else title
        chapter = if (other.chapter != null) other.chapter else chapter
        filePath = if (other.filePath != null) other.filePath else filePath
        novelId = if (other.novelId != -1L) other.novelId else novelId
        pageData = if (other.pageData != null) other.pageData else pageData
        isRead = if (other.isRead != 0) other.isRead else isRead
    }

}
