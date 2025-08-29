package io.github.gmathi.novellibrary.cleaner

import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.jsoup.nodes.Document


class ScribbleHubCleaner(dataCenter: DataCenter) : HtmlCleaner(dataCenter) {


    override fun additionalProcessing(doc: Document) {
        removeCSS(doc, false)
        var contentElement = doc.selectFirst("div#chp_contents")
        contentElement?.selectFirst("div.nav_chp_fi")?.remove()
        contentElement?.selectFirst("div.ta_c_bm")?.remove()
        contentElement?.prepend("<h4>${getTitle(doc)}</h4><br>")
        do {
            contentElement?.siblingElements()?.remove()
            contentElement?.classNames()?.forEach { contentElement?.removeClass(it) }
            contentElement?.attr("style", "")
            contentElement = contentElement?.parent()
        } while (contentElement != null && contentElement.tagName() != "body")
        contentElement?.classNames()?.forEach { contentElement.removeClass(it) }
    }
}
