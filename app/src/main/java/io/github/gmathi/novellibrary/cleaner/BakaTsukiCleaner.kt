package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class BakaTsukiCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)

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

    }

    override fun getLinkedChapters(doc: Document): ArrayList<LinkedPage> {
        return getLinkedChapters(doc.location(), doc.body().select("div#content").firstOrNull())
    }

    private fun removeDirectionalLinks(contentElement: Element?) {
        contentElement?.getElementsByTag("a")?.filterIndexed { index, element ->
            element.text().contains("Previous Chapter", ignoreCase = true)
                    || element.text().contains("Next Chapter", ignoreCase = true)
                    || element.text().contains("Project Page", ignoreCase = true)
                    || element.text().contains("Index", ignoreCase = true)
        }?.forEach { it?.remove() }
        contentElement?.getElementsByTag("table")?.lastOrNull()?.remove()
    }
}