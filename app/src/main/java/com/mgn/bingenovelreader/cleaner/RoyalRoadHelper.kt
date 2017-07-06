package com.mgn.bingenovelreader.cleaner

import com.mgn.bingenovelreader.util.Constants
import org.jsoup.nodes.Document
import java.io.File


class RoyalRoadHelper : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
        doc.head().append("<link id=\"${Constants.ROYAL_ROAD_SITE_THEME_ID}\" href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
        //doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css" }?.remove()
        super.downloadCSS(doc, downloadDir)
        doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "../Site.css" }
    }

    override fun cleanDoc(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }

    override fun addTitle(doc: Document) {

    }

}
