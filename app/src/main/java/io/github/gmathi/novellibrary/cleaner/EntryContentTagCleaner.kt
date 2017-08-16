package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.net.URI

class EntryContentTagCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        removeDirectionalLinks(contentElement)

        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
        doc.head().children().remove()
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

    override fun downloadCSS(doc: Document, downloadDir: File) {
        removeCSS(doc)
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {
        val links = ArrayList<String>()
        try {
            val host = URI(doc.location()).host
            val contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
            removeDirectionalLinks(contentElement)
            val otherLinks = contentElement?.getElementsByAttributeValueContaining("href", host)
            if (otherLinks != null && otherLinks.isNotEmpty()) {
                otherLinks.mapTo(links) { it.attr("href") }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return links
    }

    private fun removeDirectionalLinks(contentElement: Element?) {
        contentElement?.getElementsByTag("a")?.filter {
            it.text().contains("Previous Chapter")
                || it.text().contains("Next Chapter")
                || it.text().contains("Project Page")
                || it.text().contains("Index")

        }?.forEach { it?.remove() }

    }


}