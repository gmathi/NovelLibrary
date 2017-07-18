package com.mgn.bingenovelreader.model

class WebPage {

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

    var id: Long? = -1
    var url: String? = null
    var redirectedUrl: String? = null
    var title: String? = null
    var chapter: String? = null
    var filePath: String? = null
    var novelId: Long = 0
    var pageData: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as WebPage?
        return if (webPage != null) id == webPage.id else false
    }

    fun copyFrom(other: WebPage) {
        id = other.id
        url = other.url
        redirectedUrl = other.redirectedUrl
        title = other.title
        chapter = other.chapter
        filePath = other.filePath
        novelId = other.novelId
        pageData = other.pageData
    }

}
