package io.github.gmathi.novellibrary.cleaner

import android.net.Uri
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.network.HostNames
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File


class ScribbleHubHelper : HtmlHelper() {


    override fun additionalProcessing(doc: Document) {
        removeCSS(doc)
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

    override fun downloadImage(element: Element, file: File): File? {
        val uri = Uri.parse(element.attr("src"))
        return if (uri.toString().contains("uploads/avatars")) null
        else super.downloadImage(element, file)
    }

    override fun toggleTheme(isDark: Boolean, doc: Document): Document {
        return super.toggleThemeDefault(isDark, doc)
    }

}
