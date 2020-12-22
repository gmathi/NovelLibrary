package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document


class CircusTranslationsCleaner : HtmlCleaner() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc, false)
        var contentElement = doc.body().getElementById("primary")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {
        return getLinkedChapters(doc.location(), doc.body().select("#primary").firstOrNull())
    }

}
