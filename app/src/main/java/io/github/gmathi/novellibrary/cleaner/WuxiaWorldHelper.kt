package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WuxiaWorldHelper : HtmlHelper() {

    val ROYAL_ROAD_SITE_THEME_ID = "siteThemeRoyalRoad"

    override fun downloadCSS(doc: Document, downloadDir: File) {
//        doc.head().append("<link id=\"$ROYAL_ROAD_SITE_THEME_ID\" href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
//        //doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css" }?.remove()
//        super.downloadCSS(doc, downloadDir)
//        doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "../Site.css" }
    }

    override fun cleanDoc(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasAttr("itemprop") && it.attr("itemprop") == "articleBody" }
        if (contentElement != null && contentElement.children().size >= 2) {
            contentElement.child(contentElement.children().size - 1).remove()
            contentElement.child(0).remove()
        }
        //contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        if (uri.toString().contains("uploads/avatars")) return null
        else return super.downloadImage(element, dir)
    }

    override fun removeJS(doc: Document) {
        super.removeJS(doc)
        doc.getElementsByTag("noscript").remove()
    }

    override fun addTitle(doc: Document) {


    }

//    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
//        val element = doc.head().getElementById(ROYAL_ROAD_SITE_THEME_ID)
//        if (element != null) {
//            element.removeAttr("href")
//            if (dataCenter.isDarkTheme)
//                element.attr("href", "../Site-dark.css")
//            else
//                element.attr("href", "../Site.css")
//        } else {
//            if (isDark)
//                doc.head().getElementsByTag("link").firstOrNull {
//                    it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css"
//                }?.attr("href", "/Content/Themes/Bootstrap/Site-dark.css")
//            else
//                doc.head().getElementsByTag("link").firstOrNull {
//                    it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site-dark.css"
//                }?.attr("href", "/Content/Themes/Bootstrap/Site.css")
//        }
//        return doc
//    }


}
