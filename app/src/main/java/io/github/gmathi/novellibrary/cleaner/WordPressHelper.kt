package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WordPressHelper : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
        super.downloadCSS(doc, downloadDir)
    }

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
        contentElement.getElementsByClass("wpcnt")?.remove()
        contentElement.getElementById("jp-post-flair")?.remove()
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

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        if (isDark) {
            doc.head().append("<style id=\"darkTheme\">" +
                "body { background-color:#131313; color:rgba(255, 255, 255, 0.8); } </style> ")
        } else {
            doc.head().getElementById("darkTheme")?.remove()
        }

        return doc
    }
}
