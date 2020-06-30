package io.github.gmathi.novellibrary.cleaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class WattpadHelper : HtmlHelper() {
    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)

        // Get second half URL
        val urlRegex = """^[ \n\t]*window\.prefetched *= *\{".+?":\{"data":\{.*?"text_url":\{"text":"([^"]+?)"""".toRegex()
        val url = doc.getElementsByTag("script").mapNotNull {
            val finds = urlRegex.find(it.data())
            if (finds == null) null
            else finds.groups[1]?.value
        }.firstOrNull()
        super.removeJS(doc)

        // Get important elements
        val titleElem = doc.selectFirst("header h2")!!
        val articleElem = doc.selectFirst("main article")!!
        val preElem = doc.selectFirst("div.page pre")!!
        val contentElem = preElem.parent()

        // remove most intrusions
        titleElem.siblingElements().remove() // metadata after title
        preElem.select("span.comment-marker").remove() // comments??
        preElem.unwrap() // move contents of pre to its parent (pre is monospace with no wrapping)
        articleElem.children().filter { !it.hasClass("container") }.forEach { it.remove() } // novel info and next page or something

        // remove ALL other unneeded elements (the nuclear option)
        var it: Element? = articleElem
        while (it != null) {
            val parent = it.parent()
            it.siblingElements()?.remove()
            it.unwrap(); it = parent
            if (it == doc.body()) break
        }

        // Request and append second half
        if (url != null)
            contentElem.append(runBlocking { withContext(Dispatchers.IO) {
                    Jsoup.connect(url).execute().body()
            } } )
        else
            contentElem.append("<br /><p><b>ERROR: Failed to load second half of chapter.</b></p>")
    }

    override fun removeJS(doc: Document) {
        // may not be the best option but code in additionalProcessing needs the script tags
    }
}
