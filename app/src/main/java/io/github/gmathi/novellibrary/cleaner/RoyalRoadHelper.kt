package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class RoyalRoadHelper : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
        doc.head().append("<link href=\"/Content/Themes/Bootstrap/Site-dark.css\" rel=\"stylesheet\">")
        //doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css" }?.remove()
        super.downloadCSS(doc, downloadDir)
        doc.head().getElementsByTag("link").firstOrNull { it.hasAttr("href") && it.attr("href") == "../Site.css" }
    }

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("chapter-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
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

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        if (isDark) {
            doc.head().getElementsByTag("link").firstOrNull {
                it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site.css"
            }?.attr("href", "/Content/Themes/Bootstrap/Site-dark.css")

            doc.head().getElementsByTag("link").firstOrNull {
                it.hasAttr("href") && it.attr("href") == "../Site.css"
            }?.attr("href", "../Site-dark.css")

        } else {
            doc.head().getElementsByTag("link").firstOrNull {
                it.hasAttr("href") && it.attr("href") == "/Content/Themes/Bootstrap/Site-dark.css"
            }?.attr("href", "/Content/Themes/Bootstrap/Site.css")

            doc.head().getElementsByTag("link").firstOrNull {
                it.hasAttr("href") && it.attr("href") == "../Site-dark.css"
            }?.attr("href", "../Site.css")
        }

        return doc

    }
}