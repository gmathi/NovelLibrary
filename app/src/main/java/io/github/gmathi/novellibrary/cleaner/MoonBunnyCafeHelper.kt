package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class MoonBunnyCafeHelper : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
//        contentElement?.child(0)?.remove()
//        contentElement?.child(contentElement.children().size - 1)?.remove()
//        //contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")

        contentElement?.getElementsByTag("a")?.firstOrNull {
            it.text().contains("Previous Chapter")
                || it.text().contains("Next Chapter")
                || it.text().contains("Project Page")
        }?.parent()?.remove()
        contentElement?.getElementsByTag("a")?.firstOrNull {
            it.text().contains("Previous Chapter")
                || it.text().contains("Next Chapter")
                || it.text().contains("Project Page")
        }?.parent()?.remove()

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

    override fun downloadCSS(doc: Document, downloadDir: File) {
        //super.downloadCSS(doc, downloadDir)
        removeCSS(doc)
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {

        val links = ArrayList<String>()
        val contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("entry-content") }
        val otherLinks = contentElement?.getElementsByAttributeValueContaining("href", HostNames.MOON_BUNNY_CAFE)
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links
    }


}