package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File


class GeneralClassTagHelper(private val url: String, private val tagName: String, private val className: String, private val appendTitle: Boolean = true) : HtmlHelper() {

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
        doc.head()?.getElementsByTag("style")?.remove()
        doc.head()?.getElementsByTag("link")?.remove()

        val contentElement = doc.body().select("$tagName.$className")

        //Fix for volarenovels.com
        if (url.contains("volarenovels.com")) {
            val elements = contentElement.select("[id*=announcement]")
            contentElement.removeAll(elements)
        }

        contentElement.forEach { element -> element.children()?.forEach { cleanCSSFromChildren(it) } }
        if (appendTitle)
            contentElement.prepend("<h4>${getTitle(doc)}</h4><br>")

        if (!dataCenter.enableDirectionalLinks)
            removeDirectionalLinks(contentElement)

        doc.getElementsByClass("post-navigation")?.remove()

        if (!dataCenter.showChapterComments) {
            doc.getElementsByClass("comments-container")?.remove()
            doc.getElementsByClass("respond-container")?.remove()
        }



        doc.body().children().remove()
        doc.body().classNames().forEach { doc.body().removeClass(it) }
        doc.body().append(contentElement?.outerHtml())
//        do {
//            contentElement?.siblingElements()?.remove()
//            contentElement = contentElement?.parent()
//            cleanClassAndIds(contentElement)
//        } while (contentElement != null && contentElement.tagName() != "body")
//        contentElement?.getElementsByClass("wpcnt")?.remove()
//        contentElement?.getElementById("jp-post-flair")?.remove()

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

    override fun getLinkedChapters(doc: Document): ArrayList<String> {
        val url = doc.location()
        val links = ArrayList<String>()
        val otherLinks = doc.body().getElementsByTag(tagName).firstOrNull { it.hasClass(className) }?.getElementsByAttributeValueContaining("href", this.url)?.filter { !it.attr("href").contains(url) }
        if (otherLinks != null && otherLinks.isNotEmpty()) {
            otherLinks.mapTo(links) { it.attr("href") }
        }
        return links
    }

    private fun removeDirectionalLinks(contentElement: Elements) {
        contentElement.forEach { element ->
            element.getElementsByTag("a")?.filter {
                it.text().equals("Previous Chapter", ignoreCase = true)
                        || it.text().equals("Next Chapter", ignoreCase = true)
                        || it.text().equals("Project Page", ignoreCase = true)
                        || it.text().equals("Index", ignoreCase = true)
                        || it.text().equals("[Previous Chapter]", ignoreCase = true)
                        || it.text().equals("[Next Chapter]", ignoreCase = true)
                        || it.text().equals("[Table of Contents]", ignoreCase = true)
                        || it.text().equals("Next", ignoreCase = true)
                        || it.text().equals("TOC", ignoreCase = true)
                        || it.text().equals("Previous", ignoreCase = true)

            }?.forEach { it?.remove() }
        }
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }
}
