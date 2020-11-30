package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File


class NeovelHelper() : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.head()?.getElementsByTag("style")?.remove()
        doc.head()?.getElementsByTag("link")?.remove()

        val titleElement = doc.body().select("div.title")
        val title = titleElement.text()
        val contentElement = titleElement.next()
        val commentsElement = doc.body().select("div.footerBody")

        contentElement.forEach { element -> element.children()?.forEach { cleanCSSFromChildren(it) } }
        contentElement.prepend("<h4>${title}</h4><br>")

        doc.body().children().remove()
        doc.body().classNames().forEach { doc.body().removeClass(it) }
        doc.body().append(contentElement?.outerHtml())
        if (dataCenter.showChapterComments) {
            doc.body().append(commentsElement?.outerHtml())
        }
        doc.getElementById("custom-background-css")?.remove()

    }

    override fun downloadImage(element: Element, file: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, file)
    }

    override fun removeJS(doc: Document) {
        super.removeJS(doc)
        doc.getElementsByTag("noscript").remove()
    }
}
