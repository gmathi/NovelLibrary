package com.mgn.bingenovelreader.cleaner

import org.jsoup.nodes.Document


class RoyalRoadHelper : HtmlHelper() {

    override fun cleanDoc(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }

}
