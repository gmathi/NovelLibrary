package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class WuxiaWorldHelper : HtmlHelper() {


    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        var contentElement = doc.selectFirst("div.p-15")
        contentElement?.selectFirst("div.font-resize")?.remove()
        contentElement?.select("span[style]")?.forEach { it?.removeAttr("style") }

        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement?.attr("style", "")
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }

        if (!dataCenter.enableDirectionalLinks)
            doc.getElementsByTag("a")?.filter { it.text() == "Next Chapter" || it.text() == "Previous Chapter" }?.forEach { it.remove() }
        doc.getElementById("custom-background-css")?.remove()
    }

    override fun downloadImage(element: Element, file: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, file)
    }

    override fun getLinkedChapters(doc: Document): ArrayList<String> {

        val links = ArrayList<String>()
        val otherLinks = doc.selectFirst("div.fr-view")?.getElementsByAttributeValueContaining("href", HostNames.WUXIA_WORLD)
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }

}
