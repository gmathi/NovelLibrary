package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WordPressHelper : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
        //super.downloadCSS(doc, downloadDir)
        removeCSS(doc)
    }

    override fun additionalProcessing(doc: Document) {
        doc.head()?.getElementsByTag("link")?.remove()
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            cleanClassAndIds(contentElement)
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        cleanClassAndIds(contentElement)
        contentElement?.getElementsByClass("wpcnt")?.remove()
        contentElement?.getElementById("jp-post-flair")?.remove()
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        if (uri.toString().contains("uploads/avatars")) return null
        else return super.downloadImage(element, dir)
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
}
