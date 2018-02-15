package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class BakaTsukiCleaner : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.head()?.getElementsByTag("style")?.remove()
        doc.head()?.getElementsByTag("link")?.remove()

        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "content" }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")

        if (!dataCenter.enableDirectionalLinks)
            removeDirectionalLinks(contentElement)

        doc.getElementsByClass("post-navigation")?.remove()


        if (!dataCenter.showChapterComments) {
            doc.getElementsByClass("comments-container")?.remove()
            doc.getElementsByClass("respond-container")?.remove()
        }

        contentElement?.children()?.forEach {
            cleanCSSFromChildren(it)
        }

        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
            cleanClassAndIds(contentElement)
        } while (contentElement != null && contentElement.tagName() != "body")

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
        val otherLinks = doc.body().getElementsByTag("div").firstOrNull { it.id() == "content" }?.getElementsByAttributeValueContaining("href", HostNames.BAKA_TSUKI)?.filter { !it.attr("href").contains(url) }
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
        contentElement?.getElementsByTag("table")?.lastOrNull()?.remove()
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }
}