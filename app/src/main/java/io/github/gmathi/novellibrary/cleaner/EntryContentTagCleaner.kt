package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.net.URI

class EntryContentTagCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        removeDirectionalLinks(contentElement)

        do {
            contentElement?.siblingElements()?.remove()
            cleanClassAndIds(contentElement)
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        cleanClassAndIds(contentElement)
        doc.head().children().remove()
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

    override fun removeJS(doc: Document) {
        super.removeJS(doc)
        doc.getElementsByTag("noscript").remove()
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

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }


}