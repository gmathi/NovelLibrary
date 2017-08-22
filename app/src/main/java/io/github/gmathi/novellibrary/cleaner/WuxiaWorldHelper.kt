package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WuxiaWorldHelper : HtmlHelper() {


    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.getElementsByAttributeValue("itemprop", "articleBody").firstOrNull()
        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
        doc.getElementsByTag("a").filter { it.text() == "Next Chapter" || it.text() == "Previous Chapter" }.forEach { it.remove() }
        doc.getElementById("custom-background-css")?.remove()
    }

    override fun downloadCSS(doc: Document, downloadDir: File) {
        //no need to download any, remove them instead
        removeCSS(doc)
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        if (uri.toString().contains("uploads/avatars")) return null
        else return super.downloadImage(element, dir)
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {

        val links = ArrayList<String>()
        val otherLinks = doc.getElementsByAttributeValue("itemprop", "articleBody").firstOrNull()?.getElementsByAttributeValueContaining("href", HostNames.WUXIA_WORLD)
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links
    }

}
