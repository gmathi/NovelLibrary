package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class RoyalRoadHelper : HtmlHelper() {

//    override fun downloadCSS(doc: Document, downloadDir: File) {
//
//        //doc.head().getElementsByAttributeValueContaining("href", "/main-bundle")?.remove()
////        doc.head().append("<link href=\"/Content/Themes/Bootstrap/Site.css\" rel=\"stylesheet\">")
////        doc.head().append("<link href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
////        doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css" }?.remove()
//        super.downloadCSS(doc, downloadDir)
//    }

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.head()?.getElementsByTag("style")?.remove()
        doc.head()?.getElementsByTag("link")?.remove()

        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
        contentElement?.getElementsByAttributeValue("id", "title")?.remove()
        contentElement?.prepend("<h4 id='title'>${getTitle(doc)}</h4><br>")


        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

//    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
//
//        //doc.head().getElementsByAttributeValueContaining("href", "/Content/Themes/Styles/main-bundle.min.css")?.remove()
//        //doc.head().append("<link href=\"../main-bundle.min.css\" rel=\"stylesheet\">")?.remove()
//        doc.head().getElementsByAttributeValueContaining("href", "/Content/Themes/Bootstrap/Site-dark.css")?.remove()
//        doc.head().getElementsByAttributeValueContaining("href", "/Content/Themes/Bootstrap/Site.css")?.remove()
//        doc.head().getElementsByAttributeValueContaining("href", "../Site-dark.css")?.remove()
//        doc.head().getElementsByAttributeValueContaining("href", "../Site.css")?.remove()
//
//        if (isDark) {
//            doc.head().append("<link href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
//            doc.head().append("<link href=\"../Site-dark.css\" rel=\"stylesheet\">")
//
//        } else {
//            doc.head().append("<link href=\"/Content/Themes/Bootstrap/Site.css\" rel=\"stylesheet\">")
//            doc.head().append("<link href=\"../Site.css\" rel=\"stylesheet\">")
//
//        }
//
//        return doc
//
//    }

}