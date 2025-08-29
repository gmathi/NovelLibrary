package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class GoogleDocsCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        cleanAttributes(doc.body())
    }

    private fun cleanAttributes(contentElement: Element?) {
        if (contentElement?.tagName() != "a" || contentElement.tagName() != "img")
            contentElement?.clearAttributes()
        contentElement?.children()?.forEach {
            cleanAttributes(it)
        }
    }
}