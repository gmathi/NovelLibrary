package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class BlueSilverTranslationsHelper : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
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

        //Convert the iframe to link
        val iFrames = doc.getElementsByTag("iframe")
        iFrames?.forEach {
            it.parent().append("Chapter(?) Link - <a href=\"${it.attr("src")}\" style=\"word-wrap:break-word;\">${it.attr("src")}</a>")
            it.remove()
        }

    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {

        val links = ArrayList<String>()
        val otherLinks = doc.getElementsByAttributeValue("itemprop", "articleBody").firstOrNull()?.getElementsByAttributeValueContaining("href", HostNames.WORD_PRESS)
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        val otherLinks2 = doc.getElementsByAttributeValueContaining("src", "docs.google.com")
        if (otherLinks2 != null && otherLinks2.isNotEmpty()) {
            otherLinks2.mapTo(links) { it.attr("src") }
        }
        val otherLinks3 = doc.getElementsByAttributeValueContaining("href", "docs.google.com")
        if (otherLinks3 != null && otherLinks3.isNotEmpty()) {
            otherLinks3.mapTo(links) { it.attr("href") }
        }
        return links
    }
}