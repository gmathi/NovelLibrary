package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class GeneralIdTagHelper(val tagName: String, val id: String) : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        var contentElement = doc.body().getElementsByTag(tagName).firstOrNull { it.id() == id }
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        cleanCSSFromChildren(contentElement)
        do {
            contentElement?.siblingElements()?.remove()
            cleanClassAndIds(contentElement)
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        cleanClassAndIds(contentElement)
        contentElement?.getElementsByClass("wpcnt")?.remove()
        contentElement?.getElementById("jp-post-flair")?.remove()
    }

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }

    override fun removeJS(doc: Document) {
        super.removeJS(doc)
        doc.getElementsByTag("noscript").remove()
    }

    override fun downloadCSS(doc: Document, downloadDir: File) {
        removeCSS(doc)
    }
}