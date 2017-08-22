package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

class TumblrCleaner : HtmlHelper() {

    override fun downloadCSS(doc: Document, downloadDir: File) {
        //super.downloadCSS(doc, downloadDir)
        removeCSS(doc)
    }

    override fun additionalProcessing(doc: Document) {
        doc.getElementsByTag("link")?.remove()
        doc.getElementsByTag("style")?.remove()
        var contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("textpostbody") }
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

    override fun downloadImage(element: Element, dir: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, dir)
    }
}