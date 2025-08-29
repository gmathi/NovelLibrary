package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.other.LinkedPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WattPadCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {
    override fun additionalProcessing(doc: Document) {
        removeCSS(doc, false)

        // Get important elements
        val titleElem = doc.select("header h2").firstOrNull()
        val articleElem = doc.select("main article").firstOrNull()
        val preElem = doc.select("div.page pre").firstOrNull()
        val contentElem = preElem?.parent()

        // remove most intrusions
        titleElem?.siblingElements()?.remove() // metadata after title
        preElem?.select("span.comment-marker")?.remove() // comments??
        preElem?.unwrap() // move contents of pre to its parent (pre is monospace with no wrapping)
        articleElem?.children()?.filterIndexed { _, ele -> !ele.hasClass("container") }?.forEach { it.remove() } // novel info and next page or something

        // remove ALL other unneeded elements (the nuclear option)
        var it: Element? = articleElem
        while (it != null) {
            val parent = it.parent()
            it.siblingElements()?.remove()
            it.unwrap(); it = parent
            if (it == doc.body()) break
        }
    }

    override fun getLinkedChapters(doc: Document): ArrayList<LinkedPage> {
        return super.getLinkedChapters(doc.location(), doc.body())
    }
}