package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WuxiaWorldHelper : HtmlHelper() {


    override fun downloadCSS(doc: Document, downloadDir: File) {
        super.downloadCSS(doc, downloadDir)
    }

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasAttr("itemprop") && it.attr("itemprop") == "articleBody" }
        if (contentElement != null && contentElement.children().size >= 2) {
            contentElement.child(contentElement.children().size - 1).remove()
            contentElement.child(0).remove()
        }
        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
        doc.getElementById("custom-background-css")?.remove()
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

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        if (isDark) {
            doc.head().getElementById("darkTheme")?.remove()
            doc.head().append("<style id=\"darkTheme\">" +
                "body { background-color:#131313; color:rgba(255, 255, 255, 0.8); font-family: 'Open Sans',sans-serif; line-height: 1.5; padding:20px;} </style> ")
        } else {
            doc.head().getElementById("darkTheme")?.remove()
            doc.head().append("<style id=\"darkTheme\">" +
                "body { background-color:rgba(255, 255, 255, 0.8); color:#131313; font-family: 'Open Sans',sans-serif; line-height: 1.5; padding:20px;} </style> ")
        }

        return doc
    }

    override fun addTitle(doc: Document) {
    }

}
