package com.mgn.bingenovelreader.models

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

}
