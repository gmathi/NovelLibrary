package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ChrysanthemumgardenCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {
    override fun additionalProcessing(doc: Document) {
        val body = doc.body() ?: return

        body.getElementById("top-bar")?.remove()
        body.getElementById("site-navigation")?.remove()
        body.getElementById("masthead")?.remove()

        body.getElementById("right-sidebar")?.remove()

        body.getElementsByTag("footer").remove()

        val content = body.getElementById("content") ?: return
        content.getElementsByClass("chrys-iklan").remove()
        content.getElementsByClass("announcement").remove()

        val primary = content.getElementById("primary") ?: return
        val main = primary.getElementById("main") ?: return
        // only remove first navigation
        removeFirstDirectoryLinks(main)
        if (!dataCenter.enableDirectionalLinks) {
            removeFirstDirectoryLinks(main)
        }

        // remove tables of contents
        main.getElementsByClass("toc").remove()
        main.getElementsByClass("post-author").remove()
        main.getElementsByClass("related-novels").remove()
        main.getElementsByClass("fixed-action-btn").forEach {
            val floatingButton = it.getElementsByClass("btn-floating")
            if (floatingButton.isNotEmpty() && floatingButton.hasClass("btn-large")) {
                floatingButton.remove()
            }
        }

        val comments = main.getElementById("comments") ?: return
        if (!dataCenter.showChapterComments) {
            comments.remove()
        } else {
            // Remove "Post comment"
            comments.getElementById("respond")?.remove()
        }
    }

    override fun getTitle(doc: Document): String? {
        return doc.getElementsByClass("chapter-title").first()?.text() ?: super.getTitle(doc)
    }

    private fun removeFirstDirectoryLinks(main: Element) {
        main.getElementsByClass("navigation").firstOrNull {
            it.hasClass("post-navigation")
        }?.remove()
    }
}