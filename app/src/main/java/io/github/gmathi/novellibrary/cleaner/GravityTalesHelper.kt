package io.github.gmathi.novellibrary.cleaner

import org.jsoup.nodes.Document


class GravityTalesHelper : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementById("chapterContent")
        //contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement = contentElement?.parent()
        } while (contentElement?.tagName() != "body")
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        if (isDark) {
            doc.head().append("<style id=\"darkTheme\">" +
                ".container.wrap {background-color:#131313;} body { background-color:#131313; color:rgba(255, 255, 255, 0.8); } </style> ")
        } else {
            doc.head().getElementById("darkTheme")?.remove()
        }

        return doc
    }

}
