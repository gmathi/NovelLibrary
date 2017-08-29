package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class GeneralClassTagHelper(private val hostName: String, private val tagName: String, private val className: String) : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.head()?.getElementsByTag("style")?.remove()
        doc.head()?.getElementsByTag("link")?.remove()

        var contentElement = doc.body().getElementsByTag(tagName).firstOrNull { it.hasClass(className) }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        removeDirectionalLinks(contentElement)
        cleanCSSFromChildren(contentElement)

        do {
            contentElement?.siblingElements()?.remove()
            cleanClassAndIds(contentElement)
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        cleanClassAndIds(contentElement)
        contentElement?.getElementsByClass("wpcnt")?.remove()
        contentElement?.getElementById("jp-post-flair")?.remove()
        doc.getElementById("custom-background-css")?.remove()

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
        val url = doc.location()
        val links = ArrayList<String>()
        val otherLinks = doc.body().getElementsByTag(tagName).firstOrNull { it.hasClass(className) }?.getElementsByAttributeValueContaining("href", hostName)?.filter { !it.attr("href").contains(url) }
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links
    }

    private fun removeDirectionalLinks(contentElement: Element?) {
        contentElement?.getElementsByTag("a")?.filter {
            it.text().contains("Previous Chapter", ignoreCase = true)
                || it.text().contains("Next Chapter", ignoreCase = true)
                || it.text().contains("Project Page", ignoreCase = true)
                || it.text().contains("Index", ignoreCase = true)

        }?.forEach { it?.remove() }

    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }
}
