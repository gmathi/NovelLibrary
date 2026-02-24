package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document


class NeovelCleaner() : HtmlCleaner() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)

        val titleElement = doc.body().select("div.title")
        val title = titleElement.text()
        val contentElement = titleElement.next()
        val commentsElement = doc.body().select("div.footerBody")

        contentElement.forEach { element -> element.children()?.forEach { cleanCSSFromChildren(it) } }
        contentElement.prepend("<h4>${title}</h4><br>")

        doc.body().children().remove()
        doc.body().classNames().forEach { doc.body().removeClass(it) }
        contentElement?.outerHtml()?.let { doc.body().append(it) }
        if (dataCenter.showChapterComments) {
            commentsElement?.outerHtml()?.let { doc.body().append(it) }
        }

    }
}
