package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File
import java.util.regex.Pattern


class QidianCleaner : HtmlHelper() {

    //This value is set in `removeJS()` which gets called in `WebPageDBFragment.kt : Line 354`
    private var continueReadingUrl: String? = null

    override fun additionalProcessing(doc: Document) {
        removeCSS(doc, false)
        if (continueReadingUrl != null) {
            doc.body().children().remove()
            doc.body().classNames()?.forEach { doc.body().removeClass(it) }
            doc.body().append("<p/><p/> Download the novel to by pass this publisher page. <p/> <a id=\"continueReadingLink\" href=\"https://$continueReadingUrl\">Go To Chapter</a>")
            return
        }

        var contentElement = doc.body().getElementById("continueReadingLink")
        if (contentElement == null) {
            contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("cha-words") }
            if (contentElement == null)
                contentElement = doc.body().getElementsByTag("div").firstOrNull { it.hasClass("cha-txt") }
            contentElement?.prepend(doc.body().getElementsByTag("div").firstOrNull { it.hasClass("cha-tit") }?.outerHtml() ?: "")
        }
        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }
    }

    override fun removeJS(doc: Document) {
        //ex: g_data.url = '//www.webnovel.com/book/7141795406000005/21895371038980697';
        val p = Pattern.compile("g_data.url\\s=\\s'//(.*?)';", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
        try {
            val m = p.matcher(doc.toString())
            if (m.find()) {
                continueReadingUrl = m.group(1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.removeJS(doc)
    }

//    override fun getLinkedChapters(doc: Document): ArrayList<String> {
//        val p = Pattern.compile("g_data.url\\s=\\s'//(.*?)';", Pattern.DOTALL or Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE or Pattern.MULTILINE) // Regex for the value of the key
//        try {
//            val m = p.matcher(doc.toString())
//            if (m.find()) {
//                continueReadingUrl = m.group(1)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        val links = ArrayList<String>()
//        val otherLink = doc.getElementById("continueReadingLink")?.attr("href")
//        if (otherLink != null)
//            links.add("https://www.webnovel.com" + otherLink)
//        if (continueReadingUrl != null) {
//            links.add("https://$continueReadingUrl")
//        }
//        return links
//    }
}
