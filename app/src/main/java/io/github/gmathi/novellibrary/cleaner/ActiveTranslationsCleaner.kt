package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document

class ActiveTranslationsCleaner : HtmlCleaner() {
    override fun additionalProcessing(doc: Document) {
        // Grab the CSS that contains the rest of chapter text and preserve it.
        val cssChapter = doc.select("div[class*='entry-content']>style").outerHtml()

        removeCSS(doc)

        val contentElement = doc.select("div[class*='entry-content']")

        contentElement.prepend("<h4>${getTitle(doc)}</h4><br>")

        if (!dataCenter.enableDirectionalLinks)
            doc.select("div.nnl_container")?.remove()

        if (!dataCenter.showChapterComments) {
            doc.getElementById("comments")?.remove()
        }

        doc.body().children().remove()
        doc.body().classNames().forEach { doc.body().removeClass(it) }
        doc.body().append(contentElement?.outerHtml())
        doc.body().append(cssChapter)
        // Restore user-select
        // Since a lot of text is based on CSS pseudo elements, text selection is still broken,
        // but at least it is selectable somewhat.
        doc.body().append(
            """
            <style>
                *,*::before,*::after {
                    user-select: initial !important;
                    
                    top: initial!important;
                    bottom: initial!important;
                    left: initial!important;
                    right: initial!important;
                }
            </style>
            """.trimIndent()
        )
    }
}