package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document


class WuxiaWorldCleaner : HtmlCleaner() {


    override fun additionalProcessing(doc: Document) {
        removeCSS(doc, false)
        var contentElement = doc.selectFirst("div.p-15")
        contentElement?.selectFirst("div.font-resize")?.remove()
        cleanCSSFromChildren(contentElement)

        do {
            cleanClassAndIds(contentElement)
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }

        if (!dataCenter.enableDirectionalLinks)
            doc.getElementsByTag("a")?.filter { it.text() == "Next Chapter" || it.text() == "Previous Chapter" }?.forEach { it.remove() }
    }

    override fun getLinkedChapters(doc: Document): ArrayList<LinkedPage> {

        val links = ArrayList<String>()
        val otherLinks = doc.selectFirst("div.fr-view")?.getElementsByAttributeValueContaining("href", HostNames.WUXIA_WORLD)
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links.mapTo(ArrayList()) { LinkedPage(it, it) }
    }
}
