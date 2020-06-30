package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class FoxtellerCleaner : HtmlHelper() {
    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)

        val chapter = doc.getElementById("chapter")
        chapter.selectFirst(".toolbar-wrapper").remove()
        chapter.selectFirst(".page-header .toc").remove()
        chapter.selectFirst(".above-content").remove()
        chapter.selectFirst(".page-footer").remove()
        chapter.selectFirst(".page-comments").remove()

        // just unwrap it all. everything.
        var it: Element? = chapter
        while (it != null) {
            it = it.parent()
            if (it == doc.body() || it == null) break
            it.siblingElements()?.remove()
            it.unwrap()
        }
    }
}
