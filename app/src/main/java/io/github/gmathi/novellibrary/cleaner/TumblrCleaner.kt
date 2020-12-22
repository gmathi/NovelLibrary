package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document

class TumblrCleaner : HtmlCleaner() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("textpostbody") }

        if (contentElement == null) {
            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.id() == "content" }
        }

        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            cleanClassAndIds(contentElement)
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        cleanClassAndIds(contentElement)
        contentElement?.getElementsByClass("wpcnt")?.remove()
        contentElement?.getElementById("jp-post-flair")?.remove()
    }
}